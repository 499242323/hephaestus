package com.example.springaidemo.login.auth.service;

import com.example.springaidemo.login.auth.domain.LoginSessionUser;
import com.example.springaidemo.login.auth.dto.LoginRequest;
import com.example.springaidemo.login.auth.dto.LoginResponse;
import com.example.springaidemo.login.log.dto.LoginLogClientInfo;
import jakarta.servlet.http.HttpSession;

public interface AuthService {

    String SESSION_USER_KEY = "HEPHAESTUS_LOGIN_USER";

    LoginResponse login(LoginRequest request, HttpSession session, LoginLogClientInfo clientInfo);

    LoginResponse login(LoginRequest request, HttpSession session);

    LoginSessionUser currentUser(HttpSession session);
}
