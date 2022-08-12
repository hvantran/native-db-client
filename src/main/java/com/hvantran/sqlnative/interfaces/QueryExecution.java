package com.hvantran.sqlnative.interfaces;

import com.hvantran.sqlnative.repository.proxy.QueryInfo;
import com.hvantran.sqlnative.utils.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface QueryExecution {

    <T> List<T> execute(QueryInfo queryInfo, Connection connection, Class<T> klass) throws SQLException;

    int execute(QueryInfo queryInfo, Connection connection) throws SQLException;

    String generateQueryString(QueryInfo queryInfo);

    void validateQueryInfo(QueryInfo queryInfo);

}
