package com.stockpro.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles OAuth2 login failures and redirects user to frontend with error details
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        log.error("OAuth2 login failed: {}", exception.getMessage());

        String errorMessage = "Authentication failed. Please try again.";
        if (exception.getMessage() != null) {
            errorMessage = exception.getMessage();
        }

        response.sendRedirect(
                frontendUrl + "/auth?oauth2=failed&error=" +
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)
        );
    }
}

