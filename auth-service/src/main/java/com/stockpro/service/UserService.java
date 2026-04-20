package com.stockpro.service;

import com.stockpro.dtos.AuthResponse;
import com.stockpro.dtos.LoginRequest;
import com.stockpro.dtos.RegisterRequest;
import com.stockpro.dtos.UpdateProfileRequest;
import com.stockpro.dtos.UserResponseDTO;

public interface UserService {

    /*
     * AUTH - REGISTRATION
     */
    String registerRequest(RegisterRequest registerRequest);

    AuthResponse registerUser(String email, String otp);

    /*
     * AUTH - LOGIN
     */
    AuthResponse loginUser(LoginRequest loginRequest);

    /*
     * FORGOT PASSWORD FLOW
     */
    String initiateForgetPassword(String email);

    String verifyOtp(String email, String otp);

    String resetPassword(String email, String newPassword);
    
    /*
     * Update User
     */    
    UserResponseDTO updateProfile(String email, UpdateProfileRequest updateUser);
    
    public String verifyEmailUpdate(String currentEmail, String otp);
    
    public UserResponseDTO getUserByEmail(String email);

    void deactivateUser(Long id);

    void logout(String token);
}