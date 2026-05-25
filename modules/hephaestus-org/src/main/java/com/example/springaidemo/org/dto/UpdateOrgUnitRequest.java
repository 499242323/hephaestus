package com.example.springaidemo.org.dto;

public record UpdateOrgUnitRequest(
        String unitCode,
        String unitName,
        Integer sortOrder,
        Boolean enabled
) {
}
