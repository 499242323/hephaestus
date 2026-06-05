package olympus.hephaestus.login.log.dto;

import olympus.hephaestus.mybatis.page.PageInfo;

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
