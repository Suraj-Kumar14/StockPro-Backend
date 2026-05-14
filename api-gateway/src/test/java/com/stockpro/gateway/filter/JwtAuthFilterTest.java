package com.stockpro.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtAuthFilterTest {

    private static final String SECRET = "12345678901234567890123456789012";

    @Test
    void filterShouldAllowOpenPathWithoutInjectingTracingHeaders() {
        JwtAuthFilter filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);

        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, serverWebExchange -> {
            serverWebExchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void filterShouldForwardUserHeaders() {
        JwtAuthFilter filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
        String token = buildToken("admin@stockpro.local", "ADMIN");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/payments/summary")
                .header("Authorization", "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        final ServerHttpSnapshot[] snapshotRef = new ServerHttpSnapshot[1];

        GatewayFilterChain chain = serverWebExchange -> {
            snapshotRef[0] = new ServerHttpSnapshot(
                    serverWebExchange.getRequest().getHeaders().getFirst("X-User-Email"),
                    serverWebExchange.getRequest().getHeaders().getFirst("X-User-Role"));
            serverWebExchange.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        ServerHttpSnapshot snapshot = snapshotRef[0];
        assertEquals("admin@stockpro.local", snapshot.userEmail());
        assertEquals("ADMIN", snapshot.userRole());
    }

    @Test
    void filterShouldRejectUnauthorizedRequestsWithoutLeakingToken() {
        JwtAuthFilter filter = new JwtAuthFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/reports/totalValue").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, serverWebExchange -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    private String buildToken(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private record ServerHttpSnapshot(String userEmail, String userRole) {
    }
}
