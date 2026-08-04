package com.restaurantqr.platform.config;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@restaurantqr.com");
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("sendSimpleEmail sends message with correct sender and recipient")
    void sendSimpleEmail_success() {
        emailService.sendSimpleEmail("user@example.com", "Test Subject", "Test Body");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@restaurantqr.com");
        assertThat(sentMessage.getTo()).containsExactly("user@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Test Subject");
        assertThat(sentMessage.getText()).isEqualTo("Test Body");
    }

    @Test
    @DisplayName("sendPasswordResetEmail constructs valid reset link")
    void sendPasswordResetEmail_success() {
        emailService.sendPasswordResetEmail("owner@example.com", "secret-token-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("owner@example.com");
        assertThat(sentMessage.getText()).contains("http://localhost:4200/forgot-password?token=secret-token-123");
    }

    @Test
    @DisplayName("sendSubscriptionExpirationWarning formats warning message properly")
    void sendSubscriptionExpirationWarning_success() {
        emailService.sendSubscriptionExpirationWarning("owner@example.com", "Tasty Bites", 3);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getSubject()).contains("3 day(s)");
        assertThat(sentMessage.getText()).contains("Tasty Bites");
        assertThat(sentMessage.getText()).contains("will expire in 3 day(s)");
    }

    @Test
    @DisplayName("sendHtmlEmail creates and sends MimeMessage")
    void sendHtmlEmail_success() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("user@example.com", "HTML Title", "<h1>Welcome</h1>");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendSimpleEmail skips dispatch when mailEnabled is false")
    void sendSimpleEmail_disabled() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        emailService.sendSimpleEmail("user@example.com", "Test Subject", "Test Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendSimpleEmail catches SMTP exception without throwing to caller")
    void sendSimpleEmail_handlesExceptionGracefully() {
        doThrow(new MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Must not throw exception
        emailService.sendSimpleEmail("user@example.com", "Test", "Body");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
