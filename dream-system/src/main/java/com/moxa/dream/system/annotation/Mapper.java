package com.moxa.dream.system.annotation;

import com.moxa.dream.util.common.NullObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口可执行标识
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mapper {
    /**
     * 接口方法与SQL语句映射辅助类
     *
     * @return
     */
    Class<?> value() default NullObject.class;
}
