package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.LoginRequest;
import com.stockpro.web.dto.request.RegisterUserRequest;
import com.stockpro.web.dto.request.UpdateUserRequest;
import com.stockpro.web.dto.response.AuthResponse;
import com.stockpro.web.dto.response.UserResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "authServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface AuthServiceClient {

    @PostMapping("/auth/user/login")
    AuthResponse login(@RequestBody LoginRequest request);

    @GetMapping("/auth/user/{email}")
    UserResponse getUserByEmail(@PathVariable("email") String email,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @GetMapping("/auth/user/all")
    List<UserResponse> getAllUsers();

    @PostMapping("/auth/user/register-request")
    String registerRequest(@RequestBody RegisterUserRequest request);

    @PostMapping("/auth/user/update-profile/{email}")
    UserResponse updateProfile(@PathVariable("email") String email, @RequestBody UpdateUserRequest request);

    @DeleteMapping("/auth/user/deactivate/{userId}")
    void deactivateUser(@PathVariable("userId") Long userId);

    @PostMapping("/auth/user/logout")
    String logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
