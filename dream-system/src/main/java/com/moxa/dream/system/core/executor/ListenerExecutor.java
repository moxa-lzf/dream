package com.moxa.dream.system.core.executor;

import com.moxa.dream.system.config.MappedStatement;
import com.moxa.dream.system.core.listener.*;
import com.moxa.dream.system.core.listener.factory.ListenerFactory;
import com.moxa.dream.system.core.session.SessionFactory;
import com.moxa.dream.util.common.ObjectUtil;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ListenerExecutor implements Executor {
    protected ListenerFactory listenerFactory;
    protected Executor nextExecutor;

    public ListenerExecutor(Executor nextExecutor, ListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
        this.nextExecutor = nextExecutor;
    }

    @Override
    public Object query(MappedStatement mappedStatement) throws SQLException {
        QueryListener[] queryListeners = null;
        if (listenerFactory != null) {
            queryListeners = listenerFactory.getQueryListener();
        }
        return execute(mappedStatement, queryListeners, (ms) -> nextExecutor.query(mappedStatement));
    }

    @Override
    public Object update(MappedStatement mappedStatement) throws SQLException {
        UpdateListener[] updateListeners = null;
        if (listenerFactory != null) {
            updateListeners = listenerFactory.getUpdateListener();
        }
        return execute(mappedStatement, updateListeners, (ms) -> nextExecutor.update(mappedStatement));
    }

    @Override
    public Object insert(MappedStatement mappedStatement) throws SQLException {
        InsertListener[] insertListeners = null;
        if (listenerFactory != null) {
            insertListeners = listenerFactory.getInsertListener();
        }
        return execute(mappedStatement, insertListeners, (ms) -> nextExecutor.insert(mappedStatement));
    }

    @Override
    public Object delete(MappedStatement mappedStatement) throws SQLException {
        DeleteListener[] deleteListeners = null;
        if (listenerFactory != null) {
            deleteListeners = listenerFactory.getDeleteListener();
        }
        return execute(mappedStatement, deleteListeners, (ms) -> nextExecutor.delete(mappedStatement));
    }

    protected Object execute(MappedStatement mappedStatement, Listener[] listeners, Function<MappedStatement, Object> function) throws SQLException {
        if (!ObjectUtil.isNull(listeners)) {
            if (beforeListeners(listeners, mappedStatement)) {
                Object result;
                try {
                    result = function.apply(mappedStatement);
                } catch (Exception e) {
                    exceptionListeners(listeners, e, mappedStatement);
                    throw e;
                }
                afterReturnListeners(listeners, result, mappedStatement);
                return result;
            } else {
                return null;
            }
        } else {
            return function.apply(mappedStatement);
        }
    }

    @Override
    public Object batch(List<MappedStatement> mappedStatements) throws SQLException {
        return nextExecutor.batch(mappedStatements);
    }

    @Override
    public boolean isAutoCommit() {
        return nextExecutor.isAutoCommit();
    }

    @Override
    public void commit() {
        nextExecutor.commit();
    }

    @Override
    public void rollback() {
        nextExecutor.rollback();
    }

    @Override
    public void close() {
        nextExecutor.close();
    }

    @Override
    public SessionFactory getSessionFactory() {
        return nextExecutor.getSessionFactory();
    }

    protected boolean beforeListeners(Listener[] listeners, MappedStatement mappedStatement) {
        boolean success = true;
        for (Listener listener : listeners) {
            success = success & listener.before(mappedStatement);
        }
        return success;
    }

    protected void afterReturnListeners(Listener[] listeners, Object result, MappedStatement mappedStatement) {
        for (Listener listener : listeners) {
            listener.afterReturn(result, mappedStatement);
        }
    }

    protected void exceptionListeners(Listener[] listeners, Exception e, MappedStatement mappedStatement) {
        for (Listener listener : listeners) {
            listener.exception(e, mappedStatement);
        }
    }

}
