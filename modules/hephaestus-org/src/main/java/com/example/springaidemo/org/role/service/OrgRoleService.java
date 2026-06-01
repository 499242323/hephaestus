package com.example.springaidemo.org.role.service;

import com.example.springaidemo.org.role.dto.OrgRoleRequest;
import com.example.springaidemo.org.role.dto.OrgRoleResponse;
import com.example.springaidemo.org.role.dto.OrgRoleTreeNode;
import com.example.springaidemo.org.role.dto.UpdateRolePeopleRequest;

import java.util.List;

public interface OrgRoleService {

    List<OrgRoleTreeNode> getRoleTree(Long currentPersonId);

    List<OrgRoleResponse> listRoles(Long currentPersonId, Long unitId, String keyword, Boolean enabled);

    OrgRoleResponse getRole(Long currentPersonId, Long roleId);

    OrgRoleResponse createRole(Long currentPersonId, OrgRoleRequest request);

    OrgRoleResponse updateRole(Long currentPersonId, Long roleId, OrgRoleRequest request);

    void deleteRole(Long currentPersonId, Long roleId);

    OrgRoleResponse updateRolePeople(Long currentPersonId, Long roleId, UpdateRolePeopleRequest request);

    void refreshRolePeoplePermissions(Long currentPersonId, Long roleId);
}
