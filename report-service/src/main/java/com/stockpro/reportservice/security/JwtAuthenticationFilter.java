package com.stockpro.reportservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATH_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs");

    private final String secret;

    public JwtAuthenticationFilter(String secret) {
        this.secret = secret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isPublicPath(request.getRequestURI()) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String token = authorizationHeader.substring(7);
                Claims claims = parseClaims(token);
                String email = claims.getSubject();
                String role = claims.get("role", String.class);
                Long userId = claims.get("userId", Long.class);

                if (email != null && role != null) {
                    AuthenticatedUser principal = new AuthenticatedUser(userId, email, role, token);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    token,
                                    mapAuthorities(role).stream()
                                            .map(SimpleGrantedAuthority::new)
                                            .toList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Set<String> mapAuthorities(String roleClaim) {
        String normalizedRole = normalizeRole(roleClaim);
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        authorities.add("ROLE_" + normalizedRole);

        switch (normalizedRole) {
            case "INVENTORY_MANAGER" -> authorities.add("ROLE_MANAGER");
            case "PURCHASE_OFFICER" -> authorities.add("ROLE_OFFICER");
            case "WAREHOUSE_STAFF" -> authorities.add("ROLE_STAFF");
            case "MANAGER" -> authorities.add("ROLE_INVENTORY_MANAGER");
            case "OFFICER" -> authorities.add("ROLE_PURCHASE_OFFICER");
            case "STAFF" -> authorities.add("ROLE_WAREHOUSE_STAFF");
            default -> {
            }
        }

        return authorities;
    }

    private String normalizeRole(String roleClaim) {
        String normalized = roleClaim.toUpperCase(Locale.ROOT).trim();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return normalized;
    }
}
