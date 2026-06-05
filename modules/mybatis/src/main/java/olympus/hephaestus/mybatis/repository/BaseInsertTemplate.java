package olympus.hephaestus.mybatis.repository;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import org.apache.ibatis.builder.annotation.ProviderContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BaseInsertTemplate {

    private BaseInsertTemplate() {
    }

    public static String dynamicSQL(Object parameterObject, ProviderContext context) {
        Class<?> entityType = BaseTemplateSupport.resolveEntityType(context);
        String tableName = BaseTemplateSupport.resolveTableName(entityType);
        List<BaseTemplateSupport.ColumnField> fields = resolveInsertableFields(entityType);

        StringBuilder sql = new StringBuilder();
        sql.append("<script>");
        sql.append("INSERT INTO ").append(tableName).append(" (");
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append(fields.get(index).columnName());
        }
        sql.append(") VALUES ");
        sql.append("<foreach collection=\"_list\" item=\"item\" separator=\",\">(");
        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("#{item.").append(fields.get(index).fieldName()).append("}");
        }
        sql.append(")</foreach>");
        sql.append("</script>");
        return sql.toString();
    }

    private static List<BaseTemplateSupport.ColumnField> resolveInsertableFields(Class<?> entityType) {
        List<BaseTemplateSupport.ColumnField> fields = new ArrayList<>();
        for (Field field : entityType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId != null) {
                if (tableId.type() == IdType.AUTO) {
                    continue;
                }
                fields.add(new BaseTemplateSupport.ColumnField(field.getName(), tableId.value().isBlank() ? field.getName() : tableId.value()));
                continue;
            }

            if (isImplicitAutoId(field)) {
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
            throw new IllegalStateException("No insertable fields found for entity: " + entityType.getName());
        }
        return fields;
    }

    private static boolean isImplicitAutoId(Field field) {
        return "id".equals(field.getName());
    }
}
