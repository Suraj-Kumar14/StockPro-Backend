package com.stockpro.auth.service;

import java.util.List;

import com.stockpro.user.dto.ChangePasswordRequest;
import com.stockpro.user.dto.RegisterRequest;
import com.stockpro.user.dto.UpdateProfileRequest;
import com.stockpro.user.entity.User;

public interface AuthService {

    User register(RegisterRequest request);

    String login(String email, String password);

    void logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    User getUserById(Long userId);

    User getUserByEmail(String email);

    User updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void deactivateUser(Long userId);

    List<User> getAllUsers();
}