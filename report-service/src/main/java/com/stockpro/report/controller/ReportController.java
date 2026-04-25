package com.stockpro.report.controller;

import com.stockpro.report.dto.request.GenerateReportRequest;
import com.stockpro.report.dto.request.ReportFilterRequest;
import com.stockpro.report.dto.request.TakeSnapshotRequest;
import com.stockpro.report.dto.response.DeadStockResponse;
import com.stockpro.report.dto.response.GeneratedReportResponse;
import com.stockpro.report.dto.response.InventorySnapshotResponse;
import com.stockpro.report.dto.response.InventoryTurnoverResponse;
import com.stockpro.report.dto.response.LowStockReportResponse;
import com.stockpro.report.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.report.dto.response.SlowMovingProductResponse;
import com.stockpro.report.dto.response.StockMovementSummaryResponse;
import com.stockpro.report.dto.response.TopMovingProductResponse;
import com.stockpro.report.dto.response.TotalStockValueResponse;
import com.stockpro.report.dto.response.WarehouseStockValueResponse;
import com.stockpro.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/reports")
@Tag(name = "Report Management", description = "Inventory analytics, snapshot creation and export APIs.")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private static final String FILTER_EXAMPLE = """
            {
              "warehouseId": 1,
              "productId": 501,
              "supplierId": 10,
              "fromDate": "2026-04-01",
              "toDate": "2026-04-25",
              "page": 0,
              "size": 20,
              "sortBy": "totalMovementQuantity",
              "sortDir": "desc"
            }
            """;

    private static final String SNAPSHOT_EXAMPLE = """
            {
              "snapshotDate": "2026-04-25",
              "warehouseId": 1
            }
            """;

    private static final String GENERATE_EXAMPLE = """
            {
              "reportType": "LOW_STOCK",
              "warehouseId": 1,
              "fromDate": "2026-04-01",
              "toDate": "2026-04-25",
              "format": "CSV",
              "requestedBy": "inventory.manager@stockpro.com"
            }
            """;

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/snapshot")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Take inventory snapshot",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "TakeSnapshot", value = SNAPSHOT_EXAMPLE))))
    public ResponseEntity<InventorySnapshotResponse> takeSnapshot(@Valid @RequestBody TakeSnapshotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.takeSnapshot(request));
    }

    @GetMapping("/total-value")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get total stock valuation for a snapshot date")
    public ResponseEntity<TotalStockValueResponse> getTotalStockValue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        return ResponseEntity.ok(reportService.getTotalStockValue(snapshotDate));
    }

    @GetMapping("/by-warehouse")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get stock valuation grouped by warehouse")
    public ResponseEntity<List<WarehouseStockValueResponse>> getStockValueByWarehouse(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        return ResponseEntity.ok(reportService.getStockValueByWarehouse(snapshotDate));
    }

    @PostMapping("/turnover")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Get inventory turnover report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "TurnoverFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<List<InventoryTurnoverResponse>> getInventoryTurnover(
            @Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getInventoryTurnover(request));
    }

    @PostMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Get low stock report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "LowStockFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<Page<LowStockReportResponse>> getLowStockReport(@Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getLowStockReport(request));
    }

    @PostMapping("/movement-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Get stock movement summary report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "MovementSummaryFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<StockMovementSummaryResponse> getStockMovementSummary(
            @Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getStockMovementSummary(request));
    }

    @PostMapping("/top-moving")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Get top moving products report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "TopMovingFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<Page<TopMovingProductResponse>> getTopMovingProducts(
            @Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getTopMovingProducts(request));
    }

    @PostMapping("/slow-moving")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'WAREHOUSE_STAFF')")
    @Operation(
            summary = "Get slow moving products report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SlowMovingFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<Page<SlowMovingProductResponse>> getSlowMovingProducts(
            @Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getSlowMovingProducts(request));
    }

    @PostMapping("/dead-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Get dead stock report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "DeadStockFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<Page<DeadStockResponse>> getDeadStock(@Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getDeadStock(request));
    }

    @PostMapping("/po-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASE_OFFICER', 'INVENTORY_MANAGER', 'MANAGER')")
    @Operation(
            summary = "Get purchase order summary report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "PurchaseOrderFilter", value = FILTER_EXAMPLE))))
    public ResponseEntity<PurchaseOrderSummaryResponse> getPoSummary(@Valid @RequestBody ReportFilterRequest request) {
        return ResponseEntity.ok(reportService.getPOSummary(request));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER')")
    @Operation(
            summary = "Generate export-ready inventory report",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "GenerateReport", value = GENERATE_EXAMPLE))))
    public ResponseEntity<GeneratedReportResponse> generateInventoryReport(
            @Valid @RequestBody GenerateReportRequest request) {
        return ResponseEntity.ok(reportService.generateInventoryReport(request));
    }
}
