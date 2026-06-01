package com.example.springaidemo.org.role.repository;

import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import com.example.springaidemo.org.role.domain.OrgPersonPermissionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgPersonPermissionRepository extends BaseAbstractRepository<OrgPersonPermissionEntity, Long> {

    @Delete("DELETE FROM heph_person_permission WHERE person_id = #{personId}")
    void deleteByPersonId(@Param("personId") Long personId);

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
}
