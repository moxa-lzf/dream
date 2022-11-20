package com.moxa.dream.template.mapper;

import com.moxa.dream.system.annotation.Batch;
import com.moxa.dream.system.config.Configuration;
import com.moxa.dream.system.config.MethodInfo;
import com.moxa.dream.system.core.session.Session;
import com.moxa.dream.system.table.TableInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

public class InsertBatchTemplateMapper extends InsertTemplateMapper {
    public InsertBatchTemplateMapper(Session session) {
        super(session);
    }

    @Override
    protected MethodInfo doGetMethodInfo(Configuration configuration, TableInfo tableInfo, List<Field> fieldList) {
        MethodInfo methodInfo = super.doGetMethodInfo(configuration, tableInfo, fieldList);
        methodInfo.set(Batch.class, new Batch() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Batch.class;
            }

            @Override
            public int value() {
                return 1000;
            }
        });
        return methodInfo;
    }
}
