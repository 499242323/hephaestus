package com.example.springaidemo.mybatis.repository;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.annotation.ProviderContext;

public final class BaseTemplateSupport {

    private BaseTemplateSupport() {
    }

    static Class<?> resolveEntityType(ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        for (java.lang.reflect.Type type : mapperType.getGenericInterfaces()) {
            if (type instanceof java.lang.reflect.ParameterizedType parameterizedType
                    && parameterizedType.getRawType() instanceof Class<?> rawType
                    && BaseAbstractRepository.class.isAssignableFrom(rawType)) {
                java.lang.reflect.Type entityType = parameterizedType.getActualTypeArguments()[0];
                if (entityType instanceof Class<?> entityClass) {
                    return entityClass;
                }
            }
        }
        throw new IllegalStateException("Cannot resolve entity type for mapper: " + mapperType.getName());
    }

    static String resolveTableName(Class<?> entityType) {
        TableName tableName = entityType.getAnnotation(TableName.class);
        if (tableName != null && !tableName.value().isBlank()) {
            return tableName.value();
        }
        throw new IllegalStateException("Missing @TableName on entity: " + entityType.getName());
    }

    record ColumnField(String fieldName, String columnName) {
    }
}
