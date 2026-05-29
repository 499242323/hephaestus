package com.example.springaidemo.login.log.dto;

import com.example.springaidemo.mybatis.page.PageInfo;

import java.util.List;

public record LoginLogPageResponse(
        List<LoginLogResponse> items,
        int page,
        int pageSize,
        long total
) {

    public static LoginLogPageResponse from(PageInfo<LoginLogResponse> pageInfo) {
        return new LoginLogPageResponse(
                pageInfo.items(),
                pageInfo.page(),
                pageInfo.pageSize(),
                pageInfo.total()
        );
    }
}
