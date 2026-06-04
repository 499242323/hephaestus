package com.example.springaidemo.login.register.support;

import com.example.springaidemo.login.register.domain.EmailVerificationScene;
import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterMailSenderTest {

    @Test
    void sendVerificationCodeBuildsHtmlRegisterMailWithPurpose() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        Environment environment = mock(Environment.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        when(mailSender.createMimeMessage()).thenReturn(message);
        when(environment.getProperty("app.mail.from-address")).thenReturn("noreply@example.com");

        RegisterMailSender sender = new RegisterMailSender(mailSender, environment);

        sender.sendVerificationCode("user@example.com", "135790", 10, EmailVerificationScene.REGISTER);

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Hephaestus 注册账号验证码");
        assertThat(((InternetAddress) message.getFrom()[0]).getAddress()).isEqualTo("noreply@example.com");
        assertThat(readMultipartText((MimeMultipart) message.getContent()))
                .contains("您正在使用邮箱验证码注册 Hephaestus 账号。")
                .contains("Hephaestus 收到了邮箱 <strong>user@example.com</strong> 的注册账号请求。")
                .contains("135790")
                .contains("此验证码将在 <strong>10 分钟</strong> 后失效。");
    }

    @Test
    void sendVerificationCodeBuildsHtmlResetMailWithPurpose() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        Environment environment = mock(Environment.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

        when(mailSender.createMimeMessage()).thenReturn(message);
        when(environment.getProperty("app.mail.from-address")).thenReturn("");
        when(environment.getProperty("spring.mail.username")).thenReturn("mail-user@example.com");

        RegisterMailSender sender = new RegisterMailSender(mailSender, environment);

        sender.sendVerificationCode("reset@example.com", "246810", 5, EmailVerificationScene.RESET_PASSWORD);

        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Hephaestus 重置密码验证码");
        assertThat(((InternetAddress) message.getFrom()[0]).getAddress()).isEqualTo("mail-user@example.com");
        assertThat(readMultipartText((MimeMultipart) message.getContent()))
                .contains("您正在使用邮箱验证码重置 Hephaestus 账号密码。")
                .contains("重置密码请求")
                .contains("246810")
                .contains("不要将验证码告知他人");
    }

    private String readMultipartText(MimeMultipart multipart) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < multipart.getCount(); index++) {
            BodyPart bodyPart = multipart.getBodyPart(index);
            Object content = bodyPart.getContent();
            if (content instanceof MimeMultipart nested) {
                builder.append(readMultipartText(nested));
            } else {
                builder.append(content);
            }
        }
        return builder.toString();
    }
}
