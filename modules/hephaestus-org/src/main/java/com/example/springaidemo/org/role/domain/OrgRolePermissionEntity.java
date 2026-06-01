package com.example.springaidemo.org.role.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("heph_role_permission")
public class OrgRolePermissionEntity {

    @TableField("role_id")
    private Long roleId;

    @TableField("permission_id")
    private Long permissionId;

    @TableField("permission_code")
    private String permissionCode;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}
