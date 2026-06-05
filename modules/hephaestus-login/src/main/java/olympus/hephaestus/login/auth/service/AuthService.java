package olympus.hephaestus.login.auth.service;

import olympus.hephaestus.login.auth.domain.LoginSessionUser;
import olympus.hephaestus.login.auth.dto.LoginRequest;
import olympus.hephaestus.login.auth.dto.LoginResponse;
import olympus.hephaestus.login.log.dto.LoginLogClientInfo;
import jakarta.servlet.http.HttpSession;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpSession session, LoginLogClientInfo clientInfo);

    LoginResponse login(LoginRequest request, HttpSession session);

    LoginSessionUser currentUser(HttpSession session);
}
