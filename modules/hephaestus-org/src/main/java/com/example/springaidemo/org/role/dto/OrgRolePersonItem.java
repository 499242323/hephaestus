package com.example.springaidemo.org.role.dto;

public record OrgRolePersonItem(
        Long id,
        String personCode,
        String personName,
        String username,
        Long unitId,
        String unitName
) {
}
