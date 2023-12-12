package com.dream.template.mapper;

import com.dream.antlr.invoker.Invoker;
import com.dream.antlr.util.AntlrUtil;
import com.dream.system.antlr.invoker.ForEachInvoker;
import com.dream.system.antlr.invoker.MarkInvoker;
import com.dream.system.config.Configuration;
import com.dream.system.config.MappedStatement;
import com.dream.system.config.MethodInfo;
import com.dream.system.core.session.Session;
import com.dream.system.dialect.DialectFactory;
import com.dream.system.table.ColumnInfo;
import com.dream.system.table.TableInfo;
import com.dream.system.table.factory.TableFactory;
import com.dream.system.util.SystemUtil;
import com.dream.util.common.ObjectMap;
import com.dream.util.common.ObjectUtil;
import com.dream.util.exception.DreamRunTimeException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class AbstractMapper {
    public static final String DREAM_TEMPLATE_PARAM = "dream_template_param";
    protected Session session;
    protected Map<String, MethodInfo> methodInfoMap = new HashMap<>(4);
    private DialectFactory dialectFactory;

    public AbstractMapper(Session session) {
        this.session = session;
        this.dialectFactory = session.getConfiguration().getDialectFactory();
    }

    public Object execute(Class<?> type, Object arg) {
        String key = getKey(type, arg);
        MethodInfo methodInfo = methodInfoMap.get(key);
        if (methodInfo == null) {
            synchronized (this) {
                methodInfo = methodInfoMap.get(key);
                if (methodInfo == null) {
                    String table = getTableName(type);
                    if (ObjectUtil.isNull(table)) {
                        throw new DreamRunTimeException(type.getName() + "未绑定表");
                    }
                    Configuration configuration = this.session.getConfiguration();
                    TableFactory tableFactory = configuration.getTableFactory();
                    TableInfo tableInfo = tableFactory.getTableInfo(table);
                    if (tableInfo == null) {
                        throw new DreamRunTimeException("表'" + table + "'未在TableFactory注册");
                    }
                    methodInfo = getMethodInfo(configuration, tableInfo, type, arg);
                    String id = getId();
                    if (!ObjectUtil.isNull(id)) {
                        methodInfo.setId(id);
                    }
                    methodInfoMap.put(key, methodInfo);
                }
            }
        }
        return execute(methodInfo, arg);
    }

    protected String getKey(Class<?> type, Object arg) {
        return arg == null ? type.getName() : type.getName() + ":" + arg.getClass().getName();
    }

    protected Object execute(MethodInfo methodInfo, Object arg) {
        MappedStatement mappedStatement;
        try {
            mappedStatement = dialectFactory.compile(methodInfo, wrapArg(arg));
        } catch (Exception e) {
            throw new DreamRunTimeException(e);
        }
        return execute(mappedStatement);
    }

    protected Object execute(MappedStatement mappedStatement) {
        return session.execute(mappedStatement);
    }

    protected Map<String, Object> wrapArg(Object arg) {
        if (arg != null) {
            if (arg instanceof Map) {
                return (Map<String, Object>) arg;
            } else {
                return new ObjectMap(arg);
            }
        } else {
            return new HashMap<>(4);
        }
    }

    protected abstract MethodInfo getMethodInfo(Configuration configuration, TableInfo tableInfo, Class type, Object arg);

    protected String getTableName(Class<?> type) {
        return SystemUtil.getTableName(type);
    }

    protected String getIdWhere(TableInfo tableInfo) {
        List<ColumnInfo> primKeys = tableInfo.getPrimKeys();
        if (primKeys == null || primKeys.isEmpty()) {
            throw new DreamRunTimeException("表'" + tableInfo.getTable() + "'未注册主键");
        }
        if (primKeys.size() > 1) {
            throw new DreamRunTimeException("表'" + tableInfo.getTable() + "'存在多个主键");
        }
        ColumnInfo columnInfo = primKeys.get(0);
        return "where " + tableInfo.getTable() + "." + columnInfo.getColumn() + "=" + AntlrUtil.invokerSQL(MarkInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE, columnInfo.getName());
    }

    protected String getIdsWhere(TableInfo tableInfo) {
        List<ColumnInfo> primKeys = tableInfo.getPrimKeys();
        if (primKeys == null || primKeys.isEmpty()) {
            throw new DreamRunTimeException("表'" + tableInfo.getTable() + "'未注册主键");
        }
        if (primKeys.size() > 1) {
            throw new DreamRunTimeException("表'" + tableInfo.getTable() + "'存在多个主键");
        }
        ColumnInfo columnInfo = primKeys.get(0);
        return "where " + tableInfo.getTable() + "." + columnInfo.getColumn() + " in(" + AntlrUtil.invokerSQL(ForEachInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE, DREAM_TEMPLATE_PARAM) + ")";
    }

    protected String getId() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String packageName = this.getClass().getPackage().getName();
        for (int i = 1; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();
            if (!className.startsWith(packageName)) {
                return className + "." + stackTrace[i].getMethodName();
            }
        }
        return null;
    }
}
