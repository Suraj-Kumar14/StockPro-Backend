package com.stockpro.reportservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.reportservice.entity.InventorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class InventorySnapshotRepositoryTest {

    @Autowired
    private InventorySnapshotRepository inventorySnapshotRepository;

    private final LocalDate today = LocalDate.of(2026, 5, 9);

    @BeforeEach
    void setUp() {
        inventorySnapshotRepository.deleteAll();
        inventorySnapshotRepository.save(snapshot(today.minusDays(1), 1L, 10L, new BigDecimal("50.0000")));
        inventorySnapshotRepository.save(snapshot(today, 1L, 10L, new BigDecimal("60.0000")));
        inventorySnapshotRepository.save(snapshot(today, 2L, 11L, new BigDecimal("40.0000")));
    }

    @Test
    void shouldSupportDateWarehouseAndProductQueries() {
        assertEquals(2, inventorySnapshotRepository.findBySnapshotDate(today).size());
        assertEquals(2, inventorySnapshotRepository.findBySnapshotDate(today, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(3, inventorySnapshotRepository.findBySnapshotDateBetween(today.minusDays(1), today).size());
        assertEquals(2, inventorySnapshotRepository.findByProductIdAndSnapshotDateBetween(1L, today.minusDays(1), today).size());
        assertEquals(2, inventorySnapshotRepository.findByWarehouseIdAndSnapshotDateBetween(10L, today.minusDays(1), today).size());
        assertTrue(inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(today, 1L, 10L).isPresent());
    }

    @Test
    void shouldReturnAggregateLatestAndExistenceValues() {
        assertEquals(new BigDecimal("100.0000"), inventorySnapshotRepository.sumTotalStockValue(today));
        assertEquals(new BigDecimal("60.0000"), inventorySnapshotRepository.sumStockValueByWarehouse(10L, today));
        assertEquals(today, inventorySnapshotRepository.findLatestSnapshotDate());
        assertEquals(2, inventorySnapshotRepository.findLatestSnapshot().size());
        assertEquals(2, inventorySnapshotRepository.findBySnapshotDateOrderByWarehouseIdAscProductIdAsc(today).size());
        assertEquals(new BigDecimal("150.0000"), inventorySnapshotRepository.sumStockValueBetween(today.minusDays(1), today));
        assertTrue(inventorySnapshotRepository.existsBySnapshotDate(today));
    }

    private InventorySnapshot snapshot(LocalDate date, Long productId, Long warehouseId, BigDecimal totalValue) {
        return InventorySnapshot.builder()
                .snapshotDate(date)
                .productId(productId)
                .productSku("SKU-" + productId)
                .productName("Product " + productId)
                .warehouseId(warehouseId)
                .warehouseCode("WH-" + warehouseId)
                .warehouseName("Warehouse " + warehouseId)
                .quantity(new BigDecimal("10.0000"))
                .reservedQuantity(new BigDecimal("2.0000"))
                .availableQuantity(new BigDecimal("8.0000"))
                .unitCost(new BigDecimal("5.0000"))
                .totalValue(totalValue)
                .build();
    }
}
