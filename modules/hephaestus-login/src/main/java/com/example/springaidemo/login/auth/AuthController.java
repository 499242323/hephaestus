package com.example.springaidemo.login.auth;

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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpSession session) {
        log.info("收到登录请求，username={}, encrypted={}",
                request == null ? null : request.username(),
                request != null && request.encrypted());
        return authService.login(request, session);
    }

    @GetMapping("/me")
    public LoginSessionUser me(HttpSession session) {
        return authService.currentUser(session);
    }

    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session) {
        session.invalidate();
        return new LoginResponse(true, null, "退出成功");
    }
}
