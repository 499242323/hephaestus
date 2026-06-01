package com.example.springaidemo.org.role.repository;

import com.example.springaidemo.mybatis.repository.BaseAbstractRepository;
import com.example.springaidemo.org.role.domain.OrgRoleTypeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgRoleTypeRepository extends BaseAbstractRepository<OrgRoleTypeEntity, Long> {

    @Select("""
            SELECT id,
                   type_code AS typeCode,
                   type_name AS typeName,
                   admin_flag AS adminFlag,
                   sort_order AS sortOrder,
                   enabled
            FROM heph_role_type
            WHERE enabled = 1
            ORDER BY sort_order, id
            """)
    List<OrgRoleTypeEntity> findEnabled();
}
