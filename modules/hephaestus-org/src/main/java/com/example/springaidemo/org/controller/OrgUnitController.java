package com.example.springaidemo.org.controller;

import com.example.springaidemo.org.domain.OrgUnitTreeNode;
import com.example.springaidemo.org.dto.CreateOrgUnitRequest;
import com.example.springaidemo.org.dto.UpdateOrgUnitRequest;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import com.example.springaidemo.org.service.OrgUnitService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org/units")
public class OrgUnitController {

    private final OrgUnitService orgUnitService;
    private final OrgPermissionGuard permissionGuard;

    public OrgUnitController(OrgUnitService orgUnitService,
                             OrgPermissionGuard permissionGuard) {
        this.orgUnitService = orgUnitService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/tree")
    public List<OrgUnitTreeNode> getTree(@RequestHeader("X-Person-Id") Long personId) {
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_VIEW);
        return orgUnitService.getUnitTree(personId);
    }

    @PostMapping
    public OrgUnitEntity create(@RequestHeader("X-Person-Id") Long personId,
                                @RequestBody CreateOrgUnitRequest request) {
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_CREATE);
        return orgUnitService.createUnit(personId, request);
    }

    @PutMapping("/{id}")
    public OrgUnitEntity update(@RequestHeader("X-Person-Id") Long personId,
                                @PathVariable("id") Long id,
                                @RequestBody UpdateOrgUnitRequest request) {
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_UPDATE);
        return orgUnitService.updateUnit(personId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("X-Person-Id") Long personId,
                       @PathVariable("id") Long id) {
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_DELETE);
        orgUnitService.deleteUnit(personId, id);
    }
}
