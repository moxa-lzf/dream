package com.dream.template.condition;

import com.dream.antlr.invoker.Invoker;
import com.dream.antlr.util.AntlrUtil;
import com.dream.system.antlr.invoker.ForEachInvoker;

public class InCondition implements Condition {

    @Override
    public String getCondition(String table, String column, String field) {
        return table + "." + column + " in (" + AntlrUtil.invokerSQL(ForEachInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE, field) + ")";
    }
}
