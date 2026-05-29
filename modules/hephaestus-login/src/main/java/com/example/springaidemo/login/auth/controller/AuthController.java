package com.example.springaidemo.login.auth.controller;

import com.example.springaidemo.login.auth.domain.LoginSessionUser;
import com.example.springaidemo.login.auth.dto.LoginRequest;
import com.example.springaidemo.login.auth.dto.LoginResponse;
import com.example.springaidemo.login.auth.exception.LoginException;
import com.example.springaidemo.login.auth.service.AuthService;
import com.example.springaidemo.login.log.dto.LoginLogClientInfo;
import com.example.springaidemo.login.log.service.LoginLogService;
import com.example.springaidemo.login.log.support.LoginLogClientInfoResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginLogService loginLogService;
    private final LoginLogClientInfoResolver clientInfoResolver;

    public AuthController(AuthService authService,
                          LoginLogService loginLogService,
                          LoginLogClientInfoResolver clientInfoResolver) {
        this.authService = authService;
        this.loginLogService = loginLogService;
        this.clientInfoResolver = clientInfoResolver;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpSession session, HttpServletRequest servletRequest) {
        log.info("收到登录请求, username={}, encrypted={}",
                request == null ? null : request.username(),
                request != null && request.encrypted());
        LoginLogClientInfo clientInfo = clientInfoResolver.resolve(servletRequest);
        return authService.login(request, session, clientInfo);
    }

    @GetMapping("/me")
    public LoginSessionUser me(HttpSession session) {
        return authService.currentUser(session);
    }

    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session, HttpServletRequest request) {
        LoginSessionUser user = null;
        try {
            user = authService.currentUser(session);
        } catch (LoginException exception) {
            log.debug("Logout requested without active login session");
        }
        LoginLogClientInfo clientInfo = clientInfoResolver.resolve(request);
        loginLogService.recordLogout(user, session.getId(), clientInfo);
        session.invalidate();
        return new LoginResponse(true, null, "退出成功");
    }
}
