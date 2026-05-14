package com.stockpro.warehouseservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.warehouseservice.client.ProductCatalogClient;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.CapacityExceededException;
import com.stockpro.warehouseservice.exception.InvalidOperationException;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import com.stockpro.warehouseservice.exception.StockNotAvailableException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryOperationServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @InjectMocks
    private InventoryOperationService inventoryOperationService;

    private Warehouse warehouse;
    private StockLevel stockLevel;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .warehouseId(1L)
                .capacity(100)
                .usedCapacity(40)
                .isActive(true)
                .build();
        stockLevel = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(10L)
                .quantity(30)
                .reservedQuantity(5)
                .build();
    }

    @Test
    void getWarehouseForMutation_shouldRefreshUsedCapacity() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.sumQuantityByWarehouseId(1L)).thenReturn(63);

        Warehouse result = inventoryOperationService.getWarehouseForMutation(1L);

        assertEquals(63, result.getUsedCapacity());
    }

    @Test
    void getWarehouseForMutation_shouldThrowWhenMissing() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> inventoryOperationService.getWarehouseForMutation(99L));
    }

    @Test
    void getOrCreateStockLevel_shouldReturnExistingOrNewInstance() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 99L))
                .thenReturn(Optional.empty());

        StockLevel existing = inventoryOperationService.getOrCreateStockLevel(1L, 10L);
        StockLevel created = inventoryOperationService.getOrCreateStockLevel(1L, 99L);

        assertEquals(1L, existing.getStockId());
        assertEquals(0, created.getQuantity());
        assertEquals(0, created.getReservedQuantity());
    }

    @Test
    void resolveThresholds_shouldUseRequestValuesOrFallbackToProductService() {
        when(productCatalogClient.getProductById(10L)).thenReturn(ProductLookupResponseDTO.builder()
                .productId(10L)
                .reorderLevel(7)
                .maxStockLevel(50)
                .build());

        InventoryOperationService.ThresholdSettings explicit =
                inventoryOperationService.resolveThresholds(10L, 5, 40);
        InventoryOperationService.ThresholdSettings resolved =
                inventoryOperationService.resolveThresholds(10L, null, null);

        assertEquals(5, explicit.reorderLevel());
        assertEquals(40, explicit.maxStockLevel());
        assertEquals(7, resolved.reorderLevel());
        assertEquals(50, resolved.maxStockLevel());
    }

    @Test
    void resolveThresholds_shouldKeepNullsWhenProductLookupFails() {
        when(productCatalogClient.getProductById(10L))
                .thenThrow(new ProductLookupException("Lookup failed", true));

        InventoryOperationService.ThresholdSettings resolved =
                inventoryOperationService.resolveThresholds(10L, null, null);

        assertEquals(null, resolved.reorderLevel());
        assertEquals(null, resolved.maxStockLevel());
    }

    @Test
    void handleAdjustment_shouldRouteToReceiptIssueOrNoOp() {
        InventoryOperationService.StockMutation receipt =
                inventoryOperationService.handleAdjustment(warehouse, stockLevel, 45, null);
        stockLevel.setQuantity(30);
        warehouse.setUsedCapacity(40);
        InventoryOperationService.StockMutation issue =
                inventoryOperationService.handleAdjustment(warehouse, stockLevel, 20, "manual issue");
        stockLevel.setQuantity(30);
        stockLevel.setReservedQuantity(5);
        InventoryOperationService.StockMutation noOp =
                inventoryOperationService.handleAdjustment(warehouse, stockLevel, 30, "");

        assertEquals("RECEIPT", receipt.operationType());
        assertEquals(15, receipt.quantityChanged());
        assertEquals("ISSUE", issue.operationType());
        assertEquals(-10, issue.quantityChanged());
        assertEquals("ADJUSTMENT", noOp.operationType());
        assertEquals("Stock adjusted with no quantity delta", noOp.reason());
    }

    @Test
    void handleAdjustment_shouldRejectInvalidTargetQuantity() {
        assertThrows(InvalidOperationException.class,
                () -> inventoryOperationService.handleAdjustment(warehouse, stockLevel, -1, null));

        stockLevel.setReservedQuantity(8);
        assertThrows(InvalidOperationException.class,
                () -> inventoryOperationService.handleAdjustment(warehouse, stockLevel, 7, null));
    }

    @Test
    void handleReceipt_shouldIncreaseWarehouseUsageAndValidateCapacity() {
        InventoryOperationService.StockMutation mutation =
                inventoryOperationService.handleReceipt(warehouse, stockLevel, 10, "receipt");

        assertEquals(40, stockLevel.getQuantity());
        assertEquals(50, warehouse.getUsedCapacity());
        assertEquals(10, mutation.quantityChanged());

        assertThrows(CapacityExceededException.class,
                () -> inventoryOperationService.handleReceipt(warehouse, stockLevel, 70, "overflow"));
    }

    @Test
    void handleIssue_shouldReduceStockAndRejectInsufficientAvailability() {
        InventoryOperationService.StockMutation mutation =
                inventoryOperationService.handleIssue(warehouse, stockLevel, 10, "issue");

        assertEquals(20, stockLevel.getQuantity());
        assertEquals(30, warehouse.getUsedCapacity());
        assertEquals(-10, mutation.quantityChanged());

        stockLevel.setQuantity(6);
        stockLevel.setReservedQuantity(5);
        assertThrows(StockNotAvailableException.class,
                () -> inventoryOperationService.handleIssue(warehouse, stockLevel, 2, "issue"));
    }

    @Test
    void reserveAndReleaseReservation_shouldHandleSuccessAndValidationFailures() {
        InventoryOperationService.ReservationMutation reserveMutation =
                inventoryOperationService.reserveStock(stockLevel, 6);

        assertEquals(11, stockLevel.getReservedQuantity());
        assertEquals(6, reserveMutation.quantityChanged());

        InventoryOperationService.ReservationMutation releaseMutation =
                inventoryOperationService.releaseReservation(stockLevel, 3);
        assertEquals(8, stockLevel.getReservedQuantity());
        assertEquals(11, releaseMutation.previousReservedQuantity());

        assertThrows(StockNotAvailableException.class,
                () -> inventoryOperationService.reserveStock(stockLevel, 100));
        assertThrows(InvalidOperationException.class,
                () -> inventoryOperationService.releaseReservation(stockLevel, 20));
    }

    @Test
    void applyBinLocationAndSaveMethods_shouldValidateState() {
        inventoryOperationService.applyThresholds(stockLevel,
                new InventoryOperationService.ThresholdSettings(9, 90));
        inventoryOperationService.applyBinLocation(stockLevel, "BIN-22");
        inventoryOperationService.applyBinLocation(stockLevel, "   ");

        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);

        assertEquals(9, stockLevel.getReorderLevel());
        assertEquals("BIN-22", stockLevel.getBinLocation());
        assertEquals(stockLevel, inventoryOperationService.saveStockLevel(stockLevel));
        assertEquals(warehouse, inventoryOperationService.saveWarehouse(warehouse));

        stockLevel.setReservedQuantity(100);
        assertThrows(InvalidOperationException.class,
                () -> inventoryOperationService.saveStockLevel(stockLevel));

        warehouse.setUsedCapacity(200);
        assertThrows(CapacityExceededException.class,
                () -> inventoryOperationService.saveWarehouse(warehouse));
    }

    @Test
    void saveWarehouse_shouldRejectNegativeUsedCapacity() {
        warehouse.setUsedCapacity(-1);

        assertThrows(InvalidOperationException.class,
                () -> inventoryOperationService.saveWarehouse(warehouse));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void validatePositiveQuantityBranchesShouldNotThrowForValidInputs() {
        assertDoesNotThrow(() -> inventoryOperationService.handleReceipt(warehouse, stockLevel, 1, "ok"));
    }
}
