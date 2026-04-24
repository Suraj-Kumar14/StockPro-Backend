package com.stockpro.warehouse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class StockLevelRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Test
    void findByWarehouseIdAndProductIdAndSumQuantityShouldWork() {
        Warehouse warehouse = warehouseRepository.save(Warehouse.builder()
                .name("Central Hub")
                .location("Bengaluru")
                .address("Electronics City")
                .managerId(21L)
                .capacity(500)
                .usedCapacity(0)
                .isActive(Boolean.TRUE)
                .build());

        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(warehouse.getWarehouseId())
                .productId(101L)
                .quantity(new BigDecimal("25.0000"))
                .reservedQuantity(new BigDecimal("5.0000"))
                .location("A-01")
                .build());
        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(warehouse.getWarehouseId())
                .productId(102L)
                .quantity(new BigDecimal("10.0000"))
                .reservedQuantity(new BigDecimal("0.0000"))
                .location("A-02")
                .build());

        assertThat(stockLevelRepository.findByWarehouseIdAndProductId(warehouse.getWarehouseId(), 101L)).isPresent();
        assertThat(stockLevelRepository.findByWarehouseId(warehouse.getWarehouseId())).hasSize(2);
        assertThat(stockLevelRepository.sumQuantityByWarehouseId(warehouse.getWarehouseId()))
                .isEqualByComparingTo("35.0000");
    }
}
