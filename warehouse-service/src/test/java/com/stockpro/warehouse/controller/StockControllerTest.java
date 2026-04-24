package com.stockpro.warehouse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouse.config.JwtAuthenticationFilter;
import com.stockpro.warehouse.dto.request.ReserveStockRequest;
import com.stockpro.warehouse.dto.request.UpdateStockRequest;
import com.stockpro.warehouse.dto.response.LowStockItemResponse;
import com.stockpro.warehouse.dto.response.StockLevelResponse;
import com.stockpro.warehouse.service.WarehouseService;
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

@WebMvcTest(StockController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void updateStockShouldReturnResponse() throws Exception {
        UpdateStockRequest request = UpdateStockRequest.builder()
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("25.0000"))
                .referenceId(9001L)
                .referenceType("PURCHASE_ORDER")
                .location("A-01")
                .build();

        when(warehouseService.updateStock(any(UpdateStockRequest.class))).thenReturn(StockLevelResponse.builder()
                .stockId(10L)
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("25.0000"))
                .reservedQuantity(new BigDecimal("0.0000"))
                .availableQuantity(new BigDecimal("25.0000"))
                .location("A-01")
                .lastUpdated(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build());

        mockMvc.perform(put("/api/v1/stock/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockId").value(10))
                .andExpect(jsonPath("$.quantity").value(25.0));
    }

    @Test
    void reserveStockShouldReturnResponse() throws Exception {
        ReserveStockRequest request = ReserveStockRequest.builder()
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("5.0000"))
                .referenceId(12001L)
                .referenceType("SALES_ORDER")
                .build();

        when(warehouseService.reserveStock(any(ReserveStockRequest.class))).thenReturn(StockLevelResponse.builder()
                .stockId(10L)
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("25.0000"))
                .reservedQuantity(new BigDecimal("5.0000"))
                .availableQuantity(new BigDecimal("20.0000"))
                .location("A-01")
                .build());

        mockMvc.perform(post("/api/v1/stock/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(5.0));
    }

    @Test
    void getLowStockItemsShouldReturnPage() throws Exception {
        when(warehouseService.getLowStockItems(0, 20)).thenReturn(new PageImpl<>(List.of(
                LowStockItemResponse.builder()
                        .warehouseId(1L)
                        .productId(501L)
                        .quantity(new BigDecimal("10.0000"))
                        .reservedQuantity(new BigDecimal("2.0000"))
                        .availableQuantity(new BigDecimal("8.0000"))
                        .location("A-01")
                        .build())));

        mockMvc.perform(get("/api/v1/stock/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(501));
    }
}
