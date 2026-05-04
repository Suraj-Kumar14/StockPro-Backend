package com.stockpro.reportservice.controller;

import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.dto.response.AlertSummaryReportResponse;
import com.stockpro.reportservice.dto.response.DeadStockResponse;
import com.stockpro.reportservice.dto.response.ExecutiveDashboardResponse;
import com.stockpro.reportservice.dto.response.InventorySnapshotResponse;
import com.stockpro.reportservice.dto.response.InventoryTurnoverResponse;
import com.stockpro.reportservice.dto.response.InventoryValuationResponse;
import com.stockpro.reportservice.dto.response.LowStockReportItem;
import com.stockpro.reportservice.dto.response.OverstockReportItem;
import com.stockpro.reportservice.dto.response.PaymentSummaryReportResponse;
import com.stockpro.reportservice.dto.response.ProductValuationItem;
import com.stockpro.reportservice.dto.response.PurchaseSummaryResponse;
import com.stockpro.reportservice.dto.response.SlowMovingProductResponse;
import com.stockpro.reportservice.dto.response.StockMovementReportItem;
import com.stockpro.reportservice.dto.response.StockSummaryResponse;
import com.stockpro.reportservice.dto.response.SupplierPerformanceReportResponse;
import com.stockpro.reportservice.dto.response.TopMovingProductResponse;
import com.stockpro.reportservice.dto.response.WarehouseValuationItem;
import com.stockpro.reportservice.enums.ExportFormat;
import com.stockpro.reportservice.security.AuthenticatedUser;
import com.stockpro.reportservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report Service", description = "Role-based inventory, purchase, payment, alert, and dashboard reporting APIs")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/inventory/valuation")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    @Operation(summary = "Get inventory valuation report")
    public InventoryValuationResponse getInventoryValuation(@ModelAttribute ReportFilterRequest request) {
        return reportService.getInventoryValuation(request);
    }

    @GetMapping("/inventory/stock-summary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER','WAREHOUSE_STAFF','STAFF')")
    public StockSummaryResponse getStockSummary(@ModelAttribute ReportFilterRequest request) {
        return reportService.getStockSummary(request);
    }

    @GetMapping("/inventory/product-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public Page<ProductValuationItem> getProductStock(@ModelAttribute ReportFilterRequest request) {
        return reportService.getProductStockReport(request);
    }

    @GetMapping("/inventory/warehouse-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','WAREHOUSE_STAFF','STAFF')")
    public Page<WarehouseValuationItem> getWarehouseStock(@ModelAttribute ReportFilterRequest request) {
        return reportService.getWarehouseStockReport(request);
    }

    @GetMapping("/inventory/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER','WAREHOUSE_STAFF','STAFF')")
    public Page<LowStockReportItem> getLowStock(@ModelAttribute ReportFilterRequest request) {
        return reportService.getLowStockReport(request);
    }

    @GetMapping("/inventory/overstock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public Page<OverstockReportItem> getOverstock(@ModelAttribute ReportFilterRequest request) {
        return reportService.getOverstockReport(request);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public Page<StockMovementReportItem> getMovements(@ModelAttribute ReportFilterRequest request) {
        return reportService.getStockMovementReport(request);
    }

    @GetMapping("/movements/turnover")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public List<InventoryTurnoverResponse> getTurnover(@ModelAttribute ReportFilterRequest request) {
        return reportService.getInventoryTurnoverReport(request);
    }

    @GetMapping("/movements/top-moving-products")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public List<TopMovingProductResponse> getTopMoving(@ModelAttribute ReportFilterRequest request) {
        return reportService.getTopMovingProducts(request);
    }

    @GetMapping("/movements/slow-moving-products")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public List<SlowMovingProductResponse> getSlowMoving(@ModelAttribute ReportFilterRequest request) {
        return reportService.getSlowMovingProducts(request);
    }

    @GetMapping("/movements/dead-stock")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public List<DeadStockResponse> getDeadStock(@ModelAttribute ReportFilterRequest request) {
        return reportService.getDeadStockReport(request);
    }

    @GetMapping("/purchase/summary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public PurchaseSummaryResponse getPurchaseSummary(@ModelAttribute ReportFilterRequest request) {
        return reportService.getPurchaseSummary(request);
    }

    @GetMapping("/purchase/supplier-performance")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public Page<SupplierPerformanceReportResponse> getSupplierPerformance(@ModelAttribute ReportFilterRequest request) {
        return reportService.getSupplierPerformanceReport(request);
    }

    @GetMapping("/purchase/supplier-performance/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public SupplierPerformanceReportResponse getSupplierPerformanceById(@PathVariable Long supplierId, @ModelAttribute ReportFilterRequest request) {
        return reportService.getSupplierPerformance(supplierId, request);
    }

    @GetMapping("/payments/summary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public PaymentSummaryReportResponse getPaymentSummary(@ModelAttribute ReportFilterRequest request) {
        return reportService.getPaymentSummary(request);
    }

    @GetMapping("/alerts/summary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public AlertSummaryReportResponse getAlertSummary(@ModelAttribute ReportFilterRequest request) {
        return reportService.getAlertSummary(request);
    }

    @GetMapping("/dashboard/executive")
    @PreAuthorize("hasRole('ADMIN')")
    public ExecutiveDashboardResponse getExecutiveDashboard() {
        return reportService.getExecutiveDashboard();
    }

    @GetMapping("/dashboard/my")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER','WAREHOUSE_STAFF','STAFF')")
    public ExecutiveDashboardResponse getMyDashboard(Authentication authentication) {
        AuthenticatedUser user = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal ? principal : null;
        return reportService.getRoleDashboard(user != null ? user.role() : "", user != null ? user.userId() : null);
    }

    @PostMapping("/snapshots/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> runSnapshot(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        reportService.createInventorySnapshotForDate(date != null ? date : LocalDate.now());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/snapshots")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public Page<InventorySnapshotResponse> getSnapshots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reportService.getInventorySnapshots(date, page, size);
    }

    @GetMapping("/snapshots/trend")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public List<InventorySnapshotResponse> getSnapshotTrend(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return reportService.getSnapshotTrend(productId, warehouseId, fromDate, toDate);
    }

    @GetMapping("/export/inventory-valuation")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public ResponseEntity<byte[]> exportInventoryValuation(@ModelAttribute ReportFilterRequest request, @RequestParam ExportFormat format) {
        return fileResponse("inventory-valuation", format, reportService.exportInventoryValuation(request, format));
    }

    @GetMapping("/export/stock-movements")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER')")
    public ResponseEntity<byte[]> exportStockMovements(@ModelAttribute ReportFilterRequest request, @RequestParam ExportFormat format) {
        return fileResponse("stock-movements", format, reportService.exportStockMovementReport(request, format));
    }

    @GetMapping("/export/purchase-summary")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public ResponseEntity<byte[]> exportPurchaseSummary(@ModelAttribute ReportFilterRequest request, @RequestParam ExportFormat format) {
        return fileResponse("purchase-summary", format, reportService.exportPurchaseSummary(request, format));
    }

    @GetMapping("/export/supplier-performance")
    @PreAuthorize("hasAnyRole('ADMIN','INVENTORY_MANAGER','MANAGER','PURCHASE_OFFICER','OFFICER')")
    public ResponseEntity<byte[]> exportSupplierPerformance(@ModelAttribute ReportFilterRequest request, @RequestParam ExportFormat format) {
        return fileResponse("supplier-performance", format, reportService.exportSupplierPerformance(request, format));
    }

    @GetMapping("/export/executive-dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExecutiveDashboard(@RequestParam ExportFormat format) {
        return fileResponse("executive-dashboard", format, reportService.exportExecutiveDashboard(format));
    }

    private ResponseEntity<byte[]> fileResponse(String baseName, ExportFormat format, byte[] content) {
        String extension = switch (format) {
            case CSV -> "csv";
            case EXCEL -> "xlsx";
            case PDF -> "pdf";
        };
        MediaType mediaType = switch (format) {
            case CSV -> MediaType.parseMediaType("text/csv");
            case EXCEL -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case PDF -> MediaType.APPLICATION_PDF;
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment().filename(baseName + "-" + LocalDate.now() + "." + extension).build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
