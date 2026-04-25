package com.stockpro.alert.security;

import com.stockpro.alert.exception.InvalidAlertRequestException;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidAlertRequestException("Authenticated user information is required.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        throw new InvalidAlertRequestException("Authenticated user information is required.");
    }

    public boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (String role : roles) {
            String expectedAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            if (authorities.stream().anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()))) {
                return true;
            }
        }

        return false;
    }
}
