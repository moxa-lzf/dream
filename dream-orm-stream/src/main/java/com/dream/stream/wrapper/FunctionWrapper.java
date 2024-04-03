package com.dream.stream.wrapper;

import com.dream.antlr.smt.FunctionStatement;
import com.dream.antlr.smt.ListColumnStatement;
import com.dream.antlr.smt.Statement;
import com.dream.antlr.smt.SymbolStatement;

public class FunctionWrapper {
    private ListColumnStatement columnStatement = new ListColumnStatement(",");

    public FunctionWrapper ascii(String column) {
        return functionWrapper(new FunctionStatement.AsciiStatement(), col(column));
    }

    public FunctionWrapper len(String column) {
        return functionWrapper(new FunctionStatement.CharLengthStatement(), col(column));
    }

    public FunctionWrapper length(String column) {
        return functionWrapper(new FunctionStatement.LengthStatement(), col(column));
    }

    protected Statement col(String column) {
        return new SymbolStatement.LetterStatement(String.valueOf(column));
    }

    protected FunctionWrapper functionWrapper(FunctionStatement functionStatement, Statement... statements) {
        return functionWrapper(functionStatement, ",", statements);
    }

    protected FunctionWrapper functionWrapper(FunctionStatement functionStatement, String split, Statement... statements) {
        ListColumnStatement listColumnStatement = new ListColumnStatement(split);
        for (Statement statement : statements) {
            listColumnStatement.add(statement);
        }
        functionStatement.setParamsStatement(listColumnStatement);
        this.columnStatement.add(functionStatement);
        return this;
    }

    public ListColumnStatement getColumnStatement() {
        return columnStatement;
    }
}
