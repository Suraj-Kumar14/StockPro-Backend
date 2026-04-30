package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockAuditRequestDTO;
import com.stockpro.warehouseservice.dto.StockAuditResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.service.InventoryOperationService;
import com.stockpro.warehouseservice.service.StockAlertService;
import com.stockpro.warehouseservice.service.StockAuditService;
import com.stockpro.warehouseservice.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAuditServiceTest {

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockAlertService stockAlertService;

    @Mock
    private InventoryOperationService inventoryOperationService;

    @InjectMocks
    private StockAuditService stockAuditService;

    private StockAuditRequestDTO request;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        request = new StockAuditRequestDTO();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setCountedQuantity(42);
        request.setReason("Cycle count discrepancy");
        request.setBinLocation("A-10");
        request.setReorderLevel(10);
        request.setMaxStockLevel(100);

        warehouse = Warehouse.builder()
                .warehouseId(1L)
                .capacity(500)
                .usedCapacity(50)
                .isActive(true)
                .build();
    }

    @Test
    void performAudit_shouldUpdateStockAndRecordDiscrepancy_whenWarehouseExists() {
        StockLevel existing = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(100L)
                .quantity(50)
                .reservedQuantity(5)
                .binLocation("A-01")
                .reorderLevel(10)
                .maxStockLevel(100)
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(warehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 100L)).thenReturn(existing);
        when(inventoryOperationService.resolveThresholds(100L, 10, 100))
                .thenReturn(new InventoryOperationService.ThresholdSettings(10, 100));
        when(inventoryOperationService.handleAdjustment(
                warehouse, existing, 42, "Cycle count discrepancy"))
                .thenAnswer(invocation -> {
                    existing.setQuantity(42);
                    return new InventoryOperationService.StockMutation(
                            "ISSUE", -8, 50, 42, "Cycle count discrepancy");
                });
        when(inventoryOperationService.saveStockLevel(existing)).thenReturn(existing);
        when(inventoryOperationService.saveWarehouse(warehouse)).thenReturn(warehouse);

        StockAuditResponseDTO result = stockAuditService.performAudit(request);

        assertEquals(50, result.getSystemQuantity());
        assertEquals(42, result.getCountedQuantity());
        assertEquals(-8, result.getDiscrepancy());
        verify(stockMovementService)
                .recordAudit(1L, 100L, -8, 50, 42, "Cycle count discrepancy");
        verify(stockAlertService).syncAlerts(any(StockLevel.class), any(), any());
    }

    @Test
    void performAudit_shouldThrowException_whenWarehouseDoesNotExist() {
        when(inventoryOperationService.getWarehouseForMutation(1L))
                .thenThrow(new WarehouseNotFoundException("Warehouse not found with ID: 1"));

        WarehouseNotFoundException exception = assertThrows(
                WarehouseNotFoundException.class,
                () -> stockAuditService.performAudit(request)
        );

        assertEquals("Warehouse not found with ID: 1", exception.getMessage());
    }
}
