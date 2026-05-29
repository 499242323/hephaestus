package com.example.springaidemo.login.auth.dto;

import com.example.springaidemo.login.auth.domain.LoginSessionUser;

public record LoginResponse(boolean success, LoginSessionUser user, String message) {
}
