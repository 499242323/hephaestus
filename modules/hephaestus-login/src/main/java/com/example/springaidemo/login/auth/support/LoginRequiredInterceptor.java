package com.example.springaidemo.login.auth.support;

import com.example.springaidemo.login.auth.config.LoginAuthProperties;
import com.example.springaidemo.login.auth.domain.LoginSessionUser;
import com.example.springaidemo.org.support.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    private static final List<String> FALLBACK_WHITELIST = List.of(
            "/login.html",
            "/login.css",
            "/login.js",
            "/webjars/**",
            "/auth/login",
            "/api/system-config/public/**",
            "/api/media/files/*",
            "/favicon.ico",
            "/error",
            "/**/*.css",
            "/**/*.js",
            "/**/*.png",
            "/**/*.jpg",
            "/**/*.jpeg",
            "/**/*.gif",
            "/**/*.svg",
            "/**/*.ico",
            "/**/*.woff",
            "/**/*.woff2"
    );

    private final LoginAuthProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public LoginRequiredInterceptor(LoginAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isWhitelistRequest(request)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (SessionUtils.hasLoginUser(session, LoginSessionUser.class)) {
            return true;
        }

        if (isApiRequest(request)) {
            log.warn("未登录访问 API, method={}, uri={}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response);
        } else {
            log.warn("未登录访问页面, method={}, uri={}", request.getMethod(), request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/login.html");
        }
        return false;
    }

    private boolean isWhitelistRequest(HttpServletRequest request) {
        String path = resolvePath(request);
        for (String pattern : whitelistPaths()) {
            if (StringUtils.hasText(pattern) && pathMatcher.match(pattern.trim(), path)) {
                return true;
            }
        }
        return false;
    }

    private List<String> whitelistPaths() {
        List<String> paths = properties.getWhitelist().getPaths();
        return paths == null || paths.isEmpty() ? FALLBACK_WHITELIST : paths;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String path = resolvePath(request);
        return path.startsWith("/api/") || path.startsWith("/auth/me") || path.startsWith("/auth/logout");
    }

    private String resolvePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"未登录或登录已过期\"}");
    }
}
