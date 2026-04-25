package com.stockpro.report.client;

import com.stockpro.report.config.FeignAuthForwardingConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "${movement.service.name:MOVEMENT-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${movement.service.path:/api/v1/movements}")
public interface MovementServiceClient {

    @GetMapping("/date-range")
    List<MovementClientResponse> getMovementsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate);

    @GetMapping("/history")
    List<MovementClientResponse> getMovementHistory(@RequestParam Long productId, @RequestParam Long warehouseId);
}
