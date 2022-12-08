package com.moxa.dream.template.mapper;

import com.moxa.dream.system.config.BatchMappedStatement;
import com.moxa.dream.system.config.MethodInfo;
import com.moxa.dream.system.core.session.Session;
import com.moxa.dream.template.attach.AttachMent;

import java.util.List;

public class BatchUpdateByIdMapper extends UpdateByIdMapper {
    ThreadLocal<Integer> threadLocal = new ThreadLocal();

    public BatchUpdateByIdMapper(Session session, AttachMent attachMent) {
        super(session, attachMent);
    }

    @Override
    protected Object execute(MethodInfo methodInfo, Object arg) {
        Integer batchSize = threadLocal.get();
        return session.execute(new BatchMappedStatement(methodInfo, (List<?>) arg, batchSize));
    }

    public Object execute(Class<?> type, List<?> viewList, int batchSize) {
        threadLocal.set(batchSize);
        try {
            return super.execute(type, viewList);
        } finally {
            threadLocal.remove();
        }
    }
}
