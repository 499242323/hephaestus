package com.example.springaidemo.org.role.dto;

import java.util.List;

public record UpdateRolePeopleRequest(
        List<Long> personIds
) {
}
