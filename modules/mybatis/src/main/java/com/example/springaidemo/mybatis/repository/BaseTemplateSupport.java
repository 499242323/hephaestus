package com.example.springaidemo.mybatis.repository;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class BaseTemplateSupport {

    private BaseTemplateSupport() {
    }

    static Class<?> resolveEntityType(ProviderContext context) {
        Class<?> mapperType = context.getMapperType();
        for (Type type : mapperType.getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?> && BaseAbstractRepository.class.isAssignableFrom((Class<?>) rawType)) {
                    Type entityType = parameterizedType.getActualTypeArguments()[0];
                    if (entityType instanceof Class<?>) {
                        return (Class<?>) entityType;
                    }
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

    static final class ColumnField {

        private final String fieldName;
        private final String columnName;

        ColumnField(String fieldName, String columnName) {
            this.fieldName = fieldName;
            this.columnName = columnName;
        }

        String fieldName() {
            return fieldName;
        }

        String columnName() {
            return columnName;
        }
    }
}
