package com.stockpro.report.config;

import com.stockpro.report.security.AuthenticatedUser;
import com.stockpro.report.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                Long userId = jwtService.extractUserId(token);
                List<SimpleGrantedAuthority> authorities = buildAuthorities(role);

                if (StringUtils.hasText(email) && userId != null && jwtService.isTokenValid(token)) {
                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email, role, authorities);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception exception) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    private List<SimpleGrantedAuthority> buildAuthorities(String roleClaim) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (!StringUtils.hasText(roleClaim)) {
            return authorities;
        }

        String normalizedRole = normalizeRole(roleClaim);
        authorities.add(new SimpleGrantedAuthority(normalizedRole));

        if ("ROLE_INVENTORY_MANAGER".equals(normalizedRole)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
        }

        return authorities;
    }

    private String normalizeRole(String role) {
        String cleanedRole = role.trim().toUpperCase(Locale.ROOT);
        return cleanedRole.startsWith("ROLE_") ? cleanedRole : "ROLE_" + cleanedRole;
    }
}
