package olympus.hephaestus.login.config.repository;

import olympus.hephaestus.login.config.domain.SystemConfigValueEntity;
import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SystemConfigValueRepository extends BaseAbstractRepository<SystemConfigValueEntity, Long> {

    @Select("""
            <script>
            SELECT id,
                   config_code AS configCode,
                   config_value AS configValue,
                   updated_by AS updatedBy,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM sys_config_value
            WHERE config_code IN
            <foreach collection="codes" item="code" open="(" separator="," close=")">
                #{code}
            </foreach>
            </script>
            """)
    List<SystemConfigValueEntity> findByCodes(@Param("codes") Collection<String> codes);

    @Insert("""
            INSERT INTO sys_config_value (config_code, config_value, updated_by, updated_at)
            VALUES (#{configCode}, #{configValue}, #{updatedBy}, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                config_value = VALUES(config_value),
                updated_by = VALUES(updated_by),
                updated_at = CURRENT_TIMESTAMP
            """)
    void upsert(SystemConfigValueEntity entity);
}
