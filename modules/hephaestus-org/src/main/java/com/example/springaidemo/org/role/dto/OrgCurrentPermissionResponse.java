package com.example.springaidemo.org.role.dto;

import java.util.List;

public record OrgCurrentPermissionResponse(
        boolean admin,
        List<String> permissions
) {
}
