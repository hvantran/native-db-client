package com.hvantran.sqlnative.interfaces;

import com.hvantran.sqlnative.repository.proxy.QueryInfo;
import com.hvantran.sqlnative.utils.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface QueryExecution {

    Pair<ResultSet, PreparedStatement> execute(QueryInfo queryBuilder, Connection statement);

    String generateQueryString(QueryInfo queryInfo);

    void validateQueryInfo(QueryInfo queryInfo);

}
