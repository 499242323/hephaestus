package com.example.springaidemo.org.role.dto;

import java.util.List;

public record UpdatePersonRolesRequest(
        List<Long> roleIds
) {
}
