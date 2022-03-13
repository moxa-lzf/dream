package com.moxa.dream.module.core.executor;

import com.moxa.dream.module.config.Configuration;
import com.moxa.dream.module.core.statementhandler.BatchStatementHandler;
import com.moxa.dream.module.core.statementhandler.StatementHandler;

public class BatchExecutor extends AbstractExecutor {
    private Executor executor;

    public BatchExecutor(Executor executor, Configuration configuration, boolean autoCommit) {
        super(configuration, autoCommit);
        this.executor = executor;
    }

    @Override
    protected StatementHandler createStatementHandler() {
        return new BatchStatementHandler(executor.getStatementHandler());
    }

    @Override
    public void commit() {
        try {
            flushStatement(true);
        } finally {
            super.commit();
        }
    }

    @Override
    public void rollback() {
        try {
            flushStatement(false);
        } finally {
            super.rollback();
        }
    }

    @Override
    public void close() {
        try {
            flushStatement(false);
        } finally {
            super.close();
        }
    }

    public int[] flushStatement(boolean rollback) {
        return statementHandler.flushStatement(rollback);
    }
}
