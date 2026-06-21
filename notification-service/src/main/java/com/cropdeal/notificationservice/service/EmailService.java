package com.cropdeal.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@cropdeal.com}")
    private String fromAddress;

    /**
     * Sends an email asynchronously.
     * Guarded by notification.email.enabled — set to false in dev to skip SMTP.
     */
    @Async
    public void sendEmail(String toEmail, String subject, String body) {
        if (!emailEnabled) {
            log.info("[EMAIL SKIPPED - dev mode] To: {} | Subject: {}", toEmail, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} | Subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            // Non-fatal — in-app notification is already persisted
        }
    }
}
