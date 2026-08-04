package com.restaurantqr.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around Spring's JavaMailSender. Kept deliberately simple
 * (plain-text email) so it works out of the box with the SMTP credentials
 * already configured in application.yml — swap in an HTML template later
 * if you want richer branding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/forgot-password?token=" + resetToken;

        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your QResto password");
        message.setText(
                "We received a request to reset your QResto password.\n\n" +
                "Click the link below to choose a new password (valid for 1 hour):\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email."
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            // Don't let an SMTP outage surface as an error to the caller — the
            // token is already saved, so the user can still be helped manually
            // and forgotPassword() always returns a generic success response anyway.
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
