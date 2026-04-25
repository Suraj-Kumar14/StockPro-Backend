package com.stockpro.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.report.config.JwtAuthenticationFilter;
import com.stockpro.report.dto.request.GenerateReportRequest;
import com.stockpro.report.dto.request.ReportFilterRequest;
import com.stockpro.report.dto.request.TakeSnapshotRequest;
import com.stockpro.report.dto.response.GeneratedReportResponse;
import com.stockpro.report.dto.response.InventorySnapshotResponse;
import com.stockpro.report.dto.response.LowStockReportResponse;
import com.stockpro.report.dto.response.TotalStockValueResponse;
import com.stockpro.report.export.ReportFormat;
import com.stockpro.report.export.ReportType;
import com.stockpro.report.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void takeSnapshotShouldReturnCreatedResponse() throws Exception {
        TakeSnapshotRequest request = TakeSnapshotRequest.builder()
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .warehouseId(1L)
                .build();

        when(reportService.takeSnapshot(any(TakeSnapshotRequest.class))).thenReturn(InventorySnapshotResponse.builder()
                .snapshotId(1L)
                .warehouseId(1L)
                .productId(101L)
                .quantity(new BigDecimal("10.0000"))
                .stockValue(new BigDecimal("200.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .createdAt(LocalDateTime.of(2026, 4, 25, 0, 0))
                .build());

        mockMvc.perform(post("/api/v1/reports/snapshot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshotId").value(1))
                .andExpect(jsonPath("$.warehouseId").value(1));
    }

    @Test
    void getTotalStockValueShouldReturnValue() throws Exception {
        when(reportService.getTotalStockValue(LocalDate.of(2026, 4, 25))).thenReturn(TotalStockValueResponse.builder()
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .totalStockValue(new BigDecimal("390.0000"))
                .build());

        mockMvc.perform(get("/api/v1/reports/total-value").param("snapshotDate", "2026-04-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStockValue").value(390.0));
    }

    @Test
    void getLowStockReportShouldReturnPage() throws Exception {
        when(reportService.getLowStockReport(any(ReportFilterRequest.class))).thenReturn(new PageImpl<>(List.of(
                LowStockReportResponse.builder()
                        .productId(101L)
                        .productName("Product 101")
                        .warehouseId(1L)
                        .availableQuantity(new BigDecimal("5.0000"))
                        .reorderLevel(new BigDecimal("10.0000"))
                        .build())));

        mockMvc.perform(post("/api/v1/reports/low-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReportFilterRequest.builder().build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(101))
                .andExpect(jsonPath("$.content[0].availableQuantity").value(5.0));
    }

    @Test
    void generateInventoryReportShouldReturnMetadata() throws Exception {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .reportType(ReportType.LOW_STOCK)
                .format(ReportFormat.CSV)
                .requestedBy("inventory.manager@stockpro.com")
                .build();

        when(reportService.generateInventoryReport(any(GenerateReportRequest.class))).thenReturn(GeneratedReportResponse.builder()
                .reportType(ReportType.LOW_STOCK)
                .format(ReportFormat.CSV)
                .fileName("low_stock.csv")
                .fileUrl("file:///tmp/low_stock.csv")
                .generatedAt(LocalDateTime.of(2026, 4, 25, 10, 0))
                .build());

        mockMvc.perform(post("/api/v1/reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("LOW_STOCK"))
                .andExpect(jsonPath("$.format").value("CSV"))
                .andExpect(jsonPath("$.fileName").value("low_stock.csv"));
    }
}
