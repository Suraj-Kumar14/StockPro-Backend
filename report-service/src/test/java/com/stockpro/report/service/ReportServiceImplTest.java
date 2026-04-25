package com.stockpro.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.report.client.LowStockItemClientResponse;
import com.stockpro.report.client.MovementClientResponse;
import com.stockpro.report.client.MovementServiceClient;
import com.stockpro.report.client.MovementTypeClient;
import com.stockpro.report.client.PageResponse;
import com.stockpro.report.client.ProductClientResponse;
import com.stockpro.report.client.ProductServiceClient;
import com.stockpro.report.client.PurchaseOrderClientResponse;
import com.stockpro.report.client.PurchaseServiceClient;
import com.stockpro.report.client.WarehouseClientResponse;
import com.stockpro.report.client.WarehouseServiceClient;
import com.stockpro.report.client.WarehouseStockLevelClientResponse;
import com.stockpro.report.config.ReportProperties;
import com.stockpro.report.dto.request.GenerateReportRequest;
import com.stockpro.report.dto.request.ReportFilterRequest;
import com.stockpro.report.dto.request.TakeSnapshotRequest;
import com.stockpro.report.dto.response.GeneratedReportResponse;
import com.stockpro.report.dto.response.InventorySnapshotResponse;
import com.stockpro.report.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.report.entity.InventorySnapshot;
import com.stockpro.report.export.ReportExporter;
import com.stockpro.report.export.ReportFormat;
import com.stockpro.report.export.ReportType;
import com.stockpro.report.mapper.InventorySnapshotMapper;
import com.stockpro.report.repository.ProductAverageStockValueProjection;
import com.stockpro.report.repository.ReportRepository;
import com.stockpro.report.repository.WarehouseStockValueProjection;
import com.stockpro.report.security.AuthenticatedUser;
import com.stockpro.report.security.SecurityUtils;
import com.stockpro.report.service.impl.ReportServiceImpl;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private InventorySnapshotMapper inventorySnapshotMapper;

    @Mock
    private WarehouseServiceClient warehouseServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private MovementServiceClient movementServiceClient;

    @Mock
    private PurchaseServiceClient purchaseServiceClient;

    @Mock
    private ReportExporter reportExporter;

    @Mock
    private ReportEventPublisher reportEventPublisher;

    @Mock
    private SecurityUtils securityUtils;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        ReportProperties reportProperties = new ReportProperties();
        reportProperties.getSnapshot().setPageSize(50);
        reportProperties.getDeadStock().setThresholdDays(90);
        reportProperties.getExport().setDirectory("target/test-report-exports");

        reportService = new ReportServiceImpl(
                reportRepository,
                inventorySnapshotMapper,
                warehouseServiceClient,
                productServiceClient,
                movementServiceClient,
                purchaseServiceClient,
                List.of(reportExporter),
                reportEventPublisher,
                securityUtils,
                reportProperties,
                "REPORT-SERVICE");
    }

    @Test
    void takeSnapshotShouldCreateSnapshotsAndPublishEvent() {
        when(warehouseServiceClient.searchStock(any())).thenReturn(new PageResponse<>(List.of(
                WarehouseStockLevelClientResponse.builder()
                        .warehouseId(1L)
                        .productId(101L)
                        .quantity(new BigDecimal("10.0000"))
                        .build()), 0, 50, 1, 1, true));
        when(reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(1L, 101L, LocalDate.of(2026, 4, 25)))
                .thenReturn(false);
        when(productServiceClient.getProductById(101L)).thenReturn(ProductClientResponse.builder()
                .productId(101L)
                .sku("SKU-101")
                .name("Product 101")
                .costPrice(new BigDecimal("20.0000"))
                .build());
        when(warehouseServiceClient.getWarehouseById(1L)).thenReturn(WarehouseClientResponse.builder()
                .warehouseId(1L)
                .name("Central")
                .build());
        when(securityUtils.getCurrentUserOrNull()).thenReturn(new AuthenticatedUser(
                1L,
                "admin@stockpro.com",
                "ADMIN",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(reportRepository.save(any(InventorySnapshot.class))).thenAnswer(invocation -> {
            InventorySnapshot snapshot = invocation.getArgument(0);
            snapshot.setSnapshotId(99L);
            return snapshot;
        });
        when(inventorySnapshotMapper.toResponse(any(InventorySnapshot.class))).thenReturn(InventorySnapshotResponse.builder()
                .snapshotId(99L)
                .warehouseId(1L)
                .productId(101L)
                .quantity(new BigDecimal("10.0000"))
                .stockValue(new BigDecimal("200.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .build());

        InventorySnapshotResponse response = reportService.takeSnapshot(TakeSnapshotRequest.builder()
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .warehouseId(1L)
                .build());

        assertThat(response.getSnapshotId()).isEqualTo(99L);
        verify(reportEventPublisher).publishSnapshotCompleted(any());
    }

    @Test
    void takeSnapshotShouldReturnExistingSnapshotWhenDuplicateExists() {
        InventorySnapshot existingSnapshot = InventorySnapshot.builder()
                .snapshotId(45L)
                .warehouseId(1L)
                .productId(101L)
                .quantity(new BigDecimal("10.0000"))
                .stockValue(new BigDecimal("200.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .build();

        when(warehouseServiceClient.searchStock(any())).thenReturn(new PageResponse<>(List.of(
                WarehouseStockLevelClientResponse.builder()
                        .warehouseId(1L)
                        .productId(101L)
                        .quantity(new BigDecimal("10.0000"))
                        .build()), 0, 50, 1, 1, true));
        when(reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(1L, 101L, LocalDate.of(2026, 4, 25)))
                .thenReturn(true);
        when(reportRepository.findBySnapshotDateAndFilters(LocalDate.of(2026, 4, 25), 1L, null))
                .thenReturn(List.of(existingSnapshot));
        when(inventorySnapshotMapper.toResponse(existingSnapshot)).thenReturn(InventorySnapshotResponse.builder()
                .snapshotId(45L)
                .warehouseId(1L)
                .productId(101L)
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .build());

        InventorySnapshotResponse response = reportService.takeSnapshot(TakeSnapshotRequest.builder()
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .warehouseId(1L)
                .build());

        assertThat(response.getSnapshotId()).isEqualTo(45L);
        verify(reportRepository, never()).save(any(InventorySnapshot.class));
    }

    @Test
    void getTotalStockValueShouldCalculateUsingLatestSnapshot() {
        when(reportRepository.findLatestSnapshotDateOnOrBefore(null)).thenReturn(LocalDate.of(2026, 4, 25));
        when(reportRepository.sumStockValueBySnapshotDate(LocalDate.of(2026, 4, 25)))
                .thenReturn(new BigDecimal("390.0000"));

        assertThat(reportService.getTotalStockValue(null).getTotalStockValue())
                .isEqualByComparingTo("390.0000");
    }

    @Test
    void getStockValueByWarehouseShouldMapProjection() {
        WarehouseStockValueProjection projection = new WarehouseStockValueProjection() {
            @Override
            public Long getWarehouseId() {
                return 1L;
            }

            @Override
            public String getWarehouseName() {
                return "Central";
            }

            @Override
            public BigDecimal getTotalStockValue() {
                return new BigDecimal("240.0000");
            }
        };

        when(reportRepository.findLatestSnapshotDateOnOrBefore(LocalDate.of(2026, 4, 25)))
                .thenReturn(LocalDate.of(2026, 4, 25));
        when(reportRepository.sumStockValueByWarehouse(LocalDate.of(2026, 4, 25))).thenReturn(List.of(projection));

        assertThat(reportService.getStockValueByWarehouse(LocalDate.of(2026, 4, 25)))
                .singleElement()
                .extracting("warehouseName")
                .isEqualTo("Central");
    }

    @Test
    void getDeadStockShouldRespectConfiguredThreshold() {
        InventorySnapshot latestSnapshot = InventorySnapshot.builder()
                .warehouseId(1L)
                .productId(101L)
                .productName("Product 101")
                .quantity(new BigDecimal("5.0000"))
                .snapshotDate(LocalDate.of(2026, 4, 25))
                .build();

        when(reportRepository.findLatestSnapshots(null, null)).thenReturn(List.of(latestSnapshot));
        when(productServiceClient.getAllProducts()).thenReturn(List.of(ProductClientResponse.builder()
                .productId(101L)
                .name("Product 101")
                .build()));
        when(movementServiceClient.getMovementHistory(101L, 1L)).thenReturn(List.of(MovementClientResponse.builder()
                .productId(101L)
                .warehouseId(1L)
                .movementType(MovementTypeClient.STOCK_OUT)
                .quantity(new BigDecimal("1.0000"))
                .movementDate(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build()));

        assertThat(reportService.getDeadStock(ReportFilterRequest.builder()
                .fromDate(LocalDate.of(2026, 4, 25))
                .toDate(LocalDate.of(2026, 4, 25))
                .page(0)
                .size(20)
                .build()).getContent())
                .hasSize(1);
    }

    @Test
    void getPoSummaryShouldFilterPurchaseOrders() {
        when(purchaseServiceClient.getPOsByDateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of(
                        PurchaseOrderClientResponse.builder()
                                .poId(1L)
                                .supplierId(10L)
                                .warehouseId(1L)
                                .totalAmount(new BigDecimal("100.0000"))
                                .orderDate(LocalDate.of(2026, 4, 2))
                                .build(),
                        PurchaseOrderClientResponse.builder()
                                .poId(2L)
                                .supplierId(10L)
                                .warehouseId(1L)
                                .totalAmount(new BigDecimal("250.0000"))
                                .orderDate(LocalDate.of(2026, 4, 15))
                                .build(),
                        PurchaseOrderClientResponse.builder()
                                .poId(3L)
                                .supplierId(11L)
                                .warehouseId(2L)
                                .totalAmount(new BigDecimal("500.0000"))
                                .orderDate(LocalDate.of(2026, 4, 20))
                                .build()));

        PurchaseOrderSummaryResponse response = reportService.getPOSummary(ReportFilterRequest.builder()
                .supplierId(10L)
                .warehouseId(1L)
                .fromDate(LocalDate.of(2026, 4, 1))
                .toDate(LocalDate.of(2026, 4, 30))
                .build());

        assertThat(response.getTotalPOs()).isEqualTo(2L);
        assertThat(response.getTotalSpend()).isEqualByComparingTo("350.0000");
    }

    @Test
    void generateInventoryReportShouldExportCsvAndPublishEvents() throws Exception {
        when(warehouseServiceClient.getLowStockItems(0, 50)).thenReturn(new PageResponse<>(List.of(
                LowStockItemClientResponse.builder()
                        .warehouseId(1L)
                        .productId(101L)
                        .availableQuantity(new BigDecimal("5.0000"))
                        .build()), 0, 50, 1, 1, true));
        when(productServiceClient.getAllProducts()).thenReturn(List.of(ProductClientResponse.builder()
                .productId(101L)
                .name("Product 101")
                .reorderLevel(new BigDecimal("10.0000"))
                .build()));
        when(reportExporter.supports(ReportFormat.CSV)).thenReturn(true);
        when(reportExporter.export(eq(ReportType.LOW_STOCK), eq(ReportFormat.CSV), any(), any(), any()))
                .thenReturn(Path.of("target", "test-report-exports", "low_stock.csv"));
        when(securityUtils.getCurrentUserOrNull()).thenReturn(new AuthenticatedUser(
                1L,
                "inventory.manager@stockpro.com",
                "INVENTORY_MANAGER",
                List.of(new SimpleGrantedAuthority("ROLE_INVENTORY_MANAGER"))));

        GeneratedReportResponse response = reportService.generateInventoryReport(GenerateReportRequest.builder()
                .reportType(ReportType.LOW_STOCK)
                .format(ReportFormat.CSV)
                .build());

        assertThat(response.getFileName()).isEqualTo("low_stock.csv");
        verify(reportEventPublisher).publishReportGenerationRequested(any());
        verify(reportEventPublisher).publishReportGenerationCompleted(any());
    }
}
