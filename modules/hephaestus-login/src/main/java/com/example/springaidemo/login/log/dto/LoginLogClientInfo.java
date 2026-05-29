package com.example.springaidemo.login.log.dto;

public record LoginLogClientInfo(
        String clientIp,
        String userAgent,
        String requestUri
) {
}
