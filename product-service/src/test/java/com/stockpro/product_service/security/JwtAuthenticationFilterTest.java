package com.stockpro.product_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "my-secret-key-ayush-chouhan-stockpro-secret-key-2024";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

	@Test
	void doFilter_shouldAuthenticateWhenTokenIsValid() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(SECRET);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/products");
		request.addHeader("Authorization", "Bearer " + buildToken());
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals(200, response.getStatus());
		assertNotNull(SecurityContextHolder.getContext().getAuthentication());
		assertInstanceOf(
				AuthenticatedUser.class,
				SecurityContextHolder.getContext().getAuthentication().getPrincipal());
	}

	@Test
	void doFilter_shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(SECRET);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/products");
		request.addHeader("Authorization", "Bearer invalid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals(401, response.getStatus());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void doFilter_shouldSkipAuthenticationWhenNoBearerTokenIsPresent() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(SECRET);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/v1/products");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals(200, response.getStatus());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

    @Test
    void authenticatedUserRecord_shouldExposeFields() {
        AuthenticatedUser user = new AuthenticatedUser(99L, "user@stockpro.com", "MANAGER", "token");

        assertEquals(99L, user.userId());
        assertEquals("user@stockpro.com", user.email());
        assertEquals("MANAGER", user.role());
        assertEquals("token", user.token());
    }

    private String buildToken() {
        return Jwts.builder()
                .setSubject("admin@stockpro.com")
                .claim("role", "ADMIN")
                .claim("userId", 10L)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}
