package com.stockpro.reportservice.controller;

import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.dto.response.DeadStockResponse;
import com.stockpro.reportservice.dto.response.GeneratedInventoryReportResponse;
import com.stockpro.reportservice.dto.response.InventoryTurnoverReportResponse;
import com.stockpro.reportservice.dto.response.InventoryValuationResponse;
import com.stockpro.reportservice.dto.response.LowStockReportItem;
import com.stockpro.reportservice.dto.response.PurchaseSummaryResponse;
import com.stockpro.reportservice.dto.response.SlowMovingProductResponse;
import com.stockpro.reportservice.dto.response.TopMovingProductResponse;
import com.stockpro.reportservice.dto.response.WarehouseValuationItem;
import com.stockpro.reportservice.enums.ReportPeriod;
import com.stockpro.reportservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report Service", description = "Case-study inventory and purchase analytics APIs")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/totalValue")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','WAREHOUSE_STAFF','STAFF')")
    @Operation(summary = "Get total inventory valuation")
    public InventoryValuationResponse getTotalValue(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return reportService.getTotalStockValue(warehouseId, asOfDate);
    }

    @GetMapping("/byWarehouse")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','WAREHOUSE_STAFF','STAFF')")
    @Operation(summary = "Get stock valuation grouped by warehouse")
    public List<WarehouseValuationItem> getByWarehouse(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return reportService.getStockValueByWarehouse(asOfDate);
    }

    @GetMapping("/turnover")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    @Operation(summary = "Get inventory turnover report")
    public InventoryTurnoverReportResponse getTurnover(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId) {
        return reportService.getInventoryTurnover(from, to, warehouseId);
    }

    @GetMapping("/lowStock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER','WAREHOUSE_STAFF','STAFF')")
    @Operation(summary = "Get low-stock report")
    public Page<LowStockReportItem> getLowStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reportService.getLowStockReport(ReportFilterRequest.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .page(page)
                .size(size)
                .build());
    }

    @GetMapping("/topMoving")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    @Operation(summary = "Get top moving products")
    public List<TopMovingProductResponse> getTopMoving(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "10") int size) {
        return reportService.getTopMovingProducts(ReportFilterRequest.builder()
                .fromDate(from)
                .toDate(to)
                .warehouseId(warehouseId)
                .size(size)
                .build());
    }

    @GetMapping("/slowMoving")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    @Operation(summary = "Get slow-moving products")
    public List<SlowMovingProductResponse> getSlowMoving(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "5") int threshold) {
        validatePositive("threshold", threshold);
        return reportService.getSlowMovingProducts(ReportFilterRequest.builder()
                .fromDate(from)
                .toDate(to)
                .warehouseId(warehouseId)
                .size(Integer.MAX_VALUE)
                .build(), threshold);
    }

    @GetMapping("/deadStock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    @Operation(summary = "Get dead-stock report")
    public List<DeadStockResponse> getDeadStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "90") long days) {
        validatePositive("days", days);
        return reportService.getDeadStockReport(ReportFilterRequest.builder()
                .warehouseId(warehouseId)
                .size(Integer.MAX_VALUE)
                .build(), days);
    }

    @GetMapping("/poSummary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    @Operation(summary = "Get purchase order summary report")
    public PurchaseSummaryResponse getPoSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) ReportPeriod period) {
        return reportService.getPurchaseSummary(ReportFilterRequest.builder()
                .fromDate(from != null ? from : fromDate)
                .toDate(to != null ? to : toDate)
                .warehouseId(warehouseId)
                .supplierId(supplierId)
                .period(period)
                .size(Integer.MAX_VALUE)
                .build());
    }

    @GetMapping("/generateReport")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER','WAREHOUSE_STAFF','STAFF')")
    @Operation(summary = "Generate consolidated inventory analytics report")
    public GeneratedInventoryReportResponse generateReport(
            @ModelAttribute ReportFilterRequest request,
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "90") long deadStockDays) {
        validatePositive("threshold", threshold);
        validatePositive("deadStockDays", deadStockDays);
        return reportService.generateInventoryReport(request, threshold, deadStockDays);
    }

    private void validatePositive(String fieldName, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
