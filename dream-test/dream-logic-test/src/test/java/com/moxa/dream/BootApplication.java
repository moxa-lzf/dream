package com.moxa.dream;

import com.moxa.dream.antlr.invoker.Invoker;
import com.moxa.dream.boot.bean.ConfigurationBean;
import com.moxa.dream.drive.listener.DebugListener;
import com.moxa.dream.mate.logic.inject.LogicInject;
import com.moxa.dream.mate.logic.invoker.LogicInvoker;
import com.moxa.dream.system.cache.Cache;
import com.moxa.dream.system.cache.CacheFactory;
import com.moxa.dream.system.cache.DefaultCacheFactory;
import com.moxa.dream.system.core.listener.Listener;
import com.moxa.dream.system.inject.Inject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class BootApplication {
    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }

    @Bean
    public CacheFactory cacheFactory() {
        return new DefaultCacheFactory() {
            @Override
            public Cache getCache() {
                return null;
            }
        };
    }

    @Bean
    public Listener[] listeners() {
        return new Listener[]{new DebugListener()};
    }

    @Bean
    public Invoker invoker() {
        return new LogicInvoker();
    }

    @Bean
    public Inject[] injects() {
        return new Inject[]{new LogicInject(() -> "del_flag")};
    }

    @Bean
    public ConfigurationBean configurationBean() {
        ConfigurationBean configurationBean = new ConfigurationBean(Arrays.asList("com.moxa.dream.base"), Arrays.asList("com.moxa.dream.base"));
        return configurationBean;
    }
}
