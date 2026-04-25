package com.stockpro.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.web.dto.response.AuthResponse;
import com.stockpro.web.exception.ApiClientException;
import com.stockpro.web.exception.ApiErrorResponse;
import com.stockpro.web.security.StockProUserPrincipal;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

@Configuration
public class FeignSupportConfig {

    @Bean
    RequestInterceptor bearerTokenRelayInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return;
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof StockProUserPrincipal userPrincipal)) {
                return;
            }

            if (StringUtils.hasText(userPrincipal.getToken())) {
                requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + userPrincipal.getToken());
            }
        };
    }

    @Bean
    ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new ErrorDecoder() {
            private final ErrorDecoder defaultDecoder = new Default();

            @Override
            public Exception decode(String methodKey, Response response) {
                String message = "Downstream service call failed.";

                if (response.body() != null) {
                    try (InputStream inputStream = response.body().asInputStream()) {
                        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        if (StringUtils.hasText(body)) {
                            try {
                                ApiErrorResponse apiError = objectMapper.readValue(body, ApiErrorResponse.class);
                                if (StringUtils.hasText(apiError.getMessage())) {
                                    message = apiError.getMessage();
                                } else {
                                    message = body;
                                }
                            } catch (IOException ignored) {
                                message = body;
                            }
                        }
                    } catch (IOException ignored) {
                        message = "Downstream service returned an unreadable error response.";
                    }
                }

                if (response.status() >= 400) {
                    return new ApiClientException(response.status(), methodKey, message);
                }

                return defaultDecoder.decode(methodKey, response);
            }
        };
    }
}
