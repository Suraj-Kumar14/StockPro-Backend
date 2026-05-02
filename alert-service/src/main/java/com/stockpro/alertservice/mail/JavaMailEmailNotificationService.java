package com.stockpro.alertservice.mail;

import com.stockpro.alertservice.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Slf4j
@RequiredArgsConstructor
public class JavaMailEmailNotificationService implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Override
    public boolean sendCriticalAlertEmail(Alert alert, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send email for alert {} because recipient email could not be resolved", alert.getAlertId());
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipientEmail);
            message.setSubject("[CRITICAL] " + alert.getTitle());
            message.setText(alert.getMessage());
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email for alert {}", alert.getAlertId(), ex);
            return false;
        }
    }
}
