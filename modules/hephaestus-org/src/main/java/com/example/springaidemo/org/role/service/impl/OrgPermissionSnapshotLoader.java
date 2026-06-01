package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.role.config.OrgPermissionCacheConfig;
import com.example.springaidemo.org.role.domain.OrgPersonPermissionEntity;
import com.example.springaidemo.org.role.repository.OrgPersonPermissionRepository;
import com.example.springaidemo.org.role.repository.OrgRoleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrgPermissionSnapshotLoader {

    private static final String ADMIN_ROLE_TYPE = "admin";

    private final OrgRoleRepository roleRepository;
    private final OrgPersonPermissionRepository personPermissionRepository;

    public OrgPermissionSnapshotLoader(OrgRoleRepository roleRepository,
                                       OrgPersonPermissionRepository personPermissionRepository) {
        this.roleRepository = roleRepository;
        this.personPermissionRepository = personPermissionRepository;
    }

    @Cacheable(cacheNames = OrgPermissionCacheConfig.PERSON_PERMISSION_CACHE, key = "#p0", unless = "#result == null")
    public OrgPermissionCache.PermissionSnapshot loadSnapshot(Long personId) {
        boolean admin = roleRepository.existsEnabledRoleTypeByPersonId(personId, ADMIN_ROLE_TYPE);
        if (admin) {
            return new OrgPermissionCache.PermissionSnapshot(true, Set.of());
        }
        Set<String> permissionCodes = personPermissionRepository.findByPersonId(personId).stream()
                .map(OrgPersonPermissionEntity::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        return new OrgPermissionCache.PermissionSnapshot(false, permissionCodes);
    }
}
