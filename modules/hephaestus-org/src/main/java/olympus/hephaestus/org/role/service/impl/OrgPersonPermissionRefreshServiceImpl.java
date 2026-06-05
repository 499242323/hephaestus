package olympus.hephaestus.org.role.service.impl;

import olympus.hephaestus.org.role.domain.OrgPersonPermissionEntity;
import olympus.hephaestus.org.role.repository.OrgPersonPermissionRepository;
import olympus.hephaestus.org.role.repository.OrgPersonRoleRepository;
import olympus.hephaestus.org.role.service.OrgPersonPermissionRefreshService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OrgPersonPermissionRefreshServiceImpl implements OrgPersonPermissionRefreshService {

    private final OrgPersonPermissionRepository personPermissionRepository;
    private final OrgPersonRoleRepository personRoleRepository;
    private final OrgPermissionCache permissionCache;

    public OrgPersonPermissionRefreshServiceImpl(OrgPersonPermissionRepository personPermissionRepository,
                                                 OrgPersonRoleRepository personRoleRepository,
                                                 OrgPermissionCache permissionCache) {
        this.personPermissionRepository = personPermissionRepository;
        this.personRoleRepository = personRoleRepository;
        this.permissionCache = permissionCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPersonPermissions(Long personId) {
        if (personId == null) {
            return;
        }
        refreshPersonPermissions(List.of(personId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPersonPermissions(Collection<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return;
        }
        List<Long> distinctPersonIds = personIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctPersonIds.isEmpty()) {
            return;
        }
        personPermissionRepository.deleteByPersonIds(distinctPersonIds);
        List<OrgPersonPermissionEntity> permissions = toDistinctPersonPermissions(
                personPermissionRepository.findRolePermissionsByPersonIds(distinctPersonIds)
        );
        if (!permissions.isEmpty()) {
            personPermissionRepository.insertBatch(permissions);
        }
        permissionCache.evictAfterCommit(distinctPersonIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        refreshPersonPermissions(personRoleRepository.findPersonIdsByRoleId(roleId));
    }

    private List<OrgPersonPermissionEntity> toDistinctPersonPermissions(List<OrgPersonPermissionEntity> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        Map<String, OrgPersonPermissionEntity> distinct = new LinkedHashMap<>();
        for (OrgPersonPermissionEntity permission : permissions) {
            if (permission.getPersonId() == null || permission.getPermissionCode() == null
                    || permission.getPermissionId() == null) {
                continue;
            }
            String key = permission.getPersonId() + ":" + permission.getPermissionCode();
            distinct.putIfAbsent(key, permission);
        }
        return List.copyOf(distinct.values());
    }
}
