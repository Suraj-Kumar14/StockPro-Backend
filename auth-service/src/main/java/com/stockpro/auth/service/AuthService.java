package com.stockpro.auth.service;

import java.util.List;

import com.stockpro.user.entity.User;

public interface AuthService {

    User register(User user);

    String login(String email, String password);

    void logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    User getUserById(int userId);

    User getUserByEmail(String email);

    User updateProfile(int userId, User user);

    void changePassword(int userId, String newPassword);

    void deactivateUser(int userId);
    
    List<User> getAllUsers();
}