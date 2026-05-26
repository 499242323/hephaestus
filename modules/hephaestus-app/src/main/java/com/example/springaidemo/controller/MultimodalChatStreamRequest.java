package com.example.springaidemo.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class MultimodalChatStreamRequest {

    private String message;
    private MultipartFile file;
    private String sessionId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String normalizeMessage() {
        return message == null ? "" : message.trim();
    }

    public String normalizeConversationId() {
        return StringUtils.hasText(sessionId) ? sessionId.trim() : "default";
    }
}
