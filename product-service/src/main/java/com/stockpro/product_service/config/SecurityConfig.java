package com.stockpro.product_service.config;

import com.stockpro.product_service.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

	private static final String[] PUBLIC_WHITELIST = {
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/v3/api-docs/**",
			"/v3/api-docs.yaml",

			"/actuator",
			"/actuator/",
			"/actuator/**"
	};

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtSecret);

		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> {})
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PUBLIC_WHITELIST).permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint((request, response, exception) ->
								response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized"))
						.accessDeniedHandler((request, response, exception) ->
								response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden")))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}