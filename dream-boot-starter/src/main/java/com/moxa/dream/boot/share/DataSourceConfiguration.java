package com.moxa.dream.boot.share;

import com.moxa.dream.boot.autoconfigure.DreamProperties;
import com.moxa.dream.mate.share.datasource.ShareDataSource;
import com.moxa.dream.mate.share.session.ShareMapperInvokerFactory;
import com.moxa.dream.system.mapper.MapperInvokeFactory;
import com.moxa.dream.util.common.ObjectWrapper;
import com.moxa.dream.util.exception.DreamRunTimeException;
import com.moxa.dream.util.reflect.ReflectUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DataSourceConfiguration implements ImportAware {
    private Class<? extends DataSource> dataSourceType;


    @Bean
    public DataSource dataSource(DreamProperties dreamProperties) {
        Map<String, Map<String, Object>> datasource = dreamProperties.getDatasource();
        if (datasource == null || datasource.isEmpty()) {
            throw new DreamRunTimeException("数据源未配置");
        }
        Map<String, DataSource> dataSourceMap = new HashMap<>(4);
        datasource.forEach((k, v) -> {
            ObjectWrapper dataSourceWrapper = ObjectWrapper.wrapper(ReflectUtil.create(dataSourceType), false);
            v.forEach((key, value) -> dataSourceWrapper.set(key, value));
            dataSourceMap.put(k, (DataSource) dataSourceWrapper.getObject());
        });
        return new ShareDataSource(dataSourceMap);
    }

    @Bean
    public MapperInvokeFactory mapperInvokeFactory() {
        return new ShareMapperInvokerFactory();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> annotationAttributes = importMetadata.getAnnotationAttributes(EnableShare.class.getName());
        this.dataSourceType = (Class<? extends DataSource>) annotationAttributes.get("value");
    }
}
