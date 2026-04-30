package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.WarehouseRequestDTO;
import com.stockpro.warehouseservice.dto.WarehouseResponseDTO;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.CapacityExceededException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import com.stockpro.warehouseservice.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;
    private WarehouseRequestDTO request;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .warehouseId(1L)
                .name("Main Warehouse")
                .location("Mumbai")
                .address("123 Main Street")
                .managerId(11L)
                .capacity(1000)
                .usedCapacity(250)
                .phone("9999999999")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        request = new WarehouseRequestDTO();
        request.setName("Main Warehouse");
        request.setLocation("Mumbai");
        request.setAddress("123 Main Street");
        request.setManagerId(11L);
        request.setCapacity(1000);
        request.setPhone("9999999999");
    }

    @Test
    void createWarehouse_shouldPersistActiveWarehouse_whenNameIsAvailable() {
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        WarehouseResponseDTO result = warehouseService.createWarehouse(request);

        assertEquals("Main Warehouse", result.getName());
        assertEquals("Mumbai", result.getLocation());
        assertTrue(result.getIsActive());
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_shouldThrowException_whenNameAlreadyExists() {
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(request)
        );

        assertEquals("Warehouse with name 'Main Warehouse' already exists", exception.getMessage());
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void getWarehouseById_shouldReturnWarehouse_whenWarehouseExists() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        WarehouseResponseDTO result = warehouseService.getWarehouseById(1L);

        assertEquals(1L, result.getWarehouseId());
        assertEquals("Main Warehouse", result.getName());
    }

    @Test
    void getWarehouseById_shouldThrowException_whenWarehouseDoesNotExist() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        WarehouseNotFoundException exception = assertThrows(
                WarehouseNotFoundException.class,
                () -> warehouseService.getWarehouseById(99L)
        );

        assertEquals("Warehouse not found with ID: 99", exception.getMessage());
    }

    @Test
    void getActiveWarehouses_shouldReturnOnlyActiveWarehouses_whenRepositoryReturnsMatches() {
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of(warehouse));

        List<WarehouseResponseDTO> result = warehouseService.getActiveWarehouses();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void updateWarehouse_shouldPersistUpdatedWarehouse_whenWarehouseExists() {
        WarehouseRequestDTO updateRequest = new WarehouseRequestDTO();
        updateRequest.setName("North Warehouse");
        updateRequest.setLocation("Delhi");
        updateRequest.setAddress("Warehouse District");
        updateRequest.setManagerId(44L);
        updateRequest.setCapacity(1200);
        updateRequest.setPhone("8888888888");

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByName("North Warehouse")).thenReturn(false);
        when(stockLevelRepository.sumQuantityByWarehouseId(1L)).thenReturn(250);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponseDTO result = warehouseService.updateWarehouse(1L, updateRequest);

        assertEquals("North Warehouse", result.getName());
        assertEquals("Delhi", result.getLocation());
        assertEquals(44L, result.getManagerId());
        assertEquals(1200, result.getCapacity());
        assertEquals(250, result.getUsedCapacity());
    }

    @Test
    void updateWarehouse_shouldThrowException_whenCapacityFallsBelowCurrentUsage() {
        WarehouseRequestDTO updateRequest = new WarehouseRequestDTO();
        updateRequest.setName("Main Warehouse");
        updateRequest.setLocation("Mumbai");
        updateRequest.setAddress("123 Main Street");
        updateRequest.setManagerId(11L);
        updateRequest.setCapacity(200);
        updateRequest.setPhone("9999999999");

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.sumQuantityByWarehouseId(1L)).thenReturn(250);

        CapacityExceededException exception = assertThrows(
                CapacityExceededException.class,
                () -> warehouseService.updateWarehouse(1L, updateRequest)
        );

        assertEquals("Warehouse capacity cannot be reduced below current stock usage: 250",
                exception.getMessage());
    }

    @Test
    void updateWarehouse_shouldThrowException_whenUpdatedNameBelongsToAnotherWarehouse() {
        Warehouse existingWarehouse = Warehouse.builder()
                .warehouseId(1L)
                .name("Old Warehouse")
                .location("Pune")
                .capacity(500)
                .usedCapacity(50)
                .isActive(true)
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existingWarehouse));
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> warehouseService.updateWarehouse(1L, request)
        );

        assertEquals("Warehouse with name 'Main Warehouse' already exists", exception.getMessage());
    }

    @Test
    void deactivateWarehouse_shouldMarkWarehouseInactive_whenWarehouseExists() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        warehouseService.deactivateWarehouse(1L);

        assertFalse(warehouse.getIsActive());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void assignManager_shouldUpdateManagerId_whenWarehouseExists() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        warehouseService.assignManager(1L, 77L);

        assertEquals(77L, warehouse.getManagerId());
        verify(warehouseRepository).save(warehouse);
    }
}
