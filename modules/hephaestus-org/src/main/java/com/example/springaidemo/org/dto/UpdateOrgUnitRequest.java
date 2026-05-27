package com.example.springaidemo.org.dto;

public record UpdateOrgUnitRequest(
        String unitCode,
        String unitName,
        Long parentId,
        Integer sortOrder,
        Boolean enabled
) {
}
