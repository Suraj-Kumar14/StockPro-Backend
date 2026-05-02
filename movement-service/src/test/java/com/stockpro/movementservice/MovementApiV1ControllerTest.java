package com.stockpro.movementservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpro.movementservice.config.SecurityConfig;
import com.stockpro.movementservice.controller.MovementApiV1Controller;
import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementResponse;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import com.stockpro.movementservice.service.MovementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
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
                .andExpect(status().isOk());
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

    private MovementResponse sampleResponse() {
        return new MovementResponse(
                1L, "MOV-20260501-000001", 10L, null, "Widget", 20L, null, "Main Warehouse",
                MovementType.STOCK_IN, MovementDirection.IN, new BigDecimal("5.0000"), new BigDecimal("1.0000"),
                new BigDecimal("5.0000"), new BigDecimal("10.0000"), ReferenceType.GRN, "500", "GRN-500", 100L, null,
                MovementReasonCode.PURCHASE_RECEIPT, "received", null, false, LocalDateTime.now(), LocalDateTime.now(), "movement-service", "corr");
    }
}
