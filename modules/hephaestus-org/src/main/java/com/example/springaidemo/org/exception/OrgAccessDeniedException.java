package com.example.springaidemo.org.exception;

public class OrgAccessDeniedException extends RuntimeException {

    public OrgAccessDeniedException(String message) {
        super(message);
    }
}
