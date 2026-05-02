package com.stockpro.reportservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpro.reportservice.config.SecurityConfig;
import com.stockpro.reportservice.dto.response.ExecutiveDashboardResponse;
import com.stockpro.reportservice.dto.response.InventoryValuationResponse;
import com.stockpro.reportservice.service.ReportService;
import java.math.BigDecimal;
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
    void getInventoryValuation_shouldReturn200ForAdmin() throws Exception {
        when(reportService.getInventoryValuation(any())).thenReturn(new InventoryValuationResponse(
                BigDecimal.TEN, BigDecimal.ONE, 1, 1, List.of(), Map.of(), List.of()));

        mockMvc.perform(get("/api/v1/reports/inventory/valuation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getInventoryValuation_shouldReturn403ForStaff() throws Exception {
        mockMvc.perform(get("/api/v1/reports/inventory/valuation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getExecutiveDashboard_shouldReturn200ForAdmin() throws Exception {
        when(reportService.getExecutiveDashboard()).thenReturn(new ExecutiveDashboardResponse(
                0, 0, BigDecimal.ZERO, 0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/v1/reports/dashboard/executive"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void runSnapshot_shouldReturn403ForNonAdmin() throws Exception {
        doNothing().when(reportService).createInventorySnapshotForDate(any());

        mockMvc.perform(post("/api/v1/reports/snapshots/run"))
                .andExpect(status().isForbidden());
    }
}
