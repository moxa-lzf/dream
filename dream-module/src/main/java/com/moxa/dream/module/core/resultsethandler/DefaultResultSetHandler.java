package com.moxa.dream.module.core.resultsethandler;

import com.moxa.dream.antlr.invoker.ScanInvoker;
import com.moxa.dream.module.annotation.View;
import com.moxa.dream.module.cache.CacheKey;
import com.moxa.dream.module.config.Configuration;
import com.moxa.dream.module.core.executor.Executor;
import com.moxa.dream.module.dialect.DialectFactory;
import com.moxa.dream.module.mapped.MappedColumn;
import com.moxa.dream.module.mapped.MappedResult;
import com.moxa.dream.module.mapped.MappedStatement;
import com.moxa.dream.module.mapper.EachInfo;
import com.moxa.dream.module.mapper.factory.MapperFactory;
import com.moxa.dream.module.reflect.factory.ObjectFactory;
import com.moxa.dream.module.reflect.wrapper.ObjectFactoryWrapper;
import com.moxa.dream.module.reflect.wrapper.PropertyInfo;
import com.moxa.dream.module.table.ColumnInfo;
import com.moxa.dream.module.table.TableInfo;
import com.moxa.dream.module.typehandler.handler.TypeHandler;
import com.moxa.dream.util.common.LowHashSet;
import com.moxa.dream.util.common.ObjectUtil;
import com.moxa.dream.util.reflect.ReflectUtil;
import com.moxa.dream.util.wrapper.ObjectWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class DefaultResultSetHandler implements ResultSetHandler {
    private MapperFactory mapperFactory;
    private DialectFactory dialectFactory;
    private Executor executor;

    public DefaultResultSetHandler(Configuration configuration, Executor executor) {
        this.mapperFactory = configuration.getMapperFactory();
        this.dialectFactory = configuration.getDialectFactory();
        this.executor = executor;

    }

    @Override
    public Object result(ResultSet resultSet, MappedStatement mappedStatement) throws SQLException {
        MappedResult mappedResult = getMappedResult(resultSet, mappedStatement);
        ObjectFactory resultObjectFactory = mappedResult.newRowObjectFactory();
        if (mappedResult.isSimple()) {
            while (resultSet.next()) {
                ObjectFactory objectFactory = doSimpleResult(resultSet, mappedStatement, mappedResult);
                Object result = objectFactory.getObject();
                resultObjectFactory.set(null, result);
                eachList(result, mappedStatement.getEachInfoList());
            }
        } else {
            Map<CacheKey, Object> cacheMap = new HashMap<>();
            while (resultSet.next()) {
                ObjectFactory objectFactory = doNestedResult(resultSet, mappedStatement, mappedResult, cacheMap);
                if (objectFactory != null) {
                    Object result = objectFactory.getObject();
                    resultObjectFactory.set(null, result);
                    eachList(result, mappedStatement.getEachInfoList());
                }
            }
            cacheMap.clear();
        }
        return resultObjectFactory.getObject();
    }

    protected ObjectFactory doNestedResult(ResultSet resultSet, MappedStatement mappedStatement, MappedResult mappedResult, Map<CacheKey, Object> cacheMap) throws SQLException {
        Map<String, MappedResult> childMappedResultMap = mappedResult.getChildResultMappingMap();
        ObjectFactory targetObjectFactory = null;
        boolean returnNull = false;
        for (String fieldName : childMappedResultMap.keySet()) {
            MappedResult childMappedResult = childMappedResultMap.get(fieldName);
            ObjectFactory childObjectFactory;
            boolean simple;
            if (simple = childMappedResult.isSimple()) {
                childObjectFactory = doSimpleResult(resultSet, mappedStatement, childMappedResult);
            } else {
                childObjectFactory = doNestedResult(resultSet, mappedStatement, mappedResult, cacheMap);
            }
            if (!simple && childObjectFactory == null)
                continue;
            if (targetObjectFactory == null) {
                CacheKey cacheKey = new CacheKey();
                MappedColumn[] primaryList = mappedResult.getPrimaryList();
                if (ObjectUtil.isNull(primaryList)) {
                    primaryList = mappedResult.getMappedColumnList();
                }
                Object[] idList = new Object[primaryList.length + 1];
                idList[0] = mappedResult;
                for (int i = 0; i < primaryList.length; i++) {
                    idList[i + 1] = primaryList[i].getValue(resultSet);
                }
                cacheKey.update(idList);
                targetObjectFactory = (ObjectFactory) cacheMap.get(cacheKey);
                if (targetObjectFactory == null) {
                    targetObjectFactory = doSimpleResult(resultSet, mappedStatement, mappedResult);
                    cacheMap.put(cacheKey, targetObjectFactory);
                } else {
                    returnNull = true;
                }
            }
            Class<? extends Collection> rowType = childMappedResult.getRowType();
            if (rowType != null) {
                Collection rowList = (Collection) targetObjectFactory.get(childMappedResult.getPropertyInfo());
                if (rowList == null) {
                    rowList = (Collection) ObjectFactoryWrapper.wrapper(rowType).newObjectFactory().getObject();
                    targetObjectFactory.set(childMappedResult.getPropertyInfo(), rowList);
                }
                rowList.add(childObjectFactory.getObject());
            } else {
                targetObjectFactory.set(childMappedResult.getPropertyInfo(), childObjectFactory.getObject());
            }
        }
        if (returnNull)
            return null;
        else
            return targetObjectFactory;
    }

    protected ObjectFactory doSimpleResult(ResultSet resultSet, MappedStatement mappedStatement, MappedResult mappedResult) throws SQLException {
        MappedColumn[] mappedColumnList = mappedResult.getColumnMappingList();
        ObjectFactory objectFactory = mappedResult.newColObjectFactory();
        for (MappedColumn mappedColumn : mappedColumnList) {
            objectFactory.set(mappedColumn.getPropertyInfo(), mappedColumn.getValue(resultSet));
        }
        return objectFactory;
    }

    protected MappedResult getMappedResult(ResultSet resultSet, MappedStatement mappedStatement) throws SQLException {
        MappedResult mappedResult = mappedStatement.get(MappedResult.class);
        if (mappedResult == null) {
            synchronized (this.getClass().getName()) {
                mappedResult = mappedStatement.get(MappedResult.class);
                if (mappedResult == null) {
                    mappedResult = createMappedResult(resultSet, mappedStatement);
                    mappedStatement.set(MappedResult.class, mappedResult);
                }
            }
        }
        return mappedResult;
    }

    protected MappedResult createMappedResult(ResultSet resultSet, MappedStatement mappedStatement) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        Class colType = mappedStatement.getColType();
        if (colType == Object.class && columnCount > 1)
            colType = HashMap.class;
        MappedResult mappedResult = new MappedResult(mappedStatement.getRowType(), colType, null);
        Map<String, ScanInvoker.TableScanInfo> tableScanInfoMap = mappedStatement.getTableScanInfoMap();
        LowHashSet tableSet = tableScanInfoMap.values().stream().map(tableScanInfo -> tableScanInfo.getTable()).collect(LowHashSet::new, LowHashSet::add, (left, right) -> {
            left.addAll(right);
        });
        for (int i = 1; i <= columnCount; i++) {
            int jdbcType = metaData.getColumnType(i);
            String columnLabel = metaData.getColumnLabel(i);
            String tableName = metaData.getTableName(i);
            String link = getLink(mappedStatement, tableName, columnLabel);
            PropertyInfo propertyInfo = new PropertyInfo();
            propertyInfo.setLabel(link);
            boolean primary = isPrimary(tableName, columnLabel, mappedStatement);
            MappedColumn mappedColumn = new MappedColumn(i, jdbcType, tableName, propertyInfo, primary);
            boolean success = linkHandler(mappedColumn, mappedStatement, mappedResult, tableSet);
            ObjectUtil.requireTrue(success, "Property '" + link + "' mapping failure");
        }
        return mappedResult;
    }

    protected boolean isPrimary(String tableName, String columnLabel, MappedStatement mappedStatement) {
        if (ObjectUtil.isNull(tableName))
            return false;
        Configuration configuration = mappedStatement.getConfiguration();
        TableInfo tableInfo = configuration.getTableFactory().getTableInfo(tableName);
        if (tableInfo == null)
            return false;
        String fieldName = tableInfo.getFieldName(columnLabel);
        if (ObjectUtil.isNull(fieldName))
            return false;
        ColumnInfo columnInfo = tableInfo.getColumnInfo(fieldName);
        return columnInfo.isPrimary();
    }

    protected String getLink(MappedStatement mappedStatement, String tableName, String columnLabel) {
        Configuration configuration = mappedStatement.getConfiguration();
        TableInfo tableInfo = configuration.getTableFactory().getTableInfo(tableName);
        String link = null;
        if (tableInfo != null) {
            link = tableInfo.getFieldName(columnLabel);
        }
        if (ObjectUtil.isNull(link))
            return columnLabel;
        else return link;
    }


    protected boolean linkHandler(MappedColumn mappedColumn, MappedStatement mappedStatement, MappedResult mappedResult, Set<String> tableSet) {
        Class colType = mappedResult.getColType();
        if (ReflectUtil.isBaseClass(colType))
            return linkHandlerForBase(mappedColumn, mappedStatement, mappedResult, tableSet);
        else if (Map.class.isAssignableFrom(colType)) {
            return linkHandlerForMap(mappedColumn, mappedStatement, mappedResult, tableSet);
        } else {
            return linkHandlerForClass(mappedColumn, mappedStatement, mappedResult, tableSet);
        }
    }

    protected boolean linkHandlerForBase(MappedColumn mappedColumn, MappedStatement mappedStatement, MappedResult mappedResult, Set<String> tableSet) {
        mappedColumn.setTypeHandler(mappedStatement.getConfiguration().getTypeHandlerFactory().getTypeHandler(mappedResult.getColType(), mappedColumn.getJdbcType()));
        mappedResult.add(mappedColumn);
        return true;
    }

    protected boolean linkHandlerForMap(MappedColumn mappedColumn, MappedStatement mappedStatement, MappedResult mappedResult, Set<String> tableSet) {
        mappedColumn.setTypeHandler(mappedStatement.getConfiguration().getTypeHandlerFactory().getTypeHandler(Object.class, mappedColumn.getJdbcType()));
        mappedResult.add(mappedColumn);
        return true;
    }

    protected String toLow(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    protected void fillPropertyInfo(List<Method> methodList, Field field, PropertyInfo propertyInfo) {
        String fieldName = field.getName();
        Class<?> fieldType = field.getType();
        if (!fieldName.equals(propertyInfo.getLabel()))
            propertyInfo.setLabel(field.getName());
        if (field != propertyInfo.getField()) {
            propertyInfo.setField(field);
        }
        if (!ObjectUtil.isNull(methodList)) {
            for (Method method : methodList) {
                String name = method.getName();
                if (name.startsWith("set")) {
                    if (name.length() > 3) {
                        name = toLow(name.substring(3));
                        if (fieldName.equals(name) && propertyInfo.getWriteMethod() == null) {
                            Parameter[] parameters = method.getParameters();
                            if (parameters.length == 1) {
                                if (fieldType == parameters[0].getType()) {
                                    propertyInfo.setWriteMethod(method);
                                }
                            }
                        }
                    }
                } else if (name.startsWith("get")) {
                    if (name.length() > 3) {
                        name = toLow(name.substring(3));
                        if (fieldName.equals(name) && propertyInfo.getReadMethod() == null) {
                            Parameter[] parameters = method.getParameters();
                            if (parameters.length == 0) {
                                propertyInfo.setReadMethod(method);
                            }
                        }
                    }
                } else if (name.startsWith("is") && (fieldType == boolean.class || fieldType == Boolean.class)) {
                    if (name.length() > 2) {
                        name = toLow(name.substring(2));
                        if (fieldName.equals(name) && propertyInfo.getReadMethod() == null) {
                            Parameter[] parameters = method.getParameters();
                            if (parameters.length == 0) {
                                propertyInfo.setReadMethod(method);
                            }
                        }
                    }
                }
                if (propertyInfo.getReadMethod() != null && propertyInfo.getWriteMethod() != null) {
                    break;
                }
            }
        }
    }

    protected boolean linkHandlerForClass(MappedColumn mappedColumn, MappedStatement mappedStatement, MappedResult mappedResult, Set<String> tableSet) {
        Class colType = mappedResult.getColType();
        String curTableName = getTableName(colType);
        List<Field> fieldList = ReflectUtil.findField(colType);
        if (!ObjectUtil.isNull(fieldList)) {
            PropertyInfo propertyInfo = mappedColumn.getPropertyInfo();
            boolean lazyLoad = false;
            for (Field field : fieldList) {
                String fieldName = field.getName();
                String link = propertyInfo.getLabel();
                if (!lazyLoad && (ObjectUtil.isNull(curTableName) || ObjectUtil.isNull(mappedColumn.getTable()) || curTableName.equalsIgnoreCase(mappedColumn.getTable()))) {
                    if (fieldName.equalsIgnoreCase(link)) {
                        TypeHandler typeHandler = mappedStatement.getConfiguration().getTypeHandlerFactory().getTypeHandler(field.getType(), mappedColumn.getJdbcType());
                        mappedColumn.setTypeHandler(typeHandler);
                        fillPropertyInfo(ReflectUtil.findMethod(colType), field, propertyInfo);
                        if (!ObjectUtil.isNull(curTableName)) {
                            mappedResult.add(mappedColumn);
                            return true;
                        } else {
                            lazyLoad = true;
                        }
                    }
                }
                Type genericType = field.getGenericType();
                String table = getTableName(genericType);
                if (!ObjectUtil.isNull(table) && tableSet.contains(table)) {
                    Map<String, MappedResult> childMappedResultMap = mappedResult.getChildResultMappingMap();
                    MappedResult childMappedResult = childMappedResultMap.get(fieldName);
                    if (childMappedResult == null) {
                        PropertyInfo childPropertyInfo = new PropertyInfo();
                        childPropertyInfo.setField(field);
                        childMappedResult = new MappedResult(ReflectUtil.getRowType(colType, field), ReflectUtil.getColType(colType, field), childPropertyInfo);
                        if (linkHandler(mappedColumn, mappedStatement, childMappedResult, tableSet)) {
                            childMappedResultMap.put(fieldName, childMappedResult);
                            return true;
                        }
                    } else {
                        if (linkHandler(mappedColumn, mappedStatement, childMappedResult, tableSet)) {
                            return true;
                        }
                    }
                }
            }
            if (lazyLoad) {
                mappedResult.add(mappedColumn);
                return true;
            }
        }
        return false;
    }

    protected String getTableName(Type type) {
        Class colType = ReflectUtil.getColType(type);
        View view = (View) colType.getDeclaredAnnotation(View.class);
        if (view == null)
            return null;
        return view.value();
    }

    protected void eachList(Object object, List<EachInfo> eachInfoList) {
        if (!ObjectUtil.isNull(eachInfoList)) {
            ObjectWrapper wrapper = ObjectWrapper.wrapper(object);
            for (EachInfo eachInfo : eachInfoList) {
                Class type = eachInfo.getType();
                Method method = eachInfo.getMethod();
                Map<String, String> argMap = eachInfo.getArgMap();
                String field = eachInfo.getField();
                Object mapper = mapperFactory.getMapper(type, (methodInfo, arg, args) -> {
                    if (argMap != null) {
                        ObjectWrapper argWrapper = ObjectWrapper.wrapper(arg);
                        for (String paramName : argMap.keySet()) {
                            String valueName = argMap.get(paramName);
                            Object value = wrapper.get(valueName);
                            argWrapper.set(paramName, value);
                        }
                    }
                    MappedStatement mappedStatement = dialectFactory.compile(methodInfo, arg);
                    return executor.query(mappedStatement);
                });
                Object result;
                try {
                    result = method.invoke(mapper, eachInfo.getArgs());
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
                if (!ObjectUtil.isNull(field)) {
                    wrapper.set(field, result);
                }
            }
        }
    }
}
