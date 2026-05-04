package com.stockpro.warehouseservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@ExtendWith(MockitoExtension.class)
class WarehouseManagementServiceTest {

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

        when(warehouseRepository.findByIsActive(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warehouse)));

        warehouseManagementService.getAllWarehouses(true, 0, 100, "warehouseName", "asc");

        PageRequest expected = PageRequest.of(0, 100, org.springframework.data.domain.Sort.by("name").ascending());
        verify(warehouseRepository).findByIsActive(true, expected);
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
                "Invalid sortBy 'foo'. Allowed values: warehouseId, name, code, location, managerId, capacity, usedCapacity, isActive, createdAt, updatedAt",
                exception.getMessage());
    }
}
