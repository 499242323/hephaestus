package olympus.hephaestus.org.role.repository;

import olympus.hephaestus.org.role.domain.OrgPersonRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgPersonRoleRepository {

    @Select("SELECT role_id FROM heph_person_role WHERE person_id = #{personId} ORDER BY role_id")
    List<Long> findRoleIdsByPersonId(@Param("personId") Long personId);

    @Select("SELECT person_id FROM heph_person_role WHERE role_id = #{roleId} ORDER BY person_id")
    List<Long> findPersonIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM heph_person_role WHERE person_id = #{personId}")
    void deleteByPersonId(@Param("personId") Long personId);

    @Delete("DELETE FROM heph_person_role WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO heph_person_role (person_id, role_id)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.personId}, #{item.roleId})
            </foreach>
            </script>
            """)
    void insertBatch(@Param("items") List<OrgPersonRoleEntity> items);
}
