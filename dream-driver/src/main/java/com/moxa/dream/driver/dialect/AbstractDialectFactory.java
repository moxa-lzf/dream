package com.moxa.dream.driver.dialect;

import com.moxa.dream.antlr.config.Assist;
import com.moxa.dream.antlr.exception.InvokerException;
import com.moxa.dream.antlr.expr.PackageExpr;
import com.moxa.dream.antlr.expr.SqlExpr;
import com.moxa.dream.antlr.factory.AntlrInvokerFactory;
import com.moxa.dream.antlr.factory.InvokerFactory;
import com.moxa.dream.antlr.factory.MyFunctionFactory;
import com.moxa.dream.antlr.invoker.$Invoker;
import com.moxa.dream.antlr.invoker.ScanInvoker;
import com.moxa.dream.antlr.read.ExprReader;
import com.moxa.dream.antlr.smt.PackageStatement;
import com.moxa.dream.antlr.sql.*;
import com.moxa.dream.driver.antlr.factory.DriverInvokerFactory;
import com.moxa.dream.driver.antlr.wrapper.AnnotationWrapper;
import com.moxa.dream.driver.antlr.wrapper.ScanWrapper;
import com.moxa.dream.driver.antlr.wrapper.Wrapper;
import com.moxa.dream.driver.page.wrapper.PageWrapper;
import com.moxa.dream.system.cache.CacheKey;
import com.moxa.dream.system.config.Configuration;
import com.moxa.dream.system.dialect.DialectFactory;
import com.moxa.dream.system.mapped.MappedParam;
import com.moxa.dream.system.mapped.MappedSql;
import com.moxa.dream.system.mapped.MappedStatement;
import com.moxa.dream.system.mapper.MethodInfo;
import com.moxa.dream.system.table.ColumnInfo;
import com.moxa.dream.system.table.TableInfo;
import com.moxa.dream.system.table.factory.TableFactory;
import com.moxa.dream.system.typehandler.factory.TypeHandlerFactory;
import com.moxa.dream.system.typehandler.handler.TypeHandler;
import com.moxa.dream.util.common.ObjectUtil;
import com.moxa.dream.util.common.ObjectWrapper;
import com.moxa.dream.util.exception.DreamRunTimeException;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractDialectFactory implements DialectFactory {
    private int split;
    private ToSQL toSQL;

    public AbstractDialectFactory() {
        this(5);
    }

    public AbstractDialectFactory(int split) {
        this.split = split;
        if (split <= 0) {
            throw new DreamRunTimeException("SQL分割次数必须大于0");
        }
        toSQL = getToSQL();
    }

    @Override
    public PackageStatement compile(MethodInfo methodInfo) {
        String sql = methodInfo.getSql();
        ExprReader exprReader = new ExprReader(sql, getMyFunctionFactory());
        SqlExpr sqlExpr = new PackageExpr(exprReader);
        PackageStatement statement = (PackageStatement) sqlExpr.expr();
        return statement;
    }

    @Override
    public MappedStatement compile(MethodInfo methodInfo, Object arg) {
        PackageStatement statement = getStatement(methodInfo);
        ScanInvoker.ScanInfo scanInfo = statement.getValue(ScanInvoker.ScanInfo.class);
        List<MappedParam> mappedParamList = null;
        List<$Invoker.ParamInfo> paramInfoList = null;
        String sql = null;
        if (scanInfo != null) {
            paramInfoList = scanInfo.getParamInfoList();
            if (paramInfoList != null) {
                sql = scanInfo.getSql();
                if (!ObjectUtil.isNull(paramInfoList)) {
                    ObjectWrapper paramWrapper = ObjectWrapper.wrapper(arg);
                    for ($Invoker.ParamInfo paramInfo : paramInfoList) {
                        paramInfo.setParamValue(paramWrapper.get(paramInfo.getParamName()));
                    }
                }
            }
        }
        if (ObjectUtil.isNull(sql)) {
            Assist assist = getAssist(methodInfo, arg);
            try {
                sql = toSQL.toStr(statement, assist, null);
            } catch (InvokerException e) {
                throw new RuntimeException(e);
            }
            if (scanInfo == null) {
                scanInfo = statement.getValue(ScanInvoker.ScanInfo.class);
            }
            $Invoker invoker = ($Invoker) assist.getInvoker(AntlrInvokerFactory.NAMESPACE, AntlrInvokerFactory.$);
            if (invoker != null) {
                paramInfoList = invoker.getParamInfoList();
            } else {
                paramInfoList = new ArrayList<>();
                scanInfo.setParamInfoList(paramInfoList);
            }
        }
        if (!ObjectUtil.isNull(paramInfoList)) {
            mappedParamList = new ArrayList<>();
            ParamTypeMap paramTypeMap = methodInfo.get(ParamTypeMap.class);
            if (paramTypeMap == null) {
                synchronized (this) {
                    paramTypeMap = methodInfo.get(ParamTypeMap.class);
                    if (paramTypeMap == null) {
                        paramTypeMap = new ParamTypeMap();
                        methodInfo.set(ParamTypeMap.class, paramTypeMap);
                    }
                }
            }
            Configuration configuration = methodInfo.getConfiguration();
            Map<String, ScanInvoker.ParamScanInfo> paramScanInfoMap = scanInfo.getParamScanInfoMap();
            for ($Invoker.ParamInfo paramInfo : paramInfoList) {
                ParamType paramType = paramTypeMap.get(paramInfo.getParamName());
                if (paramType == null) {
                    paramType = getParamType(configuration, scanInfo, paramScanInfoMap, paramInfo);
                    paramTypeMap.put(paramInfo.getParamName(), paramType);
                }
                mappedParamList.add(getMappedParam(paramType.getColumnInfo(), paramInfo.getParamValue(), paramType.getTypeHandler()));
            }
        }
        CacheKey uniqueKey = methodInfo.getSqlKey();
        if (!ObjectUtil.isNull(mappedParamList)) {
            uniqueKey.update(mappedParamList.stream()
                    .map(mappedParam -> mappedParam.getParamValue())
                    .collect(Collectors.toList())
                    .toArray());
        }
        return new MappedStatement
                .Builder()
                .methodInfo(methodInfo)
                .mappedSql(new MappedSql(scanInfo.getCommand(), sql, scanInfo.getTableScanInfoMap()))
                .mappedParamList(mappedParamList)
                .arg(arg)
                .uniqueKey(uniqueKey)
                .build();
    }

    protected PackageStatement getStatement(MethodInfo methodInfo) {
        PackageStatement statement = methodInfo.getStatement();
        if (statement == null) {
            synchronized (this) {
                statement = methodInfo.getStatement();
                if (statement == null) {
                    methodInfo.compile();
                }
                statement = methodInfo.getStatement();
            }
        }
        return statement;
    }

    protected Assist getAssist(MethodInfo methodInfo, Object arg) {
        Map<Class, Object> customMap = new HashMap<>();
        customMap.put(MethodInfo.class, methodInfo);
        if (arg == null) {
            arg = new HashMap<>();
        }
        customMap.put(ObjectWrapper.class, ObjectWrapper.wrapper(arg));
        List<InvokerFactory> invokerFactoryList = getDefaultInvokerFactoryList();
        return new Assist(invokerFactoryList, customMap);
    }

    protected List<InvokerFactory> getDefaultInvokerFactoryList() {
        List<InvokerFactory> invokerFactoryList = new ArrayList<>();
        invokerFactoryList.addAll(Arrays.asList(new AntlrInvokerFactory(), new DriverInvokerFactory()));
        return invokerFactoryList;
    }

    protected ParamType getParamType(Configuration configuration, ScanInvoker.ScanInfo scanInfo, Map<String, ScanInvoker.ParamScanInfo> paramScanInfoMap, $Invoker.ParamInfo paramInfo) {
        TypeHandlerFactory typeHandlerFactory = configuration.getTypeHandlerFactory();
        TableFactory tableFactory = configuration.getTableFactory();
        ScanInvoker.ParamScanInfo paramScanInfo = paramScanInfoMap.get(paramInfo.getParamName());
        Object value = paramInfo.getParamValue();
        if (paramScanInfo != null) {
            String table = paramScanInfo.getTable();
            String column = paramScanInfo.getColumn();
            TableInfo tableInfo = null;
            Map<String, ScanInvoker.TableScanInfo> tableScanInfoMap = scanInfo.getTableScanInfoMap();
            if (ObjectUtil.isNull(table)) {
                Collection<ScanInvoker.TableScanInfo> tableScanInfoList = tableScanInfoMap.values();
                if (tableFactory != null) {
                    for (ScanInvoker.TableScanInfo tableScanInfo : tableScanInfoList) {
                        tableInfo = tableFactory.getTableInfo(tableScanInfo.getTable());
                        if (tableInfo != null) {
                            break;
                        }
                    }
                }
            } else {
                ScanInvoker.TableScanInfo tableScanInfo = tableScanInfoMap.get(table);
                if (tableScanInfo != null) {
                    table = tableScanInfo.getTable();
                }
                if (tableFactory != null) {
                    tableInfo = tableFactory.getTableInfo(table);
                }
            }
            int jdbcType = Types.NULL;
            ColumnInfo columnInfo = null;
            if (tableInfo != null) {
                String fieldName = tableInfo.getFieldName(column);
                if (ObjectUtil.isNull(fieldName)) {
                    fieldName = column;
                }
                columnInfo = tableInfo.getColumnInfo(fieldName);
                if (columnInfo != null) {
                    jdbcType = columnInfo.getJdbcType();
                }
            }
            return new ParamType(columnInfo, typeHandlerFactory.getTypeHandler(value == null ? Object.class : value.getClass(), jdbcType));
        } else {
            return new ParamType(null, typeHandlerFactory.getTypeHandler(value == null ? Object.class : value.getClass(), Types.NULL));
        }
    }

    protected MappedParam getMappedParam(ColumnInfo columnInfo, Object paramValue, TypeHandler typeHandler) {
        int jdbcType;
        if (columnInfo != null) {
            jdbcType = columnInfo.getJdbcType();
        } else {
            jdbcType = Types.NULL;
        }
        return new MappedParam(jdbcType, paramValue, typeHandler);
    }

    @Override
    public CacheKey getCacheKey(MethodInfo methodInfo) {
        String sql = methodInfo.getSql();
        char[] charList = sql.toCharArray();
        int index = 0;
        for (int i = 0; i < charList.length; i++) {
            char c;
            if (!Character.isWhitespace(c = charList[i])) {
                charList[index++] = Character.toLowerCase(c);
            }
        }
        if (split > index)
            split = index;
        Object[] hashObj = new Object[split + 2];
        int len = (int) Math.ceil(index / (double) split);
        for (int i = 0; i < split; i++) {
            int sPoint = i * len;
            int size = Math.min((i + 1) * len, index) - sPoint;
            char[] tempChars = new char[size];
            System.arraycopy(charList, sPoint, tempChars, 0, size);
            hashObj[i] = new String(tempChars);
        }
        hashObj[split] = methodInfo.getColType();
        hashObj[split + 1] = methodInfo.getRowType();
        CacheKey cacheKey = new CacheKey();
        cacheKey.update(hashObj);
        return cacheKey;
    }

    protected MyFunctionFactory getMyFunctionFactory() {
        return null;
    }

    protected ToSQL getToSQL() {
        switch (getDbType()) {
            case MYSQL:
                return new ToMYSQL();
            case MSSQL:
                return new ToMSSQL();
            case PGSQL:
                return new ToPGSQL();
            case ORACLE:
                return new ToORACLE();
            default:
                throw new DreamRunTimeException(getDbType() + "类型尚未不支持");
        }
    }

    @Override
    public void compileAfter(MethodInfo methodInfo) {
        List<Wrapper> allWrapperList = new ArrayList<>();
        List<Wrapper> beforeWrapperList = Arrays.asList(new AnnotationWrapper());
        List<Wrapper> wrapperList = getWrapList();
        List<Wrapper> afterWrapperList = Arrays.asList(new ScanWrapper());
        allWrapperList.addAll(beforeWrapperList);
        if (!ObjectUtil.isNull(wrapperList)) {
            allWrapperList.addAll(wrapperList);
        }
        allWrapperList.addAll(afterWrapperList);
        for (Wrapper wrapper : allWrapperList) {
            wrapper.wrapper(methodInfo);
        }
    }

    protected List<Wrapper> getWrapList() {
        return Arrays.asList(new PageWrapper());
    }


    static class ParamTypeMap {
        private final Map<String, ParamType> paramTypeMap = new HashMap<>();

        public ParamType get(String param) {
            return paramTypeMap.get(param);
        }

        public void put(String param, ParamType paramType) {
            paramTypeMap.put(param, paramType);
        }
    }

    static class ParamType {
        private final ColumnInfo columnInfo;
        private final TypeHandler typeHandler;

        public ParamType(ColumnInfo columnInfo, TypeHandler typeHandler) {
            this.columnInfo = columnInfo;
            this.typeHandler = typeHandler;
        }

        public ColumnInfo getColumnInfo() {
            return columnInfo;
        }

        public TypeHandler getTypeHandler() {
            return typeHandler;
        }
    }
}
