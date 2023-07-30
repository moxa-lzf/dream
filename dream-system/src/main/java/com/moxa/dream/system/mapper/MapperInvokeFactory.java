package com.moxa.dream.system.mapper;

import com.moxa.dream.system.core.session.Session;

public interface MapperInvokeFactory {
    /**
     * MapperInvoke对象创建工厂
     *
     * @param session 数据操作的会话
     * @return
     */
    MapperInvoke getMapperInvoke(Session session);
}
