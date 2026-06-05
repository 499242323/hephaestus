package olympus.hephaestus.org.role.repository;

import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import olympus.hephaestus.org.role.domain.OrgPermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgPermissionRepository extends BaseAbstractRepository<OrgPermissionEntity, Long> {

    @Select("""
            SELECT id,
                   permission_code AS permissionCode,
                   permission_name AS permissionName,
                   permission_group AS permissionGroup,
                   permission_section AS permissionSection,
                   sort_order AS sortOrder,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_permission
            WHERE enabled = 1
            ORDER BY permission_group, permission_section, sort_order, id
            """)
    List<OrgPermissionEntity> findEnabled();

    @Select("""
            <script>
            SELECT id,
                   permission_code AS permissionCode,
                   permission_name AS permissionName,
                   permission_group AS permissionGroup,
                   permission_section AS permissionSection,
                   sort_order AS sortOrder,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_permission
            WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            ORDER BY sort_order, id
            </script>
            """)
    List<OrgPermissionEntity> findByIds(@Param("ids") List<Long> ids);
}
