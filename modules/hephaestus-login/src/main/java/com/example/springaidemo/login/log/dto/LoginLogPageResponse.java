package com.example.springaidemo.login.log.dto;

import java.util.List;

public record LoginLogPageResponse(
        List<LoginLogResponse> items,
        int page,
        int pageSize,
        long total
) {
}
