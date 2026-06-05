package olympus.hephaestus.login.register.service.impl;

import olympus.hephaestus.login.auth.exception.LoginException;
import olympus.hephaestus.login.auth.support.RsaPasswordCryptoService;
import olympus.hephaestus.login.config.LoginConfigConst;
import olympus.hephaestus.login.config.service.SystemConfigService;
import olympus.hephaestus.login.register.domain.EmailVerificationCodeEntity;
import olympus.hephaestus.login.register.domain.EmailVerificationScene;
import olympus.hephaestus.login.register.dto.EmailRegisterResponse;
import olympus.hephaestus.login.register.dto.RegisterRequest;
import olympus.hephaestus.login.register.dto.ResetPasswordAccountItem;
import olympus.hephaestus.login.register.dto.ResetPasswordRequest;
import olympus.hephaestus.login.register.dto.SendEmailCodeRequest;
import olympus.hephaestus.login.register.dto.VerifyResetPasswordCodeRequest;
import olympus.hephaestus.login.register.dto.VerifyResetPasswordCodeResponse;
import olympus.hephaestus.login.register.repository.EmailVerificationCodeRepository;
import olympus.hephaestus.login.register.service.EmailRegisterService;
import olympus.hephaestus.login.register.support.EmailCodeGenerator;
import olympus.hephaestus.login.register.support.EmailCodeHasher;
import olympus.hephaestus.login.register.support.RegisterMailSender;
import olympus.hephaestus.org.domain.OrgPersonListRow;
import olympus.hephaestus.org.entity.OrgPersonEntity;
import olympus.hephaestus.org.repository.OrgPersonRepository;
import olympus.hephaestus.org.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class EmailRegisterServiceImpl implements EmailRegisterService {

    private static final int DEFAULT_REGISTER_UNIT_ID = 2;
    private static final int DEFAULT_EXPIRE_MINUTES = 10;
    private static final int DEFAULT_RESEND_SECONDS = 60;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final OrgPersonRepository orgPersonRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final EmailCodeHasher emailCodeHasher;
    private final EmailCodeGenerator emailCodeGenerator;
    private final RegisterMailSender registerMailSender;
    private final SystemConfigService systemConfigService;
    private final RsaPasswordCryptoService rsaPasswordCryptoService;

    public EmailRegisterServiceImpl(OrgPersonRepository orgPersonRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    EmailVerificationCodeRepository emailVerificationCodeRepository,
                                    EmailCodeHasher emailCodeHasher,
                                    EmailCodeGenerator emailCodeGenerator,
                                    RegisterMailSender registerMailSender,
                                    SystemConfigService systemConfigService,
                                    RsaPasswordCryptoService rsaPasswordCryptoService) {
        this.orgPersonRepository = orgPersonRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.emailCodeHasher = emailCodeHasher;
        this.emailCodeGenerator = emailCodeGenerator;
        this.registerMailSender = registerMailSender;
        this.systemConfigService = systemConfigService;
        this.rsaPasswordCryptoService = rsaPasswordCryptoService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailRegisterResponse sendRegisterCode(SendEmailCodeRequest request, String clientIp) {
        return sendCode(request, clientIp, EmailVerificationScene.REGISTER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailRegisterResponse sendResetPasswordCode(SendEmailCodeRequest request, String clientIp) {
        return sendCode(request, clientIp, EmailVerificationScene.RESET_PASSWORD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailRegisterResponse register(RegisterRequest request) {
        String email = requireEmail(request == null ? null : request.getEmail());
        String username = requireText(request.getUsername(), "用户名不能为空");
        String personName = requireText(request.getPersonName(), "姓名不能为空");
        String password = requireText(request.getPassword(), "密码不能为空");
        requirePasswordConfirmed(password, request.getConfirmPassword());
        String code = requireText(request.getCode(), "验证码不能为空");

        if (orgPersonRepository.countByEmail(email) >= 3) {
            throw new LoginException("同一邮箱最多注册三个账号");
        }
        if (orgPersonRepository.getByUsername(username) != null) {
            throw new LoginException("用户名已存在");
        }
        EmailVerificationCodeEntity verificationCode = verifyCode(email, EmailVerificationScene.REGISTER, code);

        long unitId = resolveRegisterUnitId();
        String resolvedPassword = resolvePassword(password, request.isEncrypted());
        OrgPersonEntity person = new OrgPersonEntity();
        person.setPersonCode("REG" + System.currentTimeMillis());
        person.setPersonName(personName);
        person.setUsername(username);
        person.setEmail(email);
        person.setPassword(resolvedPassword);
        person.setUnitId(unitId);
        person.setEnabled(true);
        person.setSourceType("REGISTER");
        orgPersonRepository.save(person);
        emailVerificationCodeRepository.markUsed(verificationCode.getId());
        return EmailRegisterResponse.success("注册成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmailRegisterResponse resetPassword(ResetPasswordRequest request) {
        String email = requireEmail(request == null ? null : request.getEmail());
        String username = requireText(request.getUsername(), "用户名不能为空");
        String password = requireText(request.getPassword(), "密码不能为空");
        requirePasswordConfirmed(password, request.getConfirmPassword());
        String code = requireText(request.getCode(), "验证码不能为空");

        OrgPersonEntity person = orgPersonRepository.getByEmailAndUsername(email, username);
        if (person == null) {
            throw new LoginException("邮箱、用户名或验证码不正确");
        }
        EmailVerificationCodeEntity verificationCode = verifyCode(email, EmailVerificationScene.RESET_PASSWORD, code);
        orgPersonRepository.updatePassword(person.getId(), resolvePassword(password, request.isEncrypted()));
        emailVerificationCodeRepository.markUsed(verificationCode.getId());
        return EmailRegisterResponse.success("密码重置成功");
    }

    @Override
    public VerifyResetPasswordCodeResponse verifyResetPasswordCode(VerifyResetPasswordCodeRequest request) {
        String email = requireEmail(request == null ? null : request.getEmail());
        String code = requireText(request.getCode(), "验证码不能为空");
        verifyCode(email, EmailVerificationScene.RESET_PASSWORD, code);
        List<ResetPasswordAccountItem> accounts = orgPersonRepository.findResetAccountsByEmail(email).stream()
                .map(this::toResetPasswordAccountItem)
                .toList();
        if (accounts.isEmpty()) {
            throw new LoginException("该邮箱未绑定可重置的账号");
        }
        return VerifyResetPasswordCodeResponse.success("验证通过", accounts);
    }

    private ResetPasswordAccountItem toResetPasswordAccountItem(OrgPersonListRow person) {
        return new ResetPasswordAccountItem(
                person.id(),
                person.username(),
                person.personName(),
                person.sourceType()
        );
    }

    private EmailRegisterResponse sendCode(SendEmailCodeRequest request, String clientIp, EmailVerificationScene scene) {
        String email = requireEmail(request == null ? null : request.getEmail());
        if (scene == EmailVerificationScene.RESET_PASSWORD && orgPersonRepository.countByEmail(email) <= 0) {
            throw new LoginException("该邮箱未绑定账号");
        }
        int resendSeconds = systemConfigService.getInt(LoginConfigConst.EMAIL_CODE_RESEND_SECONDS, DEFAULT_RESEND_SECONDS);
        EmailVerificationCodeEntity latest = emailVerificationCodeRepository.findLatest(email, scene.name());
        LocalDateTime now = LocalDateTime.now();
        if (latest != null && latest.getCreatedAt() != null && latest.getCreatedAt().plusSeconds(resendSeconds).isAfter(now)) {
            throw new LoginException("验证码发送太频繁，请稍后再试");
        }

        int expireMinutes = systemConfigService.getInt(LoginConfigConst.EMAIL_CODE_EXPIRE_MINUTES, DEFAULT_EXPIRE_MINUTES);
        String code = emailCodeGenerator.generate();
        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity();
        entity.setEmail(email);
        entity.setScene(scene.name());
        entity.setCodeHash(emailCodeHasher.hash(email, scene, code));
        entity.setExpireAt(now.plusMinutes(expireMinutes));
        entity.setSendIp(clientIp);
        entity.setCreatedAt(now);
        emailVerificationCodeRepository.save(entity);
        registerMailSender.sendVerificationCode(email, code, expireMinutes, scene);
        return EmailRegisterResponse.success("验证码已发送");
    }

    private EmailVerificationCodeEntity verifyCode(String email, EmailVerificationScene scene, String code) {
        EmailVerificationCodeEntity latest = emailVerificationCodeRepository.findLatest(email, scene.name());
        if (latest == null || latest.getUsedAt() != null || latest.getExpireAt() == null || latest.getExpireAt().isBefore(LocalDateTime.now())) {
            throw invalidCodeException(scene);
        }
        String expectedHash = emailCodeHasher.hash(email, scene, code);
        if (!expectedHash.equals(latest.getCodeHash())) {
            throw invalidCodeException(scene);
        }
        return latest;
    }

    private LoginException invalidCodeException(EmailVerificationScene scene) {
        if (scene == EmailVerificationScene.RESET_PASSWORD) {
            return new LoginException("验证码不正确或已过期，请使用最新邮件中的验证码");
        }
        return new LoginException("验证码不正确或已过期");
    }

    private long resolveRegisterUnitId() {
        int unitId = systemConfigService.getInt(LoginConfigConst.REGISTER_UNIT_ID, DEFAULT_REGISTER_UNIT_ID);
        if (unitId <= 0 || orgUnitRepository.getById((long) unitId) == null) {
            throw new LoginException("注册部门不存在，请联系管理员");
        }
        return unitId;
    }

    private String resolvePassword(String password, boolean encrypted) {
        if (encrypted && systemConfigService.getBoolean(LoginConfigConst.PASSWORD_ENCRYPT_ENABLED, false)) {
            return rsaPasswordCryptoService.decrypt(password);
        }
        return password;
    }

    private void requirePasswordConfirmed(String password, String confirmPassword) {
        if (!StringUtils.hasText(confirmPassword)) {
            throw new LoginException("确认密码不能为空");
        }
        if (!password.equals(confirmPassword)) {
            throw new LoginException("两次输入的密码不一致");
        }
    }

    private String requireEmail(String email) {
        String normalized = normalizeEmail(email);
        if (!StringUtils.hasText(normalized) || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new LoginException("邮箱格式不正确");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new LoginException(message);
        }
        return value.trim();
    }
}
