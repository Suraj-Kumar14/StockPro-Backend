package com.stockpro.alert.client;

import com.stockpro.alert.config.FeignAuthForwardingConfig;
import com.stockpro.alert.dto.response.AuthUserResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "${auth.service.name:AUTH-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${auth.service.path:/auth/user}")
public interface AuthServiceClient {

    @GetMapping("/all")
    List<AuthUserResponse> getAllUsers();
}
