package com.stockpro.alert.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardingConfig {

    @Value("${integration.service-token:}")
    private String serviceToken;

    @Bean
    public RequestInterceptor authorizationForwardingInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

            if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(authorizationHeader)) {
                    template.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
                    return;
                }
            }

            if (StringUtils.hasText(serviceToken)) {
                String token = serviceToken.startsWith("Bearer ") ? serviceToken : "Bearer " + serviceToken;
                template.header(HttpHeaders.AUTHORIZATION, token);
            }
        };
    }
}
