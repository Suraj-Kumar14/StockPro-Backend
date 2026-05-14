package com.stockpro.paymentservice.security;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role,
        String token) {
}
