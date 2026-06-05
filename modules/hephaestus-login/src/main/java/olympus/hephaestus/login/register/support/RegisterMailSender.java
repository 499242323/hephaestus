package olympus.hephaestus.login.register.support;

import olympus.hephaestus.login.register.domain.EmailVerificationScene;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Component
public class RegisterMailSender {

    private static final String FROM_ADDRESS_PROPERTY = "app.mail.from-address";
    private static final String SPRING_MAIL_USERNAME_PROPERTY = "spring.mail.username";

    private final JavaMailSender mailSender;
    private final Environment environment;

    public RegisterMailSender(JavaMailSender mailSender, Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    public void sendVerificationCode(String to, String code, long expireMinutes, EmailVerificationScene scene) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(requireFromAddress());
            helper.setTo(to);
            helper.setSubject(mailSubject(scene));
            helper.setText(mailText(to, code, expireMinutes, scene), mailHtml(to, code, expireMinutes, scene));
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("验证码邮件创建失败", exception);
        }
    }

    private String mailSubject(EmailVerificationScene scene) {
        return "Hephaestus " + scenePurpose(scene) + "验证码";
    }

    private String scenePurpose(EmailVerificationScene scene) {
        return scene == EmailVerificationScene.RESET_PASSWORD ? "重置密码" : "注册账号";
    }

    private String sceneDescription(EmailVerificationScene scene) {
        if (scene == EmailVerificationScene.RESET_PASSWORD) {
            return "您正在使用邮箱验证码重置 Hephaestus 账号密码。";
        }
        return "您正在使用邮箱验证码注册 Hephaestus 账号。";
    }

    private String mailText(String to, String code, long expireMinutes, EmailVerificationScene scene) {
        return sceneDescription(scene) + "\n"
                + "邮箱：" + to + "\n"
                + "验证码用途：" + scenePurpose(scene) + "\n"
                + "验证码：" + code + "\n"
                + "此验证码将在 " + expireMinutes + " 分钟后失效。若非本人操作，请忽略本邮件。";
    }

    private String mailHtml(String to, String code, long expireMinutes, EmailVerificationScene scene) {
        String escapedTo = escapeHtml(to);
        String escapedCode = escapeHtml(code);
        String purpose = scenePurpose(scene);
        String description = sceneDescription(scene);
        return """
                <!doctype html>
                <html lang="zh-CN">
                <body style="margin:0;padding:0;background:#f6f8fb;font-family:Arial,'Microsoft YaHei',sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f8fb;padding:32px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #d9dee7;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="padding:34px 34px 26px;text-align:center;">
                              <div style="display:inline-block;width:42px;height:42px;border-radius:12px;background:#e8f1ff;color:#4040b3;line-height:42px;font-size:24px;font-weight:800;">H</div>
                              <h1 style="margin:16px 0 8px;font-size:24px;line-height:1.35;color:#111827;">%s</h1>
                              <p style="margin:0;color:#4b5563;font-size:14px;line-height:1.7;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 34px;">
                              <div style="height:1px;background:#e5e7eb;"></div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:26px 34px 8px;font-size:15px;line-height:1.8;color:#1f2937;">
                              <p style="margin:0 0 14px;">Hephaestus 收到了邮箱 <strong>%s</strong> 的%s请求。</p>
                              <p style="margin:0 0 12px;">请使用以下验证码完成操作：</p>
                              <div style="display:inline-block;padding:6px 12px;border-radius:999px;background:#eef4ff;color:#2f4f9f;font-size:13px;line-height:1.4;">验证码用途：%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:12px 34px 22px;">
                              <div style="font-size:42px;letter-spacing:8px;line-height:1.25;color:#111827;font-weight:500;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 34px 32px;font-size:14px;line-height:1.8;color:#4b5563;">
                              <p style="margin:0 0 8px;">此验证码将在 <strong>%d 分钟</strong> 后失效。</p>
                              <p style="margin:0;">若非本人操作，请忽略本邮件，不要将验证码告知他人。</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(purpose + "验证", description, escapedTo, purpose, purpose, escapedCode, expireMinutes);
    }

    private String escapeHtml(String value) {
        return String.valueOf(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String requireFromAddress() {
        String fromAddress = environment.getProperty(FROM_ADDRESS_PROPERTY);
        if (StringUtils.hasText(fromAddress)) {
            return fromAddress.trim();
        }

        String username = environment.getProperty(SPRING_MAIL_USERNAME_PROPERTY);
        if (StringUtils.hasText(username)) {
            return username.trim();
        }

        throw new IllegalStateException("未配置发件人邮箱地址 app.mail.from-address 或 spring.mail.username");
    }
}
