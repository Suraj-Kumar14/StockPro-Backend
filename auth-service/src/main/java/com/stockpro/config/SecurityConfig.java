package com.stockpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable()) // - THIS IS KEY!
				.authorizeHttpRequests(authz -> authz
						.requestMatchers("/auth/**", "/swagger-ui/**", "/h2-console/**", "/v3/api-docs/**", "/actuator/**").permitAll()
						.anyRequest().authenticated());
		// For H2 Console
		http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
		return http.build();
	}
}