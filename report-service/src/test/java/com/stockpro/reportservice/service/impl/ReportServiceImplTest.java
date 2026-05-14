package com.stockpro.reportservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.reportservice.client.ReportingDataClient;
import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.dto.response.AlertSummaryReportResponse;
import com.stockpro.reportservice.dto.response.GeneratedInventoryReportResponse;
import com.stockpro.reportservice.dto.response.InventorySnapshotResponse;
import com.stockpro.reportservice.entity.InventorySnapshot;
import com.stockpro.reportservice.enums.ExportFormat;
import com.stockpro.reportservice.enums.ReportPeriod;
import com.stockpro.reportservice.exception.DataNotFoundException;
import com.stockpro.reportservice.exception.ReportGenerationException;
import com.stockpro.reportservice.export.ReportExportService;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import com.stockpro.reportservice.service.ReportEventPublisher;
import com.stockpro.reportservice.service.ReportService;
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
import org.springframework.beans.factory.ObjectProvider;
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

    @Mock
    private ObjectProvider<ReportService> selfProvider;

    @Mock
    private ReportService proxiedReportService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reportService, "deadStockDays", 90);
        ReflectionTestUtils.setField(reportService, "slowMovingDays", 30);
        lenient().when(selfProvider.getObject()).thenReturn(proxiedReportService);
    }

    @Test
    void getInventoryValuation_shouldCalculateTotalValueCorrectly() {
        stubInventoryData();
        when(inventorySnapshotRepository.findLatestSnapshotDate()).thenReturn(null);

        var response = reportService.getInventoryValuation(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).build());

        assertThat(response.totalInventoryValue()).isEqualByComparingTo("70.0");
        assertThat(response.totalQuantity()).isEqualByComparingTo("23.0");
        assertThat(response.warehouseBreakdown()).hasSize(2);
        assertThat(response.productBreakdown()).hasSize(2);
        assertThat(response.warnings()).contains("Snapshot data unavailable for requested date. Falling back to live stock data.");
    }

    @Test
    void getStockSummary_shouldReturnCorrectCounts() {
        when(reportingDataClient.getProducts()).thenReturn(List.of(
                new ReportingDataClient.ProductRecord(1L, "SKU-1", "Widget", "Hardware", "BrandA", BigDecimal.TEN, BigDecimal.ONE, 5, 10, true),
                new ReportingDataClient.ProductRecord(2L, "SKU-2", "Bolt", "Fasteners", "BrandB", new BigDecimal("2.0"), BigDecimal.ONE, 4, 15, true)));
        when(reportingDataClient.getStocks(any())).thenReturn(List.of(
                new ReportingDataClient.StockRecord(100L, 11L, "Main Warehouse", 1L, "Widget", "SKU-1", 3, 1, 2, "A1"),
                new ReportingDataClient.StockRecord(101L, 12L, "Overflow Warehouse", 2L, "Bolt", "SKU-2", 20, 0, 20, "B1")));

        var response = reportService.getStockSummary(ReportFilterRequest.builder().build());

        assertThat(response.totalProducts()).isEqualTo(2);
        assertThat(response.totalWarehouses()).isEqualTo(2);
        assertThat(response.lowStockCount()).isEqualTo(1);
        assertThat(response.overstockCount()).isEqualTo(1);
        assertThat(response.totalAvailableQuantity()).isEqualByComparingTo("22.0");
    }

    @Test
    void createInventorySnapshotForDate_shouldSkipDuplicates() {
        stubInventoryData();
        when(inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(LocalDate.of(2026, 5, 1), 1L, 11L))
                .thenReturn(Optional.of(InventorySnapshot.builder().snapshotId(99L).build()));
        when(inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(LocalDate.of(2026, 5, 1), 2L, 12L))
                .thenReturn(Optional.empty());

        reportService.createInventorySnapshotForDate(LocalDate.of(2026, 5, 1));

        verify(inventorySnapshotRepository, times(1)).save(any());
    }

    @Test
    void snapshotDelegationMethods_shouldUseProxyTarget() {
        LocalDate requestedDate = LocalDate.of(2026, 5, 1);

        reportService.takeSnapshot(requestedDate);
        reportService.takeSnapshot(null);
        reportService.createDailyInventorySnapshot();

        verify(proxiedReportService).createInventorySnapshotForDate(requestedDate);
        verify(proxiedReportService, times(3)).createInventorySnapshotForDate(any(LocalDate.class));
    }

    @Test
    void inventoryReportMethods_shouldBuildPagesAndThresholds() {
        stubInventoryData();
        when(inventorySnapshotRepository.findLatestSnapshotDate()).thenReturn(null);
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
            assertThat(item.severity()).isEqualTo("WARNING");
            assertThat(item.recommendedAction()).contains("replenishment");
        });
        assertThat(overstock.getContent()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(2L);
            assertThat(item.availableQuantity()).isEqualByComparingTo("20.0");
            assertThat(item.maxStockLevel()).isEqualByComparingTo("15.0");
        });
        assertThat(movementReport.getContent()).singleElement().satisfies(item -> {
            assertThat(item.movementType()).isEqualTo("STOCK_OUT");
            assertThat(item.totalValue()).isEqualByComparingTo("40.0");
        });
    }

    @Test
    void movementAnalysisMethods_shouldComputeTurnoverTopSlowAndDeadStock() {
        stubInventoryData();
        when(reportingDataClient.searchAllMovements(any())).thenReturn(List.of(
                movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(5)),
                movement(2L, 1L, "Widget", "STOCK_IN", "IN", new BigDecimal("2.0"), new BigDecimal("20.0"), LocalDateTime.now().minusDays(3)),
                movement(3L, 2L, "Bolt", "STOCK_OUT", "OUT", new BigDecimal("1.0"), new BigDecimal("2.0"), LocalDateTime.now().minusDays(120))));
        when(inventorySnapshotRepository.findBySnapshotDateBetween(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9))).thenReturn(List.of(
                snapshot(1L, LocalDate.of(2026, 5, 1), 1L, 11L, new BigDecimal("10.0"), new BigDecimal("100.0")),
                snapshot(2L, LocalDate.of(2026, 5, 1), 2L, 12L, new BigDecimal("20.0"), new BigDecimal("40.0")),
                snapshot(3L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("6.0"), new BigDecimal("60.0")),
                snapshot(4L, LocalDate.of(2026, 5, 9), 2L, 12L, new BigDecimal("19.0"), new BigDecimal("38.0"))));

        var turnover = reportService.getInventoryTurnover(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9), null);
        var topMoving = reportService.getTopMovingProducts(ReportFilterRequest.builder().size(10).build());
        var slowMoving = reportService.getSlowMovingProducts(ReportFilterRequest.builder().size(10).build());
        var deadStock = reportService.getDeadStockReport(ReportFilterRequest.builder().size(10).build());

        assertThat(turnover.productTurnover()).extracting("productId").contains(1L, 2L);
        assertThat(turnover.note()).contains("COGS is estimated");
        assertThat(topMoving.get(0).productId()).isEqualTo(1L);
        assertThat(slowMoving).extracting("productId").contains(2L);
        assertThat(deadStock).extracting("productId").contains(2L);
    }

    @Test
    void generateInventoryReport_shouldReturnConsolidatedCaseStudyResponse() {
        stubInventoryData();
        when(reportingDataClient.searchAllMovements(any())).thenReturn(List.of(
                movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(5)),
                movement(2L, 1L, "Widget", "STOCK_IN", "IN", new BigDecimal("2.0"), new BigDecimal("20.0"), LocalDateTime.now().minusDays(3)),
                movement(3L, 2L, "Bolt", "STOCK_OUT", "OUT", new BigDecimal("1.0"), new BigDecimal("2.0"), LocalDateTime.now().minusDays(120))));
        when(reportingDataClient.searchPurchaseOrders(any())).thenReturn(List.of(
                purchaseOrder(1L, 1L, "RECEIVED", new BigDecimal("400.0"), false, LocalDate.now().minusDays(4), LocalDateTime.now().minusDays(5)),
                purchaseOrder(2L, 1L, "PENDING_APPROVAL", new BigDecimal("200.0"), true, null, LocalDateTime.now().minusDays(2))));
        when(inventorySnapshotRepository.findLatestSnapshotDate()).thenReturn(null);
        when(inventorySnapshotRepository.findBySnapshotDateBetween(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 9))).thenReturn(List.of(
                snapshot(1L, LocalDate.of(2026, 5, 1), 1L, 11L, new BigDecimal("10.0"), new BigDecimal("100.0")),
                snapshot(2L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("6.0"), new BigDecimal("60.0"))));

        GeneratedInventoryReportResponse report = reportService.generateInventoryReport(
                ReportFilterRequest.builder()
                        .fromDate(LocalDate.of(2026, 5, 1))
                        .toDate(LocalDate.of(2026, 5, 9))
                        .size(10)
                        .build(),
                5,
                90L);

        assertThat(report.valuation().totalInventoryValue()).isNotNull();
        assertThat(report.stockValueByWarehouse()).hasSize(2);
        assertThat(report.turnover().turnoverRate()).isNotNull();
        assertThat(report.lowStock()).hasSize(1);
        assertThat(report.topMovingProducts()).isNotEmpty();
        assertThat(report.slowMovingProducts()).isNotEmpty();
        assertThat(report.deadStock()).extracting("productId").contains(2L);
        assertThat(report.poSummary().totalPurchaseOrders()).isEqualTo(2);
    }

    @Test
    void convenienceAndThresholdMethods_shouldSupportCaseStudyOverrides() {
        stubInventoryData();
        when(reportingDataClient.searchAllMovements(any())).thenReturn(List.of(
                movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("1.0"), new BigDecimal("10.0"), LocalDateTime.now().minusDays(10)),
                movement(2L, 2L, "Bolt", "STOCK_OUT", "OUT", new BigDecimal("1.0"), new BigDecimal("2.0"), LocalDateTime.now().minusDays(120))));
        when(inventorySnapshotRepository.findLatestSnapshotDate()).thenReturn(LocalDate.of(2026, 5, 9));
        when(inventorySnapshotRepository.findBySnapshotDate(LocalDate.of(2026, 5, 9))).thenReturn(List.of(
                snapshot(1L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("3.0"), new BigDecimal("30.0")),
                snapshot(2L, LocalDate.of(2026, 5, 9), 2L, 12L, new BigDecimal("20.0"), new BigDecimal("40.0"))));

        var totalValue = reportService.getTotalStockValue(null, LocalDate.of(2026, 5, 9));
        var byWarehouse = reportService.getStockValueByWarehouse(LocalDate.of(2026, 5, 9));
        var slowMoving = reportService.getSlowMovingProducts(ReportFilterRequest.builder().size(10).build(), 1);
        var deadStock = reportService.getDeadStockReport(ReportFilterRequest.builder().size(10).build(), 90L);

        assertThat(totalValue.totalInventoryValue()).isEqualByComparingTo("70.0");
        assertThat(byWarehouse).hasSize(2);
        assertThat(slowMoving).extracting("productId").contains(1L, 2L);
        assertThat(deadStock).extracting("productId").contains(2L);
    }

    @Test
    void purchasePaymentAndAlertMethods_shouldAggregateAndSurfaceFailures() {
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
                payment(1L, "PAY-1", 1L, "PO-1", 1L, "Acme", "PAID", "RAZORPAY", new BigDecimal("400.0"), BigDecimal.ZERO),
                payment(2L, "PAY-2", 2L, "PO-2", 1L, "Acme", "PARTIALLY_PAID", "NEFT", new BigDecimal("300.0"), new BigDecimal("20.0")),
                payment(3L, "PAY-3", 3L, "PO-3", 2L, "Bravo", "APPROVED", "NEFT", BigDecimal.ZERO, new BigDecimal("30.0"))));
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
        assertThat(paymentSummary.totalPayments()).isEqualTo(4);
        assertThat(paymentSummary.supplierBreakdown()).hasSize(2);
        assertThat(paymentSummary.methodBreakdown()).containsEntry("RAZORPAY", 1L);
        assertThat(alertSummary.alertsByType()).containsEntry("LOW_STOCK", 2L);

        when(reportingDataClient.getPaymentSummary()).thenThrow(new RuntimeException("payment service down"));
        ReportFilterRequest paymentSummaryRequest = ReportFilterRequest.builder().size(10).build();
        assertThrows(RuntimeException.class, () -> reportService.getPaymentSummary(paymentSummaryRequest));

        when(reportingDataClient.getSuppliers()).thenThrow(new RuntimeException("supplier service down"));
        assertThat(reportService.getSupplierPerformanceReport(ReportFilterRequest.builder().size(10).build()).getContent())
                .extracting("supplierName")
                .containsOnly("Unknown supplier", "Unknown supplier");

        ReportFilterRequest missingSupplierRequest = ReportFilterRequest.builder().size(10).build();
        assertThrows(DataNotFoundException.class, () -> reportService.getSupplierPerformance(99L, missingSupplierRequest));
    }

    @Test
    void purchaseSummary_shouldHandleOrdersWithMissingStatusWithoutFailing() {
        when(reportingDataClient.searchPurchaseOrders(any())).thenReturn(List.of(
                purchaseOrder(1L, 1L, null, new BigDecimal("400.0"), false, LocalDate.now().minusDays(4), LocalDateTime.now().minusDays(5))));

        var summary = reportService.getPurchaseOrderReport(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 13), null, null);

        assertThat(summary.totalPurchaseOrders()).isEqualTo(1);
        assertThat(summary.statusBreakdown()).containsEntry("UNKNOWN", 1L);
    }

    @Test
    void purchaseSummary_shouldReturnZeroSummaryWhenNoOrdersExist() {
        when(reportingDataClient.searchPurchaseOrders(any())).thenReturn(List.of());

        var summary = reportService.getPurchaseSummary(ReportFilterRequest.builder().size(10).build());

        assertThat(summary.totalPurchaseOrders()).isZero();
        assertThat(summary.totalSpend()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.supplierBreakdown()).isEmpty();
        assertThat(summary.warehouseBreakdown()).isEmpty();
        assertThat(summary.statusBreakdown()).isEmpty();
    }

    @Test
    void purchaseSummary_shouldWrapDownstreamFailureWithFriendlyMessage() {
        when(reportingDataClient.searchPurchaseOrders(any()))
                .thenThrow(new ReportGenerationException("Failed to call PURCHASE-SERVICE from searchPurchaseOrders"));

        var exception = assertThrows(ReportGenerationException.class,
                () -> reportService.getPurchaseSummary(ReportFilterRequest.builder().size(10).build()));

        assertThat(exception.getMessage()).isEqualTo("Purchase summary data is temporarily unavailable.");
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
                payment(1L, "PAY-1", 1L, "PO-1", 1L, "Acme", "PAID", "RAZORPAY", new BigDecimal("400.0"), BigDecimal.ZERO)));
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
        when(reportingDataClient.searchAllMovements(any())).thenReturn(List.of(
                movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(1)),
                movement(2L, 2L, "Bolt", "STOCK_IN", "IN", new BigDecimal("3.0"), new BigDecimal("6.0"), LocalDateTime.now().minusDays(2))));
        when(inventorySnapshotRepository.findLatestSnapshotDate()).thenReturn(LocalDate.of(2026, 5, 9));
        when(inventorySnapshotRepository.findBySnapshotDate(LocalDate.of(2026, 5, 9))).thenReturn(List.of(
                snapshot(10L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0"))));
        when(inventorySnapshotRepository.findBySnapshotDateOrderByWarehouseIdAscProductIdAsc(LocalDate.of(2026, 5, 9))).thenReturn(List.of(
                snapshot(10L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0"))));
        when(inventorySnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of(
                snapshot(10L, LocalDate.of(2026, 5, 1), 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0")),
                snapshot(11L, LocalDate.of(2026, 5, 9), 1L, 11L, new BigDecimal("6.0"), new BigDecimal("60.0"))));
        when(inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(eq(LocalDate.of(2026, 5, 9)), any(), any()))
                .thenReturn(Optional.empty());
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
    void executiveDashboardReport_shouldReturnFallbackDefaultsWhenSectionsFail() {
        when(reportingDataClient.searchPurchaseOrders(any())).thenThrow(new RuntimeException("purchase down"));
        when(reportingDataClient.getPaymentSummary()).thenThrow(new RuntimeException("payment down"));
        when(reportingDataClient.getSystemAlertSummary()).thenThrow(new RuntimeException("alerts down"));
        when(reportingDataClient.getRecentAlerts(true)).thenThrow(new RuntimeException("alerts down"));
        when(reportingDataClient.searchAllMovements(any())).thenThrow(new RuntimeException("movement down"));
        when(reportingDataClient.getProducts()).thenThrow(new RuntimeException("products down"));

        var response = reportService.getExecutiveDashboardReport();

        assertThat(response.totalProducts()).isZero();
        assertThat(response.activeProducts()).isZero();
        assertThat(response.totalWarehouses()).isZero();
        assertThat(response.inventoryValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.topMovingProducts()).isEmpty();
        assertThat(response.recentAlerts()).isEmpty();
        assertThat(response.valuationTrend()).hasSize(7).allSatisfy(point ->
                assertThat(point.value()).isEqualByComparingTo(BigDecimal.ZERO));
        assertThat(response.purchaseTrend()).isEmpty();
        assertThat(response.unavailableSections()).contains(
                "inventory", "purchase", "payments", "alerts", "movements", "product-service");
    }

    @Test
    void getInventorySnapshots_withFilters_shouldApplyDefaultDatesAndFiltering() {
        LocalDate snapshotDate = LocalDate.of(2026, 5, 9);
        when(inventorySnapshotRepository.findLatestSnapshotDate()).thenReturn(snapshotDate);
        when(inventorySnapshotRepository.findBySnapshotDateBetween(snapshotDate, snapshotDate)).thenReturn(List.of(
                snapshot(1L, snapshotDate, 1L, 11L, new BigDecimal("5.0"), new BigDecimal("50.0")),
                snapshot(2L, snapshotDate, 2L, 12L, new BigDecimal("6.0"), new BigDecimal("12.0"))));

        var page = reportService.getInventorySnapshots(11L, 1L, null, null, 0, 10);

        assertThat(page.getContent()).singleElement().satisfies(item -> {
            assertThat(item.warehouseId()).isEqualTo(11L);
            assertThat(item.productId()).isEqualTo(1L);
        });
    }

    @Test
    void snapshotTrend_shouldRejectInvalidDateRange() {
        LocalDate fromDate = LocalDate.of(2026, 5, 9);
        LocalDate toDate = LocalDate.of(2026, 5, 1);
        assertThrows(IllegalArgumentException.class, () -> reportService.getSnapshotTrend(1L, 1L, fromDate, toDate));
    }

    @Test
    void normalization_shouldCoverPeriodBranchesAndPaginationDefaults() {
        stubInventoryData();

        reportService.getStockSummary(null);
        reportService.getStockSummary(ReportFilterRequest.builder().period(ReportPeriod.TODAY).size(0).page(-2).build());
        reportService.getStockSummary(ReportFilterRequest.builder().period(ReportPeriod.LAST_7_DAYS).size(10).build());
        reportService.getStockSummary(ReportFilterRequest.builder().period(ReportPeriod.THIS_MONTH).size(10).build());
        reportService.getStockSummary(ReportFilterRequest.builder().period(ReportPeriod.LAST_MONTH).size(10).build());
        reportService.getStockSummary(ReportFilterRequest.builder()
                .period(ReportPeriod.CUSTOM)
                .fromDate(LocalDate.of(2026, 5, 1))
                .size(10)
                .build());

        ArgumentCaptor<ReportFilterRequest> requestCaptor = ArgumentCaptor.forClass(ReportFilterRequest.class);
        verify(reportingDataClient, times(6)).getStocks(requestCaptor.capture());
        List<ReportFilterRequest> captured = requestCaptor.getAllValues();

        assertThat(captured.get(0).getSize()).isEqualTo(10);
        assertThat(captured.get(0).getPage()).isZero();
        assertThat(captured.get(0).getFromDate()).isNull();
        assertThat(captured.get(0).getToDate()).isNull();

        LocalDate today = LocalDate.now();
        assertThat(captured.get(1).getFromDate()).isEqualTo(today);
        assertThat(captured.get(1).getToDate()).isEqualTo(today);
        assertThat(captured.get(1).getSize()).isEqualTo(10);
        assertThat(captured.get(1).getPage()).isZero();

        assertThat(captured.get(2).getFromDate()).isEqualTo(today.minusDays(6));
        assertThat(captured.get(2).getToDate()).isEqualTo(today);

        assertThat(captured.get(3).getFromDate()).isEqualTo(today.withDayOfMonth(1));
        assertThat(captured.get(3).getToDate()).isEqualTo(today);

        LocalDate previousMonth = today.minusMonths(1);
        assertThat(captured.get(4).getFromDate()).isEqualTo(previousMonth.withDayOfMonth(1));
        assertThat(captured.get(4).getToDate()).isEqualTo(previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()));

        assertThat(captured.get(5).getFromDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(captured.get(5).getToDate()).isEqualTo(today);
    }

    @Test
    void detailStatusTurnoverAndExportMethods_shouldCoverRemainingReportPaths() {
        stubInventoryData();
        when(reportingDataClient.searchAllMovements(any())).thenReturn(List.of(
                movement(1L, 1L, "Widget", "STOCK_OUT", "OUT", new BigDecimal("4.0"), new BigDecimal("40.0"), LocalDateTime.now().minusDays(3)),
                movement(2L, 2L, "Bolt", "STOCK_OUT", "OUT", new BigDecimal("2.0"), new BigDecimal("4.0"), LocalDateTime.now().minusDays(2))));
        when(inventorySnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of(
                snapshot(1L, LocalDate.now().minusDays(6), 1L, 11L, new BigDecimal("8.0"), new BigDecimal("80.0")),
                snapshot(2L, LocalDate.now(), 1L, 11L, new BigDecimal("4.0"), new BigDecimal("40.0")),
                snapshot(3L, LocalDate.now().minusDays(6), 2L, 12L, new BigDecimal("10.0"), new BigDecimal("20.0")),
                snapshot(4L, LocalDate.now(), 2L, 12L, new BigDecimal("8.0"), new BigDecimal("16.0"))));
        when(reportingDataClient.searchPayments(any())).thenReturn(List.of(
                payment(1L, "PAY-1", 1L, "PO-1", 1L, "Acme", "PAID", "RAZORPAY", new BigDecimal("400.0"), BigDecimal.ZERO),
                payment(2L, "PAY-2", 2L, "PO-2", 2L, "Bravo", "APPROVED", "NEFT", new BigDecimal("150.0"), new BigDecimal("25.0"))));

        when(reportingDataClient.getPurchaseOrder(1L)).thenReturn(purchaseOrderDetail(
                1L, "PO-1", "Acme", "Main Warehouse", "APPROVED",
                "Damaged items", null,
                LocalDateTime.now().minusDays(1), null));
        when(reportingDataClient.getPurchaseOrder(2L)).thenReturn(purchaseOrderDetail(
                2L, "PO-2", "Bravo", "Overflow Warehouse", "REJECTED",
                null, "Pricing mismatch",
                null, LocalDateTime.now().minusHours(12)));
        when(reportingDataClient.getPaymentsByPurchaseOrder(1L)).thenReturn(List.of(
                payment(10L, "PAY-10", 1L, "PO-1", 1L, "Acme", "PAID", "RAZORPAY", new BigDecimal("250.0"), BigDecimal.ZERO)));
        when(reportingDataClient.getRemainingAmount(1L)).thenReturn(
                new ReportingDataClient.RemainingAmountRecord(1L, "PO-1", new BigDecimal("500.0"), new BigDecimal("250.0"), new BigDecimal("250.0"), "PARTIAL"));
        when(reportingDataClient.searchPurchaseOrders(any())).thenReturn(List.of(
                purchaseOrder(1L, 1L, "CANCELLED", new BigDecimal("500.0"), false, null, LocalDateTime.now().minusDays(3)),
                purchaseOrder(2L, 2L, "REJECTED", new BigDecimal("300.0"), false, null, LocalDateTime.now().minusDays(2))));

        when(reportExportService.exportExcel(any(), any(), any())).thenReturn("excel-bytes".getBytes());
        when(reportExportService.exportPdf(any(), any(), any())).thenReturn("pdf-bytes".getBytes());

        var detail = reportService.getPurchaseOrderDetailReport(1L);
        var billingReport = reportService.getPaymentBillingReport(ReportFilterRequest.builder().size(10).build());
        var cancelledReport = reportService.getCancelledPurchaseOrderReport(ReportFilterRequest.builder().size(10).build());
        var rejectedReport = reportService.getRejectedPurchaseOrderReport(ReportFilterRequest.builder().size(10).build());
        var turnoverReport = reportService.getInventoryTurnoverReport(ReportFilterRequest.builder().toDate(LocalDate.now()).size(10).build());
        byte[] excelExport = reportService.exportInventoryValuation(ReportFilterRequest.builder().size(10).build(), ExportFormat.EXCEL);
        byte[] pdfExport = reportService.exportExecutiveDashboard(ExportFormat.PDF);

        assertThat(detail.items()).hasSize(1);
        assertThat(detail.timeline()).hasSize(1);
        assertThat(detail.paymentSummary().remainingAmount()).isEqualByComparingTo("250.0");

        assertThat(billingReport.getContent()).hasSize(2);
        assertThat(billingReport.getContent().get(0).paymentNumber()).isEqualTo("PAY-1");

        assertThat(cancelledReport.cancelledCount()).isEqualTo(2);
        assertThat(cancelledReport.cancelledOrders()).hasSize(2);
        assertThat(cancelledReport.cancelledOrders()).extracting("actionedByName").contains("Approver 1", "Approver 2");

        assertThat(rejectedReport.rejectedCount()).isEqualTo(2);
        assertThat(rejectedReport.rejectedOrders()).hasSize(2);

        assertThat(turnoverReport).hasSize(2);
        assertThat(turnoverReport).extracting("productId").contains(1L, 2L);

        assertThat(new String(excelExport)).isEqualTo("excel-bytes");
        assertThat(new String(pdfExport)).isEqualTo("pdf-bytes");
    }

    private void stubInventoryData() {
        when(reportingDataClient.getProducts()).thenReturn(List.of(
                new ReportingDataClient.ProductRecord(1L, "SKU-1", "Widget", "Hardware", "BrandA", BigDecimal.TEN, BigDecimal.ONE, 5, 10, true),
                new ReportingDataClient.ProductRecord(2L, "SKU-2", "Bolt", "Fasteners", "BrandB", new BigDecimal("2.0"), BigDecimal.ONE, 4, 15, true)));
        lenient().when(reportingDataClient.getWarehouses()).thenReturn(List.of(
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

    private ReportingDataClient.PaymentRecord payment(Long paymentId, String paymentNumber, Long purchaseOrderId, String poNumber,
                                                      Long supplierId, String supplierName, String status, String paymentMethod,
                                                      BigDecimal paymentAmount, BigDecimal remainingAmount) {
        return new ReportingDataClient.PaymentRecord(
                paymentId,
                paymentNumber,
                purchaseOrderId,
                poNumber,
                supplierId,
                supplierName,
                status,
                paymentMethod,
                paymentAmount,
                new BigDecimal("500.0"),
                BigDecimal.ZERO,
                remainingAmount,
                "INR",
                LocalDate.now(),
                "TXN-" + paymentId,
                "RZP-ORDER-" + paymentId,
                "RZP-PAY-" + paymentId,
                1L,
                2L,
                LocalDateTime.now(),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now());
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

    private ReportingDataClient.PurchaseOrderDetailRecord purchaseOrderDetail(
            Long id,
            String poNumber,
            String supplierName,
            String warehouseName,
            String status,
            String cancellationReason,
            String rejectionReason,
            LocalDateTime cancelledAt,
            LocalDateTime rejectedAt) {
        return new ReportingDataClient.PurchaseOrderDetailRecord(
                id,
                poNumber,
                id,
                supplierName,
                11L,
                warehouseName,
                100L + id,
                "Creator " + id,
                200L + id,
                "Approver " + id,
                status,
                new BigDecimal("450.0"),
                new BigDecimal("25.0"),
                BigDecimal.ZERO,
                new BigDecimal("25.0"),
                new BigDecimal("500.0"),
                LocalDate.now().plusDays(2),
                null,
                "NET30",
                "Case study order",
                "Approved",
                rejectionReason,
                cancellationReason,
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(2),
                rejectedAt,
                cancelledAt,
                null,
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().minusDays(1),
                false,
                "PARTIAL",
                false,
                List.of(new ReportingDataClient.PurchaseOrderLineItemRecord(
                        1L, 1L, "SKU-1", "Widget", 5, 3, 2, BigDecimal.TEN, new BigDecimal("50.0"), "Keep chilled")),
                List.of(new ReportingDataClient.PurchaseOrderHistoryRecord(
                        1L, "APPROVED", "SUBMITTED", "APPROVED", 200L + id, "Reviewed", LocalDateTime.now().minusDays(2))));
    }
}
