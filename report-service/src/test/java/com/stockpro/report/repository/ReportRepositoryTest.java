package com.stockpro.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpro.report.entity.InventorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void snapshotQueriesShouldReturnTotalsAndLatestRows() {
        reportRepository.save(InventorySnapshot.builder()
                .warehouseId(1L)
                .productId(101L)
                .quantity(new BigDecimal("10.0000"))
                .costPrice(new BigDecimal("20.0000"))
                .stockValue(new BigDecimal("200.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 24))
                .warehouseName("Central")
                .productName("Product 101")
                .productSku("SKU-101")
                .source("MANUAL")
                .createdBy("admin@stockpro.com")
                .build());
        reportRepository.save(InventorySnapshot.builder()
                .warehouseId(1L)
                .productId(101L)
                .quantity(new BigDecimal("12.0000"))
                .costPrice(new BigDecimal("20.0000"))
                .stockValue(new BigDecimal("240.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .warehouseName("Central")
                .productName("Product 101")
                .productSku("SKU-101")
                .source("SCHEDULED")
                .createdBy("SYSTEM")
                .build());
        reportRepository.save(InventorySnapshot.builder()
                .warehouseId(2L)
                .productId(102L)
                .quantity(new BigDecimal("5.0000"))
                .costPrice(new BigDecimal("30.0000"))
                .stockValue(new BigDecimal("150.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .warehouseName("South")
                .productName("Product 102")
                .productSku("SKU-102")
                .source("SCHEDULED")
                .createdBy("SYSTEM")
                .build());

        assertThat(reportRepository.findBySnapshotDate(LocalDate.of(2026, 4, 25))).hasSize(2);
        assertThat(reportRepository.sumStockValueBySnapshotDate(LocalDate.of(2026, 4, 25)))
                .isEqualByComparingTo("390.0000");
        assertThat(reportRepository.sumStockValueByWarehouse(LocalDate.of(2026, 4, 25))).hasSize(2);
        assertThat(reportRepository.findFirstByWarehouseIdAndProductIdOrderBySnapshotDateDescCreatedAtDesc(1L, 101L))
                .isPresent()
                .get()
                .extracting(InventorySnapshot::getSnapshotDate)
                .isEqualTo(LocalDate.of(2026, 4, 25));
        assertThat(reportRepository.findLatestSnapshotDateOnOrBefore(LocalDate.of(2026, 4, 26)))
                .isEqualTo(LocalDate.of(2026, 4, 25));
    }
}
