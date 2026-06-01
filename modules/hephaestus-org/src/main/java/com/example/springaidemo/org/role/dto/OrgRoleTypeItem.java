package com.example.springaidemo.org.role.dto;

public record OrgRoleTypeItem(
        String typeCode,
        String typeName,
        Boolean adminFlag,
        Integer sortOrder
) {
}
