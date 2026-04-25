package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.GenerateReportRequest;
import com.stockpro.web.dto.request.ReportFilterRequest;
import com.stockpro.web.dto.response.ApiPageResponse;
import com.stockpro.web.dto.response.DeadStockResponse;
import com.stockpro.web.dto.response.GeneratedReportResponse;
import com.stockpro.web.dto.response.InventoryTurnoverResponse;
import com.stockpro.web.dto.response.LowStockReportResponse;
import com.stockpro.web.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.web.dto.response.TopMovingProductResponse;
import com.stockpro.web.dto.response.TotalStockValueResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "reportServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface ReportServiceClient {

    @GetMapping("/api/v1/reports/total-value")
    TotalStockValueResponse getTotalStockValue(@RequestParam(required = false) LocalDate snapshotDate);

    @PostMapping("/api/v1/reports/turnover")
    List<InventoryTurnoverResponse> getInventoryTurnover(@RequestBody ReportFilterRequest request);

    @PostMapping("/api/v1/reports/low-stock")
    ApiPageResponse<LowStockReportResponse> getLowStockReport(@RequestBody ReportFilterRequest request);

    @PostMapping("/api/v1/reports/top-moving")
    ApiPageResponse<TopMovingProductResponse> getTopMovingProducts(@RequestBody ReportFilterRequest request);

    @PostMapping("/api/v1/reports/dead-stock")
    ApiPageResponse<DeadStockResponse> getDeadStock(@RequestBody ReportFilterRequest request);

    @PostMapping("/api/v1/reports/generate")
    GeneratedReportResponse generateInventoryReport(@RequestBody GenerateReportRequest request);

    @PostMapping("/api/v1/reports/po-summary")
    PurchaseOrderSummaryResponse getPurchaseOrderSummary(@RequestBody ReportFilterRequest request);
}
