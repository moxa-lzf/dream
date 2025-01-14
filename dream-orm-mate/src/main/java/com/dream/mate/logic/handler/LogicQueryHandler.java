package com.dream.mate.logic.handler;

import com.dream.antlr.config.Assist;
import com.dream.antlr.exception.AntlrException;
import com.dream.antlr.handler.AbstractHandler;
import com.dream.antlr.handler.Handler;
import com.dream.antlr.invoker.Invoker;
import com.dream.antlr.smt.*;
import com.dream.antlr.sql.ToSQL;
import com.dream.antlr.util.AntlrUtil;
import com.dream.mate.logic.invoker.LogicInvoker;
import com.dream.mate.util.MateUtil;
import com.dream.system.antlr.handler.scan.QueryScanHandler;
import com.dream.system.antlr.invoker.ScanInvoker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class LogicQueryHandler extends AbstractHandler {
    private LogicInvoker logicInvoker;
    private Deque<QueryStatement> queryDeque = new ArrayDeque<>();

    public LogicQueryHandler(LogicInvoker logicInvoker) {
        this.logicInvoker = logicInvoker;
    }

    @Override
    protected Statement handlerBefore(Statement statement, Assist assist, ToSQL toSQL, List<Invoker> invokerList, int life) throws AntlrException {
        queryDeque.push((QueryStatement) statement);
        return statement;
    }

    @Override
    protected boolean interest(Statement statement, Assist assist) {
        return statement instanceof QueryStatement;
    }

    @Override
    protected Handler[] handlerBound() {
        return new Handler[]{new LogicFromHandler()};
    }

    @Override
    protected String handlerAfter(Statement statement, Assist assist, String sql, int life) throws AntlrException {
        queryDeque.poll();
        return sql;
    }

    class LogicFromHandler extends AbstractHandler {
        @Override
        protected Statement handlerBefore(Statement statement, Assist assist, ToSQL toSQL, List<Invoker> invokerList, int life) throws AntlrException {
            FromStatement fromStatement = (FromStatement) statement;
            ScanInvoker.TableScanInfo tableScanInfo = new QueryScanHandler(null).getTableScanInfo(fromStatement.getMainTable(), true);
            if (tableScanInfo != null) {
                String table = tableScanInfo.getTable();
                if (logicInvoker.isLogicDelete(assist, table)) {
                    String logicColumn = logicInvoker.getLogicColumn(table);
                    ConditionStatement conditionStatement = new ConditionStatement();
                    conditionStatement.setLeft(AntlrUtil.listColumnStatement(".", new SymbolStatement.SingleMarkStatement(tableScanInfo.getAlias()), new SymbolStatement.SingleMarkStatement(logicColumn)));
                    conditionStatement.setOper(new OperStatement.EQStatement());
                    conditionStatement.setRight(new SymbolStatement.LetterStatement(logicInvoker.getNormalValue()));
                    QueryStatement queryStatement = queryDeque.peek();
                    WhereStatement whereStatement = queryStatement.getWhereStatement();
                    if (whereStatement == null) {
                        whereStatement = new WhereStatement();
                        whereStatement.setStatement(conditionStatement);
                        queryStatement.setWhereStatement(whereStatement);
                    } else {
                        MateUtil.appendWhere(whereStatement, conditionStatement);
                    }
                }
            }
            return statement;
        }

        @Override
        protected boolean interest(Statement statement, Assist assist) {
            return statement instanceof FromStatement;
        }

        @Override
        protected Handler[] handlerBound() {
            return new Handler[]{new JoinHandler()};
        }

        class JoinHandler extends AbstractHandler {
            @Override
            protected Statement handlerBefore(Statement statement, Assist assist, ToSQL toSQL, List<Invoker> invokerList, int life) throws AntlrException {
                JoinStatement joinStatement = (JoinStatement) statement;
                ScanInvoker.TableScanInfo tableScanInfo = new QueryScanHandler(null).getTableScanInfo(joinStatement.getJoinTable(), false);
                if (tableScanInfo != null) {
                    String table = tableScanInfo.getTable();
                    if (logicInvoker.isLogicDelete(assist, table)) {
                        String logicColumn = logicInvoker.getLogicColumn(table);
                        ConditionStatement conditionStatement = new ConditionStatement();
                        conditionStatement.setLeft(AntlrUtil.listColumnStatement(".", new SymbolStatement.SingleMarkStatement(tableScanInfo.getAlias()), new SymbolStatement.SingleMarkStatement(logicColumn)));
                        conditionStatement.setOper(new OperStatement.EQStatement());
                        conditionStatement.setRight(new SymbolStatement.LetterStatement(logicInvoker.getNormalValue()));
                        Statement joinOnStatement = joinStatement.getOn();
                        if (joinOnStatement instanceof ConditionStatement && ((ConditionStatement) joinOnStatement).getOper() instanceof OperStatement.ORStatement) {
                            joinOnStatement = new BraceStatement(joinOnStatement);
                        }
                        ConditionStatement joinConditionStatement = new ConditionStatement();
                        joinConditionStatement.setLeft(joinOnStatement);
                        joinConditionStatement.setOper(new OperStatement.ANDStatement());
                        joinConditionStatement.setRight(conditionStatement);
                        joinStatement.setOn(joinConditionStatement);
                    }
                }
                return statement;
            }

            @Override
            protected boolean interest(Statement statement, Assist assist) {
                return statement instanceof JoinStatement;
            }
        }
    }
}
