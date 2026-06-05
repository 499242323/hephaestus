package olympus.hephaestus.login.register.controller;

import olympus.hephaestus.login.register.dto.EmailRegisterResponse;
import olympus.hephaestus.login.register.dto.RegisterRequest;
import olympus.hephaestus.login.register.dto.ResetPasswordRequest;
import olympus.hephaestus.login.register.dto.SendEmailCodeRequest;
import olympus.hephaestus.login.register.dto.VerifyResetPasswordCodeRequest;
import olympus.hephaestus.login.register.dto.VerifyResetPasswordCodeResponse;
import olympus.hephaestus.login.register.service.EmailRegisterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class EmailRegisterController {

    private final EmailRegisterService emailRegisterService;

    public EmailRegisterController(EmailRegisterService emailRegisterService) {
        this.emailRegisterService = emailRegisterService;
    }

    @PostMapping("/email-code/register")
    public EmailRegisterResponse sendRegisterCode(@RequestBody SendEmailCodeRequest request,
                                                  HttpServletRequest httpRequest) {
        return emailRegisterService.sendRegisterCode(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/email-code/reset-password")
    public EmailRegisterResponse sendResetPasswordCode(@RequestBody SendEmailCodeRequest request,
                                                       HttpServletRequest httpRequest) {
        return emailRegisterService.sendResetPasswordCode(request, resolveClientIp(httpRequest));
    }

    @PostMapping("/register")
    public EmailRegisterResponse register(@RequestBody RegisterRequest request) {
        return emailRegisterService.register(request);
    }

    @PostMapping("/reset-password")
    public EmailRegisterResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        return emailRegisterService.resetPassword(request);
    }

    @PostMapping("/reset-password/verify")
    public VerifyResetPasswordCodeResponse verifyResetPasswordCode(@RequestBody VerifyResetPasswordCodeRequest request) {
        return emailRegisterService.verifyResetPasswordCode(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        return request.getRemoteAddr();
    }
}
