package com.stockpro.reportservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpro.reportservice.config.SecurityConfig;
import com.stockpro.reportservice.dto.response.AlertSummaryReportResponse;
import com.stockpro.reportservice.dto.response.ExecutiveDashboardResponse;
import com.stockpro.reportservice.dto.response.InventorySnapshotResponse;
import com.stockpro.reportservice.dto.response.PaymentSummaryReportResponse;
import com.stockpro.reportservice.dto.response.PurchaseSummaryResponse;
import com.stockpro.reportservice.dto.response.StockSummaryResponse;
import com.stockpro.reportservice.dto.response.SupplierPerformanceReportResponse;
import com.stockpro.reportservice.enums.ExportFormat;
import com.stockpro.reportservice.security.AuthenticatedUser;
import com.stockpro.reportservice.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-test-secret-test-secret-123456")
class ReportControllerExpandedTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @WithMockUser(roles = "MANAGER")
    void inventoryAndMovementEndpoints_shouldReturnPayloadsForAuthorizedUsers() throws Exception {
        when(reportService.getStockSummary(any())).thenReturn(new StockSummaryResponse(
                2L, 1L, new BigDecimal("20.0"), new BigDecimal("2.0"), new BigDecimal("18.0"), 1L, 1L, 0L));
        when(reportService.getProductStockReport(any())).thenReturn(new PageImpl<>(List.of()));
        when(reportService.getWarehouseStockReport(any())).thenReturn(new PageImpl<>(List.of()));
        when(reportService.getLowStockReport(any())).thenReturn(new PageImpl<>(List.of()));
        when(reportService.getOverstockReport(any())).thenReturn(new PageImpl<>(List.of()));
        when(reportService.getStockMovementReport(any())).thenReturn(new PageImpl<>(List.of()));
        when(reportService.getInventoryTurnoverReport(any())).thenReturn(List.of());
        when(reportService.getTopMovingProducts(any())).thenReturn(List.of());
        when(reportService.getSlowMovingProducts(any())).thenReturn(List.of());
        when(reportService.getDeadStockReport(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/inventory/stock-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(2));

        mockMvc.perform(get("/api/v1/reports/inventory/product-stock"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/inventory/warehouse-stock"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/inventory/low-stock"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/inventory/overstock"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/movements"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/movements/turnover").param("fromDate", "2026-05-01").param("toDate", "2026-05-09"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/movements/top-moving-products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/movements/slow-moving-products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/movements/dead-stock"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void purchaseAndPaymentEndpoints_shouldReturnPayloadsForAuthorizedUsers() throws Exception {
        when(reportService.getPurchaseSummary(any())).thenReturn(new PurchaseSummaryResponse(
                3L, 1L, 1L, 1L, 0L, 1L, new BigDecimal("1000.0"), new BigDecimal("600.0"), new BigDecimal("200.0")));
        when(reportService.getSupplierPerformanceReport(any())).thenReturn(new PageImpl<>(List.of(
                new SupplierPerformanceReportResponse(1L, "Acme", 2L, 1L, 0L, new BigDecimal("500.0"), new BigDecimal("3.5"), new BigDecimal("4.8")))));
        when(reportService.getSupplierPerformance(eq(1L), any())).thenReturn(
                new SupplierPerformanceReportResponse(1L, "Acme", 2L, 1L, 0L, new BigDecimal("500.0"), new BigDecimal("3.5"), new BigDecimal("4.8")));
        when(reportService.getPaymentSummary(any())).thenReturn(new PaymentSummaryReportResponse(
                4L, 2L, 1L, 0L, new BigDecimal("700.0"), new BigDecimal("120.0"), List.of()));

        mockMvc.perform(get("/api/v1/reports/purchase/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchaseOrders").value(3));

        mockMvc.perform(get("/api/v1/reports/purchase/supplier-performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supplierName").value("Acme"));

        mockMvc.perform(get("/api/v1/reports/purchase/supplier-performance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(1));

        mockMvc.perform(get("/api/v1/reports/payments/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").value(4));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void alertDashboardSnapshotAndExportEndpoints_shouldReturnExpectedResponses() throws Exception {
        when(reportService.getAlertSummary(any())).thenReturn(new AlertSummaryReportResponse(5L, 2L, 1L, 2L, Map.of("LOW_STOCK", 3L)));
        when(reportService.getExecutiveDashboard()).thenReturn(new ExecutiveDashboardResponse(
                10L, 2L, new BigDecimal("900.0"), 1L, 1L, 2L, 1L, new BigDecimal("1200.0"), new BigDecimal("800.0"), 1L, 3L,
                List.of(), List.of(), List.of(), List.of(), List.of()));
        when(reportService.getInventorySnapshots(eq(LocalDate.of(2026, 5, 9)), eq(0), eq(10))).thenReturn(new PageImpl<>(List.of(
                new InventorySnapshotResponse(1L, LocalDate.of(2026, 5, 9), 10L, "SKU-10", "Widget", 20L, "WH-20", "Main",
                        new BigDecimal("5.0"), BigDecimal.ONE, new BigDecimal("4.0"), new BigDecimal("10.0"), new BigDecimal("50.0"), LocalDateTime.now()))));
        when(reportService.getSnapshotTrend(eq(10L), eq(20L), eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 9))))
                .thenReturn(List.of());
        when(reportService.exportInventoryValuation(any(), eq(ExportFormat.CSV))).thenReturn("csv".getBytes());
        when(reportService.exportStockMovementReport(any(), eq(ExportFormat.EXCEL))).thenReturn("excel".getBytes());
        when(reportService.exportPurchaseSummary(any(), eq(ExportFormat.PDF))).thenReturn("pdf".getBytes());
        when(reportService.exportSupplierPerformance(any(), eq(ExportFormat.CSV))).thenReturn("supplier".getBytes());
        when(reportService.exportExecutiveDashboard(eq(ExportFormat.EXCEL))).thenReturn("dashboard".getBytes());

        mockMvc.perform(get("/api/v1/reports/alerts/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").value(5));

        mockMvc.perform(get("/api/v1/reports/dashboard/executive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(10));

        mockMvc.perform(post("/api/v1/reports/snapshots/run").param("date", "2026-05-09"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reports/snapshots").param("date", "2026-05-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productSku").value("SKU-10"));

        mockMvc.perform(get("/api/v1/reports/snapshots/trend")
                        .param("productId", "10")
                        .param("warehouseId", "20")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-09"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/export/inventory-valuation").param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inventory-valuation")));

        mockMvc.perform(get("/api/v1/reports/export/stock-movements").param("format", "EXCEL"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("spreadsheetml.sheet")));

        mockMvc.perform(get("/api/v1/reports/export/purchase-summary").param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        mockMvc.perform(get("/api/v1/reports/export/supplier-performance").param("format", "CSV"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/export/executive-dashboard").param("format", "EXCEL"))
                .andExpect(status().isOk());
    }

    @Test
    void myDashboard_shouldForwardAuthenticatedUserPrincipal() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(77L, "manager@example.com", "MANAGER", "token");
        when(reportService.getRoleDashboard("MANAGER", 77L)).thenReturn(new ExecutiveDashboardResponse(
                1L, 1L, BigDecimal.ONE, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L,
                List.of(), List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/reports/dashboard/my")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(1));

        verify(reportService).getRoleDashboard("MANAGER", 77L);
    }
}
