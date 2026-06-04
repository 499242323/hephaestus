package com.example.springaidemo.login.register.dto;

public class EmailRegisterResponse {

    private boolean success;
    private String message;

    public EmailRegisterResponse() {
    }

    public EmailRegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static EmailRegisterResponse success(String message) {
        return new EmailRegisterResponse(true, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
