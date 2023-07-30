package com.moxa.dream.mate.permission.inject;

import com.moxa.dream.system.config.MethodInfo;
import com.moxa.dream.system.table.TableInfo;

public interface PermissionHandler {

    /**
     * 判断是否应用数据权限
     *
     * @param methodInfo mapper方法详尽信息
     * @param tableInfo  主表详尽信息
     * @return
     */
    boolean isPermissionInject(MethodInfo methodInfo, TableInfo tableInfo);

    /**
     * 获取数据权限SQL
     *
     * @param alias 主表别名
     * @return
     */
    String getPermission(String alias);

}
