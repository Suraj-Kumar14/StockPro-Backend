package com.stockpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
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
						.requestMatchers("/auth/user/**").authenticated()
						.anyRequest().authenticated())
				.headers(headers -> headers.frameOptions(frame -> frame.disable()))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}