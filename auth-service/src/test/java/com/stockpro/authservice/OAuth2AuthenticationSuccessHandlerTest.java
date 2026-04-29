package com.stockpro.authservice;

import com.stockpro.authservice.dto.LoginResponseDTO;
import com.stockpro.authservice.security.OAuth2AuthenticationSuccessHandler;
import com.stockpro.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    @Test
    void onAuthenticationSuccess_shouldRedirectWithTokens_whenGoogleLoginSucceeds() throws Exception {
        ReflectionTestUtils.setField(successHandler, "frontendUrl", "http://localhost:4200");

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn("user@example.com");
        when(oauth2User.getAttribute("name")).thenReturn("Google User");
        when(authService.handleGoogleLogin("user@example.com", "Google User"))
                .thenReturn(new LoginResponseDTO("access token", "refresh token"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        String redirectedUrl = response.getRedirectedUrl();
        assertTrue(redirectedUrl.startsWith("http://localhost:4200/oauth-success?token="));
        assertTrue(redirectedUrl.contains("refreshToken="));
        assertTrue(redirectedUrl.contains("access+token"));
        assertTrue(redirectedUrl.contains("refresh+token"));
    }
}
