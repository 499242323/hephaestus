package olympus.hephaestus.org.controller;

import olympus.hephaestus.org.domain.OrgUnitTreeNode;
import olympus.hephaestus.org.dto.CreateOrgUnitRequest;
import olympus.hephaestus.org.dto.UpdateOrgUnitRequest;
import olympus.hephaestus.org.entity.OrgUnitEntity;
import olympus.hephaestus.org.role.constant.OrgPermissionCodes;
import olympus.hephaestus.org.role.service.OrgPermissionGuard;
import olympus.hephaestus.org.service.OrgUnitService;
import olympus.hephaestus.org.support.OrgCurrentPersonResolver;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org/units")
public class OrgUnitController {

    private final OrgUnitService orgUnitService;
    private final OrgPermissionGuard permissionGuard;
    private final OrgCurrentPersonResolver currentPersonResolver;

    public OrgUnitController(OrgUnitService orgUnitService,
                             OrgPermissionGuard permissionGuard,
                             OrgCurrentPersonResolver currentPersonResolver) {
        this.orgUnitService = orgUnitService;
        this.permissionGuard = permissionGuard;
        this.currentPersonResolver = currentPersonResolver;
    }

    @GetMapping("/tree")
    public List<OrgUnitTreeNode> getTree(HttpSession session) {
        Long personId = currentPersonResolver.currentPersonId(session);
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_VIEW);
        return orgUnitService.getUnitTree(personId);
    }

    @GetMapping("/{id}")
    public OrgUnitEntity get(HttpSession session,
                             @PathVariable("id") Long id) {
        Long personId = currentPersonResolver.currentPersonId(session);
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_VIEW);
        return orgUnitService.getUnit(personId, id);
    }

    @PostMapping
    public OrgUnitEntity create(HttpSession session,
                                @RequestBody CreateOrgUnitRequest request) {
        Long personId = currentPersonResolver.currentPersonId(session);
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_CREATE);
        return orgUnitService.createUnit(personId, request);
    }

    @PutMapping("/{id}")
    public OrgUnitEntity update(HttpSession session,
                                @PathVariable("id") Long id,
                                @RequestBody UpdateOrgUnitRequest request) {
        Long personId = currentPersonResolver.currentPersonId(session);
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_UPDATE);
        return orgUnitService.updateUnit(personId, id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpSession session,
                       @PathVariable("id") Long id) {
        Long personId = currentPersonResolver.currentPersonId(session);
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_UNIT_DELETE);
        orgUnitService.deleteUnit(personId, id);
    }
}
