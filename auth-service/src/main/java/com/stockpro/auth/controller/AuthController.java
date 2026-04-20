package com.stockpro.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stockpro.auth.service.AuthService;
import com.stockpro.auth.service.OtpService;
import com.stockpro.user.dto.ApiMessageResponse;
import com.stockpro.user.dto.AuthResponse;
import com.stockpro.user.dto.ChangePasswordRequest;
import com.stockpro.user.dto.ForgotPasswordRequest;
import com.stockpro.user.dto.LoginRequest;
import com.stockpro.user.dto.OtpVerificationRequest;
import com.stockpro.user.dto.RegisterRequest;
import com.stockpro.user.dto.RegisterResponse;
import com.stockpro.user.dto.ResendOtpRequest;
import com.stockpro.user.dto.ResetPasswordRequest;
import com.stockpro.user.dto.UpdateProfileRequest;
import com.stockpro.user.dto.UserResponse;
import com.stockpro.user.entity.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);

        RegisterResponse response = RegisterResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .department(user.getDepartment())
                .emailVerified(user.isEmailVerified())
                .active(user.isActive())
                .message("User registered successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiMessageResponse> verifyRegistrationOtp(@Valid @RequestBody OtpVerificationRequest request) {
        otpService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(new ApiMessageResponse("Email verified successfully"));
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<ApiMessageResponse> resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendRegistrationOtp(request.getEmail());
        return ResponseEntity.ok(new ApiMessageResponse("Registration OTP sent successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(token, "Login successful"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        otpService.sendForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok(new ApiMessageResponse("Forgot password OTP sent successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        otpService.resetPasswordWithOtp(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(new ApiMessageResponse("Password reset successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok(new ApiMessageResponse("Logout successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String authorizationHeader) {
        String newToken = authService.refreshToken(authorizationHeader);
        return ResponseEntity.ok(new AuthResponse(newToken, "Token refreshed successfully"));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId) {
        User user = authService.getUserById(userId);

        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .department(user.getDepartment())
                .emailVerified(user.isEmailVerified())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> updateProfile(@PathVariable Long userId,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        User user = authService.updateProfile(userId, request);

        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .department(user.getDepartment())
                .emailVerified(user.isEmailVerified())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<ApiMessageResponse> changePassword(@PathVariable Long userId,
                                                             @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok(new ApiMessageResponse("Password changed successfully"));
    }

    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<ApiMessageResponse> deactivateUser(@PathVariable Long userId) {
        authService.deactivateUser(userId);
        return ResponseEntity.ok(new ApiMessageResponse("User deactivated successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = authService.getAllUsers()
                .stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole())
                        .department(user.getDepartment())
                        .emailVerified(user.isEmailVerified())
                        .active(user.isActive())
                        .createdAt(user.getCreatedAt())
                        .lastLoginAt(user.getLastLoginAt())
                        .build())
                .toList();

        return ResponseEntity.ok(users);
    }
}