package com.moxa.dream.antlr.handler.scan;

import com.moxa.dream.antlr.config.Assist;
import com.moxa.dream.antlr.config.Command;
import com.moxa.dream.antlr.exception.InvokerException;
import com.moxa.dream.antlr.handler.AbstractHandler;
import com.moxa.dream.antlr.invoker.ScanInvoker;
import com.moxa.dream.antlr.smt.DeleteStatement;
import com.moxa.dream.antlr.smt.Statement;
import com.moxa.dream.antlr.smt.SymbolStatement;

public class DeleteScanHandler extends AbstractHandler {
    private final ScanInvoker.ScanInfo scanInfo;

    public DeleteScanHandler(ScanInvoker.ScanInfo scanInfo) {
        this.scanInfo = scanInfo;
    }

    @Override
    protected String handlerAfter(Statement statement, Assist assist, String sql, int life) throws InvokerException {
        scanInfo.setCommand(Command.DELETE);
        DeleteStatement deleteStatement = (DeleteStatement) statement;
        scanInfo.add(new ScanInvoker.TableScanInfo(null, ((SymbolStatement) deleteStatement.getTable()).getValue(), null, true));
        return super.handlerAfter(statement, assist, sql, life);
    }

    @Override
    protected boolean interest(Statement statement, Assist sqlAssist) {
        return statement instanceof DeleteStatement;
    }
}
