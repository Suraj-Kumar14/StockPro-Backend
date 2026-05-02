package com.stockpro.alertservice.security;

public record AuthenticatedUser(Long userId, String email, String role, String token) {
}
