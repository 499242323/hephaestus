package olympus.hephaestus.org.role.repository;

import olympus.hephaestus.org.role.domain.OrgRolePermissionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgRolePermissionRepository {

    @Select("""
            <script>
            SELECT role_id AS roleId,
                   permission_id AS permissionId,
                   permission_code AS permissionCode
            FROM heph_role_permission
            WHERE role_id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
            ORDER BY role_id, permission_id
            </script>
            """)
    List<OrgRolePermissionEntity> findByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Delete("DELETE FROM heph_role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT role_id AS roleId,
                   permission_id AS permissionId,
                   permission_code AS permissionCode
            FROM heph_role_permission
            WHERE role_id = #{roleId}
            ORDER BY permission_id
            """)
    List<OrgRolePermissionEntity> findByRoleId(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO heph_role_permission (role_id, permission_id, permission_code)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.roleId}, #{item.permissionId}, #{item.permissionCode})
            </foreach>
            </script>
            """)
    void insertBatch(@Param("items") List<OrgRolePermissionEntity> items);
}
