package com.stockpro.movementservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpro.movementservice.config.SecurityConfig;
import com.stockpro.movementservice.controller.MovementApiV1Controller;
import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse;
import com.stockpro.movementservice.dto.response.MovementResponse;
import com.stockpro.movementservice.dto.response.MovementSummaryResponse;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import com.stockpro.movementservice.service.MovementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = MovementApiV1Controller.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=my-secret-key-ayush-chouhan-stockpro-secret-key-2024")
class MovementApiV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovementService movementService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void postMovements_returns201ForAdmin() throws Exception {
        when(movementService.createMovement(any(CreateMovementRequest.class), eq(null))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/movements")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateMovementRequest(
                                10L, 20L, MovementType.STOCK_IN, MovementDirection.IN, new BigDecimal("5"), new BigDecimal("1"), new BigDecimal("10"),
                                ReferenceType.GRN, "500", "GRN-500", MovementReasonCode.PURCHASE_RECEIPT, "received", LocalDateTime.now(), "movement-service", "corr"))))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void postMovements_returns403ForWarehouseStaff() throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateMovementRequest(
                                10L, 20L, MovementType.STOCK_IN, MovementDirection.IN, new BigDecimal("5"), new BigDecimal("1"), new BigDecimal("10"),
                                ReferenceType.GRN, "500", "GRN-500", MovementReasonCode.PURCHASE_RECEIPT, "received", LocalDateTime.now(), "movement-service", "corr"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getMovements_returns200ForAllowedRoles() throws Exception {
        when(movementService.getAllMovements(0, 10, "movementDate", "desc")).thenReturn(new PageImpl<>(java.util.List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementNumber").value("MOV-20260501-000001"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void postReverse_returns200ForInventoryManager() throws Exception {
        when(movementService.reverseMovement(eq(1L), any(ReverseMovementRequest.class), eq(null))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/movements/1/reverse")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "Fixing a double receipt"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void postReverse_returns403ForPurchaseOfficer() throws Exception {
        mockMvc.perform(post("/api/v1/movements/1/reverse")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "Fixing a double receipt"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void searchLookupSummaryAnalyticsAndExport_shouldReturnPayloads() throws Exception {
        when(movementService.searchMovements(any())).thenReturn(new PageImpl<>(List.of(sampleResponse())));
        when(movementService.getMovementById(1L)).thenReturn(sampleResponse());
        when(movementService.getMovementByNumber("MOV-20260501-000001")).thenReturn(sampleResponse());
        when(movementService.getMovementsByProduct(10L, 0, 10)).thenReturn(new PageImpl<>(List.of(sampleResponse())));
        when(movementService.getMovementsByWarehouse(20L, 0, 10)).thenReturn(new PageImpl<>(List.of(sampleResponse())));
        when(movementService.getMovementsByReference("GRN", "500", 0, 10)).thenReturn(new PageImpl<>(List.of(sampleResponse())));
        when(movementService.getMovementSummary(any(), any())).thenReturn(new MovementSummaryResponse(
                3L, new BigDecimal("10.0000"), new BigDecimal("5.0000"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("15.0000"), 1L, 3L));
        when(movementService.getMovementAnalytics(any(), any())).thenReturn(new MovementAnalyticsResponse(
                Map.of("STOCK_IN", 1L), Map.of("Main Warehouse", 1L), Map.of("Widget", 1L),
                Map.of("2026-05-01", new BigDecimal("5.0000")), List.of(), List.of(), List.of(), List.of()));
        when(movementService.exportMovementsToCsv(any())).thenReturn("header\nrow".getBytes());

        mockMvc.perform(get("/api/v1/movements/search").param("keyword", "MOV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementId").value(1L));

        mockMvc.perform(get("/api/v1/movements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementNumber").value("MOV-20260501-000001"));

        mockMvc.perform(get("/api/v1/movements/number/MOV-20260501-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId").value(20L));

        mockMvc.perform(get("/api/v1/movements/product/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(10L));

        mockMvc.perform(get("/api/v1/movements/warehouse/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].warehouseId").value(20L));

        mockMvc.perform(get("/api/v1/movements/reference")
                        .param("referenceType", "GRN")
                        .param("referenceId", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].referenceNumber").value("GRN-500"));

        mockMvc.perform(get("/api/v1/movements/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovements").value(3));

        mockMvc.perform(get("/api/v1/movements/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementCountByType.STOCK_IN").value(1));

        mockMvc.perform(get("/api/v1/movements/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("movements-export.csv")));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void userAndValidationEndpoints_shouldRejectInvalidAccessOrBody() throws Exception {
        mockMvc.perform(get("/api/v1/movements/user/99"))
                .andExpect(status().isForbidden());

        when(movementService.getMovementsByUser(77L, 0, 10)).thenReturn(new PageImpl<>(List.of(sampleResponse())));
        mockMvc.perform(get("/api/v1/movements/user/77")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new com.stockpro.movementservice.security.AuthenticatedUser(77L, "staff@example.com", "STAFF", "token"),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].performedBy").value(100L));

        mockMvc.perform(post("/api/v1/movements")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private MovementResponse sampleResponse() {
        return new MovementResponse(
                1L, "MOV-20260501-000001", 10L, null, "Widget", 20L, null, "Main Warehouse",
                MovementType.STOCK_IN, MovementDirection.IN, new BigDecimal("5.0000"), new BigDecimal("1.0000"),
                new BigDecimal("5.0000"), new BigDecimal("10.0000"), ReferenceType.GRN, "500", "GRN-500", 100L, null,
                MovementReasonCode.PURCHASE_RECEIPT, "received", null, false, LocalDateTime.now(), LocalDateTime.now(), "movement-service", "corr");
    }
}
