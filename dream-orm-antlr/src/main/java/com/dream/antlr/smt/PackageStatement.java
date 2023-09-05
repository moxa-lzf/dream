package com.dream.antlr.smt;

import java.util.HashMap;
import java.util.Map;

public class PackageStatement extends Statement {
    private Map<Class, Object> infoMap = new HashMap<>(4);
    private Statement statement;

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = wrapParent(statement);
    }

    public <T> void setValue(Class<T> type, T value) {
        infoMap.put(type, value);
    }

    public <T> T getValue(Class<T> type) {
        return (T) infoMap.get(type);
    }

    @Override
    protected Boolean isNeedInnerCache() {
        return isNeedInnerCache(statement);
    }

    @Override
    public PackageStatement clone() {
        PackageStatement packageStatement = (PackageStatement) super.clone();
        packageStatement.infoMap = new HashMap<>(4);
        packageStatement.infoMap.putAll(infoMap);
        packageStatement.statement = clone(statement);
        return packageStatement;
    }
}
