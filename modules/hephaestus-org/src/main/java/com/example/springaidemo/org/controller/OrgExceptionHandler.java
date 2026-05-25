package com.example.springaidemo.org.controller;

import com.example.springaidemo.org.exception.OrgAccessDeniedException;
import com.example.springaidemo.org.exception.OrgValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = {OrgUnitController.class, OrgPersonController.class})
public class OrgExceptionHandler {

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMissingHeader(MissingRequestHeaderException exception) {
        return Map.of("message", "缺少请求头: " + exception.getHeaderName());
    }

    @ExceptionHandler(OrgValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(OrgValidationException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(OrgAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied(OrgAccessDeniedException exception) {
        return Map.of("message", exception.getMessage());
    }
}
