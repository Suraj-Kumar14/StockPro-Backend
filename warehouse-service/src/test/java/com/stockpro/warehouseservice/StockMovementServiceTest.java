package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockTransferDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.InvalidOperationException;
import com.stockpro.warehouseservice.exception.StockNotAvailableException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

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

    private StockLevel sourceStock;
    private Warehouse sourceWarehouse;
    private Warehouse destinationWarehouse;

    @BeforeEach
    void setUp() {
        sourceStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(501L)
                .quantity(100)
                .reservedQuantity(20)
                .reorderLevel(15)
                .maxStockLevel(200)
                .binLocation("SRC-A1")
                .build();

        sourceWarehouse = Warehouse.builder()
                .warehouseId(1L)
                .capacity(500)
                .usedCapacity(100)
                .isActive(true)
                .build();

        destinationWarehouse = Warehouse.builder()
                .warehouseId(2L)
                .capacity(500)
                .usedCapacity(15)
                .isActive(true)
                .build();
    }

    @Test
    void reserveStock_shouldIncreaseReservedQuantity_whenAvailableStockExists() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L))
                .thenReturn(Optional.of(sourceStock));
        when(inventoryOperationService.reserveStock(sourceStock, 30))
                .thenAnswer(invocation -> {
                    sourceStock.setReservedQuantity(50);
                    return new InventoryOperationService.ReservationMutation(30, 20, 50);
                });
        when(inventoryOperationService.saveStockLevel(sourceStock)).thenReturn(sourceStock);

        stockLevelService.reserveStock(1L, 501L, 30);

        assertEquals(50, sourceStock.getReservedQuantity());
        verify(stockMovementService).recordReservation(1L, 501L, 30, 20, 50, "Stock reserved");
    }

    @Test
    void reserveStock_shouldThrowException_whenAvailableStockIsInsufficient() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L))
                .thenReturn(Optional.of(sourceStock));
        when(inventoryOperationService.reserveStock(sourceStock, 81))
                .thenThrow(new StockNotAvailableException(
                        "Insufficient available stock. Available: 80, Requested: 81"));

        StockNotAvailableException exception = assertThrows(
                StockNotAvailableException.class,
                () -> stockLevelService.reserveStock(1L, 501L, 81)
        );

        assertEquals("Insufficient available stock. Available: 80, Requested: 81",
                exception.getMessage());
    }

    @Test
    void releaseReservation_shouldThrowException_whenReleaseExceedsReservedQuantity() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L))
                .thenReturn(Optional.of(sourceStock));
        when(inventoryOperationService.releaseReservation(sourceStock, 50))
                .thenThrow(new InvalidOperationException(
                        "Cannot release more stock than is currently reserved"));

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> stockLevelService.releaseReservation(1L, 501L, 50)
        );

        assertEquals("Cannot release more stock than is currently reserved",
                exception.getMessage());
    }

    @Test
    void transferStock_shouldMoveQuantityBetweenWarehouses_whenRequestIsValid() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(501L);
        request.setQuantity(40);
        request.setReason("Transfer for customer demand");

        StockLevel destinationStock = StockLevel.builder()
                .stockId(2L)
                .warehouseId(2L)
                .productId(501L)
                .quantity(15)
                .reservedQuantity(0)
                .reorderLevel(15)
                .maxStockLevel(200)
                .binLocation("DEST-B2")
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(sourceWarehouse);
        when(inventoryOperationService.getWarehouseForMutation(2L)).thenReturn(destinationWarehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 501L)).thenReturn(sourceStock);
        when(inventoryOperationService.getOrCreateStockLevel(2L, 501L)).thenReturn(destinationStock);
        when(inventoryOperationService.resolveThresholds(501L, 15, 200))
                .thenReturn(new InventoryOperationService.ThresholdSettings(15, 200));
        when(inventoryOperationService.handleIssue(
                sourceWarehouse, sourceStock, 40, "Transfer for customer demand"))
                .thenAnswer(invocation -> {
                    sourceStock.setQuantity(60);
                    return new InventoryOperationService.StockMutation(
                            "ISSUE", -40, 100, 60, "Transfer for customer demand");
                });
        when(inventoryOperationService.handleReceipt(
                destinationWarehouse, destinationStock, 40, "Transfer for customer demand"))
                .thenAnswer(invocation -> {
                    destinationStock.setQuantity(55);
                    return new InventoryOperationService.StockMutation(
                            "RECEIPT", 40, 15, 55, "Transfer for customer demand");
                });
        when(inventoryOperationService.saveStockLevel(sourceStock)).thenReturn(sourceStock);
        when(inventoryOperationService.saveStockLevel(destinationStock)).thenReturn(destinationStock);

        stockLevelService.transferStock(request);

        assertEquals(60, sourceStock.getQuantity());
        assertEquals(55, destinationStock.getQuantity());
        verify(stockMovementService).recordTransferOut(1L, 501L, 40, 100, 60,
                "Transfer for customer demand", 2L);
        verify(stockMovementService).recordTransferIn(2L, 501L, 40, 15, 55,
                "Transfer for customer demand", 1L);
    }

    @Test
    void transferStock_shouldThrowException_whenSourceAndDestinationWarehousesMatch() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(1L);
        request.setProductId(501L);
        request.setQuantity(5);
        request.setReason("Invalid transfer");

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> stockLevelService.transferStock(request)
        );

        assertEquals("Source and destination warehouses cannot be the same",
                exception.getMessage());
    }

    @Test
    void transferStock_shouldThrowException_whenRequestedQuantityExceedsAvailableStock() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(501L);
        request.setQuantity(81);
        request.setReason("Over-allocation test");

        StockLevel destinationStock = StockLevel.builder()
                .warehouseId(2L)
                .productId(501L)
                .quantity(15)
                .reservedQuantity(0)
                .build();

        when(inventoryOperationService.getWarehouseForMutation(1L)).thenReturn(sourceWarehouse);
        when(inventoryOperationService.getWarehouseForMutation(2L)).thenReturn(destinationWarehouse);
        when(inventoryOperationService.getOrCreateStockLevel(1L, 501L)).thenReturn(sourceStock);
        when(inventoryOperationService.getOrCreateStockLevel(2L, 501L)).thenReturn(destinationStock);
        when(inventoryOperationService.resolveThresholds(501L, 15, 200))
                .thenReturn(new InventoryOperationService.ThresholdSettings(15, 200));
        when(inventoryOperationService.handleIssue(
                sourceWarehouse, sourceStock, 81, "Over-allocation test"))
                .thenThrow(new StockNotAvailableException(
                        "Insufficient available stock. Available: 80, Requested: 81"));

        StockNotAvailableException exception = assertThrows(
                StockNotAvailableException.class,
                () -> stockLevelService.transferStock(request)
        );

        assertEquals("Insufficient available stock. Available: 80, Requested: 81",
                exception.getMessage());
    }
}
