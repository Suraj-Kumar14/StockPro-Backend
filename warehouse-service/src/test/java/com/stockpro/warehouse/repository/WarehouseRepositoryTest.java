package com.stockpro.warehouse.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpro.warehouse.entity.Warehouse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class WarehouseRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Test
    void findByManagerIdAndCountByIsActiveShouldReturnExpectedResults() {
        warehouseRepository.save(Warehouse.builder()
                .name("North Hub")
                .location("Delhi")
                .address("North District")
                .managerId(11L)
                .capacity(100)
                .usedCapacity(20)
                .isActive(Boolean.TRUE)
                .phone("+91-1111111111")
                .build());
        warehouseRepository.save(Warehouse.builder()
                .name("South Hub")
                .location("Chennai")
                .address("South District")
                .managerId(11L)
                .capacity(200)
                .usedCapacity(0)
                .isActive(Boolean.FALSE)
                .phone("+91-2222222222")
                .build());

        assertThat(warehouseRepository.findByManagerId(11L)).hasSize(2);
        assertThat(warehouseRepository.findByIsActive(Boolean.TRUE)).hasSize(1);
        assertThat(warehouseRepository.countByIsActive(Boolean.FALSE)).isEqualTo(1);
    }
}
