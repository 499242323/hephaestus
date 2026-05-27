package com.example.springaidemo.login.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthService.SESSION_USER_KEY) instanceof LoginSessionUser) {
            return true;
        }

        if (isApiRequest(request)) {
            log.warn("未登录访问 API，method={}, uri={}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response);
        } else {
            log.warn("未登录访问页面，method={}, uri={}", request.getMethod(), request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/login.html");
        }
        return false;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath == null || contextPath.isBlank() ? uri : uri.substring(contextPath.length());
        return path.startsWith("/api/") || path.startsWith("/auth/me");
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"未登录或登录已过期\"}");
    }
}
