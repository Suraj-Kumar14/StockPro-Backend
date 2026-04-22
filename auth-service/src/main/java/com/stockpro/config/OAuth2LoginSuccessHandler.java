package com.stockpro.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.stockpro.entity.User;
import com.stockpro.repository.UserRepository;
import com.stockpro.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 Login Success Handler
 * Handles Google OAuth2 authentication and creates/updates user in database
 *
 * Role Assignment for OAuth2 Users:
 * - New users: WAREHOUSE_STAFF (default role for non-admin users)
 * - Existing users: Maintain their current role
 *
 * Department: Set to "GENERAL" for all OAuth2 users (can be updated by admin later)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Extract OAuth2 user information from Google
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        log.info("OAuth2 login attempt for email: {}", email);

        // Validate email from Google
        if (email == null || email.isBlank()) {
            log.warn("Email not received from Google");
            redirectFailure(response, "Email not received from Google");
            return;
        }

        // Google should always verify email, but we validate anyway
        if (emailVerified != null && !emailVerified) {
            log.warn("Google email is not verified: {}", email);
            redirectFailure(response, "Google email is not verified");
            return;
        }

        // Normalize email to lowercase
        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        User user;

        if (existingUser.isPresent()) {
            // User already exists - update existing user
            user = existingUser.get();

            // Check if account is deactivated by admin
            if (!user.isActive()) {
                log.warn("Account deactivated for user: {}", normalizedEmail);
                redirectFailure(response, "Your account is deactivated. Please contact admin.");
                return;
            }

            // Update full name if provided by Google
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName);
            }

            // Keep existing role (important: don't override admin-assigned roles)
            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole(Roles.WAREHOUSE_STAFF);
            }

            // Keep existing department
            if (user.getDepartment() == null || user.getDepartment().isBlank()) {
                user.setDepartment("GENERAL");
            }

            // Set password if not already set (random UUID for OAuth2 users)
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            }

            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            user = userRepository.save(user);
            log.info("Existing user logged in via OAuth2: {}", normalizedEmail);

        } else {
            // New user - create new account
            user = new User();
            user.setFullName((fullName != null && !fullName.isBlank()) ? fullName : "Google User");
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setPhone(null);

            // Assign default role for new OAuth2 users (WAREHOUSE_STAFF)
            // Admin can later update user role to INVENTORY_MANAGER, PURCHASE_OFFICER, or ADMIN
            user.setRole(Roles.WAREHOUSE_STAFF);

            // Set default department (can be updated by admin)
            user.setDepartment("GENERAL");

            // OAuth2 users are immediately active (email is verified by Google)
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());

            user = userRepository.save(user);
            log.info("New user created via OAuth2: {}", normalizedEmail);
        }

        // Generate JWT token for frontend
        String token = jwtService.generateToken(user.getEmail(), user.getUserId());

        // Redirect to frontend with token and user information
        response.sendRedirect(
                frontendUrl + "/auth?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&oauth2=google" +
                "&role=" + URLEncoder.encode(user.getRole(), StandardCharsets.UTF_8) +
                "&userId=" + user.getUserId()
        );
    }

    /**
     * Redirect to frontend with error message on OAuth2 failure
     */
    private void redirectFailure(HttpServletResponse response, String message) throws IOException {
        response.sendRedirect(
                frontendUrl + "/auth?oauth2=failed&error=" +
                URLEncoder.encode(message, StandardCharsets.UTF_8)
        );
    }
}