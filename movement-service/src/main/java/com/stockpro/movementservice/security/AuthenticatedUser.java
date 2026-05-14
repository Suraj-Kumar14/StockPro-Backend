package com.stockpro.movementservice.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role,
        String token) {
}
