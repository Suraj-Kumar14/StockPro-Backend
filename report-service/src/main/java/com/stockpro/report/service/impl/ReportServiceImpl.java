package com.stockpro.report.service.impl;

import com.stockpro.report.client.LowStockItemClientResponse;
import com.stockpro.report.client.MovementClientResponse;
import com.stockpro.report.client.MovementServiceClient;
import com.stockpro.report.client.MovementTypeClient;
import com.stockpro.report.client.PageResponse;
import com.stockpro.report.client.ProductClientResponse;
import com.stockpro.report.client.ProductServiceClient;
import com.stockpro.report.client.PurchaseOrderClientResponse;
import com.stockpro.report.client.PurchaseServiceClient;
import com.stockpro.report.client.StockSearchClientRequest;
import com.stockpro.report.client.WarehouseClientResponse;
import com.stockpro.report.client.WarehouseServiceClient;
import com.stockpro.report.client.WarehouseStockLevelClientResponse;
import com.stockpro.report.config.ReportProperties;
import com.stockpro.report.dto.event.InventorySnapshotCompletedEvent;
import com.stockpro.report.dto.event.ReportGenerationCompletedEvent;
import com.stockpro.report.dto.event.ReportGenerationRequestedEvent;
import com.stockpro.report.dto.request.GenerateReportRequest;
import com.stockpro.report.dto.request.ReportFilterRequest;
import com.stockpro.report.dto.request.TakeSnapshotRequest;
import com.stockpro.report.dto.response.DeadStockResponse;
import com.stockpro.report.dto.response.GeneratedReportResponse;
import com.stockpro.report.dto.response.InventorySnapshotResponse;
import com.stockpro.report.dto.response.InventoryTurnoverResponse;
import com.stockpro.report.dto.response.LowStockReportResponse;
import com.stockpro.report.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.report.dto.response.SlowMovingProductResponse;
import com.stockpro.report.dto.response.StockMovementSummaryResponse;
import com.stockpro.report.dto.response.TopMovingProductResponse;
import com.stockpro.report.dto.response.TotalStockValueResponse;
import com.stockpro.report.dto.response.WarehouseStockValueResponse;
import com.stockpro.report.entity.InventorySnapshot;
import com.stockpro.report.exception.ExternalServiceException;
import com.stockpro.report.exception.InvalidReportRequestException;
import com.stockpro.report.exception.ReportGenerationException;
import com.stockpro.report.exception.ReportNotFoundException;
import com.stockpro.report.exception.SnapshotCreationException;
import com.stockpro.report.export.ReportExporter;
import com.stockpro.report.export.ReportFormat;
import com.stockpro.report.export.ReportType;
import com.stockpro.report.mapper.InventorySnapshotMapper;
import com.stockpro.report.repository.ProductAverageStockValueProjection;
import com.stockpro.report.repository.ReportRepository;
import com.stockpro.report.repository.WarehouseStockValueProjection;
import com.stockpro.report.security.AuthenticatedUser;
import com.stockpro.report.security.SecurityUtils;
import com.stockpro.report.service.ReportEventPublisher;
import com.stockpro.report.service.ReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportServiceImpl.class);

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_SCHEDULED = "SCHEDULED";
    private static final String SYSTEM_USER = "SYSTEM";
    private static final int DEFAULT_LOOKBACK_DAYS = 30;
    private static final int EXPORT_PAGE_SIZE = 10_000;
    private static final Set<MovementTypeClient> OUTGOING_MOVEMENTS = EnumSet.of(
            MovementTypeClient.STOCK_OUT,
            MovementTypeClient.TRANSFER_OUT,
            MovementTypeClient.WRITE_OFF,
            MovementTypeClient.RETURN);

    private final ReportRepository reportRepository;
    private final InventorySnapshotMapper inventorySnapshotMapper;
    private final WarehouseServiceClient warehouseServiceClient;
    private final ProductServiceClient productServiceClient;
    private final MovementServiceClient movementServiceClient;
    private final PurchaseServiceClient purchaseServiceClient;
    private final List<ReportExporter> reportExporters;
    private final ReportEventPublisher reportEventPublisher;
    private final SecurityUtils securityUtils;
    private final ReportProperties reportProperties;
    private final String applicationName;

    public ReportServiceImpl(ReportRepository reportRepository,
            InventorySnapshotMapper inventorySnapshotMapper,
            WarehouseServiceClient warehouseServiceClient,
            ProductServiceClient productServiceClient,
            MovementServiceClient movementServiceClient,
            PurchaseServiceClient purchaseServiceClient,
            List<ReportExporter> reportExporters,
            ReportEventPublisher reportEventPublisher,
            SecurityUtils securityUtils,
            ReportProperties reportProperties,
            @Value("${spring.application.name:REPORT-SERVICE}") String applicationName) {
        this.reportRepository = reportRepository;
        this.inventorySnapshotMapper = inventorySnapshotMapper;
        this.warehouseServiceClient = warehouseServiceClient;
        this.productServiceClient = productServiceClient;
        this.movementServiceClient = movementServiceClient;
        this.purchaseServiceClient = purchaseServiceClient;
        this.reportExporters = reportExporters;
        this.reportEventPublisher = reportEventPublisher;
        this.securityUtils = securityUtils;
        this.reportProperties = reportProperties;
        this.applicationName = applicationName;
    }

    @Override
    @Transactional
    public InventorySnapshotResponse takeSnapshot(TakeSnapshotRequest request) {
        validateSnapshotRequest(request);
        List<InventorySnapshot> createdSnapshots = createSnapshots(request.getSnapshotDate(), request.getWarehouseId(), SOURCE_MANUAL);

        if (!createdSnapshots.isEmpty()) {
            return inventorySnapshotMapper.toResponse(createdSnapshots.get(0));
        }

        List<InventorySnapshot> existingSnapshots = reportRepository.findBySnapshotDateAndFilters(
                request.getSnapshotDate(),
                request.getWarehouseId(),
                null);
        if (existingSnapshots.isEmpty()) {
            throw new SnapshotCreationException("No stock data was available to create inventory snapshots.");
        }

        return inventorySnapshotMapper.toResponse(existingSnapshots.get(0));
    }

    @Override
    @Transactional
    public void takeDailySnapshot() {
        LocalDate snapshotDate = LocalDate.now();
        LOGGER.info("Starting daily inventory snapshot for snapshotDate={}", snapshotDate);

        try {
            List<InventorySnapshot> createdSnapshots = createSnapshots(snapshotDate, null, SOURCE_SCHEDULED);
            LOGGER.info("Completed daily inventory snapshot for snapshotDate={} createdSnapshots={}",
                    snapshotDate,
                    createdSnapshots.size());
        } catch (RuntimeException exception) {
            LOGGER.error("Daily inventory snapshot failed for snapshotDate={}: {}", snapshotDate, exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TotalStockValueResponse getTotalStockValue(LocalDate snapshotDate) {
        LocalDate resolvedSnapshotDate = resolveSnapshotDate(snapshotDate);
        BigDecimal totalStockValue = defaultScale(reportRepository.sumStockValueBySnapshotDate(resolvedSnapshotDate));

        return TotalStockValueResponse.builder()
                .snapshotDate(resolvedSnapshotDate)
                .totalStockValue(totalStockValue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseStockValueResponse> getStockValueByWarehouse(LocalDate snapshotDate) {
        LocalDate resolvedSnapshotDate = resolveSnapshotDate(snapshotDate);
        return reportRepository.sumStockValueByWarehouse(resolvedSnapshotDate).stream()
                .map(this::toWarehouseStockValueResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTurnoverResponse> getInventoryTurnover(ReportFilterRequest request) {
        DateRange dateRange = resolveDateRange(request);
        List<MovementClientResponse> movements = filterMovements(loadMovements(dateRange), request);
        Map<Long, BigDecimal> outgoingQuantityByProduct = movements.stream()
                .filter(movement -> movement.getProductId() != null)
                .filter(movement -> OUTGOING_MOVEMENTS.contains(movement.getMovementType()))
                .collect(Collectors.groupingBy(
                        MovementClientResponse::getProductId,
                        Collectors.reducing(BigDecimal.ZERO,
                                movement -> magnitude(movement.getQuantity()),
                                BigDecimal::add)));

        List<ProductAverageStockValueProjection> averageInventory = reportRepository
                .averageInventoryValueByProductAndDateRange(dateRange.fromDate(), dateRange.toDate());
        Map<Long, BigDecimal> averageInventoryValueByProduct = averageInventory.stream()
                .collect(Collectors.toMap(
                        ProductAverageStockValueProjection::getProductId,
                        projection -> defaultScale(projection.getAverageStockValue())));

        Set<Long> productIds = unionKeys(outgoingQuantityByProduct.keySet(), averageInventoryValueByProduct.keySet());
        if (request.getProductId() != null) {
            productIds = productIds.stream()
                    .filter(productId -> productId.equals(request.getProductId()))
                    .collect(Collectors.toSet());
        }

        Map<Long, ProductClientResponse> productMap = fetchProducts(productIds);
        List<InventoryTurnoverResponse> responses = productIds.stream()
                .map(productId -> buildInventoryTurnoverResponse(
                        productId,
                        productMap.get(productId),
                        outgoingQuantityByProduct.getOrDefault(productId, BigDecimal.ZERO),
                        averageInventoryValueByProduct.getOrDefault(productId, BigDecimal.ZERO),
                        dateRange))
                .sorted(buildInventoryTurnoverComparator(request))
                .toList();

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LowStockReportResponse> getLowStockReport(ReportFilterRequest request) {
        List<LowStockItemClientResponse> lowStockItems = loadAllLowStockItems().stream()
                .filter(item -> request.getWarehouseId() == null || Objects.equals(item.getWarehouseId(), request.getWarehouseId()))
                .filter(item -> request.getProductId() == null || Objects.equals(item.getProductId(), request.getProductId()))
                .toList();

        Map<Long, ProductClientResponse> productMap = fetchProducts(lowStockItems.stream()
                .map(LowStockItemClientResponse::getProductId)
                .collect(Collectors.toSet()));

        List<LowStockReportResponse> responses = lowStockItems.stream()
                .map(item -> {
                    ProductClientResponse product = productMap.get(item.getProductId());
                    return LowStockReportResponse.builder()
                            .productId(item.getProductId())
                            .productName(product != null ? product.getName() : null)
                            .warehouseId(item.getWarehouseId())
                            .availableQuantity(defaultScale(item.getAvailableQuantity()))
                            .reorderLevel(product != null ? defaultScale(product.getReorderLevel()) : BigDecimal.ZERO)
                            .build();
                })
                .sorted(buildLowStockComparator(request))
                .toList();

        return buildPage(responses, request.getPage(), request.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementSummaryResponse getStockMovementSummary(ReportFilterRequest request) {
        DateRange dateRange = resolveDateRange(request);
        List<MovementClientResponse> movements = filterMovements(loadMovements(dateRange), request);

        BigDecimal stockIn = sumMovementsByType(movements, MovementTypeClient.STOCK_IN);
        BigDecimal stockOut = sumMovementsByType(movements, MovementTypeClient.STOCK_OUT);
        BigDecimal adjustment = sumMovementsByType(movements, MovementTypeClient.ADJUSTMENT);
        BigDecimal transferIn = sumMovementsByType(movements, MovementTypeClient.TRANSFER_IN);
        BigDecimal transferOut = sumMovementsByType(movements, MovementTypeClient.TRANSFER_OUT);

        return StockMovementSummaryResponse.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .stockIn(stockIn)
                .stockOut(stockOut)
                .adjustment(adjustment)
                .transferIn(transferIn)
                .transferOut(transferOut)
                .fromDate(dateRange.fromDate())
                .toDate(dateRange.toDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request) {
        DateRange dateRange = resolveDateRange(request);
        List<ProductMovementAggregate> aggregates = buildProductMovementAggregates(request, dateRange).stream()
                .sorted(Comparator.comparing(ProductMovementAggregate::totalMovementQuantity).reversed()
                        .thenComparing(ProductMovementAggregate::productId))
                .toList();

        List<TopMovingProductResponse> responses = new ArrayList<>();
        for (int index = 0; index < aggregates.size(); index++) {
            ProductMovementAggregate aggregate = aggregates.get(index);
            responses.add(TopMovingProductResponse.builder()
                    .productId(aggregate.productId())
                    .productName(aggregate.productName())
                    .totalMovementQuantity(aggregate.totalMovementQuantity())
                    .rank(index + 1)
                    .build());
        }

        responses = responses.stream()
                .sorted(buildTopMovingComparator(request))
                .toList();

        return buildPage(responses, request.getPage(), request.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SlowMovingProductResponse> getSlowMovingProducts(ReportFilterRequest request) {
        DateRange dateRange = resolveDateRange(request);
        List<SlowMovingProductResponse> responses = buildProductMovementAggregates(request, dateRange).stream()
                .sorted(Comparator.comparing(ProductMovementAggregate::totalMovementQuantity)
                        .thenComparing(ProductMovementAggregate::productId))
                .map(aggregate -> SlowMovingProductResponse.builder()
                        .productId(aggregate.productId())
                        .productName(aggregate.productName())
                        .totalMovementQuantity(aggregate.totalMovementQuantity())
                        .lastMovementDate(aggregate.lastMovementDate())
                        .build())
                .sorted(buildSlowMovingComparator(request))
                .toList();

        return buildPage(responses, request.getPage(), request.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeadStockResponse> getDeadStock(ReportFilterRequest request) {
        DateRange dateRange = resolveDateRange(request);
        LocalDate referenceDate = dateRange.toDate();
        int thresholdDays = reportProperties.getDeadStock().getThresholdDays();
        List<InventorySnapshot> latestSnapshots = reportRepository.findLatestSnapshots(request.getWarehouseId(), request.getProductId());

        Map<Long, ProductClientResponse> productMap = fetchProducts(latestSnapshots.stream()
                .map(InventorySnapshot::getProductId)
                .collect(Collectors.toSet()));

        List<DeadStockResponse> responses = latestSnapshots.stream()
                .filter(snapshot -> snapshot.getQuantity() != null && snapshot.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                .map(snapshot -> buildDeadStockResponse(snapshot, productMap.get(snapshot.getProductId()), referenceDate, thresholdDays))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(buildDeadStockComparator(request))
                .toList();

        return buildPage(responses, request.getPage(), request.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderSummaryResponse getPOSummary(ReportFilterRequest request) {
        DateRange dateRange = resolveDateRange(request);
        List<PurchaseOrderClientResponse> purchaseOrders = loadPurchaseOrders(dateRange).stream()
                .filter(purchaseOrder -> request.getSupplierId() == null
                        || Objects.equals(request.getSupplierId(), purchaseOrder.getSupplierId()))
                .filter(purchaseOrder -> request.getWarehouseId() == null
                        || Objects.equals(request.getWarehouseId(), purchaseOrder.getWarehouseId()))
                .toList();

        BigDecimal totalSpend = purchaseOrders.stream()
                .map(PurchaseOrderClientResponse::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PurchaseOrderSummaryResponse.builder()
                .supplierId(request.getSupplierId())
                .warehouseId(request.getWarehouseId())
                .totalPOs((long) purchaseOrders.size())
                .totalSpend(defaultScale(totalSpend))
                .fromDate(dateRange.fromDate())
                .toDate(dateRange.toDate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public GeneratedReportResponse generateInventoryReport(GenerateReportRequest request) {
        if (request.getReportType() == null) {
            throw new InvalidReportRequestException("reportType is required.");
        }
        if (request.getFormat() == null) {
            throw new InvalidReportRequestException("format is required.");
        }

        String requestedBy = resolveRequestedBy(request.getRequestedBy());
        LOGGER.info("Generating report reportType={} format={} requestedBy={}",
                request.getReportType(),
                request.getFormat(),
                requestedBy);

        reportEventPublisher.publishReportGenerationRequested(ReportGenerationRequestedEvent.builder()
                .reportType(request.getReportType())
                .format(request.getFormat())
                .requestedBy(requestedBy)
                .requestedAt(LocalDateTime.now())
                .build());

        try {
            ReportData reportData = buildReportData(request);
            ReportExporter reportExporter = resolveReportExporter(request.getFormat());
            Path exportPath = reportExporter.export(
                    request.getReportType(),
                    request.getFormat(),
                    reportData.headers(),
                    reportData.rows(),
                    Path.of(reportProperties.getExport().getDirectory()));

            reportEventPublisher.publishReportGenerationCompleted(ReportGenerationCompletedEvent.builder()
                    .reportType(request.getReportType())
                    .format(request.getFormat())
                    .fileName(exportPath.getFileName().toString())
                    .requestedBy(requestedBy)
                    .completedAt(LocalDateTime.now())
                    .build());

            return GeneratedReportResponse.builder()
                    .reportType(request.getReportType())
                    .format(request.getFormat())
                    .fileName(exportPath.getFileName().toString())
                    .fileUrl(exportPath.toUri().toString())
                    .generatedAt(LocalDateTime.now())
                    .build();
        } catch (Exception exception) {
            throw new ReportGenerationException("Failed to generate report.", exception);
        }
    }

    private ReportData buildReportData(GenerateReportRequest request) {
        ReportFilterRequest filterRequest = ReportFilterRequest.builder()
                .warehouseId(request.getWarehouseId())
                .productId(request.getProductId())
                .supplierId(request.getSupplierId())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .page(0)
                .size(EXPORT_PAGE_SIZE)
                .sortBy("productId")
                .sortDir("asc")
                .build();

        return switch (request.getReportType()) {
            case INVENTORY_VALUATION -> buildInventoryValuationReport(request.getToDate(), request.getWarehouseId(), request.getProductId());
            case WAREHOUSE_VALUATION -> buildWarehouseValuationReport(request.getToDate(), request.getWarehouseId());
            case TURNOVER -> buildTurnoverReport(filterRequest);
            case LOW_STOCK -> buildLowStockExport(filterRequest);
            case MOVEMENT_SUMMARY -> buildMovementSummaryReport(filterRequest);
            case TOP_MOVING -> buildTopMovingExport(filterRequest);
            case SLOW_MOVING -> buildSlowMovingExport(filterRequest);
            case DEAD_STOCK -> buildDeadStockExport(filterRequest);
            case PO_SUMMARY -> buildPoSummaryReport(filterRequest);
        };
    }

    private ReportData buildInventoryValuationReport(LocalDate snapshotDate, Long warehouseId, Long productId) {
        LocalDate resolvedSnapshotDate = resolveSnapshotDate(snapshotDate);
        List<InventorySnapshot> snapshots = reportRepository.findBySnapshotDateAndFilters(resolvedSnapshotDate, warehouseId, productId);
        List<List<String>> rows = snapshots.stream()
                .map(snapshot -> List.of(
                        stringify(snapshot.getSnapshotDate()),
                        stringify(snapshot.getWarehouseId()),
                        safeText(snapshot.getWarehouseName()),
                        stringify(snapshot.getProductId()),
                        safeText(snapshot.getProductSku()),
                        safeText(snapshot.getProductName()),
                        stringify(snapshot.getQuantity()),
                        stringify(snapshot.getCostPrice()),
                        stringify(snapshot.getStockValue())))
                .toList();

        return new ReportData(
                List.of("snapshotDate", "warehouseId", "warehouseName", "productId", "productSku", "productName",
                        "quantity", "costPrice", "stockValue"),
                rows);
    }

    private ReportData buildWarehouseValuationReport(LocalDate snapshotDate, Long warehouseId) {
        List<WarehouseStockValueResponse> responses = getStockValueByWarehouse(snapshotDate).stream()
                .filter(response -> warehouseId == null || Objects.equals(response.getWarehouseId(), warehouseId))
                .toList();
        List<List<String>> rows = responses.stream()
                .map(response -> List.of(
                        stringify(response.getWarehouseId()),
                        safeText(response.getWarehouseName()),
                        stringify(response.getTotalStockValue())))
                .toList();

        return new ReportData(List.of("warehouseId", "warehouseName", "totalStockValue"), rows);
    }

    private ReportData buildTurnoverReport(ReportFilterRequest request) {
        List<List<String>> rows = getInventoryTurnover(request).stream()
                .map(response -> List.of(
                        stringify(response.getProductId()),
                        safeText(response.getProductName()),
                        stringify(response.getTurnoverRate()),
                        stringify(response.getFromDate()),
                        stringify(response.getToDate())))
                .toList();
        return new ReportData(List.of("productId", "productName", "turnoverRate", "fromDate", "toDate"), rows);
    }

    private ReportData buildLowStockExport(ReportFilterRequest request) {
        List<List<String>> rows = getLowStockReport(request).getContent().stream()
                .map(response -> List.of(
                        stringify(response.getProductId()),
                        safeText(response.getProductName()),
                        stringify(response.getWarehouseId()),
                        stringify(response.getAvailableQuantity()),
                        stringify(response.getReorderLevel())))
                .toList();
        return new ReportData(List.of("productId", "productName", "warehouseId", "availableQuantity", "reorderLevel"), rows);
    }

    private ReportData buildMovementSummaryReport(ReportFilterRequest request) {
        StockMovementSummaryResponse response = getStockMovementSummary(request);
        return new ReportData(
                List.of("productId", "warehouseId", "stockIn", "stockOut", "adjustment", "transferIn",
                        "transferOut", "fromDate", "toDate"),
                List.of(List.of(
                        stringify(response.getProductId()),
                        stringify(response.getWarehouseId()),
                        stringify(response.getStockIn()),
                        stringify(response.getStockOut()),
                        stringify(response.getAdjustment()),
                        stringify(response.getTransferIn()),
                        stringify(response.getTransferOut()),
                        stringify(response.getFromDate()),
                        stringify(response.getToDate()))));
    }

    private ReportData buildTopMovingExport(ReportFilterRequest request) {
        List<List<String>> rows = getTopMovingProducts(request).getContent().stream()
                .map(response -> List.of(
                        stringify(response.getProductId()),
                        safeText(response.getProductName()),
                        stringify(response.getTotalMovementQuantity()),
                        stringify(response.getRank())))
                .toList();
        return new ReportData(List.of("productId", "productName", "totalMovementQuantity", "rank"), rows);
    }

    private ReportData buildSlowMovingExport(ReportFilterRequest request) {
        List<List<String>> rows = getSlowMovingProducts(request).getContent().stream()
                .map(response -> List.of(
                        stringify(response.getProductId()),
                        safeText(response.getProductName()),
                        stringify(response.getTotalMovementQuantity()),
                        stringify(response.getLastMovementDate())))
                .toList();
        return new ReportData(List.of("productId", "productName", "totalMovementQuantity", "lastMovementDate"), rows);
    }

    private ReportData buildDeadStockExport(ReportFilterRequest request) {
        List<List<String>> rows = getDeadStock(request).getContent().stream()
                .map(response -> List.of(
                        stringify(response.getProductId()),
                        safeText(response.getProductName()),
                        stringify(response.getWarehouseId()),
                        stringify(response.getLastMovementDate()),
                        stringify(response.getDaysWithoutMovement())))
                .toList();
        return new ReportData(List.of("productId", "productName", "warehouseId", "lastMovementDate", "daysWithoutMovement"), rows);
    }

    private ReportData buildPoSummaryReport(ReportFilterRequest request) {
        PurchaseOrderSummaryResponse response = getPOSummary(request);
        return new ReportData(
                List.of("supplierId", "warehouseId", "totalPOs", "totalSpend", "fromDate", "toDate"),
                List.of(List.of(
                        stringify(response.getSupplierId()),
                        stringify(response.getWarehouseId()),
                        stringify(response.getTotalPOs()),
                        stringify(response.getTotalSpend()),
                        stringify(response.getFromDate()),
                        stringify(response.getToDate()))));
    }

    private ReportExporter resolveReportExporter(ReportFormat format) {
        return reportExporters.stream()
                .filter(exporter -> exporter.supports(format))
                .findFirst()
                .orElseThrow(() -> new ReportGenerationException("Export format " + format + " is not supported."));
    }

    private List<InventorySnapshot> createSnapshots(LocalDate snapshotDate, Long warehouseId, String source) {
        List<WarehouseStockLevelClientResponse> stockLevels = loadAllStockLevels(warehouseId);
        if (stockLevels.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductClientResponse> productCache = new HashMap<>();
        Map<Long, WarehouseClientResponse> warehouseCache = new HashMap<>();
        String createdBy = SOURCE_SCHEDULED.equals(source) ? SYSTEM_USER : resolveRequestedBy(null);

        List<InventorySnapshot> createdSnapshots = new ArrayList<>();
        for (WarehouseStockLevelClientResponse stockLevel : stockLevels) {
            if (stockLevel.getWarehouseId() == null || stockLevel.getProductId() == null) {
                continue;
            }

            boolean exists = reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(
                    stockLevel.getWarehouseId(),
                    stockLevel.getProductId(),
                    snapshotDate);
            if (exists) {
                continue;
            }

            ProductClientResponse product = productCache.computeIfAbsent(
                    stockLevel.getProductId(),
                    this::fetchProductById);
            WarehouseClientResponse warehouse = warehouseCache.computeIfAbsent(
                    stockLevel.getWarehouseId(),
                    this::fetchWarehouseById);

            BigDecimal quantity = defaultScale(stockLevel.getQuantity());
            BigDecimal costPrice = defaultScale(product != null ? product.getCostPrice() : BigDecimal.ZERO);
            BigDecimal stockValue = quantity.multiply(costPrice).setScale(4, RoundingMode.HALF_UP);

            InventorySnapshot snapshot = InventorySnapshot.builder()
                    .warehouseId(stockLevel.getWarehouseId())
                    .productId(stockLevel.getProductId())
                    .quantity(quantity)
                    .costPrice(costPrice)
                    .stockValue(stockValue)
                    .snapshotDate(snapshotDate)
                    .productSku(product != null ? product.getSku() : null)
                    .productName(product != null ? product.getName() : null)
                    .warehouseName(warehouse != null ? warehouse.getName() : null)
                    .source(source)
                    .createdBy(createdBy)
                    .build();

            createdSnapshots.add(reportRepository.save(snapshot));
        }

        if (!createdSnapshots.isEmpty()) {
            reportEventPublisher.publishSnapshotCompleted(InventorySnapshotCompletedEvent.builder()
                    .snapshotDate(snapshotDate)
                    .warehouseId(warehouseId)
                    .snapshotCount(createdSnapshots.size())
                    .completedAt(LocalDateTime.now())
                    .source(source)
                    .build());
        }

        return createdSnapshots;
    }

    private ProductClientResponse fetchProductById(Long productId) {
        try {
            return productServiceClient.getProductById(productId);
        } catch (Exception exception) {
            throw new ExternalServiceException("Unable to fetch product details for productId=" + productId, exception);
        }
    }

    private WarehouseClientResponse fetchWarehouseById(Long warehouseId) {
        try {
            return warehouseServiceClient.getWarehouseById(warehouseId);
        } catch (Exception exception) {
            throw new ExternalServiceException("Unable to fetch warehouse details for warehouseId=" + warehouseId, exception);
        }
    }

    private List<WarehouseStockLevelClientResponse> loadAllStockLevels(Long warehouseId) {
        List<WarehouseStockLevelClientResponse> stockLevels = new ArrayList<>();
        int page = 0;
        int size = reportProperties.getSnapshot().getPageSize();

        while (true) {
            PageResponse<WarehouseStockLevelClientResponse> response;
            try {
                response = warehouseServiceClient.searchStock(StockSearchClientRequest.builder()
                        .warehouseId(warehouseId)
                        .page(page)
                        .size(size)
                        .sortBy("lastUpdated")
                        .sortDir("desc")
                        .build());
            } catch (Exception exception) {
                throw new ExternalServiceException("Unable to load stock levels from warehouse-service.", exception);
            }

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                break;
            }

            stockLevels.addAll(response.getContent());
            if (response.isLast() || response.getNumber() + 1 >= response.getTotalPages()) {
                break;
            }
            page++;
        }

        return stockLevels;
    }

    private List<LowStockItemClientResponse> loadAllLowStockItems() {
        List<LowStockItemClientResponse> lowStockItems = new ArrayList<>();
        int page = 0;
        int size = reportProperties.getSnapshot().getPageSize();

        while (true) {
            PageResponse<LowStockItemClientResponse> response;
            try {
                response = warehouseServiceClient.getLowStockItems(page, size);
            } catch (Exception exception) {
                throw new ExternalServiceException("Unable to load low stock items from warehouse-service.", exception);
            }

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                break;
            }

            lowStockItems.addAll(response.getContent());
            if (response.isLast() || response.getNumber() + 1 >= response.getTotalPages()) {
                break;
            }
            page++;
        }

        return lowStockItems;
    }

    private List<MovementClientResponse> loadMovements(DateRange dateRange) {
        try {
            return movementServiceClient.getMovementsByDateRange(
                    dateRange.fromDate().atStartOfDay(),
                    dateRange.toDate().atTime(LocalTime.MAX));
        } catch (Exception exception) {
            throw new ExternalServiceException("Unable to load stock movements from movement-service.", exception);
        }
    }

    private List<MovementClientResponse> filterMovements(List<MovementClientResponse> movements, ReportFilterRequest request) {
        return movements.stream()
                .filter(movement -> request.getWarehouseId() == null || Objects.equals(movement.getWarehouseId(), request.getWarehouseId()))
                .filter(movement -> request.getProductId() == null || Objects.equals(movement.getProductId(), request.getProductId()))
                .toList();
    }

    private List<PurchaseOrderClientResponse> loadPurchaseOrders(DateRange dateRange) {
        try {
            return purchaseServiceClient.getPOsByDateRange(dateRange.fromDate(), dateRange.toDate());
        } catch (Exception exception) {
            throw new ExternalServiceException("Unable to load purchase orders from purchase-service.", exception);
        }
    }

    private LocalDate resolveSnapshotDate(LocalDate requestedSnapshotDate) {
        LocalDate resolvedSnapshotDate = reportRepository.findLatestSnapshotDateOnOrBefore(requestedSnapshotDate);
        if (resolvedSnapshotDate == null) {
            throw new ReportNotFoundException("No inventory snapshots are available for the requested date.");
        }
        return resolvedSnapshotDate;
    }

    private DateRange resolveDateRange(ReportFilterRequest request) {
        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();

        if (fromDate == null && toDate == null) {
            toDate = LocalDate.now();
            fromDate = toDate.minusDays(DEFAULT_LOOKBACK_DAYS);
        } else if (fromDate == null) {
            fromDate = toDate;
        } else if (toDate == null) {
            toDate = fromDate;
        }

        if (toDate.isBefore(fromDate)) {
            throw new InvalidReportRequestException("toDate cannot be before fromDate.");
        }

        return new DateRange(fromDate, toDate);
    }

    private Map<Long, ProductClientResponse> fetchProducts(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<ProductClientResponse> products = productServiceClient.getAllProducts();
            Map<Long, ProductClientResponse> productMap = products == null
                    ? new HashMap<>()
                    : products.stream()
                            .filter(product -> product.getProductId() != null)
                            .collect(Collectors.toMap(ProductClientResponse::getProductId, Function.identity(), (left, right) -> left));

            for (Long productId : productIds) {
                if (!productMap.containsKey(productId)) {
                    productMap.put(productId, productServiceClient.getProductById(productId));
                }
            }

            return productMap;
        } catch (Exception exception) {
            throw new ExternalServiceException("Unable to load product details from product-service.", exception);
        }
    }

    private InventoryTurnoverResponse buildInventoryTurnoverResponse(Long productId,
            ProductClientResponse product,
            BigDecimal outgoingQuantity,
            BigDecimal averageInventoryValue,
            DateRange dateRange) {
        BigDecimal costPrice = defaultScale(product != null ? product.getCostPrice() : BigDecimal.ZERO);
        BigDecimal outgoingValue = magnitude(outgoingQuantity).multiply(costPrice).setScale(4, RoundingMode.HALF_UP);
        BigDecimal turnoverRate = averageInventoryValue.compareTo(BigDecimal.ZERO) > 0
                ? outgoingValue.divide(averageInventoryValue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        return InventoryTurnoverResponse.builder()
                .productId(productId)
                .productName(product != null ? product.getName() : null)
                .turnoverRate(turnoverRate)
                .fromDate(dateRange.fromDate())
                .toDate(dateRange.toDate())
                .build();
    }

    private List<ProductMovementAggregate> buildProductMovementAggregates(ReportFilterRequest request, DateRange dateRange) {
        List<MovementClientResponse> movements = filterMovements(loadMovements(dateRange), request);
        Map<Long, ProductClientResponse> productMap = fetchProducts(movements.stream()
                .map(MovementClientResponse::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<Long, List<MovementClientResponse>> movementsByProduct = movements.stream()
                .filter(movement -> movement.getProductId() != null)
                .collect(Collectors.groupingBy(MovementClientResponse::getProductId));

        return movementsByProduct.entrySet().stream()
                .map(entry -> {
                    Long productId = entry.getKey();
                    List<MovementClientResponse> productMovements = entry.getValue();
                    BigDecimal totalMovement = productMovements.stream()
                            .map(MovementClientResponse::getQuantity)
                            .filter(Objects::nonNull)
                            .map(this::magnitude)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(4, RoundingMode.HALF_UP);
                    if (totalMovement.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                    }
                    LocalDateTime lastMovementDate = productMovements.stream()
                            .map(MovementClientResponse::getMovementDate)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    ProductClientResponse product = productMap.get(productId);
                    return new ProductMovementAggregate(
                            productId,
                            product != null ? product.getName() : null,
                            totalMovement,
                            lastMovementDate);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<DeadStockResponse> buildDeadStockResponse(InventorySnapshot snapshot,
            ProductClientResponse product,
            LocalDate referenceDate,
            int thresholdDays) {
        List<MovementClientResponse> movementHistory;
        try {
            movementHistory = movementServiceClient.getMovementHistory(snapshot.getProductId(), snapshot.getWarehouseId());
        } catch (Exception exception) {
            throw new ExternalServiceException(
                    "Unable to load movement history for productId=" + snapshot.getProductId()
                            + " warehouseId=" + snapshot.getWarehouseId(),
                    exception);
        }

        LocalDateTime lastMovementDate = movementHistory.stream()
                .map(MovementClientResponse::getMovementDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        long daysWithoutMovement = lastMovementDate == null
                ? ChronoUnit.DAYS.between(snapshot.getSnapshotDate(), referenceDate)
                : ChronoUnit.DAYS.between(lastMovementDate.toLocalDate(), referenceDate);

        if (daysWithoutMovement < thresholdDays) {
            return Optional.empty();
        }

        return Optional.of(DeadStockResponse.builder()
                .productId(snapshot.getProductId())
                .productName(product != null ? product.getName() : snapshot.getProductName())
                .warehouseId(snapshot.getWarehouseId())
                .lastMovementDate(lastMovementDate)
                .daysWithoutMovement(daysWithoutMovement)
                .build());
    }

    private WarehouseStockValueResponse toWarehouseStockValueResponse(WarehouseStockValueProjection projection) {
        return WarehouseStockValueResponse.builder()
                .warehouseId(projection.getWarehouseId())
                .warehouseName(projection.getWarehouseName())
                .totalStockValue(defaultScale(projection.getTotalStockValue()))
                .build();
    }

    private BigDecimal sumMovementsByType(List<MovementClientResponse> movements, MovementTypeClient movementType) {
        return movements.stream()
                .filter(movement -> movementType == movement.getMovementType())
                .map(MovementClientResponse::getQuantity)
                .filter(Objects::nonNull)
                .map(this::magnitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private String resolveRequestedBy(String requestedBy) {
        if (requestedBy != null && !requestedBy.isBlank()) {
            return requestedBy.trim();
        }

        AuthenticatedUser authenticatedUser = securityUtils.getCurrentUserOrNull();
        if (authenticatedUser != null && authenticatedUser.email() != null && !authenticatedUser.email().isBlank()) {
            return authenticatedUser.email();
        }

        return SYSTEM_USER;
    }

    private void validateSnapshotRequest(TakeSnapshotRequest request) {
        if (request == null || request.getSnapshotDate() == null) {
            throw new InvalidReportRequestException("snapshotDate is required.");
        }
        if (request.getWarehouseId() != null && request.getWarehouseId() <= 0) {
            throw new InvalidReportRequestException("warehouseId must be greater than zero.");
        }
    }

    private BigDecimal defaultScale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal magnitude(BigDecimal value) {
        return defaultScale(value).abs();
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private <T> Page<T> buildPage(List<T> items, Integer pageNumber, Integer pageSize) {
        int safePage = pageNumber == null ? 0 : Math.max(pageNumber, 0);
        int safeSize = pageSize == null ? 20 : Math.max(pageSize, 1);
        int start = Math.min(safePage * safeSize, items.size());
        int end = Math.min(start + safeSize, items.size());
        return new PageImpl<>(items.subList(start, end), PageRequest.of(safePage, safeSize), items.size());
    }

    private Set<Long> unionKeys(Set<Long> left, Set<Long> right) {
        Set<Long> values = new java.util.LinkedHashSet<>(left);
        values.addAll(right);
        return values;
    }

    private Comparator<InventoryTurnoverResponse> buildInventoryTurnoverComparator(ReportFilterRequest request) {
        Comparator<InventoryTurnoverResponse> comparator = switch (safeSortBy(request, "turnoverRate")) {
            case "productId" -> Comparator.comparing(InventoryTurnoverResponse::getProductId, Comparator.nullsLast(Long::compareTo));
            case "productName" -> Comparator.comparing(InventoryTurnoverResponse::getProductName, Comparator.nullsLast(String::compareToIgnoreCase));
            default -> Comparator.comparing(InventoryTurnoverResponse::getTurnoverRate, Comparator.nullsLast(BigDecimal::compareTo));
        };
        return isDescending(request) ? comparator.reversed() : comparator;
    }

    private Comparator<LowStockReportResponse> buildLowStockComparator(ReportFilterRequest request) {
        Comparator<LowStockReportResponse> comparator = switch (safeSortBy(request, "availableQuantity")) {
            case "productId" -> Comparator.comparing(LowStockReportResponse::getProductId, Comparator.nullsLast(Long::compareTo));
            case "productName" -> Comparator.comparing(LowStockReportResponse::getProductName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "warehouseId" -> Comparator.comparing(LowStockReportResponse::getWarehouseId, Comparator.nullsLast(Long::compareTo));
            case "reorderLevel" -> Comparator.comparing(LowStockReportResponse::getReorderLevel, Comparator.nullsLast(BigDecimal::compareTo));
            default -> Comparator.comparing(LowStockReportResponse::getAvailableQuantity, Comparator.nullsLast(BigDecimal::compareTo));
        };
        return isDescending(request) ? comparator.reversed() : comparator;
    }

    private Comparator<TopMovingProductResponse> buildTopMovingComparator(ReportFilterRequest request) {
        Comparator<TopMovingProductResponse> comparator = switch (safeSortBy(request, "rank")) {
            case "productId" -> Comparator.comparing(TopMovingProductResponse::getProductId, Comparator.nullsLast(Long::compareTo));
            case "productName" -> Comparator.comparing(TopMovingProductResponse::getProductName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "totalMovementQuantity" -> Comparator.comparing(TopMovingProductResponse::getTotalMovementQuantity,
                    Comparator.nullsLast(BigDecimal::compareTo));
            default -> Comparator.comparing(TopMovingProductResponse::getRank, Comparator.nullsLast(Integer::compareTo));
        };
        return isDescending(request) ? comparator.reversed() : comparator;
    }

    private Comparator<SlowMovingProductResponse> buildSlowMovingComparator(ReportFilterRequest request) {
        Comparator<SlowMovingProductResponse> comparator = switch (safeSortBy(request, "totalMovementQuantity")) {
            case "productId" -> Comparator.comparing(SlowMovingProductResponse::getProductId, Comparator.nullsLast(Long::compareTo));
            case "productName" -> Comparator.comparing(SlowMovingProductResponse::getProductName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "lastMovementDate" -> Comparator.comparing(SlowMovingProductResponse::getLastMovementDate,
                    Comparator.nullsLast(LocalDateTime::compareTo));
            default -> Comparator.comparing(SlowMovingProductResponse::getTotalMovementQuantity,
                    Comparator.nullsLast(BigDecimal::compareTo));
        };
        return isDescending(request) ? comparator.reversed() : comparator;
    }

    private Comparator<DeadStockResponse> buildDeadStockComparator(ReportFilterRequest request) {
        Comparator<DeadStockResponse> comparator = switch (safeSortBy(request, "daysWithoutMovement")) {
            case "productId" -> Comparator.comparing(DeadStockResponse::getProductId, Comparator.nullsLast(Long::compareTo));
            case "productName" -> Comparator.comparing(DeadStockResponse::getProductName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "warehouseId" -> Comparator.comparing(DeadStockResponse::getWarehouseId, Comparator.nullsLast(Long::compareTo));
            case "lastMovementDate" -> Comparator.comparing(DeadStockResponse::getLastMovementDate,
                    Comparator.nullsLast(LocalDateTime::compareTo));
            default -> Comparator.comparing(DeadStockResponse::getDaysWithoutMovement, Comparator.nullsLast(Long::compareTo));
        };
        return isDescending(request) ? comparator.reversed() : comparator;
    }

    private boolean isDescending(ReportFilterRequest request) {
        return request != null && request.getSortDir() != null && "desc".equalsIgnoreCase(request.getSortDir());
    }

    private String safeSortBy(ReportFilterRequest request, String defaultSort) {
        return request == null || request.getSortBy() == null || request.getSortBy().isBlank()
                ? defaultSort
                : request.getSortBy();
    }

    private record DateRange(LocalDate fromDate, LocalDate toDate) {
    }

    private record ProductMovementAggregate(Long productId,
            String productName,
            BigDecimal totalMovementQuantity,
            LocalDateTime lastMovementDate) {
    }

    private record ReportData(List<String> headers, List<List<String>> rows) {
    }
}
