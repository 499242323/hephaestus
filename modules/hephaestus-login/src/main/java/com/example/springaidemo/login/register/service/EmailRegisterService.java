package com.example.springaidemo.login.register.service;

import com.example.springaidemo.login.register.dto.EmailRegisterResponse;
import com.example.springaidemo.login.register.dto.RegisterRequest;
import com.example.springaidemo.login.register.dto.ResetPasswordRequest;
import com.example.springaidemo.login.register.dto.SendEmailCodeRequest;
import com.example.springaidemo.login.register.dto.VerifyResetPasswordCodeRequest;
import com.example.springaidemo.login.register.dto.VerifyResetPasswordCodeResponse;

public interface EmailRegisterService {

    EmailRegisterResponse sendRegisterCode(SendEmailCodeRequest request, String clientIp);

    EmailRegisterResponse sendResetPasswordCode(SendEmailCodeRequest request, String clientIp);

    EmailRegisterResponse register(RegisterRequest request);

    EmailRegisterResponse resetPassword(ResetPasswordRequest request);

    VerifyResetPasswordCodeResponse verifyResetPasswordCode(VerifyResetPasswordCodeRequest request);
}
