package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DownstreamAuthSupport {

    @Value("${purchase.internal-service-token:}")
    private String internalServiceToken;

    public void apply(HttpHeaders headers) {
        String token = resolveToken();
        if (StringUtils.hasText(token)) {
            headers.setBearerAuth(token);
        }
    }

    private String resolveToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.token();
        }
        if (StringUtils.hasText(internalServiceToken)) {
            return internalServiceToken;
        }
        return null;
    }
}
