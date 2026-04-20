package com.stockpro.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stockpro.auth.exception.BadRequestException;
import com.stockpro.auth.exception.ResourceAlreadyExistsException;
import com.stockpro.auth.exception.UnauthorizedException;
import com.stockpro.security.CustomUserDetails;
import com.stockpro.security.JwtService;
import com.stockpro.user.dto.ChangePasswordRequest;
import com.stockpro.user.dto.RegisterRequest;
import com.stockpro.user.dto.UpdateProfileRequest;
import com.stockpro.user.entity.User;
import com.stockpro.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Override
    public User register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .department(request.getDepartment())
                .emailVerified(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .lastLoginAt(null)
                .build();

        User savedUser = userRepository.save(user);

        otpService.sendRegistrationOtp(savedUser.getEmail());

        return savedUser;
    }

    @Override
    public String login(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before login");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtService.generateToken(new CustomUserDetails(user));
    }

	/*
	 * @Override public void logout(String token) { // Stateless JWT logout usually
	 * needs token blacklist support. // For now, no server-side action is required.
	 * }
	 */
    
    @Override
    public void logout(String token) {
        String jwt = extractBearerToken(token);

        if (!jwtService.validateToken(jwt)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    @Override
    public boolean validateToken(String token) {
        String jwt = extractBearerToken(token);
        return jwtService.validateToken(jwt);
    }

    @Override
    public String refreshToken(String token) {
        String jwt = extractBearerToken(token);

        if (!jwtService.validateToken(jwt)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        String email = jwtService.extractEmail(jwt);
        User user = getUserByEmail(email);

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        return jwtService.generateToken(new CustomUserDetails(user));
    }

    @Override
    public User getUserById(Long userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new BadRequestException("User not found with id: " + userId);
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));
    }

    @Override
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User existingUser = getUserById(userId);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            existingUser.setFullName(request.getFullName());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            existingUser.setPhone(request.getPhone());
        }

        if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
            existingUser.setDepartment(request.getDepartment());
        }

        return userRepository.save(existingUser);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long userId) {
        User user = getUserById(userId);

        if (!user.isActive()) {
            return;
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private String extractBearerToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token is missing");
        }

        if (token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        return token;
    }
}