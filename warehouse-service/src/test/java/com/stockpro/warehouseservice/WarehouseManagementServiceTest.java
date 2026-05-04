package com.stockpro.warehouseservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouseservice.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouseservice.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import com.stockpro.warehouseservice.service.WarehouseEventPublisher;
import com.stockpro.warehouseservice.service.WarehouseManagementService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class WarehouseManagementServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseEventPublisher warehouseEventPublisher;

    @InjectMocks
    private WarehouseManagementService warehouseManagementService;

    @Test
    void getAllWarehouses_shouldResolveNameAliasToEntityField() {
        Warehouse warehouse = Warehouse.builder()
                .warehouseId(1L)
                .name("Central Warehouse")
                .code("WH-001")
                .location("Delhi")
                .capacity(100)
                .usedCapacity(10)
                .isActive(true)
                .build();

        when(warehouseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warehouse)));

        warehouseManagementService.getAllWarehouses(true, 0, 100, "warehouseName", "asc");

        PageRequest expected = PageRequest.of(0, 100, org.springframework.data.domain.Sort.by("name").ascending());
        verify(warehouseRepository).findAll(any(Specification.class), eq(expected));
    }

    @Test
    void getAllWarehouses_shouldResolveCodeAliasToEntityField() {
        when(warehouseRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        warehouseManagementService.getAllWarehouses(null, 0, 25, "warehouseCode", "desc");

        PageRequest expected = PageRequest.of(0, 25, org.springframework.data.domain.Sort.by("code").descending());
        verify(warehouseRepository).findAll(expected);
    }

    @Test
    void getAllWarehouses_shouldRejectUnsupportedSortField() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseManagementService.getAllWarehouses(true, 0, 20, "foo", "asc"));

        assertEquals(
                "Invalid sortBy 'foo'. Allowed values: warehouseId, name, code, location, city, state, country, managerId, capacity, usedCapacity, isActive, createdAt, updatedAt",
                exception.getMessage());
    }

    @Test
    void createWarehouseRequest_shouldAcceptFrontendWarehouseFieldAliases() throws Exception {
        String json = """
                {
                  "warehouseName": "Main Warehouse",
                  "warehouseCode": "WH-MAIN-001",
                  "location": "Bhopal, Madhya Pradesh",
                  "address": "Industrial Area, Govindpura",
                  "city": "Bhopal",
                  "state": "Madhya Pradesh",
                  "country": "India",
                  "capacity": 10000,
                  "managerId": 2,
                  "phone": "9876543211",
                  "active": true
                }
                """;

        CreateWarehouseRequest request = objectMapper.readValue(json, CreateWarehouseRequest.class);

        assertEquals("Main Warehouse", request.getName());
        assertEquals("WH-MAIN-001", request.getCode());
        assertTrue(request.getIsActive());
    }

    @Test
    void updateWarehouseRequest_shouldAcceptFrontendActiveAlias() throws Exception {
        String json = """
                {
                  "warehouseName": "Main Central Warehouse",
                  "warehouseCode": "WH-MAIN-001",
                  "location": "Bhopal, Madhya Pradesh",
                  "city": "Bhopal",
                  "state": "Madhya Pradesh",
                  "country": "India",
                  "capacity": 12000,
                  "active": false
                }
                """;

        UpdateWarehouseRequest request = objectMapper.readValue(json, UpdateWarehouseRequest.class);

        assertEquals("Main Central Warehouse", request.getName());
        assertEquals("WH-MAIN-001", request.getCode());
        assertEquals(false, request.getIsActive());
    }

    @Test
    void warehouseResponse_shouldExposeTableFriendlyAliases() throws Exception {
        WarehouseResponse response = WarehouseResponse.builder()
                .warehouseId(1L)
                .name("Main Central Warehouse")
                .code("WH-MAIN-001")
                .capacity(12000)
                .usedCapacity(3000)
                .utilizationPercentage(25.0)
                .isActive(true)
                .build();

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals(1L, json.get("id").asLong());
        assertEquals("Main Central Warehouse", json.get("warehouseName").asText());
        assertEquals("WH-MAIN-001", json.get("warehouseCode").asText());
        assertTrue(json.get("active").asBoolean());
        assertEquals(25.0, json.get("utilization").asDouble());
    }
}
