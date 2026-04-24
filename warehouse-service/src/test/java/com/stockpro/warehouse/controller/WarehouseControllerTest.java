package com.stockpro.warehouse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouse.config.JwtAuthenticationFilter;
import com.stockpro.warehouse.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouse.dto.response.WarehouseResponse;
import com.stockpro.warehouse.service.WarehouseService;
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

@WebMvcTest(WarehouseController.class)
@AutoConfigureMockMvc(addFilters = false)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createWarehouseShouldReturnCreatedResponse() throws Exception {
        CreateWarehouseRequest request = CreateWarehouseRequest.builder()
                .name("Central Hub")
                .location("Bengaluru")
                .address("Electronics City")
                .managerId(101L)
                .capacity(5000)
                .phone("+91-9876543210")
                .build();

        when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class))).thenReturn(WarehouseResponse.builder()
                .warehouseId(1L)
                .name("Central Hub")
                .location("Bengaluru")
                .address("Electronics City")
                .managerId(101L)
                .capacity(5000)
                .usedCapacity(0)
                .isActive(Boolean.TRUE)
                .phone("+91-9876543210")
                .createdAt(LocalDateTime.of(2026, 4, 24, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build());

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.name").value("Central Hub"));
    }

    @Test
    void getAllWarehousesShouldReturnPage() throws Exception {
        when(warehouseService.getAllWarehouses(0, 20, "warehouseId", "asc")).thenReturn(new PageImpl<>(List.of(
                WarehouseResponse.builder()
                        .warehouseId(1L)
                        .name("Central Hub")
                        .location("Bengaluru")
                        .address("Electronics City")
                        .managerId(101L)
                        .capacity(5000)
                        .usedCapacity(100)
                        .isActive(Boolean.TRUE)
                        .build())));

        mockMvc.perform(get("/api/v1/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].warehouseId").value(1));
    }
}
