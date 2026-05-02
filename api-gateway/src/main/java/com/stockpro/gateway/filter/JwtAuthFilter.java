package com.stockpro.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final List<String> OPEN_PATH_PREFIXES = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/send-otp",
            "/auth/verify-otp",
            "/auth/reset-password",
            "/oauth2/",
            "/login/oauth2/",
            "/swagger-ui",
            "/actuator/**",
            "/webjars/",
            "/v3/api-docs",
            "/alert-service/v3/api-docs",
            "/auth-service/v3/api-docs",
            "/product-service/v3/api-docs",
            "/supplier-service/v3/api-docs",
            "/warehouse-service/v3/api-docs",
            "/movement-service/v3/api-docs",
            "/purchase-service/v3/api-docs",
            "/report-service/v3/api-docs",
            "/payment-service/v3/api-docs"
    );

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Gateway request: {} {}", request.getMethod(), path);

        // Skip JWT check for open paths
        if (isOpenPath(path)) {
            log.debug("Open path — skipping JWT: {}", path);
            return chain.filter(exchange);
        }

        // Handle CORS preflight
        if (request.getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        // Check Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {}", path);
            return unauthorized(exchange, "Missing Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseToken(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            log.debug("JWT valid for user: {}, role: {}", email, role);

            // Forward user info to downstream services via headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role != null ? role : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private boolean isOpenPath(String path) {
        return OPEN_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes();
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }
}
