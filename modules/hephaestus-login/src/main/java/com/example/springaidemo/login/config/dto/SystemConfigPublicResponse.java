package com.example.springaidemo.login.config.dto;

import java.util.Map;

public record SystemConfigPublicResponse(String groupCode, Map<String, String> items) {
}
