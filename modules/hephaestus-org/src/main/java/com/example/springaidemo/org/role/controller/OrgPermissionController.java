package com.example.springaidemo.org.role.controller;

import com.example.springaidemo.org.role.domain.OrgPersonPermissionEntity;
import com.example.springaidemo.org.role.dto.OrgCurrentPermissionResponse;
import com.example.springaidemo.org.role.dto.OrgPermissionItem;
import com.example.springaidemo.org.role.repository.OrgPersonPermissionRepository;
import com.example.springaidemo.org.role.repository.OrgPermissionRepository;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import com.example.springaidemo.org.service.OrgScopeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org")
public class OrgPermissionController {

    private final OrgPermissionRepository permissionRepository;
    private final OrgPersonPermissionRepository personPermissionRepository;
    private final OrgPermissionGuard permissionGuard;
    private final OrgScopeService orgScopeService;

    public OrgPermissionController(OrgPermissionRepository permissionRepository,
                                   OrgPersonPermissionRepository personPermissionRepository,
                                   OrgPermissionGuard permissionGuard,
                                   OrgScopeService orgScopeService) {
        this.permissionRepository = permissionRepository;
        this.personPermissionRepository = personPermissionRepository;
        this.permissionGuard = permissionGuard;
        this.orgScopeService = orgScopeService;
    }

    @GetMapping("/permissions")
    public List<OrgPermissionItem> listPermissions() {
        return permissionRepository.findEnabled().stream()
                .map(permission -> new OrgPermissionItem(
                        permission.getId(),
                        permission.getPermissionCode(),
                        permission.getPermissionName(),
                        permission.getPermissionGroup(),
                        permission.getPermissionSection(),
                        permission.getSortOrder()
                ))
                .toList();
    }

    @GetMapping("/persons/{id}/permissions")
    public List<OrgPersonPermissionEntity> listPersonPermissions(@RequestHeader("X-Person-Id") Long personId,
                                                                 @PathVariable("id") Long id) {
        orgScopeService.requirePersonInScope(personId, id);
        return personPermissionRepository.findByPersonId(id);
    }

    @GetMapping("/permissions/current")
    public OrgCurrentPermissionResponse getCurrentPermissions(@RequestHeader("X-Person-Id") Long personId) {
        return new OrgCurrentPermissionResponse(
                permissionGuard.isAdmin(personId),
                permissionGuard.listPermissionCodes(personId)
        );
    }
}
