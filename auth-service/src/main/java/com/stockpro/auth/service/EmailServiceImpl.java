package com.stockpro.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.stockpro.auth.exception.BadRequestException;
import com.stockpro.auth.exception.MailDeliveryException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@stockpro.local}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String subject, String otp, String purpose) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new BadRequestException("Recipient email is required");
        }

        if (subject == null || subject.isBlank()) {
            throw new BadRequestException("Email subject is required");
        }

        if (otp == null || otp.isBlank()) {
            throw new BadRequestException("OTP is required");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail.trim().toLowerCase());
            message.setSubject(subject);
            message.setText(
                    "Hello,\n\n" +
                    "Your OTP for " + purpose + " is: " + otp + "\n\n" +
                    "This OTP is valid for a limited time. Please do not share it with anyone.\n\n" +
                    "Regards,\nStockPro Team"
            );

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);

        } catch (MailException ex) {
            log.error("Failed to send OTP email to {}", toEmail, ex);
            throw new MailDeliveryException("Failed to send OTP email to: " + toEmail, ex);
        }
    }
}