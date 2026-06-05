package olympus.hephaestus.org.role.controller;

import olympus.hephaestus.org.role.domain.OrgPersonPermissionEntity;
import olympus.hephaestus.org.role.dto.OrgCurrentPermissionResponse;
import olympus.hephaestus.org.role.dto.OrgPermissionItem;
import olympus.hephaestus.org.role.repository.OrgPersonPermissionRepository;
import olympus.hephaestus.org.role.repository.OrgPermissionRepository;
import olympus.hephaestus.org.role.service.OrgPermissionGuard;
import olympus.hephaestus.org.service.OrgScopeService;
import olympus.hephaestus.org.support.OrgCurrentPersonResolver;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final OrgCurrentPersonResolver currentPersonResolver;

    public OrgPermissionController(OrgPermissionRepository permissionRepository,
                                   OrgPersonPermissionRepository personPermissionRepository,
                                   OrgPermissionGuard permissionGuard,
                                   OrgScopeService orgScopeService,
                                   OrgCurrentPersonResolver currentPersonResolver) {
        this.permissionRepository = permissionRepository;
        this.personPermissionRepository = personPermissionRepository;
        this.permissionGuard = permissionGuard;
        this.orgScopeService = orgScopeService;
        this.currentPersonResolver = currentPersonResolver;
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
    public List<OrgPersonPermissionEntity> listPersonPermissions(HttpSession session,
                                                                 @PathVariable("id") Long id) {
        Long personId = currentPersonResolver.currentPersonId(session);
        orgScopeService.requirePersonInScope(personId, id);
        return personPermissionRepository.findByPersonId(id);
    }

    @GetMapping("/permissions/current")
    public OrgCurrentPermissionResponse getCurrentPermissions(HttpSession session) {
        Long personId = currentPersonResolver.currentPersonId(session);
        boolean admin = permissionGuard.isAdmin(personId);
        return new OrgCurrentPermissionResponse(
                admin,
                admin ? List.of() : permissionGuard.listPermissionCodes(personId)
        );
    }
}
