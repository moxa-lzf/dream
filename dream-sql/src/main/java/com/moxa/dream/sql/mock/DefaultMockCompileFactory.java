package com.moxa.dream.sql.mock;

import com.moxa.dream.antlr.config.Command;
import com.moxa.dream.antlr.config.ExprInfo;
import com.moxa.dream.antlr.config.ExprType;
import com.moxa.dream.antlr.factory.AntlrInvokerFactory;
import com.moxa.dream.antlr.read.ExprReader;
import com.moxa.dream.system.cache.CacheKey;
import com.moxa.dream.system.config.Configuration;
import com.moxa.dream.system.config.MappedParam;
import com.moxa.dream.system.config.MappedSql;
import com.moxa.dream.system.config.MappedStatement;
import com.moxa.dream.system.typehandler.handler.ObjectTypeHandler;
import com.moxa.dream.system.typehandler.handler.TypeHandler;
import com.moxa.dream.util.common.ObjectWrapper;
import com.moxa.dream.util.exception.DreamRunTimeException;

import java.lang.reflect.Array;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultMockCompileFactory implements MockCompileFactory {
    private Configuration configuration;

    public DefaultMockCompileFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public MappedStatement compile(String sql, Object param, Class<? extends Collection> rowType, Class<?> colType, boolean cache, int timeOut) {
        Command command = Command.NONE;
        Set<String> tableList = new HashSet<>();
        List<Object> paramList = new ArrayList<>();
        ExprReader exprReader = new ExprReader(sql);
        int startIndex = 0;
        StringBuilder sqlBuilder = new StringBuilder();
        while (true) {
            ExprInfo exprInfo;
            switch (((exprInfo = exprReader.push()).getExprType())) {
                case SELECT:
                    if (command == Command.NONE) {
                        command = Command.QUERY;
                    }
                    break;
                case UPDATE:
                    if (command == Command.NONE) {
                        command = Command.UPDATE;
                    }
                    break;
                case INSERT:
                    if (command == Command.NONE) {
                        command = Command.INSERT;
                    }
                    break;
                case DELETE:
                    if (command == Command.NONE) {
                        command = Command.DELETE;
                    }
                    break;
                case FROM:
                case JOIN:
                case INTO:
                    exprInfo = exprReader.push();
                    switch (exprInfo.getExprType()) {
                        case LETTER:
                        case SINGLE_MARK:
                        case STR:
                        case JAVA_STR:
                            tableList.add(exprInfo.getInfo());
                            break;
                    }
                    break;
                case INVOKER:
                    exprInfo = exprReader.push();
                    String _info = exprInfo.getInfo();
                    switch (_info) {
                        case AntlrInvokerFactory.$:
                        case AntlrInvokerFactory.REP:
                        case AntlrInvokerFactory.FOREACH:
                            ExprInfo tempExprInfo = exprReader.push();
                            if (tempExprInfo.getExprType() == ExprType.LBRACE) {
                                sqlBuilder.append(sql, startIndex, exprInfo.getStart() - 1);
                                StringBuilder paramBuilder = new StringBuilder();
                                while ((tempExprInfo = exprReader.push()).getExprType() != ExprType.RBRACE) {
                                    paramBuilder.append(tempExprInfo.getInfo());
                                }
                                Object value = ObjectWrapper.wrapper(param, false).get(paramBuilder.toString());
                                startIndex = tempExprInfo.getEnd();
                                if (_info.equals(AntlrInvokerFactory.$)) {
                                    paramList.add(value);
                                    sqlBuilder.append("?");
                                } else if (_info.equals(AntlrInvokerFactory.REP)) {
                                    sqlBuilder.append(value);
                                } else if (_info.equals(AntlrInvokerFactory.FOREACH)) {
                                    if (value == null) {
                                        throw new DreamRunTimeException(AntlrInvokerFactory.FOREACH + "参数值不能为空");
                                    }
                                    if (value instanceof Collection) {
                                        if (((Collection<?>) value).isEmpty()) {
                                            throw new DreamRunTimeException(AntlrInvokerFactory.FOREACH + "参数值不能为空");
                                        }
                                        for (int i = 0; i < ((Collection<?>) value).size() - 1; i++) {
                                            sqlBuilder.append("?,");
                                        }
                                        sqlBuilder.append("?");
                                        paramList.addAll((Collection<?>) value);
                                    } else if (value.getClass().isArray()) {
                                        int length = Array.getLength(value);
                                        if (length == 0) {
                                            throw new DreamRunTimeException(AntlrInvokerFactory.FOREACH + "参数值不能为空");
                                        }
                                        int i = 0;
                                        for (; i < length - 1; i++) {
                                            sqlBuilder.append("?,");
                                            paramList.add(Array.get(value, i));
                                        }
                                        sqlBuilder.append("?");
                                        paramList.add(Array.get(value, i));
                                    }
                                }
                                break;
                            }
                            break;
                    }
                    break;
            }
            if (exprInfo.getExprType() == ExprType.ACC) {
                break;
            }
        }
        if (command == Command.NONE) {
            command = Command.UPDATE;
        }
        sqlBuilder.append(sql, startIndex, sql.length());
        MockMethodInfo mockMethodInfo = new MockMethodInfo(configuration, sql, command, cache, timeOut, rowType, colType);
        CacheKey methodKey = mockMethodInfo.getMethodKey();
        if (!paramList.isEmpty()) {
            methodKey.update(paramList.toArray());
        }
        return new MappedStatement.Builder()
                .methodInfo(mockMethodInfo)
                .uniqueKey(methodKey)
                .arg(param)
                .mappedSql(new MappedSql(command, sqlBuilder.toString(), tableList))
                .mappedParamList(paramList.stream().map(par -> {
                    TypeHandler typeHandler;
                    if (par == null) {
                        typeHandler = new ObjectTypeHandler();
                    } else {
                        typeHandler = configuration.getTypeHandlerFactory().getTypeHandler(par.getClass(), Types.NULL);
                    }
                    return new MappedParam(Types.NULL, par, typeHandler);
                }).collect(Collectors.toList())).build();
    }
}