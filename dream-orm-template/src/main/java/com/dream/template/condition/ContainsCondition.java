package com.dream.template.condition;

import com.dream.antlr.invoker.Invoker;
import com.dream.antlr.util.AntlrUtil;
import com.dream.system.antlr.invoker.MarkInvoker;

public class ContainsCondition implements Condition {
    @Override
    public String getCondition(String table, String column, String field) {
        return table + "." + column + " like concat('%'," + AntlrUtil.invokerSQL(MarkInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE, field) + ",'%')";
    }
}
