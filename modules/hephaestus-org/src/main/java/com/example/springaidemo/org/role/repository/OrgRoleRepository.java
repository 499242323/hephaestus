package com.example.springaidemo.org.role.repository;

import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import com.example.springaidemo.org.role.domain.OrgRoleEntity;
import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;
import com.example.springaidemo.org.role.dto.OrgPersonRoleSummaryRow;
import com.example.springaidemo.org.role.dto.OrgRolePersonItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgRoleRepository extends BaseAbstractRepository<OrgRoleEntity, Long> {

    @Select("""
            <script>
            SELECT id,
                   role_code AS roleCode,
                   role_name AS roleName,
                   role_short_name AS roleShortName,
                   role_desc AS roleDesc,
                   unit_id AS unitId,
                   role_type AS roleType,
                   role_group AS roleGroup,
                   role_property AS roleProperty,
                   enabled,
                   sort_order AS sortOrder,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_role
            WHERE unit_id IN
            <foreach collection="unitIds" item="unitId" open="(" separator="," close=")">
                #{unitId}
            </foreach>
            <if test="unitId != null">
                AND unit_id = #{unitId}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (role_name LIKE CONCAT('%', #{keyword}, '%')
                     OR role_code LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="enabled != null">
                AND enabled = #{enabled}
            </if>
            ORDER BY unit_id, sort_order, id
            </script>
            """)
    List<OrgRoleEntity> findByScope(@Param("unitIds") List<Long> unitIds,
                                    @Param("unitId") Long unitId,
                                    @Param("keyword") String keyword,
                                    @Param("enabled") Boolean enabled);

    @Select("""
            SELECT id,
                   role_code AS roleCode,
                   role_name AS roleName,
                   role_short_name AS roleShortName,
                   role_desc AS roleDesc,
                   unit_id AS unitId,
                   role_type AS roleType,
                   role_group AS roleGroup,
                   role_property AS roleProperty,
                   enabled,
                   sort_order AS sortOrder,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_role
            WHERE role_code = #{roleCode}
            """)
    OrgRoleEntity getByRoleCode(@Param("roleCode") String roleCode);

    @Select("""
            SELECT p.id,
                   p.person_code AS personCode,
                   p.person_name AS personName,
                   p.username,
                   p.unit_id AS unitId,
                   u.unit_name AS unitName
            FROM heph_person_role pr
            JOIN heph_person p ON p.id = pr.person_id
            LEFT JOIN heph_unit u ON u.id = p.unit_id
            WHERE pr.role_id = #{roleId}
            ORDER BY p.unit_id, p.id
            """)
    List<OrgRolePersonItem> findPeopleByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT r.id,
                   r.role_code AS roleCode,
                   r.role_name AS roleName,
                   r.unit_id AS unitId,
                   u.unit_name AS unitName
            FROM heph_person_role pr
            JOIN heph_role r ON r.id = pr.role_id
            LEFT JOIN heph_unit u ON u.id = r.unit_id
            WHERE pr.person_id = #{personId}
            ORDER BY r.unit_id, r.sort_order, r.id
            """)
    List<OrgPersonRoleItem> findRolesByPersonId(@Param("personId") Long personId);

    @Select("""
            <script>
            SELECT pr.person_id AS personId,
                   r.id,
                   r.role_code AS roleCode,
                   r.role_name AS roleName,
                   r.unit_id AS unitId,
                   u.unit_name AS unitName
            FROM heph_person_role pr
            JOIN heph_role r ON r.id = pr.role_id
            LEFT JOIN heph_unit u ON u.id = r.unit_id
            WHERE pr.person_id IN
            <foreach collection="personIds" item="personId" open="(" separator="," close=")">
                #{personId}
            </foreach>
            ORDER BY pr.person_id, r.unit_id, r.sort_order, r.id
            </script>
            """)
    List<OrgPersonRoleSummaryRow> findRolesByPersonIds(@Param("personIds") List<Long> personIds);

    @Select("""
            SELECT CASE WHEN COUNT(1) > 0 THEN TRUE ELSE FALSE END
            FROM heph_person_role pr
            JOIN heph_role r ON r.id = pr.role_id
            WHERE pr.person_id = #{personId}
              AND r.role_type = #{roleType}
              AND r.enabled = TRUE
            """)
    boolean existsEnabledRoleTypeByPersonId(@Param("personId") Long personId,
                                            @Param("roleType") String roleType);
}
