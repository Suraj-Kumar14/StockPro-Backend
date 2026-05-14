package com.stockpro.warehouseservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.warehouseservice.entity.StockAlert;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.StockMovement;
import com.stockpro.warehouseservice.entity.Warehouse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class WarehouseServiceRepositoryTest {

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    private Warehouse activeWarehouse;
    private Warehouse inactiveWarehouse;

    @BeforeEach
    void setUp() {
        activeWarehouse = warehouseRepository.save(Warehouse.builder()
                .name("Central")
                .code("WH-001")
                .location("Delhi")
                .city("Delhi")
                .state("Delhi")
                .country("India")
                .managerId(10L)
                .capacity(500)
                .usedCapacity(120)
                .isActive(true)
                .build());
        inactiveWarehouse = warehouseRepository.save(Warehouse.builder()
                .name("Overflow")
                .code("WH-002")
                .location("Noida")
                .city("Noida")
                .state("UP")
                .country("India")
                .managerId(11L)
                .capacity(300)
                .usedCapacity(0)
                .isActive(false)
                .build());

        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(activeWarehouse.getWarehouseId())
                .productId(100L)
                .quantity(8)
                .reservedQuantity(3)
                .reorderLevel(6)
                .maxStockLevel(30)
                .binLocation("A-1")
                .build());
        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(activeWarehouse.getWarehouseId())
                .productId(101L)
                .quantity(35)
                .reservedQuantity(0)
                .reorderLevel(5)
                .maxStockLevel(30)
                .binLocation("B-1")
                .build());
        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(inactiveWarehouse.getWarehouseId())
                .productId(100L)
                .quantity(0)
                .reservedQuantity(0)
                .reorderLevel(2)
                .maxStockLevel(10)
                .binLocation("C-1")
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .warehouseId(activeWarehouse.getWarehouseId())
                .productId(100L)
                .movementType("ISSUE")
                .quantityChanged(-3)
                .previousQuantity(11)
                .newQuantity(8)
                .reason("Dispatch")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());
        stockMovementRepository.save(StockMovement.builder()
                .warehouseId(activeWarehouse.getWarehouseId())
                .productId(100L)
                .movementType("RECEIPT")
                .quantityChanged(5)
                .previousQuantity(3)
                .newQuantity(8)
                .reason("Receipt")
                .createdAt(LocalDateTime.now())
                .build());

        stockAlertRepository.save(StockAlert.builder()
                .warehouseId(activeWarehouse.getWarehouseId())
                .productId(100L)
                .alertType("LOW_STOCK")
                .currentQuantity(5)
                .thresholdValue(6)
                .active(true)
                .acknowledged(false)
                .message("Low stock")
                .build());
    }

    @Test
    void stockLevelRepository_shouldResolveDerivedAndCustomQueries() {
        assertTrue(stockLevelRepository.findByWarehouseIdAndProductId(
                activeWarehouse.getWarehouseId(), 100L).isPresent());
        assertEquals(2, stockLevelRepository.findByWarehouseId(activeWarehouse.getWarehouseId()).size());
        assertEquals(2, stockLevelRepository.findByProductId(100L).size());
        assertEquals(2, stockLevelRepository.findLowStockItems(10).size());
        assertEquals(1, stockLevelRepository.findOutOfStockItems().size());
        assertTrue(stockLevelRepository.existsByWarehouseIdAndProductId(activeWarehouse.getWarehouseId(), 101L));
        assertEquals(2, stockLevelRepository.countByWarehouseId(activeWarehouse.getWarehouseId()));
        assertEquals(2, stockLevelRepository.findByQuantityLessThanEqual(8).size());
        assertEquals(1, stockLevelRepository.findByQuantityGreaterThanEqual(30).size());
        assertEquals(43, stockLevelRepository.sumQuantityByWarehouseId(activeWarehouse.getWarehouseId()));
    }

    @Test
    void movementAlertAndWarehouseRepositories_shouldReturnExpectedData() {
        List<StockMovement> warehouseMovements =
                stockMovementRepository.findByWarehouseIdOrderByCreatedAtDesc(activeWarehouse.getWarehouseId());
        List<StockMovement> productMovements =
                stockMovementRepository.findByProductIdOrderByCreatedAtDesc(100L);
        List<StockMovement> scopedMovements =
                stockMovementRepository.findByWarehouseIdAndProductIdOrderByCreatedAtDesc(activeWarehouse.getWarehouseId(), 100L);

        assertEquals(2, warehouseMovements.size());
        assertEquals(2, productMovements.size());
        assertEquals(2, scopedMovements.size());

        assertTrue(stockAlertRepository.findByAlertIdAndActiveTrue(
                stockAlertRepository.findAll().get(0).getAlertId()).isPresent());
        assertTrue(stockAlertRepository.findByWarehouseIdAndProductIdAndAlertTypeAndActiveTrue(
                activeWarehouse.getWarehouseId(), 100L, "LOW_STOCK").isPresent());
        assertEquals(1, stockAlertRepository.findByActiveTrueOrderByCreatedAtDesc().size());

        assertTrue(warehouseRepository.findByWarehouseId(activeWarehouse.getWarehouseId()).isPresent());
        assertTrue(warehouseRepository.findByCode("WH-001").isPresent());
        assertTrue(warehouseRepository.existsByCode("WH-001"));
        assertEquals(1, warehouseRepository.findByIsActive(true).size());
        assertEquals(1, warehouseRepository.findByManagerId(10L).size());
        assertTrue(warehouseRepository.findByName("Central").isPresent());
        assertTrue(warehouseRepository.existsByName("Central"));
        assertEquals(1, warehouseRepository.countByIsActive(true));
    }
}
