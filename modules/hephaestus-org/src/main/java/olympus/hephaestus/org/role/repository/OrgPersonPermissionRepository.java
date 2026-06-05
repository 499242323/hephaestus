package olympus.hephaestus.org.role.repository;

import olympus.hephaestus.org.role.domain.OrgPersonPermissionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface OrgPersonPermissionRepository {

    @Delete("DELETE FROM heph_person_permission WHERE person_id = #{personId}")
    void deleteByPersonId(@Param("personId") Long personId);

    @Delete("""
            <script>
            DELETE FROM heph_person_permission
            WHERE person_id IN
            <foreach collection="personIds" item="personId" open="(" separator="," close=")">
                #{personId}
            </foreach>
            </script>
            """)
    void deleteByPersonIds(@Param("personIds") Collection<Long> personIds);

    @Insert("""
            <script>
            INSERT INTO heph_person_permission (person_id, permission_id, permission_code)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.personId}, #{item.permissionId}, #{item.permissionCode})
            </foreach>
            </script>
            """)
    void insertBatch(@Param("items") List<OrgPersonPermissionEntity> items);

    @Select("""
            SELECT person_id AS personId,
                   permission_id AS permissionId,
                   permission_code AS permissionCode
            FROM heph_person_permission
            WHERE person_id = #{personId}
            ORDER BY permission_code
            """)
    List<OrgPersonPermissionEntity> findByPersonId(@Param("personId") Long personId);

    @Select("""
            <script>
            SELECT pr.person_id AS personId,
                   rp.permission_id AS permissionId,
                   rp.permission_code AS permissionCode
            FROM heph_person_role pr
            JOIN heph_role_permission rp ON rp.role_id = pr.role_id
            WHERE pr.person_id IN
            <foreach collection="personIds" item="personId" open="(" separator="," close=")">
                #{personId}
            </foreach>
            ORDER BY pr.person_id, rp.permission_code, rp.permission_id
            </script>
            """)
    List<OrgPersonPermissionEntity> findRolePermissionsByPersonIds(@Param("personIds") Collection<Long> personIds);
}
