package com.moxa.dream.system.core.session;

import com.moxa.dream.system.config.Configuration;
import com.moxa.dream.system.mapped.MethodInfo;

import java.io.Closeable;

public interface Session extends Closeable {

    <T> T getMapper(Class<T> type);

    Object execute(MethodInfo methodInfo, Object arg);

    boolean isAutoCommit();

    void commit();

    void rollback();

    void close();

    Configuration getConfiguration();
}

