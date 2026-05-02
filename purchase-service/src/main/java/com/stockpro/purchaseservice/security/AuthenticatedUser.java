package com.stockpro.purchaseservice.security;

public record AuthenticatedUser(Long userId, String email, String role, String token) {
}
