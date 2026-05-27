package com.example.springaidemo.login.auth;

import java.io.Serializable;

public record LoginSessionUser(
        Long personId,
        String username,
        String personName,
        Long unitId,
        String loginAt
) implements Serializable {
}
