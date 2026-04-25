package com.stockpro.report.client;

import com.stockpro.report.config.FeignAuthForwardingConfig;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "${purchase.service.name:PURCHASE-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${purchase.service.path:/api/v1/purchase-orders}")
public interface PurchaseServiceClient {

    @GetMapping("/date-range")
    List<PurchaseOrderClientResponse> getPOsByDateRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);
}
