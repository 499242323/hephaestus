package olympus.hephaestus.org.role.controller;

import olympus.hephaestus.org.role.dto.OrgPersonRoleItem;
import olympus.hephaestus.org.role.dto.OrgRoleRequest;
import olympus.hephaestus.org.role.dto.OrgRoleResponse;
import olympus.hephaestus.org.role.dto.OrgRoleTypeItem;
import olympus.hephaestus.org.role.dto.OrgRoleTreeNode;
import olympus.hephaestus.org.role.dto.UpdatePersonRolesRequest;
import olympus.hephaestus.org.role.dto.UpdateRolePeopleRequest;
import olympus.hephaestus.org.role.constant.OrgPermissionCodes;
import olympus.hephaestus.org.role.repository.OrgRoleTypeRepository;
import olympus.hephaestus.org.role.service.OrgPermissionGuard;
import olympus.hephaestus.org.role.service.OrgPersonRoleService;
import olympus.hephaestus.org.role.service.OrgRoleService;
import olympus.hephaestus.org.support.OrgCurrentPersonResolver;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final OrgCurrentPersonResolver currentPersonResolver;

    public OrgRoleController(OrgRoleService orgRoleService,
                             OrgPersonRoleService orgPersonRoleService,
                             OrgRoleTypeRepository orgRoleTypeRepository,
                             OrgPermissionGuard orgPermissionGuard,
                             OrgCurrentPersonResolver currentPersonResolver) {
        this.orgRoleService = orgRoleService;
        this.orgPersonRoleService = orgPersonRoleService;
        this.orgRoleTypeRepository = orgRoleTypeRepository;
        this.orgPermissionGuard = orgPermissionGuard;
        this.currentPersonResolver = currentPersonResolver;
    }

    @GetMapping("/roles/tree")
    public List<OrgRoleTreeNode> getRoleTree(HttpSession session) {
        return orgRoleService.getRoleTree(currentPersonResolver.currentPersonId(session));
    }

    @GetMapping("/role-types")
    public List<OrgRoleTypeItem> listRoleTypes(HttpSession session) {
        Long personId = currentPersonResolver.currentPersonId(session);
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
    public List<OrgRoleResponse> listRoles(HttpSession session,
                                           @RequestParam(value = "unitId", required = false) Long unitId,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return orgRoleService.listRoles(currentPersonResolver.currentPersonId(session), unitId, keyword, enabled);
    }

    @GetMapping("/roles/{id}")
    public OrgRoleResponse getRole(HttpSession session,
                                   @PathVariable("id") Long id) {
        return orgRoleService.getRole(currentPersonResolver.currentPersonId(session), id);
    }

    @PostMapping("/roles")
    public OrgRoleResponse createRole(HttpSession session,
                                      @RequestBody OrgRoleRequest request) {
        return orgRoleService.createRole(currentPersonResolver.currentPersonId(session), request);
    }

    @PutMapping("/roles/{id}")
    public OrgRoleResponse updateRole(HttpSession session,
                                      @PathVariable("id") Long id,
                                      @RequestBody OrgRoleRequest request) {
        return orgRoleService.updateRole(currentPersonResolver.currentPersonId(session), id, request);
    }

    @DeleteMapping("/roles/{id}")
    public void deleteRole(HttpSession session,
                           @PathVariable("id") Long id) {
        orgRoleService.deleteRole(currentPersonResolver.currentPersonId(session), id);
    }

    @PutMapping("/roles/{id}/people")
    public OrgRoleResponse updateRolePeople(HttpSession session,
                                            @PathVariable("id") Long id,
                                            @RequestBody UpdateRolePeopleRequest request) {
        return orgRoleService.updateRolePeople(currentPersonResolver.currentPersonId(session), id, request);
    }

    @PostMapping("/roles/{id}/refresh-permissions")
    public void refreshRolePeoplePermissions(HttpSession session,
                                             @PathVariable("id") Long id) {
        orgRoleService.refreshRolePeoplePermissions(currentPersonResolver.currentPersonId(session), id);
    }

    @GetMapping("/persons/{id}/roles")
    public List<OrgPersonRoleItem> listPersonRoles(HttpSession session,
                                                   @PathVariable("id") Long id) {
        return orgPersonRoleService.listPersonRoles(currentPersonResolver.currentPersonId(session), id);
    }

    @PutMapping("/persons/{id}/roles")
    public List<OrgPersonRoleItem> updatePersonRoles(HttpSession session,
                                                     @PathVariable("id") Long id,
                                                     @RequestBody UpdatePersonRolesRequest request) {
        return orgPersonRoleService.replacePersonRoles(currentPersonResolver.currentPersonId(session), id, request);
    }
}
