package com.moxa.dream.antlr.config;

import com.moxa.dream.antlr.factory.InvokerFactory;
import com.moxa.dream.antlr.invoker.Invoker;
import com.moxa.dream.util.common.ObjectUtil;
import com.moxa.dream.util.exception.DreamRunTimeException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assist {
    private Map<Class, Object> customObjMap;
    private Map<String, InvokerFactory> invokerFactoryMap;
    private Map<String, Invoker> sqlInvokerMap;

    public Assist(List<InvokerFactory> invokerFactoryList, Map<Class, Object> customObjMap) {
        setInvokerFactoryList(invokerFactoryList);
        this.customObjMap = customObjMap;
    }

    public void setInvokerFactoryList(List<InvokerFactory> invokerFactoryList) {
        if (!ObjectUtil.isNull(invokerFactoryList)) {
            invokerFactoryMap = new HashMap<>();
            sqlInvokerMap = new HashMap<>();
            for (InvokerFactory invokerFactory : invokerFactoryList) {
                if (invokerFactoryMap.containsKey(invokerFactory.namespace())) {
                    throw new DreamRunTimeException("命名空间'" + invokerFactory.namespace() + "已经存在");
                }
                invokerFactoryMap.put(invokerFactory.namespace(), invokerFactory);
            }
        }
    }

    public Invoker getInvoker(String namespace, String function) {
        String invokerKey;
        Invoker invoker;
        if (namespace == null) {
            invokerKey = function;
            invoker = sqlInvokerMap.get(invokerKey);
            if (invoker == null) {
                for (InvokerFactory invokerFactory : invokerFactoryMap.values()) {
                    invoker = invokerFactory.create(function);
                    if (invoker != null) {
                        invokerKey = function + ":" + invokerFactory.namespace();
                        sqlInvokerMap.put(function, invoker);
                        break;
                    }
                }
            } else
                return invoker;
        } else {
            invokerKey = function + ":" + namespace.toLowerCase();
            invoker = sqlInvokerMap.get(invokerKey);
            if (invoker == null) {
                InvokerFactory invokerFactory = invokerFactoryMap.get(namespace);
                ObjectUtil.requireNonNull(invokerFactory, "命名空间'" + namespace + "'尚未注册");
                invoker = invokerFactory.create(function);
                if (!sqlInvokerMap.containsKey(function)) {
                    sqlInvokerMap.put(function, invoker);
                }
            } else
                return invoker;
        }
        ObjectUtil.requireNonNull(invoker, "函数@" + invokerKey + "尚未注册");
        sqlInvokerMap.put(invokerKey, invoker);
        invoker.init(this);
        return invoker;
    }

    public <T> T getCustom(Class<T> type) {
        return (T) customObjMap.get(type);
    }

    public <T> void setCustom(Class<T> type, T value) {
        if (customObjMap == null) {
            customObjMap = new HashMap<>();
        }
        customObjMap.put(type, value);
    }

    public Map<String, Invoker> getSqlInvokerMap() {
        return sqlInvokerMap;
    }
}
