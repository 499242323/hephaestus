package com.example.springaidemo.org.domain;

import com.example.springaidemo.org.role.dto.OrgPersonRoleItem;

import java.util.List;

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
        Boolean enabled,
        List<OrgPersonRoleItem> roles
) {
}
