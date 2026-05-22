package com.example.springaidemo.mybatis.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class BaseUpdateTemplate {

    private BaseUpdateTemplate() {
    }

    public static String updateById(Object parameterObject, ProviderContext context) {
        Class<?> entityType = BaseTemplateSupport.resolveEntityType(context);
        String tableName = BaseTemplateSupport.resolveTableName(entityType);
        BaseTemplateSupport.ColumnField idField = resolveIdField(entityType);
        List<BaseTemplateSupport.ColumnField> updatableFields = resolveUpdatableFields(entityType, idField.fieldName());

        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("UPDATE ").append(tableName).append(" <set>");
        for (BaseTemplateSupport.ColumnField field : updatableFields) {
            sql.append("<if test=\"").append(field.fieldName()).append(" != null\">");
            sql.append(field.columnName()).append(" = #{").append(field.fieldName()).append("},");
            sql.append("</if>");
        }
        sql.append("</set>");
        sql.append(" WHERE ").append(idField.columnName()).append(" = #{").append(idField.fieldName()).append("}");
        sql.append("</script>");
        return sql.toString();
    }

    private static BaseTemplateSupport.ColumnField resolveIdField(Class<?> entityType) {
        Field implicitIdField = null;
        for (Field field : entityType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId != null) {
                String columnName = tableId.value().isBlank() ? field.getName() : tableId.value();
                return new BaseTemplateSupport.ColumnField(field.getName(), columnName);
            }

            if ("id".equals(field.getName())) {
                implicitIdField = field;
            }
        }

        if (implicitIdField != null) {
            TableField tableField = implicitIdField.getAnnotation(TableField.class);
            String columnName = tableField != null && !tableField.value().isBlank()
                    ? tableField.value()
                    : implicitIdField.getName();
            return new BaseTemplateSupport.ColumnField(implicitIdField.getName(), columnName);
        }

        throw new IllegalStateException("Missing id field on entity: " + entityType.getName());
    }

    private static List<BaseTemplateSupport.ColumnField> resolveUpdatableFields(Class<?> entityType, String idFieldName) {
        List<BaseTemplateSupport.ColumnField> fields = new ArrayList<>();
        for (Field field : entityType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic() || idFieldName.equals(field.getName())) {
                continue;
            }

            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId != null && tableId.type() == IdType.AUTO) {
                continue;
            }

            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }
            String columnName = tableField != null && !tableField.value().isBlank()
                    ? tableField.value()
                    : field.getName();
            fields.add(new BaseTemplateSupport.ColumnField(field.getName(), columnName));
        }

        if (fields.isEmpty()) {
            throw new IllegalStateException("No updatable fields found for entity: " + entityType.getName());
        }
        return fields;
    }
}
