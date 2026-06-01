package com.example.springaidemo.login.auth.service.impl;

import com.example.springaidemo.login.auth.domain.LoginSessionUser;
import com.example.springaidemo.login.auth.dto.LoginRequest;
import com.example.springaidemo.login.auth.dto.LoginResponse;
import com.example.springaidemo.login.auth.exception.LoginException;
import com.example.springaidemo.login.auth.service.AuthService;
import com.example.springaidemo.login.auth.support.RsaPasswordCryptoService;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.login.config.service.SystemConfigService;
import com.example.springaidemo.login.log.dto.LoginLogClientInfo;
import com.example.springaidemo.login.log.service.LoginLogService;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.support.SessionUtils;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final OrgPersonRepository orgPersonRepository;
    private final SystemConfigService systemConfigService;
    private final RsaPasswordCryptoService passwordCryptoService;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final LoginLogService loginLogService;

    public AuthServiceImpl(OrgPersonRepository orgPersonRepository,
                           SystemConfigService systemConfigService,
                           RsaPasswordCryptoService passwordCryptoService,
                           FindByIndexNameSessionRepository<? extends Session> sessionRepository,
                           LoginLogService loginLogService) {
        this.orgPersonRepository = orgPersonRepository;
        this.systemConfigService = systemConfigService;
        this.passwordCryptoService = passwordCryptoService;
        this.sessionRepository = sessionRepository;
        this.loginLogService = loginLogService;
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpSession session, LoginLogClientInfo clientInfo) {
        String username = request == null ? null : request.username();
        try {
            LoginResponse response = doLogin(request, session);
            loginLogService.recordSuccess(response.user(), session.getId(), clientInfo);
            return response;
        } catch (LoginException exception) {
            loginLogService.recordFailure(username, exception.getMessage(), clientInfo);
            throw exception;
        } catch (RuntimeException exception) {
            loginLogService.recordFailure(username, "登录失败", clientInfo);
            throw exception;
        }
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpSession session) {
        return login(request, session, null);
    }

    @Override
    public LoginSessionUser currentUser(HttpSession session) {
        LoginSessionUser loginSessionUser = SessionUtils.getLoginUser(session, LoginSessionUser.class);
        if (loginSessionUser != null) {
            return loginSessionUser;
        }
        throw new LoginException("未登录");
    }

    private LoginResponse doLogin(LoginRequest request, HttpSession session) {
        if (request == null || !StringUtils.hasText(request.username()) || !StringUtils.hasText(request.password())) {
            throw new LoginException("用户名和密码不能为空");
        }

        String username = request.username().trim();
        String password = resolvePassword(request);
        OrgPersonEntity person = orgPersonRepository.getByUsername(username);
        if (person == null || !Boolean.TRUE.equals(person.getEnabled()) || !password.equals(person.getPassword())) {
            throw new LoginException("用户名或密码错误");
        }

        LoginSessionUser user = new LoginSessionUser(
                person.getId(),
                person.getUsername(),
                person.getPersonName(),
                person.getUnitId(),
                Instant.now().toString()
        );
        SessionUtils.setLoginUser(session, user);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, username);
        session.setMaxInactiveInterval(resolveSessionTimeoutSeconds());
        invalidateOtherSessionsIfNecessary(username, session.getId());
        return new LoginResponse(true, user, "登录成功");
    }

    private String resolvePassword(LoginRequest request) {
        boolean encryptEnabled = systemConfigService.getBoolean(LoginConfigConst.PASSWORD_ENCRYPT_ENABLED, true);
        if (!encryptEnabled || !request.encrypted()) {
            return request.password();
        }
        return passwordCryptoService.decrypt(request.password());
    }

    private void invalidateOtherSessionsIfNecessary(String username, String currentSessionId) {
        boolean singleLoginEnabled = systemConfigService.getBoolean(LoginConfigConst.SESSION_SINGLE_LOGIN_ENABLED, false);
        if (!singleLoginEnabled) {
            return;
        }
        try {
            Map<String, ? extends Session> sessions = sessionRepository.findByPrincipalName(username);
            sessions.forEach((sessionId, existingSession) -> {
                if (!sessionId.equals(currentSessionId)) {
                    sessionRepository.deleteById(sessionId);
                }
            });
        } catch (RuntimeException exception) {
            log.error("Failed to invalidate old login sessions, username={}, currentSessionId={}",
                    username, currentSessionId, exception);
        }
    }

    private int resolveSessionTimeoutSeconds() {
        int minutes = systemConfigService.getInt(LoginConfigConst.SESSION_TIMEOUT_MINUTES, 30);
        if (minutes < 1) {
            minutes = 30;
        }
        return minutes * 60;
    }
}
