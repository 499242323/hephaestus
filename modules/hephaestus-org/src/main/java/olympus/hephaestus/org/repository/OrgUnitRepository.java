package olympus.hephaestus.org.repository;

import olympus.hephaestus.mybatis.repository.BaseAbstractRepository;
import olympus.hephaestus.org.entity.OrgUnitEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgUnitRepository extends BaseAbstractRepository<OrgUnitEntity, Long> {

    @Select("""
            SELECT id,
                   unit_code AS unitCode,
                   unit_name AS unitName,
                   parent_id AS parentId,
                   ancestor_path AS ancestorPath,
                   sort_order AS sortOrder,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_unit
            ORDER BY ancestor_path, sort_order, id
            """)
    List<OrgUnitEntity> findAllOrdered();

    @Select("""
            SELECT id,
                   unit_code AS unitCode,
                   unit_name AS unitName,
                   parent_id AS parentId,
                   ancestor_path AS ancestorPath,
                   sort_order AS sortOrder,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_unit
            WHERE unit_code = #{unitCode}
            """)
    OrgUnitEntity getByUnitCode(@Param("unitCode") String unitCode);

    @Select("""
            SELECT id,
                   unit_code AS unitCode,
                   unit_name AS unitName,
                   parent_id AS parentId,
                   ancestor_path AS ancestorPath,
                   sort_order AS sortOrder,
                   enabled,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM heph_unit
            WHERE ancestor_path = #{ancestorPath}
               OR ancestor_path LIKE #{descendantPattern}
            ORDER BY ancestor_path, sort_order, id
            """)
    List<OrgUnitEntity> findSelfAndDescendants(@Param("ancestorPath") String ancestorPath,
                                               @Param("descendantPattern") String descendantPattern);
}
