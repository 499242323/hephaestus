package olympus.hephaestus.login.register.service;

import olympus.hephaestus.login.register.dto.EmailRegisterResponse;
import olympus.hephaestus.login.register.dto.RegisterRequest;
import olympus.hephaestus.login.register.dto.ResetPasswordRequest;
import olympus.hephaestus.login.register.dto.SendEmailCodeRequest;
import olympus.hephaestus.login.register.dto.VerifyResetPasswordCodeRequest;
import olympus.hephaestus.login.register.dto.VerifyResetPasswordCodeResponse;

public interface EmailRegisterService {

    EmailRegisterResponse sendRegisterCode(SendEmailCodeRequest request, String clientIp);

    EmailRegisterResponse sendResetPasswordCode(SendEmailCodeRequest request, String clientIp);

    EmailRegisterResponse register(RegisterRequest request);

    EmailRegisterResponse resetPassword(ResetPasswordRequest request);

    VerifyResetPasswordCodeResponse verifyResetPasswordCode(VerifyResetPasswordCodeRequest request);
}
