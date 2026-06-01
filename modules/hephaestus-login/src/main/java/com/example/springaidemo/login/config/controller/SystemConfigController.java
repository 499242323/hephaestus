package com.example.springaidemo.login.config.controller;

import com.example.springaidemo.login.auth.service.AuthService;
import com.example.springaidemo.login.config.dto.SystemConfigFormResponse;
import com.example.springaidemo.login.config.dto.SystemConfigPublicResponse;
import com.example.springaidemo.login.config.dto.SystemConfigSaveRequest;
import com.example.springaidemo.login.config.dto.SystemConfigSectionResponse;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.login.config.service.SystemConfigService;
import com.example.springaidemo.login.auth.support.RsaPasswordCryptoService;
import com.example.springaidemo.org.log.dto.OperationLogRecordRequest;
import com.example.springaidemo.org.log.service.OperationLogRecorder;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final RsaPasswordCryptoService rsaPasswordCryptoService;
    private final OrgPermissionGuard permissionGuard;
    private final OperationLogRecorder operationLogRecorder;
    private final AuthService authService;

    public SystemConfigController(SystemConfigService systemConfigService,
                                  RsaPasswordCryptoService rsaPasswordCryptoService,
                                  OrgPermissionGuard permissionGuard,
                                  OperationLogRecorder operationLogRecorder,
                                  AuthService authService) {
        this.systemConfigService = systemConfigService;
        this.rsaPasswordCryptoService = rsaPasswordCryptoService;
        this.permissionGuard = permissionGuard;
        this.operationLogRecorder = operationLogRecorder;
        this.authService = authService;
    }

    @GetMapping("/forms/{groupCode}")
    public SystemConfigFormResponse getForm(@PathVariable("groupCode") String groupCode,
                                            HttpSession session) {
        Long personId = authService.currentUser(session).personId();
        ConfigPermissionScope permissionScope = resolveConfigPermissionScope(personId);
        boolean canViewLogin = permissionScope.hasPermission(OrgPermissionCodes.GENERAL_CONFIG_LOGIN_VIEW);
        boolean canViewLoginPage = permissionScope.hasPermission(OrgPermissionCodes.GENERAL_CONFIG_LOGIN_PAGE_VIEW);
        if (!canViewLogin && !canViewLoginPage) {
            permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_VIEW);
        }
        SystemConfigFormResponse response = systemConfigService.getForm(groupCode);
        if (SystemConfigService.MAIN_SYSTEM_GROUP.equals(groupCode)) {
            return filterReadableConfigForm(response, canViewLogin, canViewLoginPage);
        }
        return response;
    }

    @PutMapping("/forms/{groupCode}")
    public SystemConfigFormResponse saveForm(@PathVariable("groupCode") String groupCode,
                                             @RequestBody SystemConfigSaveRequest request,
                                             HttpSession session) {
        Long personId = authService.currentUser(session).personId();
        ConfigPermissionScope permissionScope = resolveConfigPermissionScope(personId);
        boolean canUpdateLogin = permissionScope.hasPermission(OrgPermissionCodes.GENERAL_CONFIG_LOGIN_UPDATE);
        boolean canUpdateLoginPage = permissionScope.hasPermission(OrgPermissionCodes.GENERAL_CONFIG_LOGIN_PAGE_UPDATE);
        if (!canUpdateLogin && !canUpdateLoginPage) {
            permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_UPDATE);
        }
        Map<String, String> values = request == null || request.values() == null ? Map.of() : request.values();
        if (SystemConfigService.MAIN_SYSTEM_GROUP.equals(groupCode)) {
            values = filterWritableConfigValues(values, canUpdateLogin, canUpdateLoginPage);
        }
        SystemConfigFormResponse response = systemConfigService.saveForm(groupCode, values, personId == null ? null : String.valueOf(personId));
        operationLogRecorder.recordSuccess(new OperationLogRecordRequest(
                personId,
                null,
                null,
                null,
                null,
                "system-config",
                "系统配置",
                "save-config",
                "保存配置",
                "配置",
                groupCode,
                configTargetName(groupCode),
                "保存系统配置：" + configTargetName(groupCode),
                buildConfigSaveDetail(values)
        ));
        return response;
    }

    @GetMapping("/public/{groupCode}")
    public SystemConfigPublicResponse getPublicConfig(@PathVariable("groupCode") String groupCode) {
        SystemConfigPublicResponse response = systemConfigService.getPublicConfig(groupCode);
        if (SystemConfigService.MAIN_SYSTEM_GROUP.equals(groupCode)) {
            response.items().put(LoginConfigConst.PASSWORD_ENCRYPT_PUBLIC_KEY, rsaPasswordCryptoService.publicKeyBase64());
        }
        return response;
    }

    private Map<String, String> filterWritableConfigValues(Map<String, String> values,
                                                           boolean canUpdateLogin,
                                                           boolean canUpdateLoginPage) {
        Map<String, String> filtered = new HashMap<>();
        values.forEach((code, value) -> {
            if (isLoginPageConfig(code)) {
                if (canUpdateLoginPage) {
                    filtered.put(code, value);
                }
                return;
            }
            if (canUpdateLogin) {
                filtered.put(code, value);
            }
        });
        return filtered;
    }

    private SystemConfigFormResponse filterReadableConfigForm(SystemConfigFormResponse response,
                                                              boolean canViewLogin,
                                                              boolean canViewLoginPage) {
        List<SystemConfigSectionResponse> sections = response.sections().stream()
                .map(section -> new SystemConfigSectionResponse(
                        section.sectionCode(),
                        section.sectionName(),
                        section.fields().stream()
                                .filter(field -> (isLoginPageConfig(field.code()) && canViewLoginPage)
                                        || (!isLoginPageConfig(field.code()) && canViewLogin))
                                .toList()
                ))
                .filter(section -> !section.fields().isEmpty())
                .toList();
        return new SystemConfigFormResponse(response.groupCode(), response.groupName(), sections);
    }

    private boolean isLoginPageConfig(String code) {
        if (code == null) {
            return false;
        }
        return Set.of(
                LoginConfigConst.PAGE_TITLE,
                LoginConfigConst.PAGE_SUBTITLE,
                LoginConfigConst.MOUSE_TRAIL_EFFECT,
                LoginConfigConst.PAGE_BACKGROUND_MEDIA_ID,
                LoginConfigConst.PAGE_BACKGROUND_GRID_ENABLED
        ).contains(code);
    }

    private ConfigPermissionScope resolveConfigPermissionScope(Long personId) {
        if (personId == null) {
            return new ConfigPermissionScope(false, Set.of());
        }
        boolean admin = permissionGuard.isAdmin(personId);
        if (admin) {
            return new ConfigPermissionScope(true, Set.of());
        }
        return new ConfigPermissionScope(false, Set.copyOf(permissionGuard.listPermissionCodes(personId)));
    }

    private record ConfigPermissionScope(boolean admin, Set<String> permissionCodes) {

        private boolean hasPermission(String permissionCode) {
            if (admin) {
                return true;
            }
            if (permissionCode == null || permissionCode.isBlank()) {
                return false;
            }
            for (String parentCode : parentPermissionCodes(permissionCode)) {
                if (!permissionCodes.contains(parentCode)) {
                    return false;
                }
            }
            return permissionCodes.contains(permissionCode);
        }

        private List<String> parentPermissionCodes(String permissionCode) {
            String normalizedCode = permissionCode.toLowerCase(Locale.ROOT);
            if (normalizedCode.startsWith("general.config.login-page.")) {
                return List.of("general.config", "general.config.login-page");
            }
            if (normalizedCode.startsWith("general.config.login.")) {
                return List.of("general.config", "general.config.login");
            }
            return List.of();
        }
    }

    private String configTargetName(String groupCode) {
        return SystemConfigService.MAIN_SYSTEM_GROUP.equals(groupCode) ? "主系统配置" : groupCode;
    }

    private String buildConfigSaveDetail(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "保存配置：未提交配置项。";
        }
        return values.keySet().stream()
                .sorted()
                .map(key -> isSensitiveConfigKey(key) ? key + " 已更新" : key + " 已保存")
                .collect(Collectors.joining("；"));
    }

    private boolean isSensitiveConfigKey(String key) {
        String lower = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return lower.contains("password")
                || lower.contains("private-key")
                || lower.contains("secret")
                || lower.contains("token");
    }
}
