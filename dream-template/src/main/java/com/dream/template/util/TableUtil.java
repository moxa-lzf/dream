package com.dream.template.util;

import com.dream.system.annotation.Ignore;
import com.dream.system.util.SystemUtil;
import com.dream.util.common.LowHashSet;
import com.dream.util.common.ObjectUtil;
import com.dream.util.reflect.ReflectUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

class TableUtil {
    public Set<String> getTableNameSet(Class<?> type) {
        Set<String> tableNameSet = new LowHashSet();
        getTableNameSet(type, tableNameSet);
        return tableNameSet;
    }

    private void getTableNameSet(Class<?> type, Set<String> tableNameSet) {
        String table = SystemUtil.getTableName(type);
        if (!ObjectUtil.isNull(table)) {
            if (!tableNameSet.contains(table)) {
                tableNameSet.add(table);
                List<Field> fieldList = ReflectUtil.findField(type);
                if (!ObjectUtil.isNull(fieldList)) {
                    for (Field field : fieldList) {
                        if (!SystemUtil.ignoreField(field)) {
                            if (!field.isAnnotationPresent(Ignore.class)) {
                                getTableNameSet(ReflectUtil.getColType(field.getGenericType()), tableNameSet);
                            }
                        }
                    }
                }
            }
        }
    }
}