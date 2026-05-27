package com.example.springaidemo.login.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoginWebMvcConfiguration implements WebMvcConfigurer {

    private final LoginRequiredInterceptor loginRequiredInterceptor;

    public LoginWebMvcConfiguration(LoginRequiredInterceptor loginRequiredInterceptor) {
        this.loginRequiredInterceptor = loginRequiredInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/", "/chat.html", "/api/**")
                .excludePathPatterns(
                        "/login.html",
                        "/login.css",
                        "/login.js",
                        "/auth/login",
                        "/auth/logout",
                        "/api/media/files/*",
                        "/api/system-config/public/**",
                        "/chat.css",
                        "/favicon.ico",
                        "/error"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/chat.html");
    }
}
