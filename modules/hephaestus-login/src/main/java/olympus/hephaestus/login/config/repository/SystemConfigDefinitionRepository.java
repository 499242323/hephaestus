package olympus.hephaestus.login.config.repository;

import olympus.hephaestus.login.config.domain.SystemConfigDefinitionEntity;
import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SystemConfigDefinitionRepository extends BaseAbstractRepository<SystemConfigDefinitionEntity, Long> {

    @Select("""
            SELECT id,
                   config_code AS configCode,
                   config_name AS configName,
                   config_group AS configGroup,
                   config_tab AS configTab,
                   section_code AS sectionCode,
                   section_name AS sectionName,
                   component_type AS componentType,
                   default_value AS defaultValue,
                   options_json AS optionsJson,
                   placeholder_text AS placeholderText,
                   help_text AS helpText,
                   required_flag AS requiredFlag,
                   public_flag AS publicFlag,
                   sensitive_flag AS sensitiveFlag,
                   enabled_flag AS enabledFlag,
                   sort_order AS sortOrder,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_config_definition
            WHERE config_group = #{groupCode}
              AND enabled_flag = 1
            ORDER BY sort_order, id
            """)
    List<SystemConfigDefinitionEntity> findEnabledByGroup(@Param("groupCode") String groupCode);
}
