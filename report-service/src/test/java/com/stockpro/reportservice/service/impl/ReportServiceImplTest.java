package com.stockpro.reportservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.reportservice.client.ReportingDataClient;
import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.dto.response.AlertSummaryReportResponse;
import com.stockpro.reportservice.dto.response.InventorySnapshotResponse;
import com.stockpro.reportservice.entity.InventorySnapshot;
import com.stockpro.reportservice.enums.ExportFormat;
import com.stockpro.reportservice.enums.ReportPeriod;
import com.stockpro.reportservice.exception.DataNotFoundException;
import com.stockpro.reportservice.export.ReportExportService;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import com.stockpro.reportservice.service.ReportEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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

    @Test
    void inventoryReportMethods_shouldBuildPagesAndThresholds() {
        stubInventoryData();
        when(reportingDataClient.searchMovements(any())).thenReturn(new ReportingDataClient.PageResponse<>(
                List.of(movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(2))),
                1L, 1, 0, 10));

        var productStock = reportService.getProductStockReport(ReportFilterRequest.builder().category("Hardware").brand("BrandA").size(10).build());
        var warehouseStock = reportService.getWarehouseStockReport(ReportFilterRequest.builder().size(10).build());
        var lowStock = reportService.getLowStockReport(ReportFilterRequest.builder().size(10).build());
        var overstock = reportService.getOverstockReport(ReportFilterRequest.builder().size(10).build());
        var movementReport = reportService.getStockMovementReport(ReportFilterRequest.builder().size(10).build());

        assertThat(productStock.getTotalElements()).isEqualTo(1);
        assertThat(productStock.getContent().get(0).productName()).isEqualTo("Widget");
        assertThat(warehouseStock.getTotalElements()).isEqualTo(2);
        assertThat(lowStock.getContent()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(1L);
            assertThat(item.severity()).isEqualTo("CRITICAL");
        });
        assertThat(overstock.getContent()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(2L);
            assertThat(item.excessQuantity()).isEqualByComparingTo("5.0");
        });
        assertThat(movementReport.getContent()).singleElement().satisfies(item -> {
            assertThat(item.movementType()).isEqualTo("STOCK_OUT");
            assertThat(item.totalValue()).isEqualByComparingTo("40.0");
        });
    }

    @Test
    void movementAnalysisMethods_shouldComputeTurnoverTopSlowAndDeadStock() {
        stubInventoryData();
        when(inventorySnapshotRepository.findBySnapshotDate(LocalDate.of(2026, 5, 1))).thenReturn(List.of(
                snapshot(1L, LocalDate.of(2026, 5, 1), 1L, 11L, new BigDecimal("10.0"), new BigDecimal("100.0")),
                snapshot(2L, LocalDate.of(2026, 5, 1), 2L, 12L, new BigDecimal("20.0"), new BigDecimal("40.0"))));
        when(inventorySnapshotRepository.findBySnapshotDate(LocalDate.of(2026, 5, 9))).thenReturn(List.of(
                snapshot(3L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("6.0"), new BigDecimal("60.0")),
                snapshot(4L, LocalDate.of(2026, 5, 9), 2L, 12L, new BigDecimal("19.0"), new BigDecimal("38.0"))));
        when(reportingDataClient.searchMovements(any())).thenReturn(new ReportingDataClient.PageResponse<>(
                List.of(
                        movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(5)),
                        movement(2L, 1L, "Widget", "STOCK_IN", "IN", new BigDecimal("2.0"), new BigDecimal("20.0"), LocalDateTime.now().minusDays(3)),
                        movement(3L, 2L, "Bolt", "STOCK_OUT", "OUT", new BigDecimal("1.0"), new BigDecimal("2.0"), LocalDateTime.now().minusDays(120))),
                3L, 1, 0, 10));

        var turnover = reportService.getInventoryTurnoverReport(ReportFilterRequest.builder()
                .fromDate(LocalDate.of(2026, 5, 1))
                .toDate(LocalDate.of(2026, 5, 9))
                .size(10)
                .build());
        var topMoving = reportService.getTopMovingProducts(ReportFilterRequest.builder().size(10).build());
        var slowMoving = reportService.getSlowMovingProducts(ReportFilterRequest.builder().size(10).build());
        var deadStock = reportService.getDeadStockReport(ReportFilterRequest.builder().size(10).build());

        assertThat(turnover).extracting("productId").contains(1L, 2L);
        assertThat(turnover.get(0).turnoverRatio()).isEqualByComparingTo("0.5000");
        assertThat(topMoving.get(0).productId()).isEqualTo(1L);
        assertThat(slowMoving).extracting("productId").contains(2L);
        assertThat(deadStock).extracting("productId").contains(2L);
    }

    @Test
    void purchasePaymentAndAlertMethods_shouldAggregateAndFallback() {
        when(reportingDataClient.searchPurchaseOrders(any())).thenReturn(List.of(
                purchaseOrder(1L, 1L, "RECEIVED", new BigDecimal("400.0"), false, LocalDate.now().minusDays(4), LocalDateTime.now().minusDays(5)),
                purchaseOrder(2L, 1L, "PENDING_APPROVAL", new BigDecimal("200.0"), true, null, LocalDateTime.now().minusDays(2)),
                purchaseOrder(3L, 2L, "APPROVED", new BigDecimal("300.0"), false, null, LocalDateTime.now().minusDays(1))));
        when(reportingDataClient.getSuppliers()).thenReturn(List.of(
                new ReportingDataClient.SupplierRecord(1L, "Acme", 3, new BigDecimal("4.8"), true),
                new ReportingDataClient.SupplierRecord(2L, "Bravo", 4, new BigDecimal("4.2"), true)));
        when(reportingDataClient.getPaymentSummary()).thenReturn(new ReportingDataClient.PaymentSummaryRecord(
                4L, 0L, 1L, 1L, 1L, 1L, 0L, 0L, 0L, new BigDecimal("700.0"), new BigDecimal("120.0"), new BigDecimal("50.0")));
        when(reportingDataClient.searchPayments(any())).thenReturn(List.of(
                new ReportingDataClient.PaymentRecord(1L, "PAY-1", 1L, 1L, "Acme", "PAID", new BigDecimal("400.0"), BigDecimal.ZERO),
                new ReportingDataClient.PaymentRecord(2L, "PAY-2", 2L, 1L, "Acme", "PARTIALLY_PAID", new BigDecimal("300.0"), new BigDecimal("20.0")),
                new ReportingDataClient.PaymentRecord(3L, "PAY-3", 3L, 2L, "Bravo", "APPROVED", BigDecimal.ZERO, new BigDecimal("30.0"))));
        when(reportingDataClient.getMyAlertSummary()).thenReturn(new ReportingDataClient.AlertSummaryRecord(
                6L, 3L, 1L, 2L, 1L, 2L, 3L, 2L, 1L, 1L, 1L));

        var purchaseSummary = reportService.getPurchaseSummary(ReportFilterRequest.builder().size(10).build());
        var supplierPerformance = reportService.getSupplierPerformanceReport(ReportFilterRequest.builder().size(10).build());
        var supplier = reportService.getSupplierPerformance(1L, ReportFilterRequest.builder().size(10).build());
        var paymentSummary = reportService.getPaymentSummary(ReportFilterRequest.builder().size(10).build());
        AlertSummaryReportResponse alertSummary = reportService.getAlertSummary(ReportFilterRequest.builder().size(10).build());

        assertThat(purchaseSummary.totalPurchaseOrders()).isEqualTo(3);
        assertThat(purchaseSummary.pendingApprovalCount()).isEqualTo(1);
        assertThat(supplierPerformance.getContent()).hasSize(2);
        assertThat(supplier.supplierName()).isEqualTo("Acme");
        assertThat(paymentSummary.pendingCount()).isEqualTo(3);
        assertThat(paymentSummary.supplierPayments()).hasSize(2);
        assertThat(alertSummary.alertsByType()).containsEntry("LOW_STOCK", 2L);

        when(reportingDataClient.getPaymentSummary()).thenThrow(new RuntimeException("payment service down"));
        assertThat(reportService.getPaymentSummary(ReportFilterRequest.builder().size(10).build()).totalPayments()).isZero();

        when(reportingDataClient.getSuppliers()).thenThrow(new RuntimeException("supplier service down"));
        assertThat(reportService.getSupplierPerformanceReport(ReportFilterRequest.builder().size(10).build()).getContent())
                .extracting("supplierName")
                .containsOnly("Unknown supplier", "Unknown supplier");

        assertThrows(DataNotFoundException.class,
                () -> reportService.getSupplierPerformance(99L, ReportFilterRequest.builder().size(10).build()));
    }

    @Test
    void dashboardSnapshotTrendAndExportMethods_shouldUseAvailableDataAndPublishEvents() {
        stubInventoryData();
        when(reportingDataClient.searchPurchaseOrders(any())).thenReturn(List.of(
                purchaseOrder(1L, 1L, "RECEIVED", new BigDecimal("400.0"), false, LocalDate.now().minusDays(3), LocalDateTime.now().minusDays(4)),
                purchaseOrder(2L, 1L, "PENDING", new BigDecimal("100.0"), true, null, LocalDateTime.now().minusDays(2))));
        when(reportingDataClient.getPaymentSummary()).thenReturn(new ReportingDataClient.PaymentSummaryRecord(
                2L, 0L, 0L, 1L, 0L, 1L, 0L, 0L, 0L, new BigDecimal("400.0"), new BigDecimal("100.0"), BigDecimal.ZERO));
        when(reportingDataClient.searchPayments(any())).thenReturn(List.of(
                new ReportingDataClient.PaymentRecord(1L, "PAY-1", 1L, 1L, "Acme", "PAID", new BigDecimal("400.0"), BigDecimal.ZERO)));
        when(reportingDataClient.getSystemAlertSummary()).thenThrow(new RuntimeException("alerts down"));
        when(reportingDataClient.getMyAlertSummary()).thenReturn(new ReportingDataClient.AlertSummaryRecord(
                3L, 1L, 1L, 1L, 1L, 1L, 1L, 1L, 0L, 0L, 0L));
        when(reportingDataClient.getRecentAlerts(true)).thenReturn(List.of(
                new ReportingDataClient.AlertRecord(1L, "Low stock", "CRITICAL", "LOW_STOCK", LocalDateTime.now())));
        when(reportingDataClient.searchMovements(any())).thenReturn(new ReportingDataClient.PageResponse<>(
                List.of(
                        movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(1)),
                        movement(2L, 2L, "Bolt", "STOCK_IN", "IN", new BigDecimal("3.0"), new BigDecimal("6.0"), LocalDateTime.now().minusDays(2))),
                2L, 1, 0, 10));
        when(inventorySnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of(
                snapshot(1L, LocalDate.now().minusDays(1), 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0")),
                snapshot(2L, LocalDate.now(), 1L, 11L, new BigDecimal("6.0"), new BigDecimal("60.0"))));
        when(inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(eq(LocalDate.of(2026, 5, 9)), any(), any()))
                .thenReturn(Optional.empty());
        when(inventorySnapshotRepository.findBySnapshotDate(eq(LocalDate.of(2026, 5, 9)), any())).thenReturn(new PageImpl<>(List.of(
                snapshot(10L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0")))));
        when(inventorySnapshotRepository.findBySnapshotDateBetween(eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 9)))).thenReturn(List.of(
                snapshot(10L, LocalDate.of(2026, 5, 1), 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0")),
                snapshot(11L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("6.0"), new BigDecimal("60.0"))));
        when(reportExportService.exportCsv(any(), any())).thenReturn("csv-bytes".getBytes());

        var executive = reportService.getExecutiveDashboard();
        var roleDashboard = reportService.getRoleDashboard("MANAGER", 77L);
        reportService.createInventorySnapshotForDate(LocalDate.of(2026, 5, 9));
        var snapshots = reportService.getInventorySnapshots(LocalDate.of(2026, 5, 9), 0, 10);
        List<InventorySnapshotResponse> trend = reportService.getSnapshotTrend(1L, 11L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9));
        byte[] valuationExport = reportService.exportInventoryValuation(ReportFilterRequest.builder().size(10).build(), ExportFormat.CSV);
        byte[] movementExport = reportService.exportStockMovementReport(ReportFilterRequest.builder().size(10).build(), ExportFormat.CSV);
        byte[] purchaseExport = reportService.exportPurchaseSummary(ReportFilterRequest.builder().size(10).build(), ExportFormat.CSV);
        byte[] supplierExport = reportService.exportSupplierPerformance(ReportFilterRequest.builder().size(10).build(), ExportFormat.CSV);
        byte[] dashboardExport = reportService.exportExecutiveDashboard(ExportFormat.CSV);

        assertThat(executive.unavailableSections()).contains("alerts");
        assertThat(roleDashboard.criticalAlerts()).isEqualTo(1L);
        assertThat(snapshots.getContent()).hasSize(1);
        assertThat(trend).hasSize(2);
        assertThat(new String(valuationExport)).isEqualTo("csv-bytes");
        assertThat(new String(movementExport)).isEqualTo("csv-bytes");
        assertThat(new String(purchaseExport)).isEqualTo("csv-bytes");
        assertThat(new String(supplierExport)).isEqualTo("csv-bytes");
        assertThat(new String(dashboardExport)).isEqualTo("csv-bytes");

        ArgumentCaptor<InventorySnapshot> snapshotCaptor = ArgumentCaptor.forClass(InventorySnapshot.class);
        verify(inventorySnapshotRepository, times(2)).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getAllValues()).anySatisfy(item -> {
            assertThat(item.getWarehouseCode()).isEqualTo("WH-1");
            assertThat(item.getTotalValue()).isEqualByComparingTo("30.0");
        });
        verify(reportEventPublisher, atLeast(11)).publish(any(), any());
    }

    @Test
    void snapshotTrend_shouldRejectInvalidDateRange() {
        assertThrows(IllegalArgumentException.class,
                () -> reportService.getSnapshotTrend(1L, 1L, LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 1)));
    }

    private void stubInventoryData() {
        when(reportingDataClient.getProducts()).thenReturn(List.of(
                new ReportingDataClient.ProductRecord(1L, "SKU-1", "Widget", "Hardware", "BrandA", BigDecimal.TEN, BigDecimal.ONE, 5, 10, true),
                new ReportingDataClient.ProductRecord(2L, "SKU-2", "Bolt", "Fasteners", "BrandB", new BigDecimal("2.0"), BigDecimal.ONE, 4, 15, true)));
        when(reportingDataClient.getWarehouses()).thenReturn(List.of(
                new ReportingDataClient.WarehouseRecord(11L, "Main Warehouse", "WH-1", 100, 50, true),
                new ReportingDataClient.WarehouseRecord(12L, "Overflow Warehouse", "WH-2", 100, 60, true)));
        when(reportingDataClient.getStocks(any())).thenReturn(List.of(
                new ReportingDataClient.StockRecord(100L, 11L, "Main Warehouse", 1L, "Widget", "SKU-1", 3, 1, 2, "A1"),
                new ReportingDataClient.StockRecord(101L, 12L, "Overflow Warehouse", 2L, "Bolt", "SKU-2", 20, 0, 20, "B1")));
    }

    private ReportingDataClient.MovementRecord movement(Long movementId, Long productId, String productName, String type,
                                                        String direction, BigDecimal quantity, BigDecimal totalValue, LocalDateTime movementDate) {
        return new ReportingDataClient.MovementRecord(
                movementId, "MOV-" + movementId, productId, "SKU-" + productId, productName,
                productId == 1L ? 11L : 12L, productId == 1L ? "Main Warehouse" : "Overflow Warehouse",
                productId == 1L ? "WH-1" : "WH-2", type, direction, quantity, BigDecimal.ONE, totalValue,
                "GRN", "REF-" + movementId, 99L, movementDate);
    }

    private ReportingDataClient.PurchaseOrderRecord purchaseOrder(Long id, Long supplierId, String status, BigDecimal amount,
                                                                  boolean overdue, LocalDate actualDeliveryDate, LocalDateTime createdAt) {
        return new ReportingDataClient.PurchaseOrderRecord(
                id, "PO-" + id, supplierId, supplierId == 1L ? "Acme" : "Bravo", 11L, "Main Warehouse",
                status, amount, LocalDate.now().plusDays(2), actualDeliveryDate, overdue, createdAt, null);
    }

    private InventorySnapshot snapshot(Long id, LocalDate date, Long productId, Long warehouseId, BigDecimal quantity, BigDecimal totalValue) {
        return InventorySnapshot.builder()
                .snapshotId(id)
                .snapshotDate(date)
                .productId(productId)
                .productSku("SKU-" + productId)
                .productName(productId == 1L ? "Widget" : "Bolt")
                .warehouseId(warehouseId)
                .warehouseCode(warehouseId == 11L ? "WH-1" : "WH-2")
                .warehouseName(warehouseId == 11L ? "Main Warehouse" : "Overflow Warehouse")
                .quantity(quantity)
                .reservedQuantity(BigDecimal.ONE)
                .availableQuantity(quantity.subtract(BigDecimal.ONE))
                .unitCost(productId == 1L ? BigDecimal.TEN : new BigDecimal("2.0"))
                .totalValue(totalValue)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
