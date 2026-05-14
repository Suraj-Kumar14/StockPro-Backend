package com.stockpro.reportservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpro.reportservice.config.SecurityConfig;
import com.stockpro.reportservice.dto.response.GeneratedInventoryReportResponse;
import com.stockpro.reportservice.dto.response.InventoryTurnoverReportResponse;
import com.stockpro.reportservice.dto.response.InventoryValuationResponse;
import com.stockpro.reportservice.dto.response.SlowMovingProductResponse;
import com.stockpro.reportservice.dto.response.TopMovingProductResponse;
import com.stockpro.reportservice.dto.response.PurchaseSummaryResponse;
import com.stockpro.reportservice.dto.response.DeadStockResponse;
import com.stockpro.reportservice.dto.response.WarehouseValuationItem;
import com.stockpro.reportservice.exception.ReportGenerationException;
import com.stockpro.reportservice.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=test-secret-test-secret-test-secret-123456")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTotalValue_shouldReturn200ForAdmin() throws Exception {
        when(reportService.getTotalStockValue(any(), any())).thenReturn(
                new InventoryValuationResponse(LocalDate.of(2026, 5, 13), BigDecimal.TEN, BigDecimal.ONE, 1, 1, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/reports/totalValue"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getTurnover_shouldReturn403ForStaff() throws Exception {
        mockMvc.perform(get("/api/v1/reports/turnover")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-13"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getLowStock_shouldReturn200ForManager() throws Exception {
        when(reportService.getLowStockReport(any())).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/reports/lowStock"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByWarehouse_shouldReturn200ForManager() throws Exception {
        when(reportService.getStockValueByWarehouse(any())).thenReturn(List.of(
                new WarehouseValuationItem(11L, "Main Warehouse", BigDecimal.ONE, BigDecimal.TEN)));

        mockMvc.perform(get("/api/v1/reports/byWarehouse"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getPoSummary_shouldReturn200ForOfficer() throws Exception {
        when(reportService.getPurchaseSummary(any())).thenReturn(
                new PurchaseSummaryResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13), 1,
                        BigDecimal.TEN, Map.of(), List.of(), List.of(), 0, 0, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/api/v1/reports/poSummary")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-13"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getPoSummary_shouldReturn200WithoutExplicitDates() throws Exception {
        when(reportService.getPurchaseSummary(any())).thenReturn(
                new PurchaseSummaryResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13), 0,
                        BigDecimal.ZERO, Map.of(), List.of(), List.of(), 0, 0, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/api/v1/reports/poSummary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchaseOrders").value(0))
                .andExpect(jsonPath("$.totalSpend").value(0));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getPoSummary_shouldReturn403ForForbiddenRole() throws Exception {
        mockMvc.perform(get("/api/v1/reports/poSummary")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-13"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getPoSummary_shouldReturn400ForInvalidDateRange() throws Exception {
        when(reportService.getPurchaseSummary(any()))
                .thenThrow(new IllegalArgumentException("fromDate cannot be after toDate"));

        mockMvc.perform(get("/api/v1/reports/poSummary")
                        .param("from", "2026-05-13")
                        .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getPoSummary_shouldReturn503ForDownstreamFailure() throws Exception {
        when(reportService.getPurchaseSummary(any()))
                .thenThrow(new ReportGenerationException("Purchase summary data is temporarily unavailable."));

        mockMvc.perform(get("/api/v1/reports/poSummary")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-13"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("DOWNSTREAM_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Purchase summary data is temporarily unavailable."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTopMoving_shouldReturn200ForAdmin() throws Exception {
        when(reportService.getTopMovingProducts(any())).thenReturn(List.of(
                new TopMovingProductResponse(1L, "Widget", "SKU-1", BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("2"), 2L, BigDecimal.TEN)));

        mockMvc.perform(get("/api/v1/reports/topMoving")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-13"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSlowMoving_shouldReturn200ForAdmin() throws Exception {
        when(reportService.getSlowMovingProducts(any(), eq(5))).thenReturn(List.of(
                new SlowMovingProductResponse(1L, "Widget", "SKU-1", BigDecimal.ONE, LocalDate.of(2026, 5, 1), 12L, BigDecimal.TEN)));

        mockMvc.perform(get("/api/v1/reports/slowMoving")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-13")
                        .param("threshold", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDeadStock_shouldReturn200ForAdmin() throws Exception {
        when(reportService.getDeadStockReport(any(), eq(90L))).thenReturn(List.of(
                new DeadStockResponse(1L, "Widget", "SKU-1", 11L, "Main Warehouse", BigDecimal.ONE, BigDecimal.TEN, LocalDate.of(2026, 1, 1), 120L)));

        mockMvc.perform(get("/api/v1/reports/deadStock")
                        .param("days", "90"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void generateReport_shouldReturn200ForAdmin() throws Exception {
        when(reportService.generateInventoryReport(any(), anyInt(), anyLong())).thenReturn(
                new GeneratedInventoryReportResponse(
                        new InventoryValuationResponse(LocalDate.of(2026, 5, 13), BigDecimal.TEN, BigDecimal.ONE, 1, 1, List.of(), List.of(), List.of()),
                        List.of(),
                        new InventoryTurnoverReportResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13), null, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, "note", List.of()),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        new PurchaseSummaryResponse(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13), 1, BigDecimal.TEN, Map.of(), List.of(), List.of(), 0, 0, 0, 0, 0, 0, 0),
                        List.of()));

        mockMvc.perform(get("/api/v1/reports/generateReport")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-13")
                        .param("threshold", "5")
                        .param("deadStockDays", "90"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void generateReport_shouldRejectInvalidThreshold() throws Exception {
        mockMvc.perform(get("/api/v1/reports/generateReport")
                        .param("threshold", "0"))
                .andExpect(status().isBadRequest());
    }
}
