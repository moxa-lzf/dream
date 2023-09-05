package com.dream.flex.mapper;

import com.dream.antlr.config.Assist;
import com.dream.antlr.config.Command;
import com.dream.antlr.exception.AntlrException;
import com.dream.antlr.invoker.Invoker;
import com.dream.antlr.smt.*;
import com.dream.antlr.sql.ToSQL;
import com.dream.flex.config.SqlInfo;
import com.dream.flex.def.DeleteDef;
import com.dream.flex.def.InsertDef;
import com.dream.flex.def.QueryDef;
import com.dream.flex.def.UpdateDef;
import com.dream.flex.inject.FlexInject;
import com.dream.flex.invoker.FlexMarkInvoker;
import com.dream.flex.invoker.FlexMarkInvokerStatement;
import com.dream.flex.invoker.FlexTableInvoker;
import com.dream.system.cache.CacheKey;
import com.dream.system.config.*;
import com.dream.system.core.resultsethandler.TransformResultSetHandler;
import com.dream.system.core.session.Session;
import com.dream.system.inject.PageInject;
import com.dream.system.inject.factory.InjectFactory;
import com.dream.system.typehandler.TypeHandlerNotFoundException;
import com.dream.system.typehandler.factory.TypeHandlerFactory;
import com.dream.system.typehandler.handler.ObjectTypeHandler;
import com.dream.system.typehandler.handler.TypeHandler;
import com.dream.system.util.SystemUtil;
import com.dream.util.common.NonCollection;
import com.dream.util.common.ObjectUtil;
import com.dream.util.exception.DreamRunTimeException;
import com.dream.util.tree.Tree;
import com.dream.util.tree.TreeUtil;

import java.sql.Types;
import java.util.*;

public class DefaultFlexMapper implements FlexMapper {
    private Session session;
    private Configuration configuration;
    private ToSQL toSQL;
    private TypeHandlerFactory typeHandlerFactory;
    private InjectFactory injectFactory;
    private boolean offset;

    public DefaultFlexMapper(Session session, ToSQL toSQL) {
        this.session = session;
        this.configuration = session.getConfiguration();
        this.toSQL = toSQL;
        typeHandlerFactory = configuration.getTypeHandlerFactory();
        injectFactory = configuration.getInjectFactory();
        PageInject pageInject = injectFactory.getInject(PageInject.class);
        if (pageInject != null) {
            this.offset = pageInject.isOffset();
        }
        FlexInject flexInject = injectFactory.getInject(FlexInject.class);
        if (flexInject == null) {
            injectFactory.injects(new FlexInject());
        }
        WHITE_SET.add(FlexInject.class);
    }

    @Override
    public <T> T selectOne(QueryDef queryDef, Class<T> type) {
        return selectOne(queryDef.statement(), type);
    }

    @Override
    public <T> List<T> selectList(QueryDef queryDef, Class<T> type) {
        return selectList(queryDef.statement(), type);
    }

    @Override
    public <T extends Tree> List<T> selectTree(QueryDef queryDef, Class<T> type) {
        return selectTree(queryDef.statement(), type);
    }

    @Override
    public <T> Page<T> selectPage(QueryDef queryDef, Class<T> type, Page page) {
        return selectPage(queryDef.statement(), type, page);
    }

    @Override
    public int update(UpdateDef updateDef) {
        return update(updateDef.statement());
    }

    @Override
    public int delete(DeleteDef deleteDef) {
        return delete(deleteDef.statement());
    }

    @Override
    public int insert(InsertDef insertDef) {
        return insert(insertDef.statement());
    }

    @Override
    public boolean exists(QueryDef queryDef) {
        return exists(queryDef.statement());
    }

    protected <T> T selectOne(QueryStatement statement, Class<T> type) {
        MappedStatement mappedStatement = getMappedStatement(Command.QUERY, statement, NonCollection.class, type);
        return (T) session.execute(mappedStatement);
    }

    protected <T> List<T> selectList(QueryStatement statement, Class<T> type) {
        MappedStatement mappedStatement = getMappedStatement(Command.QUERY, statement, List.class, type);
        return (List<T>) session.execute(mappedStatement);
    }

    protected <T> List<T> selectTree(QueryStatement statement, Class<T> type) {
        MethodInfo methodInfo = getMethodInfo(statement, List.class, type);
        methodInfo.setResultSetHandler(new TransformResultSetHandler<Collection, List>(result -> TreeUtil.toTree(result)));
        MappedStatement mappedStatement = getMappedStatement(methodInfo, Command.QUERY);
        return (List<T>) session.execute(mappedStatement);
    }

    protected <T> Page<T> selectPage(QueryStatement statement, Class<T> type, Page page) {
        if (page.getTotal() == 0) {
            MappedStatement countMappedStatement = getMappedStatement(Command.QUERY, countQueryStatement(statement), NonCollection.class, Long.class);
            page.setTotal((long) session.execute(countMappedStatement));
        }
        QueryStatement queryStatement = pageQueryStatement(statement, page.getStartRow(), page.getPageSize());
        MappedStatement mappedStatement = getMappedStatement(Command.QUERY, queryStatement, Collection.class, type);
        page.setRows((Collection) session.execute(mappedStatement));
        return page;
    }

    protected int update(UpdateStatement statement) {
        MappedStatement mappedStatement = getMappedStatement(Command.UPDATE, statement, NonCollection.class, Integer.class);
        return (int) session.execute(mappedStatement);
    }

    protected int delete(DeleteStatement statement) {
        MappedStatement mappedStatement = getMappedStatement(Command.DELETE, statement, NonCollection.class, Integer.class);
        return (int) session.execute(mappedStatement);
    }

    protected int insert(InsertStatement statement) {
        MappedStatement mappedStatement = getMappedStatement(Command.INSERT, statement, NonCollection.class, Integer.class);
        return (int) session.execute(mappedStatement);
    }

    protected boolean exists(QueryStatement statement) {
        SelectStatement selectStatement = statement.getSelectStatement();
        ListColumnStatement listColumnStatement = new ListColumnStatement();
        listColumnStatement.add(new SymbolStatement.NumberStatement("1"));
        selectStatement.setSelectList(listColumnStatement);
        LimitStatement limitStatement = new LimitStatement();
        limitStatement.setFirst(new SymbolStatement.NumberStatement("0"));
        limitStatement.setSecond(new SymbolStatement.NumberStatement("1"));
        statement.setLimitStatement(limitStatement);
        Integer result = this.selectOne(statement, Integer.class);
        return result != null;
    }

    protected QueryStatement pageQueryStatement(QueryStatement queryStatement, long startRow, long pageNum) {
        LimitStatement limitStatement = queryStatement.getLimitStatement();
        if (limitStatement == null) {
            limitStatement = new LimitStatement();
            if (offset) {
                limitStatement.setOffset(true);
                limitStatement.setFirst(new FlexMarkInvokerStatement(pageNum));
                limitStatement.setSecond(new FlexMarkInvokerStatement(startRow));
            } else {
                limitStatement.setOffset(false);
                limitStatement.setFirst(new FlexMarkInvokerStatement(startRow));
                limitStatement.setSecond(new FlexMarkInvokerStatement(pageNum));
            }
            queryStatement.setLimitStatement(limitStatement);
        } else {
            throw new DreamRunTimeException("采用自动分页方式，不支持手动分页");
        }
        return queryStatement;
    }

    protected QueryStatement countQueryStatement(QueryStatement statement) {
        statement.setOrderStatement(null);
        FunctionStatement.CountStatement countStatement = new FunctionStatement.CountStatement();
        ListColumnStatement paramListColumnStatement = new ListColumnStatement(",");
        paramListColumnStatement.add(new SymbolStatement.LetterStatement("*"));
        countStatement.setParamsStatement(paramListColumnStatement);
        ListColumnStatement listColumnStatement = new ListColumnStatement(",");
        listColumnStatement.add(countStatement);
        QueryStatement queryStatement = new QueryStatement();
        SelectStatement newSelectStatement = new SelectStatement();
        newSelectStatement.setSelectList(listColumnStatement);
        queryStatement.setSelectStatement(newSelectStatement);
        AliasStatement aliasStatement = new AliasStatement();
        aliasStatement.setColumn(new BraceStatement(statement));
        aliasStatement.setAlias(new SymbolStatement.SingleMarkStatement("t_tmp"));
        FromStatement fromStatement = new FromStatement();
        fromStatement.setMainTable(aliasStatement);
        queryStatement.setFromStatement(fromStatement);
        return queryStatement;
    }

    protected MethodInfo getMethodInfo(Statement statement, Class<? extends Collection> rowType, Class<?> colType) {
        PackageStatement packageStatement = new PackageStatement();
        packageStatement.setStatement(statement);
        MethodInfo methodInfo = new MethodInfo()
                .setConfiguration(configuration)
                .setStatement(packageStatement)
                .setRowType(rowType)
                .setColType(colType)
                .setCompile(Compile.ANTLR_COMPILED);
        injectFactory.inject(methodInfo, inject -> WHITE_SET.contains(inject.getClass()));
        return methodInfo;
    }

    protected MappedStatement getMappedStatement(Command command, Statement statement, Class<? extends Collection> rowType, Class<?> colType) {
        MethodInfo methodInfo = getMethodInfo(statement, rowType, colType);
        return getMappedStatement(methodInfo, command);
    }

    protected MappedStatement getMappedStatement(MethodInfo methodInfo, Command command) {
        SqlInfo sqlInfo = toSQL(methodInfo);
        List<Object> paramList = sqlInfo.getParamList();
        List<MappedParam> mappedParamList = null;
        if (!ObjectUtil.isNull(paramList)) {
            mappedParamList = new ArrayList<>(paramList.size());
            for (Object param : paramList) {
                TypeHandler typeHandler;
                if (param == null) {
                    typeHandler = new ObjectTypeHandler();
                } else {
                    try {
                        typeHandler = typeHandlerFactory.getTypeHandler(param.getClass(), Types.NULL);
                    } catch (TypeHandlerNotFoundException e) {
                        throw new DreamRunTimeException(e);
                    }
                }
                mappedParamList.add(new MappedParam().setParamValue(param).setTypeHandler(typeHandler));
            }
        }
        String sql = sqlInfo.getSql();
        CacheKey methodKey = SystemUtil.cacheKey(sql, 5, false);
        methodKey.update(methodInfo.getRowType(), methodInfo.getColType());
        methodInfo.setMethodKey(methodKey);
        CacheKey uniqueKey = methodKey.clone();
        uniqueKey.update(sqlInfo.getParamList().toArray());
        MappedSql mappedSql = new MappedSql(com.dream.system.config.Command.valueOf(command.name()), sql, sqlInfo.getTableSet());
        MappedStatement mappedStatement = new MappedStatement
                .Builder()
                .mappedParamList(mappedParamList)
                .mappedSql(mappedSql)
                .uniqueKey(uniqueKey)
                .methodInfo(methodInfo)
                .build();
        return mappedStatement;
    }

    protected SqlInfo toSQL(MethodInfo methodInfo) {
        Map<Class, Object> customMap = new HashMap<>();
        customMap.put(MethodInfo.class, methodInfo);
        customMap.put(Configuration.class, configuration);
        Assist assist = new Assist(configuration.getInvokerFactory(), customMap);
        String sql;
        try {
            sql = toSQL.toStr(methodInfo.getStatement(), assist, null);
        } catch (AntlrException e) {
            throw new DreamRunTimeException(e);
        }
        FlexMarkInvoker flexMarkInvoker = (FlexMarkInvoker) assist.getInvoker(FlexMarkInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE);
        FlexTableInvoker flexTableInvoker = (FlexTableInvoker) assist.getInvoker(FlexTableInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE);
        List<Object> paramList = flexMarkInvoker.getParamList();
        Set<String> tableSet = flexTableInvoker.getTableSet();
        return new SqlInfo(sql, paramList, tableSet);
    }
}
