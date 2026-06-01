package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.exception.OrgAccessDeniedException;
import com.example.springaidemo.org.role.domain.OrgPersonPermissionEntity;
import com.example.springaidemo.org.role.repository.OrgPersonPermissionRepository;
import com.example.springaidemo.org.role.repository.OrgRoleRepository;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class OrgPermissionGuardImpl implements OrgPermissionGuard {

    private static final String ADMIN_ROLE_TYPE = "admin";

    private final OrgRoleRepository roleRepository;
    private final OrgPersonPermissionRepository personPermissionRepository;

    public OrgPermissionGuardImpl(OrgRoleRepository roleRepository,
                                  OrgPersonPermissionRepository personPermissionRepository) {
        this.roleRepository = roleRepository;
        this.personPermissionRepository = personPermissionRepository;
    }

    @Override
    public boolean isAdmin(Long personId) {
        return personId != null && roleRepository.existsEnabledRoleTypeByPersonId(personId, ADMIN_ROLE_TYPE);
    }

    @Override
    public boolean hasPermission(Long personId, String permissionCode) {
        if (personId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        // 管理员岗位不受岗位权限勾选约束，拥有组织设置全部权限。
        if (isAdmin(personId)) {
            return true;
        }
        for (String parentCode : parentPermissionCodes(permissionCode)) {
            if (!hasPermissionCode(personId, parentCode)) {
                return false;
            }
        }
        return hasPermissionCode(personId, permissionCode);
    }

    private boolean hasPermissionCode(Long personId, String permissionCode) {
        return personPermissionRepository.findByPersonId(personId).stream()
                .map(OrgPersonPermissionEntity::getPermissionCode)
                .filter(Objects::nonNull)
                .anyMatch(permissionCode::equals);
    }

    private List<String> parentPermissionCodes(String permissionCode) {
        String normalizedCode = permissionCode.toLowerCase(Locale.ROOT);
        if (normalizedCode.startsWith("general.config.login-page.")) {
            return List.of("general.config", "general.config.login-page");
        }
        if (normalizedCode.startsWith("general.config.login.")) {
            return List.of("general.config", "general.config.login");
        }
        if (normalizedCode.startsWith("general.log.login.")) {
            return List.of("general.log", "general.log.login");
        }
        if (normalizedCode.startsWith("general.log.operation.")) {
            return List.of("general.log", "general.log.operation");
        }
        if (normalizedCode.startsWith("general.unit.")) {
            return List.of("general.unit");
        }
        if (normalizedCode.startsWith("general.person.")) {
            return List.of("general.person");
        }
        if (normalizedCode.startsWith("general.role.")) {
            return List.of("general.role");
        }
        return List.of();
    }

    @Override
    public void requirePermission(Long personId, String permissionCode) {
        if (!hasPermission(personId, permissionCode)) {
            throw new OrgAccessDeniedException("无权访问该人员功能");
        }
    }

    @Override
    public List<String> listPermissionCodes(Long personId) {
        return personPermissionRepository.findByPersonId(personId).stream()
                .map(OrgPersonPermissionEntity::getPermissionCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }
}
