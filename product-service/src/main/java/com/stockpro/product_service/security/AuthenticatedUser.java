package com.stockpro.product_service.security;

public record AuthenticatedUser(Long userId, String email, String role, String token) {
}
