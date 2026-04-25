package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.MovementSearchRequest;
import com.stockpro.web.dto.response.ApiPageResponse;
import com.stockpro.web.dto.response.MovementResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "movementServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface MovementServiceClient {

    @GetMapping("/api/v1/movements")
    ApiPageResponse<MovementResponse> getAllMovements(@RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @GetMapping("/api/v1/movements/date-range")
    List<MovementResponse> getMovementsByDateRange(@RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate);

    @PostMapping("/api/v1/movements/search")
    ApiPageResponse<MovementResponse> searchMovements(@RequestBody MovementSearchRequest request);
}
