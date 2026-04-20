package com.stockpro.auth.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String subject, String otp, String purpose);
}