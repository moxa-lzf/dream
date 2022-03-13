package com.moxa.dream.driver.factory;

import com.moxa.dream.antlr.factory.InvokerFactory;
import com.moxa.dream.antlr.factory.MyFunctionFactory;
import com.moxa.dream.driver.page.wrapper.PageWrapper;
import com.moxa.dream.module.antlr.factory.DreamInvokerFactory;
import com.moxa.dream.module.antlr.wrapper.Wrapper;
import com.moxa.dream.module.dialect.AbstractDialectFactory;
import com.moxa.dream.module.mapper.MethodInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DefaultDialectFactory extends AbstractDialectFactory {

    @Override
    protected List<InvokerFactory> getInvokerFactoryList() {
        return Arrays.asList(new DreamInvokerFactory());
    }

    @Override
    protected <T> Map<Class<? extends T>, T> getCustomMap(MethodInfo methodInfo, Object arg) {
        return null;
    }

    @Override
    protected MyFunctionFactory getMyFunctionFactory() {
        return null;
    }

    @Override
    protected List<Wrapper> getWrapList() {
        return List.of(new PageWrapper());
    }
}
