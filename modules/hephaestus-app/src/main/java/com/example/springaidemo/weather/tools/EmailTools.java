package com.example.springaidemo.weather.tools;

import jakarta.mail.internet.MimeMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EmailTools {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailTools(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    @Tool(description = "发送邮件到指定邮箱，支持自定义主题和内容")
    public String sendEmail(
            @ToolParam(description = "收件人邮箱地址") String to,
            @ToolParam(description = "邮件主题") String subject,
            @ToolParam(description = "邮件正文内容") String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(requireFromAddress());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            return "邮件发送成功，收件人：" + to + "，主题：" + subject;
        } catch (Exception exception) {
            return "邮件发送失败：" + exception.getMessage();
        }
    }

    @Tool(description = "发送HTML格式的邮件")
    public String sendHtmlEmail(
            @ToolParam(description = "收件人邮箱地址") String to,
            @ToolParam(description = "邮件主题") String subject,
            @ToolParam(description = "HTML格式邮件内容") String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(requireFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return "HTML邮件发送成功";
        } catch (Exception exception) {
            return "HTML邮件发送失败：" + exception.getMessage();
        }
    }

    private String requireFromAddress() {
        if (!StringUtils.hasText(emailProperties.getFromAddress())) {
            throw new IllegalStateException("未配置发件人邮箱地址 app.mail.from-address");
        }
        return emailProperties.getFromAddress().trim();
    }
}
