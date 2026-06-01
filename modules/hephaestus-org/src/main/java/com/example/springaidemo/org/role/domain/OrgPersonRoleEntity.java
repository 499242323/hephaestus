package com.example.springaidemo.org.role.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("heph_person_role")
public class OrgPersonRoleEntity {

    @TableField("person_id")
    private Long personId;

    @TableField("role_id")
    private Long roleId;

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
