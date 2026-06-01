package com.example.springaidemo.login.log.controller;

import com.example.springaidemo.login.log.dto.LoginLogResponse;
import com.example.springaidemo.login.log.service.LoginLogService;
import com.example.springaidemo.mybatis.page.Pagination;
import com.example.springaidemo.org.role.constant.OrgPermissionCodes;
import com.example.springaidemo.org.role.service.OrgPermissionGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs/login")
public class LoginLogController {

    private final LoginLogService loginLogService;
    private final OrgPermissionGuard permissionGuard;

    public LoginLogController(LoginLogService loginLogService,
                              OrgPermissionGuard permissionGuard) {
        this.loginLogService = loginLogService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public Pagination<LoginLogResponse> query(@RequestHeader("X-Person-Id") Long personId,
                                              @RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "operationType", required = false) String operationType,
                                              @RequestParam(value = "success", required = false) Boolean success,
                                              @RequestParam(value = "startTime", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                              @RequestParam(value = "endTime", required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                              @RequestParam(value = "page", required = false) Integer page,
                                              @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_LOG_LOGIN_VIEW);
        return loginLogService.query(keyword, operationType, success, startTime, endTime, page, pageSize);
    }
}
