package com.example.springaidemo.org.role.service.impl;

import com.example.springaidemo.org.exception.OrgAccessDeniedException;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class OrgPermissionGuardImpl implements OrgPermissionGuard {

    private final OrgPermissionCache permissionCache;

    public OrgPermissionGuardImpl(OrgPermissionCache permissionCache) {
        this.permissionCache = permissionCache;
    }

    @Override
    public boolean isAdmin(Long personId) {
        return permissionCache.getSnapshot(personId).admin();
    }

    @Override
    public boolean hasPermission(Long personId, String permissionCode) {
        if (personId == null || permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        OrgPermissionCache.PermissionSnapshot snapshot = permissionCache.getSnapshot(personId);
        if (snapshot.admin()) {
            return true;
        }
        for (String parentCode : parentPermissionCodes(permissionCode)) {
            if (!snapshot.permissionCodes().contains(parentCode)) {
                return false;
            }
        }
        return snapshot.permissionCodes().contains(permissionCode);
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
        return permissionCache.getSnapshot(personId).permissionCodes().stream()
                .distinct()
                .sorted()
                .toList();
    }
}
