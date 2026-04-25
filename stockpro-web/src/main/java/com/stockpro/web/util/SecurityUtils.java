package com.stockpro.web.util;

import com.stockpro.web.security.StockProUserPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<StockProUserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof StockProUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static Long currentUserIdOrDefault(Long fallbackValue) {
        return getCurrentUser().map(StockProUserPrincipal::getUserId).orElse(fallbackValue);
    }

    public static String currentUserEmailOrDefault(String fallbackValue) {
        return getCurrentUser().map(StockProUserPrincipal::getEmail).orElse(fallbackValue);
    }

    public static boolean hasRole(String role) {
        return getCurrentUser().map(StockProUserPrincipal::getRole).filter(role::equals).isPresent();
    }
}
