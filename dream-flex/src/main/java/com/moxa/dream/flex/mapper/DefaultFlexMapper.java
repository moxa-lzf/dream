package com.moxa.dream.flex.mapper;

import com.moxa.dream.antlr.config.Command;
import com.moxa.dream.antlr.smt.*;
import com.moxa.dream.antlr.sql.ToSQL;
import com.moxa.dream.flex.def.QueryDef;
import com.moxa.dream.flex.def.ResultInfo;
import com.moxa.dream.flex.def.SqlDef;
import com.moxa.dream.system.config.*;
import com.moxa.dream.system.core.session.Session;
import com.moxa.dream.system.inject.PageInject;
import com.moxa.dream.system.inject.factory.InjectFactory;
import com.moxa.dream.system.typehandler.TypeHandlerNotFoundException;
import com.moxa.dream.system.typehandler.factory.TypeHandlerFactory;
import com.moxa.dream.system.typehandler.handler.ObjectTypeHandler;
import com.moxa.dream.system.typehandler.handler.TypeHandler;
import com.moxa.dream.util.common.LowHashSet;
import com.moxa.dream.util.common.NonCollection;
import com.moxa.dream.util.common.ObjectUtil;
import com.moxa.dream.util.exception.DreamRunTimeException;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DefaultFlexMapper implements FlexMapper {
    private Session session;
    private ToSQL toSQL;
    private TypeHandlerFactory typeHandlerFactory;
    private boolean offset;

    public DefaultFlexMapper(Session session, ToSQL toSQL) {
        this.session = session;
        this.toSQL = toSQL;
        Configuration configuration = session.getConfiguration();
        this.typeHandlerFactory = configuration.getTypeHandlerFactory();
        InjectFactory injectFactory = configuration.getInjectFactory();
        PageInject pageInject = injectFactory.getInject(PageInject.class);
        if (pageInject != null) {
            this.offset = pageInject.isOffset();
        }
    }

    @Override
    public <T> T selectOne(SqlDef sqlDef, Class<T> type) {
        ResultInfo resultInfo = sqlDef.toSQL(toSQL);
        MappedStatement mappedStatement = getMappedStatement(resultInfo, NonCollection.class, type);
        return (T) session.execute(mappedStatement);
    }

    @Override
    public <T> List<T> selectList(SqlDef sqlDef, Class<T> type) {
        ResultInfo resultInfo = sqlDef.toSQL(toSQL);
        MappedStatement mappedStatement = getMappedStatement(resultInfo, List.class, type);
        return (List<T>) session.execute(mappedStatement);
    }

    @Override
    public <T> Page<T> selectPage(SqlDef sqlDef, Class<T> type, Page page) {
        Statement statement = sqlDef.getStatement();
        if (statement instanceof QueryStatement) {
            throw new DreamRunTimeException("抽象树不为查询");
        }
        QueryStatement queryStatement = pageQueryStatement((QueryStatement) statement, page.getStartRow(), page.getPageSize());
        ResultInfo resultInfo = new QueryDef(queryStatement).toSQL(toSQL);
        ResultInfo countResultInfo = new QueryDef(countQueryStatement(queryStatement)).toSQL(toSQL);
        MappedStatement mappedStatement = getMappedStatement(resultInfo, Collection.class, type);
        MappedStatement countMappedStatement = getMappedStatement(countResultInfo, NonCollection.class, Long.class);
        page.setTotal((long) session.execute(countMappedStatement));
        page.setRows((Collection) session.execute(mappedStatement));
        return page;
    }

    private QueryStatement pageQueryStatement(QueryStatement queryStatement, long startRow, long pageNum) {
        LimitStatement limitStatement = queryStatement.getLimitStatement();
        if (limitStatement == null) {
            limitStatement = new LimitStatement();
            if (offset) {
                limitStatement.setOffset(true);
                limitStatement.setFirst(new SymbolStatement.LetterStatement(String.valueOf(pageNum)));
                limitStatement.setSecond(new SymbolStatement.LetterStatement(String.valueOf(startRow)));
            } else {
                limitStatement.setOffset(false);
                limitStatement.setFirst(new SymbolStatement.LetterStatement(String.valueOf(startRow)));
                limitStatement.setSecond(new SymbolStatement.LetterStatement(String.valueOf(pageNum)));
            }
            queryStatement.setLimitStatement(limitStatement);
        } else {
            throw new DreamRunTimeException("采用自动分页方式，不支持手动分页");
        }
        return queryStatement;
    }

    private QueryStatement countQueryStatement(QueryStatement statement) {
        QueryStatement queryStatement = new QueryStatement();
        SelectStatement selectStatement = statement.getSelectStatement();
        queryStatement.setSelectStatement(selectStatement);
        SelectStatement countSelectStatement = new SelectStatement();
        countSelectStatement.setPreSelect(new PreSelectStatement());
        ListColumnStatement listColumnStatement = new ListColumnStatement();
        listColumnStatement.add(countFunctionStatement());
        countSelectStatement.setSelectList(listColumnStatement);
        queryStatement.setSelectStatement(countSelectStatement);
        PreSelectStatement preSelect = selectStatement.getPreSelect();
        if (!(preSelect instanceof PreDistinctSelectStatement)) {
            UnionStatement unionStatement = queryStatement.getUnionStatement();
            if (unionStatement == null) {
                queryStatement.setFromStatement(statement.getFromStatement());
                queryStatement.setWhereStatement(statement.getWhereStatement());
                queryStatement.setGroupStatement(statement.getGroupStatement());
                queryStatement.setHavingStatement(statement.getHavingStatement());
                queryStatement.setUnionStatement(statement.getUnionStatement());
                queryStatement.setForUpdateStatement(statement.getForUpdateStatement());
                return queryStatement;
            }
        }
        AliasStatement aliasStatement = new AliasStatement();
        aliasStatement.setColumn(statement);
        aliasStatement.setAlias(new SymbolStatement.LetterStatement("t_tmp"));
        FromStatement fromStatement = new FromStatement();
        fromStatement.setMainTable(aliasStatement);
        queryStatement.setFromStatement(fromStatement);
        return queryStatement;
    }

    private FunctionStatement countFunctionStatement() {
        FunctionStatement.CountStatement countStatement = new FunctionStatement.CountStatement();
        ListColumnStatement paramListColumnStatement = new ListColumnStatement(",");
        paramListColumnStatement.add(new SymbolStatement.LetterStatement("1"));
        countStatement.setParamsStatement(paramListColumnStatement);
        return countStatement;
    }

    private MappedStatement getMappedStatement(ResultInfo resultInfo, Class<? extends Collection> rowType, Class<?> colType) {
        List<Object> paramList = resultInfo.getParamList();
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

        MappedSql mappedSql = new MappedSql(Command.QUERY.name(), resultInfo.getSql(), new LowHashSet(Arrays.asList()));
        MappedStatement mappedStatement = new MappedStatement
                .Builder()
                .mappedParamList(mappedParamList)
                .mappedSql(mappedSql)
                .methodInfo(new MethodInfo().setRowType(rowType).setColType(colType).setCompile(Compile.ANTLR_COMPILED))
                .build();
        return mappedStatement;
    }
}
