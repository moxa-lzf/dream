package com.moxa.dream.system.core.statementhandler;

import com.moxa.dream.system.config.MappedParam;
import com.moxa.dream.system.config.MappedStatement;
import com.moxa.dream.util.common.ObjectUtil;

import java.sql.*;
import java.util.List;

public class PrepareStatementHandler implements StatementHandler<PreparedStatement> {

    @Override
    public PreparedStatement prepare(Connection connection, MappedStatement mappedStatement) throws SQLException {
        return connection.prepareStatement(mappedStatement.getSql(), mappedStatement.getColumnNames());
    }

    protected void doParameter(PreparedStatement statement,MappedStatement mappedStatement) throws SQLException {
        List<MappedParam> mappedParamList = mappedStatement.getMappedParamList();
        if (!ObjectUtil.isNull(mappedParamList)) {
            for (int i = 0; i < mappedParamList.size(); i++) {
                MappedParam mappedParam = mappedParamList.get(i);
                mappedParam.setParam(statement, i + 1);
            }
        }
    }

    protected void doTimeOut(PreparedStatement statement,MappedStatement mappedStatement) throws SQLException {
        int timeOut = mappedStatement.getTimeOut();
        if (timeOut != 0) {
            statement.setQueryTimeout(timeOut);
        }
    }

    @Override
    public ResultSet executeQuery(PreparedStatement statement,MappedStatement mappedStatement) throws SQLException {
        doParameter(statement,mappedStatement);
        doTimeOut(statement,mappedStatement);
        return statement.executeQuery();
    }

    @Override
    public Object executeUpdate(PreparedStatement statement,MappedStatement mappedStatement) throws SQLException {
        doParameter(statement,mappedStatement);
        return statement.executeUpdate();
    }

    @Override
    public Object executeBatch(PreparedStatement statement,List<MappedStatement> mappedStatements) throws SQLException {
        for (MappedStatement mappedStatement : mappedStatements) {
            doParameter(statement,mappedStatement);
            statement.addBatch();
        }
        int[] result = statement.executeBatch();
        statement.clearBatch();
        return result;
    }
}
