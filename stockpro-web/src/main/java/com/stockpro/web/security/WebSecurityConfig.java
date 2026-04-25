package com.stockpro.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            StockProAuthenticationProvider authenticationProvider) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error/**").permitAll()
                        .requestMatchers("/admin/**").hasRole(RoleConstants.ADMIN)
                        .requestMatchers("/purchase/**").hasAnyRole(RoleConstants.ADMIN,
                                RoleConstants.PURCHASE_OFFICER,
                                RoleConstants.INVENTORY_MANAGER,
                                RoleConstants.WAREHOUSE_STAFF)
                        .requestMatchers("/inventory/**").hasAnyRole(RoleConstants.ADMIN,
                                RoleConstants.INVENTORY_MANAGER,
                                RoleConstants.WAREHOUSE_STAFF,
                                RoleConstants.PURCHASE_OFFICER)
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage("/error/403"))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
