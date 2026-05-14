package com.stockpro.warehouseservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class DownstreamAuthSupportTest {

    private final DownstreamAuthSupport downstreamAuthSupport = new DownstreamAuthSupport();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void apply_shouldDoNothingWhenRequestContextIsMissing() {
        HttpHeaders headers = new HttpHeaders();

        downstreamAuthSupport.apply(headers);

        assertThat(headers).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void apply_shouldIgnoreBlankAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        HttpHeaders headers = new HttpHeaders();

        downstreamAuthSupport.apply(headers);

        assertThat(headers).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void apply_shouldIgnoreMissingAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        HttpHeaders headers = new HttpHeaders();

        downstreamAuthSupport.apply(headers);

        assertThat(headers).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }

    @Test
    void apply_shouldCopyAuthorizationHeaderWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        HttpHeaders headers = new HttpHeaders();

        downstreamAuthSupport.apply(headers);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-123");
    }
}
