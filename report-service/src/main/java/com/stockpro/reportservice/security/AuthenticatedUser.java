package com.stockpro.reportservice.security;

public record AuthenticatedUser(Long userId, String email, String role, String token) {
}
