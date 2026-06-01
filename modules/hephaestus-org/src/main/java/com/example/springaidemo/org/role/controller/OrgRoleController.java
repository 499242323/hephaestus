package com.example.springaidemo.org.role.controller;

import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;
import com.example.springaidemo.org.role.dto.OrgRoleRequest;
import com.example.springaidemo.org.role.dto.OrgRoleResponse;
import com.example.springaidemo.org.role.dto.OrgRoleTypeItem;
import com.example.springaidemo.org.role.dto.OrgRoleTreeNode;
import com.example.springaidemo.org.role.dto.UpdatePersonRolesRequest;
import com.example.springaidemo.org.role.dto.UpdateRolePeopleRequest;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.repository.OrgRoleTypeRepository;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import com.example.springaidemo.org.role.service.OrgPersonRoleService;
import com.example.springaidemo.org.role.service.OrgRoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org")
public class OrgRoleController {

    private final OrgRoleService orgRoleService;
    private final OrgPersonRoleService orgPersonRoleService;
    private final OrgRoleTypeRepository orgRoleTypeRepository;
    private final OrgPermissionGuard orgPermissionGuard;

    public OrgRoleController(OrgRoleService orgRoleService,
                             OrgPersonRoleService orgPersonRoleService,
                             OrgRoleTypeRepository orgRoleTypeRepository,
                             OrgPermissionGuard orgPermissionGuard) {
        this.orgRoleService = orgRoleService;
        this.orgPersonRoleService = orgPersonRoleService;
        this.orgRoleTypeRepository = orgRoleTypeRepository;
        this.orgPermissionGuard = orgPermissionGuard;
    }

    @GetMapping("/roles/tree")
    public List<OrgRoleTreeNode> getRoleTree(@RequestHeader("X-Person-Id") Long personId) {
        return orgRoleService.getRoleTree(personId);
    }

    @GetMapping("/role-types")
    public List<OrgRoleTypeItem> listRoleTypes(@RequestHeader("X-Person-Id") Long personId) {
        orgPermissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_ROLE_VIEW);
        return orgRoleTypeRepository.findEnabled().stream()
                .map(item -> new OrgRoleTypeItem(
                        item.getTypeCode(),
                        item.getTypeName(),
                        item.getAdminFlag(),
                        item.getSortOrder()
                ))
                .toList();
    }

    @GetMapping("/roles")
    public List<OrgRoleResponse> listRoles(@RequestHeader("X-Person-Id") Long personId,
                                           @RequestParam(value = "unitId", required = false) Long unitId,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return orgRoleService.listRoles(personId, unitId, keyword, enabled);
    }

    @GetMapping("/roles/{id}")
    public OrgRoleResponse getRole(@RequestHeader("X-Person-Id") Long personId,
                                   @PathVariable("id") Long id) {
        return orgRoleService.getRole(personId, id);
    }

    @PostMapping("/roles")
    public OrgRoleResponse createRole(@RequestHeader("X-Person-Id") Long personId,
                                      @RequestBody OrgRoleRequest request) {
        return orgRoleService.createRole(personId, request);
    }

    @PutMapping("/roles/{id}")
    public OrgRoleResponse updateRole(@RequestHeader("X-Person-Id") Long personId,
                                      @PathVariable("id") Long id,
                                      @RequestBody OrgRoleRequest request) {
        return orgRoleService.updateRole(personId, id, request);
    }

    @DeleteMapping("/roles/{id}")
    public void deleteRole(@RequestHeader("X-Person-Id") Long personId,
                           @PathVariable("id") Long id) {
        orgRoleService.deleteRole(personId, id);
    }

    @PutMapping("/roles/{id}/people")
    public OrgRoleResponse updateRolePeople(@RequestHeader("X-Person-Id") Long personId,
                                            @PathVariable("id") Long id,
                                            @RequestBody UpdateRolePeopleRequest request) {
        return orgRoleService.updateRolePeople(personId, id, request);
    }

    @PostMapping("/roles/{id}/refresh-permissions")
    public void refreshRolePeoplePermissions(@RequestHeader("X-Person-Id") Long personId,
                                             @PathVariable("id") Long id) {
        orgRoleService.refreshRolePeoplePermissions(personId, id);
    }

    @GetMapping("/persons/{id}/roles")
    public List<OrgPersonRoleItem> listPersonRoles(@RequestHeader("X-Person-Id") Long personId,
                                                   @PathVariable("id") Long id) {
        return orgPersonRoleService.listPersonRoles(personId, id);
    }

    @PutMapping("/persons/{id}/roles")
    public List<OrgPersonRoleItem> updatePersonRoles(@RequestHeader("X-Person-Id") Long personId,
                                                     @PathVariable("id") Long id,
                                                     @RequestBody UpdatePersonRolesRequest request) {
        return orgPersonRoleService.replacePersonRoles(personId, id, request);
    }
}
