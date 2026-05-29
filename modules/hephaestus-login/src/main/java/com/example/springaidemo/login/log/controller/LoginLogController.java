package com.example.springaidemo.login.log.controller;

import com.example.springaidemo.login.log.dto.LoginLogPageResponse;
import com.example.springaidemo.login.log.service.LoginLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs/login")
public class LoginLogController {

    private final LoginLogService loginLogService;

    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    @GetMapping
    public LoginLogPageResponse query(@RequestParam(value = "keyword", required = false) String keyword,
                                      @RequestParam(value = "operationType", required = false) String operationType,
                                      @RequestParam(value = "success", required = false) Boolean success,
                                      @RequestParam(value = "startTime", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                      @RequestParam(value = "endTime", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return loginLogService.query(keyword, operationType, success, startTime, endTime, page, pageSize);
    }
}
