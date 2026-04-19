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

import com.stockpro.security.CustomUserDetails;
import com.stockpro.security.JwtService;
import com.stockpro.user.entity.User;
import com.stockpro.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

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

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if (email == null || email.isBlank()) {
            redirectFailure(response, "Email not received from Google");
            return;
        }

        if (emailVerified != null && !emailVerified) {
            redirectFailure(response, "Google email is not verified");
            return;
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // Do not silently reactivate admin-deactivated accounts
            if (!user.isActive()) {
                redirectFailure(response, "Your account is deactivated. Please contact admin.");
                return;
            }

            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName);
            }

            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("STAFF");
            }

            if (user.getDepartment() == null || user.getDepartment().isBlank()) {
                user.setDepartment("GENERAL");
            }

            // Never use a predictable default password like GOOGLE_USER
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            }

            user.setLastLoginAt(LocalDateTime.now());
            user = userRepository.save(user);

        } else {
            user = new User();
            user.setFullName((fullName != null && !fullName.isBlank()) ? fullName : "Google User");
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setPhone(null);
            user.setRole("STAFF");
            user.setDepartment("GENERAL");
            user.setActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());

            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(new CustomUserDetails(user));

        response.sendRedirect(
                frontendUrl + "/auth?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8) +
                "&oauth2=google" +
                "&role=" + URLEncoder.encode(user.getRole(), StandardCharsets.UTF_8)
        );
    }

    private void redirectFailure(HttpServletResponse response, String message) throws IOException {
        response.sendRedirect(
                frontendUrl + "/auth?oauth2=failed&error=" +
                URLEncoder.encode(message, StandardCharsets.UTF_8)
        );
    }
}