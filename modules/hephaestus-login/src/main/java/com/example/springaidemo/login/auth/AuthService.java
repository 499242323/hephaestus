package com.example.springaidemo.login.auth;

import com.example.springaidemo.login.config.service.SystemConfigService;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class AuthService {

    public static final String SESSION_USER_KEY = "HEPHAESTUS_LOGIN_USER";

    private final OrgPersonRepository orgPersonRepository;
    private final SystemConfigService systemConfigService;
    private final RsaPasswordCryptoService passwordCryptoService;

    public AuthService(OrgPersonRepository orgPersonRepository,
                       SystemConfigService systemConfigService,
                       RsaPasswordCryptoService passwordCryptoService) {
        this.orgPersonRepository = orgPersonRepository;
        this.systemConfigService = systemConfigService;
        this.passwordCryptoService = passwordCryptoService;
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
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
        session.setAttribute(SESSION_USER_KEY, user);
        session.setMaxInactiveInterval(resolveSessionTimeoutSeconds());
        return new LoginResponse(true, user, "登录成功");
    }

    public LoginSessionUser currentUser(HttpSession session) {
        Object user = session.getAttribute(SESSION_USER_KEY);
        if (user instanceof LoginSessionUser loginSessionUser) {
            return loginSessionUser;
        }
        throw new LoginException("未登录");
    }

    private String resolvePassword(LoginRequest request) {
        boolean encryptEnabled = systemConfigService.getBoolean(LoginConfigConst.PASSWORD_ENCRYPT_ENABLED, true);
        if (!encryptEnabled || !request.encrypted()) {
            return request.password();
        }
        return passwordCryptoService.decrypt(request.password());
    }

    private int resolveSessionTimeoutSeconds() {
        int minutes = systemConfigService.getInt(LoginConfigConst.SESSION_TIMEOUT_MINUTES, 30);
        if (minutes < 1) {
            minutes = 30;
        }
        return minutes * 60;
    }
}
