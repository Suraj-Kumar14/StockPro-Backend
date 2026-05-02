package com.stockpro.reportservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.reportservice.client.ReportingDataClient;
import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.enums.ReportPeriod;
import com.stockpro.reportservice.export.ReportExportService;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import com.stockpro.reportservice.service.ReportEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private InventorySnapshotRepository inventorySnapshotRepository;

    @Mock
    private ReportingDataClient reportingDataClient;

    @Mock
    private ReportExportService reportExportService;

    @Mock
    private ReportEventPublisher reportEventPublisher;

    @InjectMocks
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reportService, "deadStockDays", 90);
        ReflectionTestUtils.setField(reportService, "slowMovingDays", 30);
    }

    @Test
    void getInventoryValuation_shouldCalculateTotalValueCorrectly() {
        when(reportingDataClient.getProducts()).thenReturn(List.of(
                new ReportingDataClient.ProductRecord(1L, "SKU-1", "Widget", "Hardware", "BrandA", BigDecimal.TEN, BigDecimal.ONE, 5, 50, true)));
        when(reportingDataClient.getWarehouses()).thenReturn(List.of(
                new ReportingDataClient.WarehouseRecord(11L, "Main Warehouse", "WH-1", 100, 50, true)));
        when(reportingDataClient.getStocks(any())).thenReturn(List.of(
                new ReportingDataClient.StockRecord(100L, 11L, "Main Warehouse", 1L, "Widget", "SKU-1", 5, 1, 4, "A1")));

        var response = reportService.getInventoryValuation(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).build());

        assertThat(response.totalInventoryValue()).isEqualByComparingTo("50.0");
        assertThat(response.totalQuantity()).isEqualByComparingTo("5.0");
        assertThat(response.valuationByWarehouse()).hasSize(1);
        assertThat(response.valuationByProduct()).hasSize(1);
    }

    @Test
    void getStockSummary_shouldReturnCorrectCounts() {
        when(reportingDataClient.getProducts()).thenReturn(List.of(
                new ReportingDataClient.ProductRecord(1L, "SKU-1", "Widget", "Hardware", "BrandA", BigDecimal.TEN, BigDecimal.ONE, 5, 10, true),
                new ReportingDataClient.ProductRecord(2L, "SKU-2", "Bolt", "Hardware", "BrandA", BigDecimal.ONE, BigDecimal.ONE, 2, 4, true)));
        when(reportingDataClient.getStocks(any())).thenReturn(List.of(
                new ReportingDataClient.StockRecord(100L, 11L, "Main Warehouse", 1L, "Widget", "SKU-1", 12, 2, 10, "A1"),
                new ReportingDataClient.StockRecord(101L, 11L, "Main Warehouse", 2L, "Bolt", "SKU-2", 1, 0, 1, "A2")));

        var response = reportService.getStockSummary(ReportFilterRequest.builder().build());

        assertThat(response.totalProducts()).isEqualTo(2);
        assertThat(response.totalWarehouses()).isEqualTo(1);
        assertThat(response.lowStockCount()).isEqualTo(1);
        assertThat(response.overstockCount()).isEqualTo(1);
    }

    @Test
    void createInventorySnapshotForDate_shouldSkipDuplicates() {
        when(reportingDataClient.getProducts()).thenReturn(List.of(
                new ReportingDataClient.ProductRecord(1L, "SKU-1", "Widget", "Hardware", "BrandA", BigDecimal.TEN, BigDecimal.ONE, 5, 10, true)));
        when(reportingDataClient.getWarehouses()).thenReturn(List.of(
                new ReportingDataClient.WarehouseRecord(11L, "Main Warehouse", "WH-1", 100, 50, true)));
        when(reportingDataClient.getStocks(any())).thenReturn(List.of(
                new ReportingDataClient.StockRecord(100L, 11L, "Main Warehouse", 1L, "Widget", "SKU-1", 12, 2, 10, "A1")));
        when(inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(LocalDate.of(2026, 5, 1), 1L, 11L))
                .thenReturn(Optional.of(com.stockpro.reportservice.entity.InventorySnapshot.builder().snapshotId(99L).build()));

        reportService.createInventorySnapshotForDate(LocalDate.of(2026, 5, 1));

        verify(inventorySnapshotRepository, never()).save(any());
    }
}
