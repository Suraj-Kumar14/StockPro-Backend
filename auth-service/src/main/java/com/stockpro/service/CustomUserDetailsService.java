package com.stockpro.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.stockpro.entity.User;
import com.stockpro.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService {

    private final UserRepository repository;

    public UserDetails loadUserByEmail(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail()) // use email, not fullName
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole()) // important
                .disabled(!user.isActive())
                .build();
    }
}