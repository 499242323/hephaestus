package com.example.springaidemo.org.dto;

public record UpdateOrgPersonRequest(
        String personCode,
        String personName,
        String username,
        String password,
        Long unitId,
        String mobile,
        String email,
        String remark,
        Boolean enabled
) {
}
