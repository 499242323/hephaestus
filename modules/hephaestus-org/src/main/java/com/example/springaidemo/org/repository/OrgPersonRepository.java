package com.example.springaidemo.org.repository;

import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrgPersonRepository extends BaseAbstractRepository<OrgPersonEntity, Long> {

    @Select("""
            <script>
            SELECT id,
                   person_code AS personCode,
                   person_name AS personName,
                   username,
                   password,
                   unit_id AS unitId,
                   avatar_media_id AS avatarMediaId,
                   mobile,
                   email,
                   remark,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_person
            WHERE unit_id IN
            <foreach collection="unitIds" item="unitId" open="(" separator="," close=")">
                #{unitId}
            </foreach>
            <if test="personName != null and personName != ''">
                AND person_name LIKE CONCAT('%', #{personName}, '%')
            </if>
            <if test="unitId != null">
                AND unit_id = #{unitId}
            </if>
            <if test="enabled != null">
                AND enabled = #{enabled}
            </if>
            ORDER BY unit_id, id
            </script>
            """)
    List<OrgPersonEntity> findByScope(@Param("unitIds") List<Long> unitIds,
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
                   remark,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_person
            WHERE person_code = #{personCode}
            """)
    OrgPersonEntity getByPersonCode(@Param("personCode") String personCode);

    @Select("SELECT COUNT(1) FROM heph_person WHERE unit_id = #{unitId}")
    long countByUnitId(@Param("unitId") Long unitId);

    @Update("UPDATE heph_person SET avatar_media_id = #{avatarMediaId} WHERE id = #{id}")
    void updateAvatarMediaId(@Param("id") Long id, @Param("avatarMediaId") Long avatarMediaId);

    @Update("UPDATE heph_person SET avatar_media_id = NULL WHERE id = #{id}")
    void clearAvatarMediaId(@Param("id") Long id);
}
