package com.hvantran.sqlnative.repository.proxy;

import com.hvantran.sqlnative.annotations.*;
import com.hvantran.sqlnative.interfaces.AppException;
import com.hvantran.sqlnative.utils.Pair;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@Builder
@Getter
@ToString
public class QueryInfo {
    private Select select;

    private From from;

    private Where where;

    private OrderBy orderBy;

    private Insert insert;

    private Values values;

    private Update update;

    private Set set;

    private Delete delete;

    private NativeQuery nativeQuery;

    private List<Pair<Param, Object>> paramPairs;

    private Class<?> mappingToClass;

    public QuerySelection getQuerySelection() {
        if (Objects.nonNull(this.nativeQuery)) {
            QuerySelection querySelection = getQuerySelectionFromNativeQuery();
            if (querySelection != null) return querySelection;
        }
        if (Objects.nonNull(this.delete)) {
            return QuerySelection.DELETE;
        }
        if (Objects.nonNull(this.insert)) {
            return QuerySelection.INSERT;
        }
        if (Objects.nonNull(this.update)) {
            return QuerySelection.UPDATE;
        }
        if (Objects.nonNull(this.select)) {
            return QuerySelection.SELECT;
        }
        throw new AppException("Unsupported query. Only accept SELECT/UPDATE/INSERT/DELETE");
    }

    private QuerySelection getQuerySelectionFromNativeQuery() {
        String nativeQueryString = this.nativeQuery.value().trim();
        if (nativeQueryString.toLowerCase().startsWith("select")) {
            return QuerySelection.SELECT;
        }
        if (nativeQueryString.toLowerCase().startsWith("update")) {
            return QuerySelection.UPDATE;
        }
        if (nativeQueryString.toLowerCase().startsWith("insert")) {
            return QuerySelection.INSERT;
        }
        if (nativeQueryString.toLowerCase().startsWith("delete")) {
            return QuerySelection.DELETE;
        }
        return null;
    }

}
