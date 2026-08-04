# SMTP Email Configuration Guide

This document explains how to set up, configure, test, and troubleshoot SMTP email delivery in **Restaurant QR Menu SaaS**.

---

## 1. Quick Overview

The application relies on Spring Boot Starter Mail (`JavaMailSender`) to send transactional emails, such as:
1. **Password Reset Emails**: Triggered via `POST /api/v1/auth/forgot-password`.
2. **Subscription Expiration Reminders**: Automatically dispatched daily by `@Scheduled` tasks to restaurant owners whose subscriptions expire within 7 days.

---

## 2. Environment Variables Reference

All mail properties can be configured dynamically without modifying `application.yml`:

| Environment Variable | Description | Default |
|----------------------|-------------|---------|
| `MAIL_ENABLED` | Global toggle to enable/disable outbound emails | `true` |
| `MAIL_HOST` | SMTP server host address | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port (typically 587 for TLS, 465 for SSL, 2525 for Mailtrap) | `587` |
| `MAIL_USERNAME` | SMTP authentication username | `""` |
| `MAIL_PASSWORD` | SMTP authentication password / App password | `""` |
| `MAIL_FROM` | Sender email address displayed in From header | `noreply@restaurantqr.com` |
| `MAIL_SMTP_AUTH` | Enable SMTP authentication | `true` |
| `MAIL_SMTP_STARTTLS_ENABLE` | Enable STARTTLS encryption | `true` |
| `MAIL_SMTP_SSL_ENABLE` | Enable SSL encryption (e.g. port 465) | `false` |
| `MAIL_TIMEOUT` | Connection and read timeout in milliseconds | `5000` |

---

## 3. Provider Configuration Guides

### Option A: Mailtrap (Recommended for Local Development & Staging)
Mailtrap acts as a fake SMTP server that traps emails into a web inbox so test emails never leak to real recipients.

1. Sign up at [Mailtrap.io](https://mailtrap.io).
2. Create an Inbox -> Navigate to **SMTP Settings**.
3. Copy your credentials into your `.env` or IDE environment settings:
```properties
MAIL_ENABLED=true
MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=<your_mailtrap_username>
MAIL_PASSWORD=<your_mailtrap_password>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_FROM=noreply@restaurantqr.com
```

---

### Option B: Gmail SMTP
To use Gmail SMTP:
1. Enable **2-Step Verification** on your Google Account.
2. Go to Google Account -> Security -> **App Passwords**.
3. Generate a 16-character password for "Mail".
4. Configure environment variables:
```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-account@gmail.com
MAIL_PASSWORD=abcd1234efgh5678
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_FROM=your-account@gmail.com
```

---

### Option C: SendGrid SMTP
1. Log in to [SendGrid](https://sendgrid.com) -> Settings -> **API Keys**.
2. Create an API key with **Mail Send** permission.
3. Configure environment variables:
```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<your_sendgrid_api_key>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_FROM=verified-sender@yourdomain.com
```

---

### Option D: Amazon SES (Simple Email Service)
1. Verify domain or sender email in AWS SES Console.
2. Go to SES Console -> **SMTP Settings** -> Create SMTP Credentials.
3. Configure environment variables:
```properties
MAIL_ENABLED=true
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=<your_ses_smtp_username>
MAIL_PASSWORD=<your_ses_smtp_password>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_ENABLE=false
MAIL_FROM=verified-sender@yourdomain.com
```

---

## 4. Error Isolation & Graceful Fallbacks

`EmailService` is designed to isolate SMTP failures:
- If SMTP credentials are wrong, or SMTP is unreachable, `EmailService` logs an error without throwing exceptions to caller HTTP controllers.
- This ensures operations like `forgotPassword` continue to return success responses to users while allowing administrators to inspect application logs for SMTP connectivity errors.
- Setting `MAIL_ENABLED=false` safely skips email dispatch during offline testing or local development without requiring an active SMTP server.
