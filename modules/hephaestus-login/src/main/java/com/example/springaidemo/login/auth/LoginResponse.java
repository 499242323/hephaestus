package com.example.springaidemo.login.auth;

public record LoginResponse(boolean success, LoginSessionUser user, String message) {
}
