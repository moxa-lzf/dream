package com.moxa.dream.util.reflection.wrapper;

import com.moxa.dream.util.common.ObjectUtil;
import com.moxa.dream.util.reflect.ReflectException;
import com.moxa.dream.util.reflect.ReflectUtil;
import com.moxa.dream.util.reflection.factory.BeanObjectFactory;
import com.moxa.dream.util.reflection.factory.ObjectFactory;
import com.moxa.dream.util.reflection.util.PropertyInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanObjectFactoryWrapper implements ObjectFactoryWrapper {
    protected Class type;
    Map<String, PropertyInfo> propertyInfoMap = new HashMap<>();

    public BeanObjectFactoryWrapper(Class type) {
        this.type = type;
        List<Method> methodList = ReflectUtil.findMethod(type);
        List<Field> fieldList = ReflectUtil.findField(type);
        for (Field field : fieldList) {
            PropertyInfo propertyInfo = getPropertyInfo(methodList, field);
            propertyInfoMap.put(field.getName(), propertyInfo);
        }

    }

    protected Object newInstance(Class<?> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    @Override
    public ObjectFactory newObjectFactory(Object target) {
        if (target == null) {
            return new BeanObjectFactory(newInstance(type), this);
        } else {
            return new BeanObjectFactory(target, this);
        }
    }

    protected PropertyInfo getPropertyInfo(List<Method> methodList, Field field) {
        PropertyInfo propertyInfo = new PropertyInfo();
        String fieldName = field.getName();
        propertyInfo.setLabel(fieldName);
        propertyInfo.setField(field);
        Class fieldType = field.getType();
        if (!ObjectUtil.isNull(methodList)) {
            for (Method method : methodList) {
                String name = method.getName();
                if (name.startsWith("set")) {
                    if (name.length() > 3) {
                        name = toLow(name.substring(3));
                        if (fieldName.equals(name) && propertyInfo.getWriteMethod() == null) {
                            Parameter[] parameters = method.getParameters();
                            if (parameters.length == 1) {
                                if (fieldType == parameters[0].getType()) {
                                    propertyInfo.setWriteMethod(method);
                                }
                            }
                        }
                    }
                } else if (name.startsWith("get")) {
                    if (name.length() > 3) {
                        name = toLow(name.substring(3));
                        if (fieldName.equals(name) && propertyInfo.getReadMethod() == null) {
                            Parameter[] parameters = method.getParameters();
                            if (parameters.length == 0) {
                                propertyInfo.setReadMethod(method);
                            }
                        }
                    }
                } else if (name.startsWith("is") && (fieldType == boolean.class || fieldType == Boolean.class)) {
                    if (name.length() > 2) {
                        name = toLow(name.substring(2));
                        if (fieldName.equals(name) && propertyInfo.getReadMethod() == null) {
                            Parameter[] parameters = method.getParameters();
                            if (parameters.length == 0) {
                                propertyInfo.setReadMethod(method);
                            }
                        }
                    }
                }
                if (propertyInfo.getReadMethod() != null && propertyInfo.getWriteMethod() != null) {
                    break;
                }
            }
        }
        return propertyInfo;
    }

    protected String toLow(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public void set(Object result, String property, Object value) {
        PropertyInfo propertyInfo = propertyInfoMap.get(property);
        Method writeMethod = propertyInfo.getWriteMethod();
        if (writeMethod != null) {
            try {
                writeMethod.invoke(result, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Field field = propertyInfo.getField();
            try {
                field.set(result, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Object get(Object result, String property) {
        PropertyInfo propertyInfo = propertyInfoMap.get(property);
        Method readMethod = propertyInfo.getReadMethod();
        if (readMethod != null) {
            try {
                return readMethod.invoke(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Field field = propertyInfo.getField();
        try {
            return field.get(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}