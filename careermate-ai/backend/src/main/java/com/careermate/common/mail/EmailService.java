package com.careermate.common.mail;

import com.careermate.common.exception.BadRequestException;
import com.careermate.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends transactional email over SMTP. When MAIL_HOST is empty the message (including the
 * password reset code) is logged instead, so local development works without an SMTP server.
 */
@Slf4j
@Service
public class EmailService {
    private final AppProperties.Mail cfg;
    private final JavaMailSenderImpl sender;

    public EmailService(AppProperties props) {
        this.cfg = props.getMail();
        this.sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getHost());
        sender.setPort(cfg.getPort());
        if (StringUtils.hasText(cfg.getUsername())) {
            sender.setUsername(cfg.getUsername());
            sender.setPassword(cfg.getPassword());
        }
        Properties mailProps = sender.getJavaMailProperties();
        mailProps.put("mail.smtp.auth", String.valueOf(cfg.isSmtpAuth()));
        mailProps.put("mail.smtp.starttls.enable", String.valueOf(cfg.isStarttls()));
        mailProps.put("mail.smtp.connectiontimeout", "10000");
        mailProps.put("mail.smtp.timeout", "10000");
        mailProps.put("mail.smtp.writetimeout", "10000");
    }

    public boolean isConfigured() {
        return StringUtils.hasText(cfg.getHost());
    }

    public void sendPasswordResetCode(String to, String name, String code, int expiryMinutes) {
        if (!isConfigured()) {
            log.warn("Mail is not configured (MAIL_HOST is empty). Password reset code for {} is {} (valid for {} minutes). "
                    + "Set MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD to send real email.", to, code, expiryMinutes);
            return;
        }
        String fromAddress = StringUtils.hasText(cfg.getFrom()) ? cfg.getFrom() : cfg.getUsername();
        String text = "Hi " + name + ",\n\n"
                + "Your CareerMate AI password reset code is: " + code + "\n\n"
                + "This code expires in " + expiryMinutes + " minutes. "
                + "If you did not request a password reset, you can safely ignore this email.\n\n"
                + "- CareerMate AI";
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, cfg.getFromName());
            helper.setTo(to);
            helper.setSubject("Your CareerMate AI password reset code");
            helper.setText(text, false);
            sender.send(message);
            log.info("Password reset code sent to {}", to);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
            throw new BadRequestException("Could not send the reset email. Please try again later.");
        }
    }
}
