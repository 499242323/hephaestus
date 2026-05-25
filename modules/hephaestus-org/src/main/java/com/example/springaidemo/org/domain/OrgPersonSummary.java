package com.example.springaidemo.org.domain;

public record OrgPersonSummary(
        Long id,
        String personCode,
        String personName,
        String username,
        String password,
        Long unitId,
        String unitName,
        Long avatarMediaId,
        String avatarAccessUrl,
        String mobile,
        String email,
        String remark,
        Boolean enabled
) {
}
