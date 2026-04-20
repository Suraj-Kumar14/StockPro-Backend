package com.stockpro.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stockpro.auth.entity.EmailOtp;
import com.stockpro.auth.exception.BadRequestException;
import com.stockpro.auth.repository.EmailOtpRepository;
import com.stockpro.user.entity.User;
import com.stockpro.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final String REGISTRATION = "REGISTRATION";
    private static final String FORGOT_PASSWORD = "FORGOT_PASSWORD";

    private final EmailOtpRepository emailOtpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.otp.expiry-minutes}")
    private long otpExpiryMinutes;

    private String generateOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private EmailOtp createOtp(String email, String purpose) {
        return EmailOtp.builder()
                .email(email)
                .otp(generateOtp())
                .purpose(purpose)
                .expiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();
    }

    private void invalidatePreviousOtps(String email, String purpose) {
        List<EmailOtp> existingOtps = emailOtpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose);
        for (EmailOtp existingOtp : existingOtps) {
            existingOtp.setUsed(true);
        }
        if (!existingOtps.isEmpty()) {
            emailOtpRepository.saveAll(existingOtps);
        }
    }

    @Override
    public void sendRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        invalidatePreviousOtps(normalizedEmail, REGISTRATION);

        EmailOtp emailOtp = createOtp(normalizedEmail, REGISTRATION);
        emailOtpRepository.save(emailOtp);

        System.out.println("Registration OTP for " + normalizedEmail + " is: " + emailOtp.getOtp());

        try {
            emailService.sendOtpEmail(
                    normalizedEmail,
                    "StockPro Registration OTP",
                    emailOtp.getOtp(),
                    "registration"
            );
        } catch (Exception ex) {
            System.out.println("Registration OTP email failed, but OTP is saved in DB for: " + normalizedEmail);
        }
    }

    @Override
    public void verifyRegistrationOtp(String email, String otp) {
        String normalizedEmail = normalizeEmail(email);

        EmailOtp emailOtp = emailOtpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByIdDesc(normalizedEmail, REGISTRATION)
                .orElseThrow(() -> new BadRequestException("OTP not found"));

        if (emailOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired");
        }

        if (otp == null || otp.isBlank()) {
            throw new BadRequestException("OTP is required");
        }

        if (!emailOtp.getOtp().equals(otp.trim())) {
            throw new BadRequestException("Invalid OTP");
        }

        emailOtp.setUsed(true);
        emailOtpRepository.save(emailOtp);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendRegistrationOtp(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        sendRegistrationOtp(normalizedEmail);
    }

    @Override
    public void sendForgotPasswordOtp(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!user.isEmailVerified()) {
            throw new BadRequestException("Email is not verified");
        }

        invalidatePreviousOtps(normalizedEmail, FORGOT_PASSWORD);

        EmailOtp emailOtp = createOtp(normalizedEmail, FORGOT_PASSWORD);
        emailOtpRepository.save(emailOtp);

        System.out.println("Forgot password OTP for " + normalizedEmail + " is: " + emailOtp.getOtp());

        try {
            emailService.sendOtpEmail(
                    normalizedEmail,
                    "StockPro Forgot Password OTP",
                    emailOtp.getOtp(),
                    "password reset"
            );
        } catch (Exception ex) {
            System.out.println("Forgot password OTP email failed, but OTP is saved in DB for: " + normalizedEmail);
        }
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        String normalizedEmail = normalizeEmail(email);

        EmailOtp emailOtp = emailOtpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByIdDesc(normalizedEmail, FORGOT_PASSWORD)
                .orElseThrow(() -> new BadRequestException("OTP not found"));

        if (emailOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired");
        }

        if (otp == null || otp.isBlank()) {
            throw new BadRequestException("OTP is required");
        }

        if (!emailOtp.getOtp().equals(otp.trim())) {
            throw new BadRequestException("Invalid OTP");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailOtp.setUsed(true);
        emailOtpRepository.save(emailOtp);
    }
}