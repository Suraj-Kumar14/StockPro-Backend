package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.dto.StockUpdateDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.rabbitmq.StockEventPublisher;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import com.stockpro.warehouseservice.service.InventoryOperationService;
import com.stockpro.warehouseservice.service.StockAlertService;
import com.stockpro.warehouseservice.service.StockLevelService;
import com.stockpro.warehouseservice.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockAlertService stockAlertService;

    @Mock
    private InventoryOperationService inventoryOperationService;

    @InjectMocks
    private StockLevelService stockLevelService;

    private StockLevel stockLevel;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        stockLevel = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(80)
                .reservedQuantity(10)
                .reorderLevel(10)
                .maxStockLevel(200)
                .binLocation("R1-S1")
                .lastUpdated(LocalDateTime.now())
                .build();

        warehouse = Warehouse.builder()
                .warehouseId(1L)
                .name("Main Warehouse")
                .capacity(500)
                .usedCapacity(80)
                .isActive(true)
                .build();
    }

    @Test
    void getStockByWarehouse_shouldReturnStockLevels_whenWarehouseExists() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseId(1L)).thenReturn(List.of(stockLevel));

        List<StockLevelResponseDTO> result = stockLevelService.getStockByWarehouse(1L);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getProductId());
        assertEquals(70, result.get(0).getAvailableQuantity());
    }

    @Test
    void getStockByWarehouse_shouldThrowException_whenWarehouseDoesNotExist() {
        when(warehouseRepository.existsById(99L)).thenReturn(false);

        WarehouseNotFoundException exception = assertThrows(
                WarehouseNotFoundException.class,
                () -> stockLevelService.getStockByWarehouse(99L)
        );

        assertEquals("Warehouse not found with ID: 99", exception.getMessage());
        verify(stockLevelRepository, never()).findByWarehouseId(99L);
    }

    @Test
    void getStockByProduct_shouldReturnStockAcrossWarehouses_whenProductIsTracked() {
        StockLevel secondWarehouseStock = StockLevel.builder()
                .stockId(2L)
                .warehouseId(2L)
                .productId(1001L)
                .quantity(30)
                .reservedQuantity(5)
                .binLocation("R2-S4")
                .build();

        when(stockLevelRepository.findByProductId(1001L))
                .thenReturn(List.of(stockLevel, secondWarehouseStock));

        List<StockLevelResponseDTO> result = stockLevelService.getStockByProduct(1001L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getWarehouseId());
        assertEquals(2L, result.get(1).getWarehouseId());
    }

    @Test
    void updateStock_shouldCreateNewStockRecord_whenStockDoesNotExist() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(2002L);
        request.setQuantity(45);
        request.setBinLocation("NEW-BIN");

        StockLevel newStock = StockLevel.builder()
                .warehouseId(1L)
                .productId(2002L)
                .quantity(0)
                .reservedQuantity(0)
                .build();

        StockLevel createdStock = StockLevel.builder()
                .stockId(3L)
                .warehouseId(1L)
                .productId(2002L)
                .quantity(45)
                .reservedQuantity(0)
                .reorderLevel(10)
                .maxStockLevel(100)
                .binLocation("NEW-BIN")
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(warehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 2002L)).thenReturn(newStock);
        when(inventoryOperationService.resolveThresholds(2002L, null, null))
                .thenReturn(new InventoryOperationService.ThresholdSettings(10, 100));
        when(inventoryOperationService.handleAdjustment(warehouse, newStock, 45, null))
                .thenAnswer(invocation -> {
                    newStock.setQuantity(45);
                    return new InventoryOperationService.StockMutation(
                            "RECEIPT", 45, 0, 45, "Stock receipt processed via stock update endpoint");
                });
        when(inventoryOperationService.saveStockLevel(newStock)).thenReturn(createdStock);
        when(inventoryOperationService.saveWarehouse(warehouse)).thenReturn(warehouse);

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, request);

        assertEquals(45, result.getQuantity());
        assertEquals("NEW-BIN", result.getBinLocation());
        verify(stockMovementService).recordReceipt(1L, 2002L, 45, 0, 45,
                "Stock receipt processed via stock update endpoint");
    }

    @Test
    void updateStock_shouldPublishLowStockEvent_whenAvailableQuantityFallsBelowReorderLevel() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(1001L);
        request.setQuantity(10);
        request.setReorderLevel(10);

        StockLevel savedStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(10)
                .reservedQuantity(1)
                .reorderLevel(10)
                .maxStockLevel(200)
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(warehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 1001L)).thenReturn(stockLevel);
        when(inventoryOperationService.resolveThresholds(1001L, 10, null))
                .thenReturn(new InventoryOperationService.ThresholdSettings(10, 200));
        when(inventoryOperationService.handleAdjustment(warehouse, stockLevel, 10, null))
                .thenAnswer(invocation -> {
                    stockLevel.setQuantity(10);
                    stockLevel.setReservedQuantity(1);
                    return new InventoryOperationService.StockMutation(
                            "ISSUE", -70, 80, 10, "Stock issue processed via stock update endpoint");
                });
        when(inventoryOperationService.saveStockLevel(stockLevel)).thenReturn(savedStock);
        when(inventoryOperationService.saveWarehouse(warehouse)).thenReturn(warehouse);

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, request);

        assertEquals(10, result.getQuantity());
        verify(stockEventPublisher).publishLowStockEvent(1001L, 1L, 9, 10);
    }

    @Test
    void updateStock_shouldPublishOverstockEvent_whenQuantityExceedsMaximumLevel() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(1001L);
        request.setQuantity(250);
        request.setMaxStockLevel(200);

        StockLevel savedStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(250)
                .reservedQuantity(0)
                .reorderLevel(10)
                .maxStockLevel(200)
                .binLocation("R1-S1")
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(warehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 1001L)).thenReturn(stockLevel);
        when(inventoryOperationService.resolveThresholds(1001L, null, 200))
                .thenReturn(new InventoryOperationService.ThresholdSettings(10, 200));
        when(inventoryOperationService.handleAdjustment(warehouse, stockLevel, 250, null))
                .thenAnswer(invocation -> {
                    stockLevel.setQuantity(250);
                    stockLevel.setReservedQuantity(0);
                    return new InventoryOperationService.StockMutation(
                            "RECEIPT", 170, 80, 250, "Stock receipt processed via stock update endpoint");
                });
        when(inventoryOperationService.saveStockLevel(stockLevel)).thenReturn(savedStock);
        when(inventoryOperationService.saveWarehouse(warehouse)).thenReturn(warehouse);

        stockLevelService.updateStock(1L, request);

        verify(stockEventPublisher).publishOverstockEvent(1001L, 1L, 250, 200);
    }

    @Test
    void updateStock_shouldNotFail_whenEventPublishingThrowsException() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(1001L);
        request.setQuantity(5);
        request.setReorderLevel(10);

        StockLevel savedStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(5)
                .reservedQuantity(0)
                .reorderLevel(10)
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(warehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 1001L)).thenReturn(stockLevel);
        when(inventoryOperationService.resolveThresholds(1001L, 10, null))
                .thenReturn(new InventoryOperationService.ThresholdSettings(10, 200));
        when(inventoryOperationService.handleAdjustment(warehouse, stockLevel, 5, null))
                .thenAnswer(invocation -> {
                    stockLevel.setQuantity(5);
                    return new InventoryOperationService.StockMutation(
                            "ISSUE", -75, 80, 5, "Stock issue processed via stock update endpoint");
                });
        when(inventoryOperationService.saveStockLevel(stockLevel)).thenReturn(savedStock);
        when(inventoryOperationService.saveWarehouse(warehouse)).thenReturn(warehouse);
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(stockEventPublisher)
                .publishLowStockEvent(1001L, 1L, 5, 10);

        assertDoesNotThrow(() -> stockLevelService.updateStock(1L, request));
    }

    @Test
    void getLowStockItems_shouldReturnItemsBelowThreshold_whenRepositoryFindsMatches() {
        when(stockLevelRepository.findLowStockItems(15)).thenReturn(List.of(stockLevel));

        List<StockLevelResponseDTO> result = stockLevelService.getLowStockItems(15);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getProductId());
    }
}
