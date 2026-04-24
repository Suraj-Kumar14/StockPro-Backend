package com.stockpro.movement.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public record AuthenticatedUser(
        Long userId,
        String email,
        String role,
        Collection<? extends GrantedAuthority> authorities) {
}
