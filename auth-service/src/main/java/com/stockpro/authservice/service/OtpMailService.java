package com.stockpro.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.stockpro.authservice.exception.EmailDeliveryException;

@Service
public class OtpMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public OtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSignupOtp(String to, String otp) {
        send(to, "StockPro signup verification OTP",
                "Your StockPro signup OTP is " + otp + ". It will expire in 10 minutes.");
    }

    public void sendPasswordResetOtp(String to, String otp) {
        send(to, "StockPro password reset OTP",
                "Your StockPro password reset OTP is " + otp + ". It will expire in 10 minutes.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new EmailDeliveryException(
                    "Unable to send OTP email. Please verify the mail configuration and try again.",
                    ex);
        }
    }
}
