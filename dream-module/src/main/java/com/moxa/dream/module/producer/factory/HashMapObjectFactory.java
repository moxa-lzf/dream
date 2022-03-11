package com.moxa.dream.module.producer.factory;


import com.moxa.dream.module.producer.PropertyInfo;
import com.moxa.dream.module.producer.wrapper.HashMapObjectFactoryWrapper;

import java.util.HashMap;
import java.util.Map;

public class HashMapObjectFactory implements ObjectFactory {
    Map<String, Object> result;
    public HashMapObjectFactory(){
        result=new HashMap<>();
    }

    @Override
    public void set(PropertyInfo propertyInfo, Object value) {
        result.put(propertyInfo.getLabel(), value);
    }

    @Override
    public Object get(PropertyInfo propertyInfo) {
        return result.get(propertyInfo.getLabel());
    }

    @Override
    public Object getObject() {
        return result;
    }
}
