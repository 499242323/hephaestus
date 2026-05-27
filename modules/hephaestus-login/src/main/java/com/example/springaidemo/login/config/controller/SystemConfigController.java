package com.example.springaidemo.login.config.controller;

import com.example.springaidemo.login.config.dto.SystemConfigFormResponse;
import com.example.springaidemo.login.config.dto.SystemConfigPublicResponse;
import com.example.springaidemo.login.config.dto.SystemConfigSaveRequest;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.login.config.service.SystemConfigService;
import com.example.springaidemo.login.auth.RsaPasswordCryptoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final RsaPasswordCryptoService rsaPasswordCryptoService;

    public SystemConfigController(SystemConfigService systemConfigService,
                                  RsaPasswordCryptoService rsaPasswordCryptoService) {
        this.systemConfigService = systemConfigService;
        this.rsaPasswordCryptoService = rsaPasswordCryptoService;
    }

    @GetMapping("/forms/{groupCode}")
    public SystemConfigFormResponse getForm(@PathVariable("groupCode") String groupCode) {
        return systemConfigService.getForm(groupCode);
    }

    @PutMapping("/forms/{groupCode}")
    public SystemConfigFormResponse saveForm(@PathVariable("groupCode") String groupCode,
                                             @RequestBody SystemConfigSaveRequest request,
                                             @RequestHeader(value = "X-Person-Id", required = false) String personId) {
        Map<String, String> values = request == null || request.values() == null ? Map.of() : request.values();
        return systemConfigService.saveForm(groupCode, values, personId);
    }

    @GetMapping("/public/{groupCode}")
    public SystemConfigPublicResponse getPublicConfig(@PathVariable("groupCode") String groupCode) {
        SystemConfigPublicResponse response = systemConfigService.getPublicConfig(groupCode);
        if (SystemConfigService.MAIN_SYSTEM_GROUP.equals(groupCode)
                && response.items().containsKey(LoginConfigConst.PASSWORD_ENCRYPT_PUBLIC_KEY)) {
            response.items().put(LoginConfigConst.PASSWORD_ENCRYPT_PUBLIC_KEY, rsaPasswordCryptoService.publicKeyBase64());
        }
        return response;
    }
}
