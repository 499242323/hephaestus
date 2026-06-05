package olympus.hephaestus.org.role.service;

import olympus.hephaestus.org.role.dto.OrgRoleRequest;
import olympus.hephaestus.org.role.dto.OrgRoleResponse;
import olympus.hephaestus.org.role.dto.OrgRoleTreeNode;
import olympus.hephaestus.org.role.dto.UpdateRolePeopleRequest;

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
