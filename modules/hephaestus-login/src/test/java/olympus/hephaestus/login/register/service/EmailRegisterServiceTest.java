package olympus.hephaestus.login.register.service;

import olympus.hephaestus.login.auth.exception.LoginException;
import olympus.hephaestus.login.auth.support.RsaPasswordCryptoService;
import olympus.hephaestus.login.config.LoginConfigConst;
import olympus.hephaestus.login.config.service.SystemConfigService;
import olympus.hephaestus.login.register.domain.EmailVerificationCodeEntity;
import olympus.hephaestus.login.register.domain.EmailVerificationScene;
import olympus.hephaestus.login.register.dto.EmailRegisterResponse;
import olympus.hephaestus.login.register.dto.RegisterRequest;
import olympus.hephaestus.login.register.dto.ResetPasswordRequest;
import olympus.hephaestus.login.register.dto.SendEmailCodeRequest;
import olympus.hephaestus.login.register.repository.EmailVerificationCodeRepository;
import olympus.hephaestus.login.register.service.impl.EmailRegisterServiceImpl;
import olympus.hephaestus.login.register.support.EmailCodeGenerator;
import olympus.hephaestus.login.register.support.EmailCodeHasher;
import olympus.hephaestus.login.register.support.RegisterMailSender;
import olympus.hephaestus.org.entity.OrgPersonEntity;
import olympus.hephaestus.org.entity.OrgUnitEntity;
import olympus.hephaestus.org.repository.OrgPersonRepository;
import olympus.hephaestus.org.repository.OrgUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailRegisterServiceTest {

    @Mock
    private OrgPersonRepository orgPersonRepository;

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @Mock
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @Mock
    private EmailCodeHasher emailCodeHasher;

    @Mock
    private EmailCodeGenerator emailCodeGenerator;

    @Mock
    private RegisterMailSender registerMailSender;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private RsaPasswordCryptoService rsaPasswordCryptoService;

    private EmailRegisterService service;

    @BeforeEach
    void setUp() {
        service = new EmailRegisterServiceImpl(
                orgPersonRepository,
                orgUnitRepository,
                emailVerificationCodeRepository,
                emailCodeHasher,
                emailCodeGenerator,
                registerMailSender,
                systemConfigService,
                rsaPasswordCryptoService
        );
    }

    @Test
    void registerRejectsFourthAccountForSameEmail() {
        RegisterRequest request = registerRequest();

        when(orgPersonRepository.countByEmail("used@example.com")).thenReturn(3L);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(LoginException.class)
                .hasMessageContaining("同一邮箱最多注册三个账号");
        verify(orgPersonRepository, never()).save(any(OrgPersonEntity.class));
    }

    @Test
    void registerSavesRegisterPersonWithConfiguredUnitAndMarksCodeUsed() {
        RegisterRequest request = registerRequest();
        OrgUnitEntity unit = new OrgUnitEntity();
        unit.setId(9L);
        EmailVerificationCodeEntity code = verificationCode(11L, EmailVerificationScene.REGISTER, "hash-123");

        when(orgPersonRepository.countByEmail("used@example.com")).thenReturn(2L);
        when(orgPersonRepository.getByUsername("new-user")).thenReturn(null);
        when(emailVerificationCodeRepository.findLatest("used@example.com", EmailVerificationScene.REGISTER.name())).thenReturn(code);
        when(emailCodeHasher.hash("used@example.com", EmailVerificationScene.REGISTER, "123456")).thenReturn("hash-123");
        when(systemConfigService.getInt(LoginConfigConst.REGISTER_UNIT_ID, 2)).thenReturn(9);
        when(orgUnitRepository.getById(9L)).thenReturn(unit);
        when(systemConfigService.getBoolean(LoginConfigConst.PASSWORD_ENCRYPT_ENABLED, false)).thenReturn(false);

        EmailRegisterResponse response = service.register(request);

        assertThat(response.isSuccess()).isTrue();
        ArgumentCaptor<OrgPersonEntity> personCaptor = ArgumentCaptor.forClass(OrgPersonEntity.class);
        verify(orgPersonRepository).save(personCaptor.capture());
        OrgPersonEntity saved = personCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("used@example.com");
        assertThat(saved.getUsername()).isEqualTo("new-user");
        assertThat(saved.getPersonName()).isEqualTo("Register User");
        assertThat(saved.getPassword()).isEqualTo("123456");
        assertThat(saved.getUnitId()).isEqualTo(9L);
        assertThat(saved.getSourceType()).isEqualTo("REGISTER");
        assertThat(saved.getEnabled()).isTrue();
        verify(emailVerificationCodeRepository).markUsed(11L);
    }

    @Test
    void resetPasswordUpdatesPasswordAndMarksCodeUsed() {
        ResetPasswordRequest request = resetPasswordRequest();
        OrgPersonEntity person = new OrgPersonEntity();
        person.setId(21L);
        EmailVerificationCodeEntity code = verificationCode(22L, EmailVerificationScene.RESET_PASSWORD, "reset-hash");

        when(orgPersonRepository.getByEmailAndUsername("reset@example.com", "reset-user")).thenReturn(person);
        when(emailVerificationCodeRepository.findLatest("reset@example.com", EmailVerificationScene.RESET_PASSWORD.name())).thenReturn(code);
        when(emailCodeHasher.hash("reset@example.com", EmailVerificationScene.RESET_PASSWORD, "654321")).thenReturn("reset-hash");
        when(systemConfigService.getBoolean(LoginConfigConst.PASSWORD_ENCRYPT_ENABLED, false)).thenReturn(false);

        EmailRegisterResponse response = service.resetPassword(request);

        assertThat(response.isSuccess()).isTrue();
        verify(orgPersonRepository).updatePassword(21L, "new-password");
        verify(emailVerificationCodeRepository).markUsed(22L);
    }

    @Test
    void sendRegisterCodeRejectsWhenLatestCodeIsTooRecent() {
        SendEmailCodeRequest request = new SendEmailCodeRequest();
        request.setEmail(" used@example.com ");
        EmailVerificationCodeEntity latest = new EmailVerificationCodeEntity();
        latest.setCreatedAt(LocalDateTime.now().minusSeconds(30));

        when(systemConfigService.getInt(LoginConfigConst.EMAIL_CODE_RESEND_SECONDS, 60)).thenReturn(60);
        when(emailVerificationCodeRepository.findLatest("used@example.com", EmailVerificationScene.REGISTER.name())).thenReturn(latest);

        assertThatThrownBy(() -> service.sendRegisterCode(request, "127.0.0.1"))
                .isInstanceOf(LoginException.class)
                .hasMessageContaining("验证码发送太频繁，请稍后再试");
        verify(emailVerificationCodeRepository, never()).save(any(EmailVerificationCodeEntity.class));
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(" Used@Example.COM ");
        request.setUsername("new-user");
        request.setPersonName("Register User");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        request.setCode("123456");
        request.setEncrypted(false);
        return request;
    }

    private ResetPasswordRequest resetPasswordRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(" Reset@Example.COM ");
        request.setUsername("reset-user");
        request.setPassword("new-password");
        request.setConfirmPassword("new-password");
        request.setCode("654321");
        request.setEncrypted(false);
        return request;
    }

    private EmailVerificationCodeEntity verificationCode(Long id, EmailVerificationScene scene, String hash) {
        EmailVerificationCodeEntity code = new EmailVerificationCodeEntity();
        code.setId(id);
        code.setEmail("used@example.com");
        code.setScene(scene.name());
        code.setCodeHash(hash);
        code.setExpireAt(LocalDateTime.now().plusMinutes(5));
        code.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        return code;
    }
}
