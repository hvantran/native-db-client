package com.hvantran.sqlnative.repository.proxy;

import com.hvantran.sqlnative.annotations.*;
import com.hvantran.sqlnative.interfaces.CheckedSupplier;
import com.hvantran.sqlnative.interfaces.QueryExecution;
import com.hvantran.sqlnative.utils.ObjectUtils;
import com.hvantran.sqlnative.utils.Pair;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public enum QuerySelection implements QueryExecution {
    SELECT {
        @Override
        public String generateQueryString(QueryInfo queryInfo) {
            LOGGER.info("Generating SELECT query string");
            String baseQuery = SELECT_QUERY_FORMAT.formatted(queryInfo.getSelect().value(), queryInfo.getFrom().value());
            StringBuilder queryBuilder = new StringBuilder(baseQuery);
            checkThenAppendString(queryInfo.getWhere() != null, queryBuilder, WHERE_FUNCTION, queryInfo);
            checkThenAppendString(queryInfo.getOrderBy() != null, queryBuilder, ORDER_BY_FUNCTION, queryInfo);
            return queryBuilder.append(";").toString();
        }

        @Override
        public void validateQueryInfo(QueryInfo queryInfo) {
            LOGGER.info("Validate SELECT query info {}", queryInfo);
            Select select = queryInfo.getSelect();
            NativeQuery nativeQuery = queryInfo.getNativeQuery();
            boolean isNotNativeQuery = nativeQuery == null;
            ObjectUtils.checkThenThrow(isNotNativeQuery &&  (select == null || StringUtils.isEmpty(select.value())),
                    "SELECT clause cannot be NULL/Empty");
            From from = queryInfo.getFrom();
            ObjectUtils.checkThenThrow(isNotNativeQuery && (from == null || StringUtils.isEmpty(from.value())),
                    "FROM clause cannot be NULL/Empty in SELECT query");
        }
    },
    INSERT {

        @Override
        public String generateQueryString(QueryInfo queryInfo) {
            LOGGER.info("Generating INSERT query string");
            String baseQuery = INSERT_QUERY_FORMAT.formatted(queryInfo.getInsert().value(), queryInfo.getValues().value());
            StringBuilder queryBuilder = new StringBuilder(baseQuery);
            return queryBuilder.append(";").toString();
        }

        @Override
        public void validateQueryInfo(QueryInfo queryInfo) {
            LOGGER.info("Validate INSERT query {}", queryInfo);
            Insert insert = queryInfo.getInsert();
            NativeQuery nativeQuery = queryInfo.getNativeQuery();
            boolean isNotNativeQuery = queryInfo.getNativeQuery() == null;
            ObjectUtils.checkThenThrow(isNotNativeQuery && (insert == null || StringUtils.isEmpty(insert.value())),
                    "INSERT clause cannot be NULL/Empty");
            Values values = queryInfo.getValues();
            ObjectUtils.checkThenThrow(isNotNativeQuery && (values == null || StringUtils.isEmpty(values.value())),
                    "VALUES clause cannot be NULL/Empty in INSERT query");
        }
    },
    UPDATE {
        @Override
        public String generateQueryString(QueryInfo queryInfo) {
            LOGGER.info("Generating UPDATE query string");
            String baseQuery = UPDATE_QUERY_FORMAT.formatted(queryInfo.getUpdate().value(), queryInfo.getSet().value());
            StringBuilder queryBuilder = new StringBuilder(baseQuery);
            checkThenAppendString(queryInfo.getWhere() != null, queryBuilder, WHERE_FUNCTION, queryInfo);
            return queryBuilder.append(";").toString();
        }

        @Override
        public void validateQueryInfo(QueryInfo queryInfo) {
            LOGGER.info("Validate UPDATE query {}", queryInfo);
            Update update = queryInfo.getUpdate();
            boolean isNotNativeQuery = queryInfo.getNativeQuery() == null;
            ObjectUtils.checkThenThrow(isNotNativeQuery && (update == null || StringUtils.isEmpty(update.value())),
                    "UPDATE clause cannot be NULL/Empty");
            Set set = queryInfo.getSet();
            ObjectUtils.checkThenThrow(isNotNativeQuery && (set == null || StringUtils.isEmpty(set.value())),
                    "SET clause cannot be NULL/Empty in UPDATE query");
        }
    },
    DELETE {
        @Override
        public String generateQueryString(QueryInfo queryInfo) {
            LOGGER.info("Generating DELETE query string");
            String baseQuery = DELETE_QUERY_FORMAT.formatted(queryInfo.getDelete().value());
            StringBuilder queryBuilder = new StringBuilder(baseQuery);
            checkThenAppendString(queryInfo.getWhere() != null, queryBuilder, WHERE_FUNCTION, queryInfo);
            return queryBuilder.append(";").toString();
        }

        @Override
        public void validateQueryInfo(QueryInfo queryInfo) {
            LOGGER.info("Validate DELETE query {}", queryInfo);
            Delete delete = queryInfo.getDelete();
            boolean isNotNativeQuery = queryInfo.getNativeQuery() == null;
            ObjectUtils.checkThenThrow(isNotNativeQuery && (delete == null || StringUtils.isEmpty(delete.value())),
                    "DELETE FROM clause cannot be NULL/Empty");
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(QuerySelection.class);

    private static final String SELECT_QUERY_FORMAT = "SELECT %s FROM %s";
    private static final String INSERT_QUERY_FORMAT = "INSERT INTO %s VALUES %s";
    private static final String UPDATE_QUERY_FORMAT = "UPDATE %s SET %s";
    private static final String DELETE_QUERY_FORMAT = "DELETE FROM %s";
    private static final Function<QueryInfo,String> WHERE_FUNCTION = (queryInfo) -> " WHERE " + queryInfo.getWhere().value();
    private static final Function<QueryInfo,String> ORDER_BY_FUNCTION = (queryInfo) -> " ORDER BY " + queryInfo.getOrderBy().value();
    private static final Predicate<NativeQuery> NOT_NATIVE_QUERY = Objects::isNull;

    protected StringBuilder checkThenAppendString(boolean test, StringBuilder stringBuilder, Function<QueryInfo, String> appendStringSup, QueryInfo queryInfo) {
        if (test) {
            stringBuilder.append(appendStringSup.apply(queryInfo));
        }
        return stringBuilder;
    }

    protected String replaceParamPlaceholders(String rawQuery, List<Pair<Param, Object>> queryParameters) {
        for (Pair<Param, Object> queryParameter: queryParameters){
            String paramName = queryParameter.getKey().value();
            Object valueObject = queryParameter.getValue();
            rawQuery = replaceParamPlaceholder(rawQuery, paramName, valueObject);
        }
        return rawQuery;
    }

    private String replaceParamPlaceholder(String inputString, String paramName, Object param) {
        return inputString.replace(String.format("{%s}", paramName), String.valueOf(param));
    }

    @Override
    public Pair<ResultSet, PreparedStatement> execute(QueryInfo queryInfo, Connection connection) {
        validateQueryInfo(queryInfo);
        String rawQueryString = getRawQueryString(queryInfo);
        String queryString = replaceParamPlaceholders(rawQueryString, queryInfo.getParamPairs());
        LOGGER.info("Executing query: {}", queryString);
        CheckedSupplier<PreparedStatement> preparedStatementCheckedSupplier = () -> connection.prepareStatement(queryString);
        PreparedStatement preparedStatement = preparedStatementCheckedSupplier.get();
        CheckedSupplier<ResultSet> resultSetCheckedSupplier = () -> {
            preparedStatement.execute();
            return preparedStatement.getResultSet();
        };
        return Pair.of(resultSetCheckedSupplier.get(), preparedStatement);
    }

    private String getRawQueryString(QueryInfo queryInfo) {
        if (queryInfo.getNativeQuery() != null) {
            return queryInfo.getNativeQuery().value();
        }
        return generateQueryString(queryInfo);
    }
}
