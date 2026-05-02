package com.stockpro.product_service.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentUserContext {

    private final AuthProfileGateway authProfileGateway;

    public Long getActorId() {
        AuthenticatedUser currentUser = getCurrentUser();
        if (currentUser.userId() != null) {
            return currentUser.userId();
        }
        return authProfileGateway.resolveUserId(currentUser.token())
                .orElseThrow(() -> new InvalidProductDataException("Unable to resolve authenticated user"));
    }

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new InvalidProductDataException("Authenticated user context is not available");
        }
        return principal;
    }
}
