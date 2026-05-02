package com.stockpro.product_service.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.stockpro.product_service.dto.response.UserProfileSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthProfileGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${auth-service.base-url:http://localhost:8081}")
    private String authServiceBaseUrl;

    public Optional<Long> resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            UserProfileSnapshot profile = restClientBuilder
                    .baseUrl(authServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/auth/profile")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(UserProfileSnapshot.class);

            return Optional.ofNullable(profile).map(UserProfileSnapshot::getUserId);
        } catch (RestClientException ex) {
            log.warn("Unable to resolve actorId from auth-service profile: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
