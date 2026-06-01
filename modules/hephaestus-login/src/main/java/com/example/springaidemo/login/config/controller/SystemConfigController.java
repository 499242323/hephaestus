package com.example.springaidemo.login.config.controller;

import com.example.springaidemo.login.config.dto.SystemConfigFormResponse;
import com.example.springaidemo.login.config.dto.SystemConfigPublicResponse;
import com.example.springaidemo.login.config.dto.SystemConfigSaveRequest;
import com.example.springaidemo.login.config.dto.SystemConfigSectionResponse;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.login.config.service.SystemConfigService;
import com.example.springaidemo.login.auth.support.RsaPasswordCryptoService;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final RsaPasswordCryptoService rsaPasswordCryptoService;
    private final OrgPermissionGuard permissionGuard;

    public SystemConfigController(SystemConfigService systemConfigService,
                                  RsaPasswordCryptoService rsaPasswordCryptoService,
                                  OrgPermissionGuard permissionGuard) {
        this.systemConfigService = systemConfigService;
        this.rsaPasswordCryptoService = rsaPasswordCryptoService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/forms/{groupCode}")
    public SystemConfigFormResponse getForm(@PathVariable("groupCode") String groupCode,
                                            @RequestHeader(value = "X-Person-Id", required = false) Long personId) {
        boolean canViewLogin = permissionGuard.hasPermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_VIEW);
        boolean canViewLoginPage = permissionGuard.hasPermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_PAGE_VIEW);
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
                                             @RequestHeader(value = "X-Person-Id", required = false) Long personId) {
        boolean canUpdateLogin = permissionGuard.hasPermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_UPDATE);
        boolean canUpdateLoginPage = permissionGuard.hasPermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_PAGE_UPDATE);
        if (!canUpdateLogin && !canUpdateLoginPage) {
            permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_CONFIG_LOGIN_UPDATE);
        }
        Map<String, String> values = request == null || request.values() == null ? Map.of() : request.values();
        if (SystemConfigService.MAIN_SYSTEM_GROUP.equals(groupCode)) {
            values = filterWritableConfigValues(values, canUpdateLogin, canUpdateLoginPage);
        }
        return systemConfigService.saveForm(groupCode, values, personId == null ? null : String.valueOf(personId));
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
}
