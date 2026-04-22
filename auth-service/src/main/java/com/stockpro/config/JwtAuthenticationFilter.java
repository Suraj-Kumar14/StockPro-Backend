package com.stockpro.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.stockpro.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Authentication Filter
 * Intercepts every request and validates JWT token from Authorization header
 * If valid token found, sets user authentication in SecurityContext
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			// Extract JWT token from Authorization header
			String token = extractToken(request);
			if (token != null) {
				// Extract email from token
				String email = jwtService.extractEmail(token);
				// Validate token is not expired
				if (email != null && !jwtService.isTokenExpired(token)) {
					// Set authentication in SecurityContext
					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, null);
					SecurityContextHolder.getContext().setAuthentication(auth);
					log.debug("JWT token validated for user: {}", email);
				}
			}
		} catch (Exception e) {
			// Invalid token, continue without authentication
			log.debug("JWT validation failed: {}", e.getMessage());
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * Extract JWT token from Authorization header
	 * Expected format: "Bearer <token>"
	 */
	private String extractToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}
		return null;
	}
}
