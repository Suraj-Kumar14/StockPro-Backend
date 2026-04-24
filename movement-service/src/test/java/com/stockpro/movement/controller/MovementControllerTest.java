package com.stockpro.movement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.MovementResponse;
import com.stockpro.movement.enums.MovementType;
import com.stockpro.movement.security.JwtAuthenticationFilter;
import com.stockpro.movement.service.MovementService;
import java.math.BigDecimal;
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

@WebMvcTest(MovementController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovementService movementService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void recordMovementShouldReturnCreatedResponse() throws Exception {
        RecordMovementRequest request = RecordMovementRequest.builder()
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("5.0000"))
                .referenceId(30L)
                .referenceType("PURCHASE_ORDER")
                .unitCost(new BigDecimal("100.0000"))
                .performedBy(99L)
                .notes("Received goods")
                .movementDate(LocalDateTime.of(2026, 4, 23, 11, 0))
                .balanceAfter(new BigDecimal("15.0000"))
                .build();

        MovementResponse response = MovementResponse.builder()
                .movementId(1L)
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("5.0000"))
                .referenceId(30L)
                .referenceType("PURCHASE_ORDER")
                .unitCost(new BigDecimal("100.0000"))
                .performedBy(99L)
                .notes("Received goods")
                .movementDate(LocalDateTime.of(2026, 4, 23, 11, 0))
                .balanceAfter(new BigDecimal("15.0000"))
                .createdAt(LocalDateTime.of(2026, 4, 23, 11, 1))
                .build();

        when(movementService.recordMovement(any(RecordMovementRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementId").value(1))
                .andExpect(jsonPath("$.movementType").value("STOCK_IN"));
    }

    @Test
    void getMovementHistoryShouldReturnHistoryList() throws Exception {
        when(movementService.getMovementHistory(10L, 20L)).thenReturn(List.of(
                MovementResponse.builder().movementId(1L).productId(10L).warehouseId(20L)
                        .movementType(MovementType.STOCK_IN).quantity(new BigDecimal("5.0000"))
                        .movementDate(LocalDateTime.of(2026, 4, 23, 11, 0)).balanceAfter(new BigDecimal("15.0000"))
                        .createdAt(LocalDateTime.of(2026, 4, 23, 11, 1)).performedBy(99L).build(),
                MovementResponse.builder().movementId(2L).productId(10L).warehouseId(20L)
                        .movementType(MovementType.STOCK_OUT).quantity(new BigDecimal("2.0000"))
                        .movementDate(LocalDateTime.of(2026, 4, 24, 11, 0)).balanceAfter(new BigDecimal("13.0000"))
                        .createdAt(LocalDateTime.of(2026, 4, 24, 11, 1)).performedBy(99L).build()));

        mockMvc.perform(get("/api/v1/movements/history")
                        .param("productId", "10")
                        .param("warehouseId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementId").value(1))
                .andExpect(jsonPath("$[1].movementType").value("STOCK_OUT"));
    }

    @Test
    void searchMovementsShouldReturnPage() throws Exception {
        MovementSearchRequest request = MovementSearchRequest.builder()
                .productId(10L)
                .page(0)
                .size(20)
                .sortBy("movementDate")
                .sortDir("desc")
                .build();

        when(movementService.searchMovements(any(MovementSearchRequest.class))).thenReturn(
                new PageImpl<>(List.of(
                        MovementResponse.builder()
                                .movementId(1L)
                                .movementType(MovementType.STOCK_IN)
                                .quantity(new BigDecimal("5.0000"))
                                .movementDate(LocalDateTime.of(2026, 4, 23, 11, 0))
                                .createdAt(LocalDateTime.of(2026, 4, 23, 11, 1))
                                .performedBy(99L)
                                .build())));

        mockMvc.perform(post("/api/v1/movements/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].movementId").value(1));
    }
}
