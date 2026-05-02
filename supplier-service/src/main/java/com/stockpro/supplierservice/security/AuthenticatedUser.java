package com.stockpro.supplierservice.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUser extends AbstractAuthenticationToken {
    private final Long userId;
    private final String username;

    public AuthenticatedUser(Long userId, String username, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.username = username;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }
}
