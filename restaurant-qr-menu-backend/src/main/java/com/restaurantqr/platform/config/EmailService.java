package com.restaurantqr.platform.config;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Enterprise Service wrapper around Spring's JavaMailSender.
 * Handles plain-text and HTML emails with configurable sender address,
 * graceful fallback when SMTP is unconfigured or disabled, and failure isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@restaurantqr.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Sends a simple plain text email.
     */
    public void sendSimpleEmail(String toEmail, String subject, String textBody) {
        if (!mailEnabled) {
            log.info("Email service disabled (MAIL_ENABLED=false). Skipping email to {}", toEmail);
            return;
        }

        try {
            var message = new SimpleMailMessage();
            message.setFrom(determineFromAddress());
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(textBody);

            mailSender.send(message);
            log.info("Plain-text email '{}' successfully sent to {}", subject, toEmail);
        } catch (Exception e) {
            log.error("Failed to send plain-text email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends an HTML formatted email.
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.info("Email service disabled (MAIL_ENABLED=false). Skipping HTML email to {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(determineFromAddress());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("HTML email '{}' successfully sent to {}", subject, toEmail);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends password reset email with action token.
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/forgot-password?token=" + resetToken;
        String subject = "Reset your QResto password";

        String textContent =
                "We received a request to reset your QResto password.\n\n" +
                "Click the link below to choose a new password (valid for 1 hour):\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email.";

        sendSimpleEmail(toEmail, subject, textContent);
    }

    /**
     * Sends subscription expiration warning to restaurant owner.
     */
    public void sendSubscriptionExpirationWarning(String toEmail, String restaurantName, int daysRemaining) {
        String subject = "Action Required: Your QResto subscription expires in " + daysRemaining + " day(s)";
        String textContent = String.format(
                "Hello,\n\nYour subscription for '%s' will expire in %d day(s).\n" +
                "Please log in to your admin dashboard to renew your subscription and avoid any menu service disruption.\n\n" +
                "Dashboard: %s\n\nThank you,\nThe QResto Team",
                restaurantName, daysRemaining, frontendUrl
        );

        sendSimpleEmail(toEmail, subject, textContent);
    }

    private String determineFromAddress() {
        if (fromAddress != null && !fromAddress.trim().isEmpty()) {
            return fromAddress;
        }
        return "noreply@restaurantqr.com";
    }
}
