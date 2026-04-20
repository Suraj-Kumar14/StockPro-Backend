package com.stockpro.auth.service;

public interface OtpService {

    void sendRegistrationOtp(String email);

    void verifyRegistrationOtp(String email, String otp);

    void resendRegistrationOtp(String email);

    void sendForgotPasswordOtp(String email);

    void resetPasswordWithOtp(String email, String otp, String newPassword);
}