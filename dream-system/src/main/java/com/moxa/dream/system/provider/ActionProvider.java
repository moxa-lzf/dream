package com.moxa.dream.system.provider;

import com.moxa.dream.system.core.action.Action;
import com.moxa.dream.system.core.resultsethandler.ResultSetHandler;
import com.moxa.dream.system.core.statementhandler.StatementHandler;

import java.util.Collection;

/**
 * 映射接口方法与SQL
 */
public interface ActionProvider {

    /**
     * SQL语句
     *
     * @return
     */
    String sql();

    /**
     * SQL执行前行为
     *
     * @return
     */
    default Action[] initActionList() {
        return null;
    }

    /**
     * SQL执行后行为
     *
     * @return
     */
    default Action[] destroyActionList() {
        return null;
    }

    /**
     * 返回的类型
     *
     * @return
     */
    default Class<? extends Collection> rowType() {
        return null;
    }

    /**
     * 返回的类型
     *
     * @return
     */
    default Class<?> colType() {
        return null;
    }

    /**
     * 是否应用缓存
     *
     * @return
     */
    default Boolean cache() {
        return null;
    }

    /**
     * 查询超时设置
     *
     * @return
     */
    default Integer timeOut() {
        return null;
    }

    /**
     * SQL操作最终类
     *
     * @return
     */
    default StatementHandler statementHandler() {
        return null;
    }

    /**
     * 映射数据库查询数据与java对象
     *
     * @return
     */
    default ResultSetHandler resultSetHandler() {
        return null;
    }
}
