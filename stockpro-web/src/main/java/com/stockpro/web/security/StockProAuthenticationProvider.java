package com.stockpro.web.security;

import com.stockpro.web.client.AuthServiceClient;
import com.stockpro.web.dto.request.LoginRequest;
import com.stockpro.web.dto.response.AuthResponse;
import com.stockpro.web.dto.response.UserResponse;
import com.stockpro.web.exception.ApiClientException;
import feign.FeignException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StockProAuthenticationProvider implements AuthenticationProvider {

    private final AuthServiceClient authServiceClient;

    public StockProAuthenticationProvider(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());

        try {
            AuthResponse authResponse = authServiceClient.login(new LoginRequest(email, password));
            if (!StringUtils.hasText(authResponse.getToken())) {
                throw new BadCredentialsException("Authentication token was not returned by auth-service.");
            }

            UserResponse user = authServiceClient.getUserByEmail(email, "Bearer " + authResponse.getToken());
            if (user == null) {
                throw new UsernameNotFoundException("User profile could not be loaded from auth-service.");
            }
            if (!user.isActive()) {
                throw new DisabledException("Your account is inactive. Contact an administrator.");
            }

            StockProUserPrincipal principal = new StockProUserPrincipal(
                    user.getUserId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getDepartment(),
                    user.isActive(),
                    authResponse.getToken());

            return UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    authResponse.getToken(),
                    principal.getAuthorities());
        } catch (FeignException.Unauthorized | FeignException.Forbidden ex) {
            throw new BadCredentialsException("Invalid email or password.", ex);
        } catch (ApiClientException ex) {
            if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
                throw new BadCredentialsException("Invalid email or password.", ex);
            }
            throw new BadCredentialsException(ex.getMessage(), ex);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
