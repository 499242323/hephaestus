package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.role.domain.OrgPersonPermissionEntity;
import com.example.springaidemo.org.role.domain.OrgRolePermissionEntity;
import com.example.springaidemo.org.role.repository.OrgPersonPermissionRepository;
import com.example.springaidemo.org.role.repository.OrgPersonRoleRepository;
import com.example.springaidemo.org.role.repository.OrgRolePermissionRepository;
import com.example.springaidemo.org.role.service.OrgPersonPermissionRefreshService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrgPersonPermissionRefreshServiceImpl implements OrgPersonPermissionRefreshService {

    private final OrgPersonPermissionRepository personPermissionRepository;
    private final OrgPersonRoleRepository personRoleRepository;
    private final OrgRolePermissionRepository rolePermissionRepository;
    private final OrgPermissionCache permissionCache;

    public OrgPersonPermissionRefreshServiceImpl(OrgPersonPermissionRepository personPermissionRepository,
                                                 OrgPersonRoleRepository personRoleRepository,
                                                 OrgRolePermissionRepository rolePermissionRepository,
                                                 OrgPermissionCache permissionCache) {
        this.personPermissionRepository = personPermissionRepository;
        this.personRoleRepository = personRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionCache = permissionCache;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPersonPermissions(Long personId) {
        if (personId == null) {
            return;
        }
        personPermissionRepository.deleteByPersonId(personId);
        List<Long> roleIds = personRoleRepository.findRoleIdsByPersonId(personId);
        if (roleIds == null || roleIds.isEmpty()) {
            permissionCache.evictAfterCommit(personId);
            return;
        }
        List<OrgRolePermissionEntity> rolePermissions = rolePermissionRepository.findByRoleIds(roleIds);
        List<OrgPersonPermissionEntity> permissions = toPersonPermissions(personId, rolePermissions);
        if (!permissions.isEmpty()) {
            personPermissionRepository.insertBatch(permissions);
        }
        permissionCache.evictAfterCommit(personId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPersonPermissions(Collection<Long> personIds) {
        if (personIds == null || personIds.isEmpty()) {
            return;
        }
        personIds.stream().distinct().forEach(this::refreshPersonPermissions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        refreshPersonPermissions(personRoleRepository.findPersonIdsByRoleId(roleId));
    }

    private List<OrgPersonPermissionEntity> toPersonPermissions(Long personId,
                                                               List<OrgRolePermissionEntity> rolePermissions) {
        if (rolePermissions == null || rolePermissions.isEmpty()) {
            return List.of();
        }
        Map<String, OrgPersonPermissionEntity> distinct = new LinkedHashMap<>();
        for (OrgRolePermissionEntity rolePermission : rolePermissions) {
            if (rolePermission.getPermissionCode() == null || rolePermission.getPermissionId() == null) {
                continue;
            }
            distinct.computeIfAbsent(rolePermission.getPermissionCode(), permissionCode -> {
                OrgPersonPermissionEntity entity = new OrgPersonPermissionEntity();
                entity.setPersonId(personId);
                entity.setPermissionId(rolePermission.getPermissionId());
                entity.setPermissionCode(permissionCode);
                return entity;
            });
        }
        return List.copyOf(distinct.values());
    }
}
