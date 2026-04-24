package com.stockpro.warehouse.client;

import com.stockpro.warehouse.config.FeignAuthForwardingConfig;
import com.stockpro.warehouse.dto.request.RecordMovementRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "${movement.service.name:MOVEMENT-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${movement.service.path:/api/v1/movements}")
public interface MovementServiceClient {

    @PostMapping
    void recordMovement(@RequestBody RecordMovementRequest request,
            @RequestHeader("X-Source-Service") String sourceService);
}
