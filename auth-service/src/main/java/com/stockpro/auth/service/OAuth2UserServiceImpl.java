package com.stockpro.auth.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.stockpro.user.entity.User;
import com.stockpro.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_provider"),
                    "Only Google login is supported");
        }

        String email = (String) attributes.get("email");
        String fullName = (String) attributes.get("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email not found from Google");
        }

        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            user.setFullName(fullName);
            user.setActive(true);
            user.setLastLoginAt(LocalDateTime.now());

            if (user.getRole() == null || user.getRole().isBlank()) {
                user.setRole("STAFF");
            }

            userRepository.save(user);
        }, () -> {
            User newUser = new User();
            newUser.setFullName(fullName);
            newUser.setEmail(email);
            newUser.setPasswordHash("");
            newUser.setPhone(null);
            newUser.setRole("STAFF");
            newUser.setDepartment("GENERAL");
            newUser.setActive(true);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setLastLoginAt(LocalDateTime.now());

            userRepository.save(newUser);
        });

        return oAuth2User;
    }
}