package com.dream.template.mapper;

import com.dream.antlr.invoker.Invoker;
import com.dream.antlr.util.AntlrUtil;
import com.dream.system.antlr.invoker.StarInvoker;
import com.dream.system.antlr.invoker.TableInvoker;
import com.dream.system.config.Command;
import com.dream.system.config.Configuration;
import com.dream.system.config.MethodInfo;
import com.dream.system.core.session.Session;
import com.dream.system.table.TableInfo;
import com.dream.template.util.TemplateUtil;

import java.util.Collection;
import java.util.Set;

public abstract class SelectMapper extends ValidateMapper {

    public SelectMapper(Session session) {
        super(session);
    }

    @Override
    protected MethodInfo getValidateMethodInfo(Configuration configuration, TableInfo tableInfo, Class type, Object arg) {
        String sql = "select " + getSelectColumn(type) + " from " +
                getFromTable(type) + " " + getOther(configuration, tableInfo, type, arg);
        return new MethodInfo()
                .setConfiguration(configuration)
                .setRowType(getRowType())
                .setColType(getColType(type))
                .setSql(sql);
    }

    protected String getSelectColumn(Class<?> type) {
        return AntlrUtil.invokerSQL(StarInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE);
    }

    protected abstract String getOther(Configuration configuration, TableInfo tableInfo, Class<?> type, Object arg);

    protected abstract Class<? extends Collection> getRowType();

    protected Class<?> getColType(Class type) {
        return type;
    }

    protected String getFromTable(Class type) {
        Set<String> tableNameSet = TemplateUtil.getTableNameSet(type);
        return AntlrUtil.invokerSQL(TableInvoker.FUNCTION, Invoker.DEFAULT_NAMESPACE, tableNameSet.toArray(new String[0])
        );
    }

    @Override
    protected Command getCommand() {
        return Command.QUERY;
    }
}