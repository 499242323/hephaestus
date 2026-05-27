package com.example.springaidemo.login.auth;

public record LoginRequest(String username, String password, boolean encrypted) {
}
