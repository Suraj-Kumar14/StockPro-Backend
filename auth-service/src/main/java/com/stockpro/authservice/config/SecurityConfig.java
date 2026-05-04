package com.stockpro.authservice.config;

import com.stockpro.authservice.security.JwtFilter;
import com.stockpro.authservice.security.OAuth2AuthenticationFailureHandler;
import com.stockpro.authservice.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2FailureHandler;

    public SecurityConfig(
            JwtFilter jwtFilter,
            OAuth2AuthenticationSuccessHandler oauth2SuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2FailureHandler) {
        this.jwtFilter = jwtFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/send-otp",
            "/auth/verify-otp",
            "/auth/reset-password",
            "/oauth2/**",
            "/login/oauth2/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS is centralized at the API gateway to avoid duplicate headers.
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated()
            )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redir -> redir.baseUri("/login/oauth2/code/*"))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )
            .exceptionHandling(e -> e
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
