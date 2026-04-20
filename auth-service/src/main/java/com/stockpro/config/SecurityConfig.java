package com.stockpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/user/welcome",
            "/auth/user/register-request",
            "/auth/user/register-user",
            "/auth/user/login",
            "/auth/user/forgot-password/**",
            "/swagger-ui/**",
            "/h2-console/**",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // admin-only APIs
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")

                        // example role-based module paths
                        .requestMatchers("/inventory/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers("/warehouse/**").hasAnyRole("ADMIN", "WAREHOUSE_STAFF")
                        .requestMatchers("/purchase/**").hasAnyRole("ADMIN", "PURCHASE_OFFICER")

                        // all auth/user APIs require login
                        .requestMatchers("/auth/user/**").authenticated()

                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}