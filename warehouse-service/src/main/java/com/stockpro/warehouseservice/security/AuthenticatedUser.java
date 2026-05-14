package com.stockpro.warehouseservice.security;

public record AuthenticatedUser(Long userId, String email, String role, String token) {
}
