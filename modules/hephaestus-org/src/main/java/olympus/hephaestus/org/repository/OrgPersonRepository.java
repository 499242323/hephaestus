package olympus.hephaestus.org.repository;

import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import olympus.hephaestus.org.domain.OrgPersonListRow;
import olympus.hephaestus.org.entity.OrgPersonEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrgPersonRepository extends BaseAbstractRepository<OrgPersonEntity, Long> {

    @Select("""
            <script>
            SELECT p.id,
                   p.person_code AS personCode,
                   p.person_name AS personName,
                   p.username,
                   p.password,
                   p.unit_id AS unitId,
                   u.unit_name AS unitName,
                   p.avatar_media_id AS avatarMediaId,
                   m.access_url AS avatarAccessUrl,
                   p.mobile,
                   p.email,
                   p.source_type AS sourceType,
                   p.remark,
                   p.enabled
            FROM heph_person cp
            JOIN heph_unit cu ON cu.id = cp.unit_id
            JOIN heph_unit u ON (u.ancestor_path = cu.ancestor_path OR u.ancestor_path LIKE CONCAT(cu.ancestor_path, '/%'))
            JOIN heph_person p ON p.unit_id = u.id
            LEFT JOIN spring_ai_media_file m ON m.id = p.avatar_media_id
            WHERE cp.id = #{currentPersonId}
            <if test="personName != null and personName != ''">
                AND p.person_name LIKE CONCAT('%', #{personName}, '%')
            </if>
            <if test="unitId != null">
                AND p.unit_id = #{unitId}
            </if>
            <if test="enabled != null">
                AND p.enabled = #{enabled}
            </if>
            ORDER BY p.unit_id, p.id
            </script>
            """)
    List<OrgPersonListRow> findListRowsByCurrentScope(@Param("currentPersonId") Long currentPersonId,
                                                      @Param("personName") String personName,
                                                      @Param("unitId") Long unitId,
                                                      @Param("enabled") Boolean enabled);

    @Select("""
            SELECT id,
                   person_code AS personCode,
                   person_name AS personName,
                   username,
                   password,
                   unit_id AS unitId,
                   avatar_media_id AS avatarMediaId,
                   mobile,
                   email,
                   source_type AS sourceType,
                   remark,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_person
            WHERE person_code = #{personCode}
            """)
    OrgPersonEntity getByPersonCode(@Param("personCode") String personCode);

    @Select("""
            SELECT id,
                   person_code AS personCode,
                   person_name AS personName,
                   username,
                   password,
                   unit_id AS unitId,
                   avatar_media_id AS avatarMediaId,
                   mobile,
                   email,
                   source_type AS sourceType,
                   remark,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_person
            WHERE username = #{username}
            """)
    OrgPersonEntity getByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) FROM heph_person WHERE email = #{email}")
    long countByEmail(@Param("email") String email);

    @Select("""
            SELECT p.id,
                   p.person_code AS personCode,
                   p.person_name AS personName,
                   p.username,
                   p.password,
                   p.unit_id AS unitId,
                   u.unit_name AS unitName,
                   p.avatar_media_id AS avatarMediaId,
                   m.access_url AS avatarAccessUrl,
                   p.mobile,
                   p.email,
                   p.source_type AS sourceType,
                   p.remark,
                   p.enabled
            FROM heph_person p
            LEFT JOIN heph_unit u ON u.id = p.unit_id
            LEFT JOIN spring_ai_media_file m ON m.id = p.avatar_media_id
            WHERE p.email = #{email}
              AND p.enabled = 1
            ORDER BY p.id
            """)
    List<OrgPersonListRow> findResetAccountsByEmail(@Param("email") String email);

    @Select("""
            SELECT id,
                   person_code AS personCode,
                   person_name AS personName,
                   username,
                   password,
                   unit_id AS unitId,
                   avatar_media_id AS avatarMediaId,
                   mobile,
                   email,
                   source_type AS sourceType,
                   remark,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_person
            WHERE email = #{email}
              AND username = #{username}
            """)
    OrgPersonEntity getByEmailAndUsername(@Param("email") String email, @Param("username") String username);

    @Select("SELECT COUNT(1) FROM heph_person WHERE unit_id = #{unitId}")
    long countByUnitId(@Param("unitId") Long unitId);

    @Update("UPDATE heph_person SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE heph_person SET avatar_media_id = #{avatarMediaId} WHERE id = #{id}")
    void updateAvatarMediaId(@Param("id") Long id, @Param("avatarMediaId") Long avatarMediaId);

    @Update("UPDATE heph_person SET avatar_media_id = NULL WHERE id = #{id}")
    void clearAvatarMediaId(@Param("id") Long id);
}
