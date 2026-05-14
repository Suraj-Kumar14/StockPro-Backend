package com.stockpro.authservice;

import com.stockpro.authservice.security.JwtFilter;
import com.stockpro.authservice.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldSkipPublicEndpoint_whenAuthLoginRequested() throws Exception {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldSkipPublicEndpoint_whenForgotPasswordRequested() throws Exception {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/forgot-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldAuthenticate_whenTokenValid() throws Exception {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("user@example.com");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractRole("valid-token")).thenReturn("STAFF");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/profile");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user@example.com", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")));
        verify(jwtUtil).validateToken("valid-token");
    }

    @Test
    void doFilter_shouldNormalizePrefixedRole_whenTokenContainsRolePrefix() throws Exception {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.isBlacklisted("prefixed-token")).thenReturn(false);
        when(jwtUtil.extractUsername("prefixed-token")).thenReturn("admin@example.com");
        when(jwtUtil.validateToken("prefixed-token")).thenReturn(true);
        when(jwtUtil.extractRole("prefixed-token")).thenReturn("ROLE_ADMIN");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/users");
        request.addHeader("Authorization", "Bearer prefixed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")));
    }

    @Test
    void doFilter_shouldReject_whenTokenBlacklisted() throws Exception {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);

        when(jwtUtil.isBlacklisted("blacklisted-token")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/profile");
        request.addHeader("Authorization", "Bearer blacklisted-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Token invalidated", response.getErrorMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
