package com.stockpro.reportservice.service.impl;

import com.stockpro.reportservice.client.ReportingDataClient;
import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.dto.response.AlertSummaryReportResponse;
import com.stockpro.reportservice.dto.response.DashboardAlertItem;
import com.stockpro.reportservice.dto.response.DeadStockResponse;
import com.stockpro.reportservice.dto.response.ExecutiveDashboardResponse;
import com.stockpro.reportservice.dto.response.InventorySnapshotResponse;
import com.stockpro.reportservice.dto.response.InventoryTurnoverResponse;
import com.stockpro.reportservice.dto.response.InventoryValuationResponse;
import com.stockpro.reportservice.dto.response.LowStockReportItem;
import com.stockpro.reportservice.dto.response.OverstockReportItem;
import com.stockpro.reportservice.dto.response.PaymentSummaryReportResponse;
import com.stockpro.reportservice.dto.response.ProductValuationItem;
import com.stockpro.reportservice.dto.response.PurchaseSummaryResponse;
import com.stockpro.reportservice.dto.response.SlowMovingProductResponse;
import com.stockpro.reportservice.dto.response.StockMovementReportItem;
import com.stockpro.reportservice.dto.response.StockSummaryResponse;
import com.stockpro.reportservice.dto.response.SupplierPaymentItem;
import com.stockpro.reportservice.dto.response.SupplierPerformanceReportResponse;
import com.stockpro.reportservice.dto.response.TopMovingProductResponse;
import com.stockpro.reportservice.dto.response.TrendPointResponse;
import com.stockpro.reportservice.dto.response.WarehouseValuationItem;
import com.stockpro.reportservice.entity.InventorySnapshot;
import com.stockpro.reportservice.enums.ExportFormat;
import com.stockpro.reportservice.enums.ReportPeriod;
import com.stockpro.reportservice.enums.ReportType;
import com.stockpro.reportservice.enums.TrendDirection;
import com.stockpro.reportservice.events.ReportEvent;
import com.stockpro.reportservice.events.ReportEventType;
import com.stockpro.reportservice.exception.DataNotFoundException;
import com.stockpro.reportservice.export.ReportExportService;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import com.stockpro.reportservice.service.ReportEventPublisher;
import com.stockpro.reportservice.service.ReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ReportingDataClient reportingDataClient;
    private final ReportExportService reportExportService;
    private final ReportEventPublisher reportEventPublisher;

    @Value("${report.dead-stock-days:90}")
    private int deadStockDays;

    @Value("${report.slow-moving-days:30}")
    private int slowMovingDays;

    @Override
    public InventoryValuationResponse getInventoryValuation(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<ProductValuationItem> items = buildValuationItems(normalized);
        BigDecimal totalValue = items.stream().map(ProductValuationItem::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQuantity = items.stream().map(ProductValuationItem::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<WarehouseValuationItem> warehouseItems = items.stream()
                .collect(Collectors.groupingBy(ProductValuationItem::warehouseId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(group -> new WarehouseValuationItem(
                        group.get(0).warehouseId(),
                        group.get(0).warehouseName(),
                        group.stream().map(ProductValuationItem::quantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                        group.stream().map(ProductValuationItem::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
        Map<String, BigDecimal> categoryMap = items.stream()
                .collect(Collectors.groupingBy(item -> Optional.ofNullable(item.category()).orElse("Uncategorized"),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, ProductValuationItem::totalValue, BigDecimal::add)));
        return new InventoryValuationResponse(
                totalValue,
                totalQuantity,
                items.stream().map(ProductValuationItem::productId).distinct().count(),
                items.stream().map(ProductValuationItem::warehouseId).distinct().count(),
                warehouseItems,
                categoryMap,
                items);
    }

    @Override
    public StockSummaryResponse getStockSummary(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<ReportingDataClient.StockRecord> stocks = reportingDataClient.getStocks(normalized);
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        BigDecimal totalQuantity = stocks.stream().map(stock -> decimal(stock.quantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReserved = stocks.stream().map(stock -> decimal(stock.reservedQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAvailable = stocks.stream().map(stock -> decimal(stock.availableQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStockCount = stocks.stream().filter(stock -> isLowStock(stock, products.get(stock.productId()))).count();
        long overstockCount = stocks.stream().filter(stock -> isOverstock(stock, products.get(stock.productId()))).count();
        long outOfStockCount = stocks.stream().filter(stock -> decimal(stock.availableQuantity()).compareTo(BigDecimal.ZERO) <= 0).count();
        return new StockSummaryResponse(
                stocks.stream().map(ReportingDataClient.StockRecord::productId).distinct().count(),
                stocks.stream().map(ReportingDataClient.StockRecord::warehouseId).distinct().count(),
                totalQuantity,
                totalReserved,
                totalAvailable,
                lowStockCount,
                overstockCount,
                outOfStockCount);
    }

    @Override
    public Page<ProductValuationItem> getProductStockReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<ProductValuationItem> items = buildValuationItems(normalized);
        return page(items, normalized);
    }

    @Override
    public Page<WarehouseValuationItem> getWarehouseStockReport(ReportFilterRequest request) {
        List<WarehouseValuationItem> items = getInventoryValuation(request).valuationByWarehouse();
        return page(items, normalizeRequest(request));
    }

    @Override
    public Page<LowStockReportItem> getLowStockReport(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        ReportFilterRequest normalized = normalizeRequest(request);
        List<LowStockReportItem> items = reportingDataClient.getStocks(normalized).stream()
                .filter(stock -> isLowStock(stock, products.get(stock.productId())))
                .map(stock -> {
                    ReportingDataClient.ProductRecord product = products.get(stock.productId());
                    BigDecimal reorderLevel = decimal(product != null ? product.reorderLevel() : 0);
                    BigDecimal available = decimal(stock.availableQuantity());
                    BigDecimal shortage = reorderLevel.subtract(available).max(BigDecimal.ZERO);
                    return new LowStockReportItem(
                            stock.productId(),
                            coalesce(stock.sku(), product != null ? product.sku() : null),
                            coalesce(stock.productName(), product != null ? product.name() : null),
                            stock.warehouseId(),
                            stock.warehouseName(),
                            available,
                            reorderLevel,
                            shortage,
                            severity(shortage, reorderLevel));
                })
                .toList();
        return page(items, normalized);
    }

    @Override
    public Page<OverstockReportItem> getOverstockReport(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        ReportFilterRequest normalized = normalizeRequest(request);
        List<OverstockReportItem> items = reportingDataClient.getStocks(normalized).stream()
                .filter(stock -> isOverstock(stock, products.get(stock.productId())))
                .map(stock -> {
                    ReportingDataClient.ProductRecord product = products.get(stock.productId());
                    BigDecimal maxLevel = decimal(product != null ? product.maxStockLevel() : 0);
                    BigDecimal quantity = decimal(stock.quantity());
                    return new OverstockReportItem(
                            stock.productId(),
                            coalesce(stock.sku(), product != null ? product.sku() : null),
                            coalesce(stock.productName(), product != null ? product.name() : null),
                            stock.warehouseId(),
                            stock.warehouseName(),
                            quantity,
                            maxLevel,
                            quantity.subtract(maxLevel).max(BigDecimal.ZERO));
                })
                .toList();
        return page(items, normalized);
    }

    @Override
    public Page<StockMovementReportItem> getStockMovementReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<StockMovementReportItem> items = reportingDataClient.searchMovements(normalized).content().stream()
                .map(movement -> new StockMovementReportItem(
                        movement.movementId(),
                        movement.movementNumber(),
                        movement.productId(),
                        movement.productSku(),
                        movement.productName(),
                        movement.warehouseId(),
                        movement.warehouseName(),
                        enumName(movement.movementType()),
                        enumName(movement.direction()),
                        safe(movement.quantity()),
                        safe(movement.unitCost()),
                        safe(movement.totalValue()),
                        enumName(movement.referenceType()),
                        movement.referenceNumber(),
                        movement.performedBy(),
                        movement.movementDate()))
                .toList();
        return page(items, normalized);
    }

    @Override
    public List<InventoryTurnoverResponse> getInventoryTurnoverReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        LocalDate from = requiredFromDate(normalized);
        LocalDate to = requiredToDate(normalized);
        Map<Long, InventorySnapshot> opening = aggregateSnapshotByProduct(snapshotRangeForDate(from));
        Map<Long, InventorySnapshot> closing = aggregateSnapshotByProduct(snapshotRangeForDate(to));
        Map<Long, BigDecimal> stockOutByProduct = reportingDataClient.searchMovements(normalized).content().stream()
                .filter(movement -> safe(movement.quantity()).compareTo(BigDecimal.ZERO) > 0)
                .filter(movement -> "OUT".equalsIgnoreCase(enumName(movement.direction())))
                .collect(Collectors.groupingBy(ReportingDataClient.MovementRecord::productId,
                        Collectors.reducing(BigDecimal.ZERO, ReportingDataClient.MovementRecord::quantity, BigDecimal::add)));
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        return stockOutByProduct.keySet().stream()
                .map(productId -> {
                    BigDecimal openingQty = opening.containsKey(productId) ? safe(opening.get(productId).getQuantity()) : BigDecimal.ZERO;
                    BigDecimal closingQty = closing.containsKey(productId) ? safe(closing.get(productId).getQuantity()) : BigDecimal.ZERO;
                    BigDecimal average = openingQty.add(closingQty).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
                    BigDecimal stockOut = stockOutByProduct.getOrDefault(productId, BigDecimal.ZERO);
                    BigDecimal turnover = average.compareTo(BigDecimal.ZERO) > 0
                            ? stockOut.divide(average, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    ReportingDataClient.ProductRecord product = products.get(productId);
                    return new InventoryTurnoverResponse(productId,
                            product != null ? product.sku() : null,
                            product != null ? product.name() : null,
                            openingQty,
                            closingQty,
                            average,
                            stockOut,
                            turnover);
                })
                .sorted(Comparator.comparing(InventoryTurnoverResponse::turnoverRatio).reversed())
                .toList();
    }

    @Override
    public List<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request) {
        return movementSummary(normalizeRequest(request)).stream()
                .sorted(Comparator.comparing(TopMovingProductResponse::totalMovementQuantity).reversed())
                .limit(10)
                .toList();
    }

    @Override
    public List<SlowMovingProductResponse> getSlowMovingProducts(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        long threshold = normalized.getPeriod() == ReportPeriod.LAST_30_DAYS ? 30 : slowMovingDays;
        return movementStaleness(normalized).stream()
                .filter(item -> item.daysSinceLastMovement() >= threshold)
                .sorted(Comparator.comparing(SlowMovingProductResponse::daysSinceLastMovement).reversed())
                .toList();
    }

    @Override
    public List<DeadStockResponse> getDeadStockReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        Map<Long, ReportingDataClient.WarehouseRecord> warehouses = warehouseMap();
        Map<Long, SlowMovingProductResponse> staleProducts = movementStaleness(normalized).stream()
                .filter(item -> item.daysSinceLastMovement() >= deadStockDays)
                .collect(Collectors.toMap(SlowMovingProductResponse::productId, Function.identity(), (left, right) -> left));
        return reportingDataClient.getStocks(normalized).stream()
                .filter(stock -> staleProducts.containsKey(stock.productId()))
                .map(stock -> {
                    SlowMovingProductResponse stale = staleProducts.get(stock.productId());
                    ReportingDataClient.ProductRecord product = products.get(stock.productId());
                    ReportingDataClient.WarehouseRecord warehouse = warehouses.get(stock.warehouseId());
                    return new DeadStockResponse(
                            stock.productId(),
                            coalesce(stock.sku(), product != null ? product.sku() : null),
                            coalesce(stock.productName(), product != null ? product.name() : null),
                            stock.warehouseId(),
                            warehouse != null ? warehouse.name() : stock.warehouseName(),
                            decimal(stock.quantity()),
                            safe(stale.stockValue()),
                            stale.lastMovementDate(),
                            stale.daysSinceLastMovement());
                })
                .toList();
    }

    @Override
    public PurchaseSummaryResponse getPurchaseSummary(ReportFilterRequest request) {
        List<ReportingDataClient.PurchaseOrderRecord> orders = reportingDataClient.searchPurchaseOrders(normalizeRequest(request));
        return buildPurchaseSummary(orders);
    }

    @Override
    public SupplierPerformanceReportResponse getSupplierPerformance(Long supplierId, ReportFilterRequest request) {
        return getSupplierPerformanceReport(request).getContent().stream()
                .filter(item -> Objects.equals(item.supplierId(), supplierId))
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Supplier performance not found for supplierId=" + supplierId));
    }

    @Override
    public Page<SupplierPerformanceReportResponse> getSupplierPerformanceReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        Map<Long, ReportingDataClient.SupplierRecord> suppliers = supplierMap();
        List<SupplierPerformanceReportResponse> items = reportingDataClient.searchPurchaseOrders(normalized).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.PurchaseOrderRecord::supplierId))
                .entrySet().stream()
                .map(entry -> buildSupplierPerformance(entry.getKey(), suppliers.get(entry.getKey()), entry.getValue()))
                .filter(item -> normalized.getSupplierId() == null || Objects.equals(item.supplierId(), normalized.getSupplierId()))
                .sorted(Comparator.comparing(SupplierPerformanceReportResponse::totalSpend).reversed())
                .toList();
        return page(items, normalized);
    }

    @Override
    public PaymentSummaryReportResponse getPaymentSummary(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        ReportingDataClient.PaymentSummaryRecord summary = reportingDataClient.getPaymentSummary();
        List<SupplierPaymentItem> supplierPayments = reportingDataClient.searchPayments(normalized).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.PaymentRecord::supplierId))
                .entrySet().stream()
                .map(entry -> new SupplierPaymentItem(
                        entry.getKey(),
                        entry.getValue().stream().map(ReportingDataClient.PaymentRecord::supplierName).filter(Objects::nonNull).findFirst().orElse("Unknown supplier"),
                        entry.getValue().stream()
                                .filter(payment -> "PAID".equalsIgnoreCase(enumName(payment.status())) || "PARTIALLY_PAID".equalsIgnoreCase(enumName(payment.status())))
                                .map(payment -> safe(payment.paymentAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().stream().map(payment -> safe(payment.remainingAmount())).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
        long pending = safeLong(summary.pendingApprovalCount()) + safeLong(summary.approvedCount()) + safeLong(summary.partiallyPaidCount());
        return new PaymentSummaryReportResponse(
                safeLong(summary.totalPayments()),
                safeLong(summary.paidCount()),
                pending,
                safeLong(summary.cancelledCount()),
                safe(summary.totalPaidAmount()),
                safe(summary.pendingPaymentAmount()),
                supplierPayments);
    }

    @Override
    public AlertSummaryReportResponse getAlertSummary(ReportFilterRequest request) {
        ReportingDataClient.AlertSummaryRecord summary = reportingDataClient.getMyAlertSummary();
        Map<String, Long> alertsByType = new LinkedHashMap<>();
        alertsByType.put("LOW_STOCK", safeLong(summary.lowStockCount()));
        alertsByType.put("OVERSTOCK", safeLong(summary.overstockCount()));
        alertsByType.put("PO_APPROVAL_PENDING", safeLong(summary.pendingPoApprovalCount()));
        alertsByType.put("PO_OVERDUE_RECEIPT", safeLong(summary.overduePoCount()));
        return new AlertSummaryReportResponse(
                safeLong(summary.totalAlerts()),
                safeLong(summary.unreadCount()),
                safeLong(summary.criticalCount()),
                safeLong(summary.warningCount()),
                alertsByType);
    }

    @Override
    public ExecutiveDashboardResponse getExecutiveDashboard() {
        List<String> unavailable = new ArrayList<>();
        InventoryValuationResponse valuation = getInventoryValuation(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build());
        StockSummaryResponse stockSummary = getStockSummary(ReportFilterRequest.builder().size(500).build());
        PurchaseSummaryResponse purchaseSummary = getPurchaseSummary(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build());
        PaymentSummaryReportResponse paymentSummary = safeCall(() -> getPaymentSummary(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build()), unavailable, "payments");
        ReportingDataClient.AlertSummaryRecord alertSummary = safeCall(reportingDataClient::getSystemAlertSummary, unavailable, "alerts");
        List<ReportingDataClient.AlertRecord> alerts = safeCall(() -> reportingDataClient.getRecentAlerts(true), unavailable, "alerts");
        return new ExecutiveDashboardResponse(
                valuation.totalProducts(),
                valuation.totalWarehouses(),
                valuation.totalInventoryValue(),
                stockSummary.lowStockCount(),
                stockSummary.overstockCount(),
                purchaseSummary.pendingApprovalCount(),
                purchaseSummary.overdueCount(),
                purchaseSummary.totalPurchaseValue(),
                paymentSummary != null ? paymentSummary.totalPaidAmount() : BigDecimal.ZERO,
                alertSummary != null ? safeLong(alertSummary.criticalCount()) : 0L,
                reportingDataClient.searchMovements(ReportFilterRequest.builder().period(ReportPeriod.TODAY).size(500).build()).content().size(),
                getTopMovingProducts(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build()),
                alerts != null ? alerts.stream().map(alert -> new DashboardAlertItem(alert.alertId(), alert.title(), enumName(alert.severity()), enumName(alert.type()), String.valueOf(alert.createdAt()))).toList() : List.of(),
                buildValuationTrend(),
                buildPurchaseTrend(),
                unavailable);
    }

    @Override
    public ExecutiveDashboardResponse getRoleDashboard(String role, Long userId) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return getExecutiveDashboard();
        }
        List<String> unavailable = new ArrayList<>();
        InventoryValuationResponse valuation = safeCall(() -> getInventoryValuation(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build()), unavailable, "inventory");
        StockSummaryResponse stockSummary = safeCall(() -> getStockSummary(ReportFilterRequest.builder().size(500).build()), unavailable, "inventory");
        PurchaseSummaryResponse purchaseSummary = safeCall(() -> getPurchaseSummary(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build()), unavailable, "purchase");
        PaymentSummaryReportResponse paymentSummary = safeCall(() -> getPaymentSummary(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(500).build()), unavailable, "payments");
        ReportingDataClient.AlertSummaryRecord myAlerts = safeCall(reportingDataClient::getMyAlertSummary, unavailable, "alerts");
        return new ExecutiveDashboardResponse(
                valuation != null ? valuation.totalProducts() : 0L,
                valuation != null ? valuation.totalWarehouses() : 0L,
                valuation != null ? valuation.totalInventoryValue() : BigDecimal.ZERO,
                stockSummary != null ? stockSummary.lowStockCount() : 0L,
                stockSummary != null ? stockSummary.overstockCount() : 0L,
                purchaseSummary != null ? purchaseSummary.pendingApprovalCount() : 0L,
                purchaseSummary != null ? purchaseSummary.overdueCount() : 0L,
                purchaseSummary != null ? purchaseSummary.totalPurchaseValue() : BigDecimal.ZERO,
                paymentSummary != null ? paymentSummary.totalPaidAmount() : BigDecimal.ZERO,
                myAlerts != null ? safeLong(myAlerts.criticalCount()) : 0L,
                reportingDataClient.searchMovements(ReportFilterRequest.builder().period(ReportPeriod.TODAY).size(100).build()).content().size(),
                getTopMovingProducts(ReportFilterRequest.builder().period(ReportPeriod.LAST_30_DAYS).size(10).build()),
                List.of(),
                buildValuationTrend(),
                buildPurchaseTrend(),
                unavailable);
    }

    @Override
    @Transactional
    public void createDailyInventorySnapshot() {
        createInventorySnapshotForDate(LocalDate.now());
    }

    @Override
    @Transactional
    public void createInventorySnapshotForDate(LocalDate date) {
        log.info("Creating inventory snapshot for {}", date);
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        Map<Long, ReportingDataClient.WarehouseRecord> warehouses = warehouseMap();
        List<ReportingDataClient.StockRecord> stocks = reportingDataClient.getStocks(ReportFilterRequest.builder().size(500).build());
        int created = 0;
        for (ReportingDataClient.StockRecord stock : stocks) {
            if (inventorySnapshotRepository.findBySnapshotDateAndProductIdAndWarehouseId(date, stock.productId(), stock.warehouseId()).isPresent()) {
                continue;
            }
            ReportingDataClient.ProductRecord product = products.get(stock.productId());
            ReportingDataClient.WarehouseRecord warehouse = warehouses.get(stock.warehouseId());
            BigDecimal quantity = decimal(stock.quantity());
            BigDecimal reserved = decimal(stock.reservedQuantity());
            BigDecimal available = decimal(stock.availableQuantity());
            BigDecimal unitCost = safe(product != null ? product.costPrice() : null);
            InventorySnapshot snapshot = InventorySnapshot.builder()
                    .snapshotDate(date)
                    .productId(stock.productId())
                    .productSku(product != null ? product.sku() : stock.sku())
                    .productName(product != null ? product.name() : stock.productName())
                    .warehouseId(stock.warehouseId())
                    .warehouseCode(warehouse != null ? warehouse.code() : null)
                    .warehouseName(warehouse != null ? warehouse.name() : stock.warehouseName())
                    .quantity(quantity)
                    .reservedQuantity(reserved)
                    .availableQuantity(available)
                    .unitCost(unitCost)
                    .totalValue(quantity.multiply(unitCost))
                    .build();
            inventorySnapshotRepository.save(snapshot);
            created++;
        }
        log.info("Inventory snapshot finished for {} with {} new records", date, created);
        publishEvent("snapshot-routing-key", ReportEventType.SNAPSHOT_CREATED, ReportType.STOCK_SUMMARY, "SUCCESS", Map.of("date", date, "count", created));
    }

    @Override
    public Page<InventorySnapshotResponse> getInventorySnapshots(LocalDate date, int page, int size) {
        Page<InventorySnapshot> snapshots = inventorySnapshotRepository.findBySnapshotDate(date, PageRequest.of(page, size));
        return snapshots.map(this::toSnapshotResponse);
    }

    @Override
    public List<InventorySnapshotResponse> getSnapshotTrend(Long productId, Long warehouseId, LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        List<InventorySnapshot> snapshots = inventorySnapshotRepository.findBySnapshotDateBetween(fromDate, toDate).stream()
                .filter(snapshot -> productId == null || Objects.equals(snapshot.getProductId(), productId))
                .filter(snapshot -> warehouseId == null || Objects.equals(snapshot.getWarehouseId(), warehouseId))
                .sorted(Comparator.comparing(InventorySnapshot::getSnapshotDate))
                .toList();
        return snapshots.stream().map(this::toSnapshotResponse).toList();
    }

    @Override
    public byte[] exportInventoryValuation(ReportFilterRequest request, ExportFormat format) {
        InventoryValuationResponse response = getInventoryValuation(request);
        List<String> headers = List.of("Warehouse", "Product", "Category", "Quantity", "Unit Cost", "Total Value");
        List<List<Object>> rows = response.valuationByProduct().stream()
                .map(item -> row(item.warehouseName(), item.productName(), item.category(), item.quantity(), item.unitCost(), item.totalValue()))
                .toList();
        return export(format, "Inventory Valuation", headers, rows);
    }

    @Override
    public byte[] exportStockMovementReport(ReportFilterRequest request, ExportFormat format) {
        List<StockMovementReportItem> items = getStockMovementReport(request).getContent();
        List<String> headers = List.of("Movement No", "Product", "Warehouse", "Type", "Direction", "Quantity", "Total Value", "Date");
        List<List<Object>> rows = items.stream()
                .map(item -> row(item.movementNumber(), item.productName(), item.warehouseName(), item.movementType(), item.direction(), item.quantity(), item.totalValue(), item.movementDate()))
                .toList();
        return export(format, "Stock Movement Report", headers, rows);
    }

    @Override
    public byte[] exportPurchaseSummary(ReportFilterRequest request, ExportFormat format) {
        PurchaseSummaryResponse response = getPurchaseSummary(request);
        List<String> headers = List.of("Metric", "Value");
        List<List<Object>> rows = List.of(
                row("Total Purchase Orders", response.totalPurchaseOrders()),
                row("Pending Approval", response.pendingApprovalCount()),
                row("Approved", response.approvedCount()),
                row("Received", response.receivedCount()),
                row("Cancelled", response.cancelledCount()),
                row("Overdue", response.overdueCount()),
                row("Total Purchase Value", response.totalPurchaseValue()),
                row("Received Purchase Value", response.receivedPurchaseValue()),
                row("Pending Purchase Value", response.pendingPurchaseValue()));
        return export(format, "Purchase Summary", headers, rows);
    }

    @Override
    public byte[] exportSupplierPerformance(ReportFilterRequest request, ExportFormat format) {
        List<SupplierPerformanceReportResponse> items = getSupplierPerformanceReport(request).getContent();
        List<String> headers = List.of("Supplier", "Orders", "Received", "Delayed", "Total Spend", "Lead Time", "Rating");
        List<List<Object>> rows = items.stream()
                .map(item -> row(item.supplierName(), item.totalOrders(), item.receivedOrders(), item.delayedOrders(), item.totalSpend(), item.averageLeadTimeDays(), item.rating()))
                .toList();
        return export(format, "Supplier Performance", headers, rows);
    }

    @Override
    public byte[] exportExecutiveDashboard(ExportFormat format) {
        ExecutiveDashboardResponse response = getExecutiveDashboard();
        List<String> headers = List.of("Metric", "Value");
        List<List<Object>> rows = List.of(
                row("Total Products", response.totalProducts()),
                row("Total Warehouses", response.totalWarehouses()),
                row("Total Inventory Value", response.totalInventoryValue()),
                row("Low Stock Count", response.lowStockCount()),
                row("Overstock Count", response.overstockCount()),
                row("Pending Purchase Approvals", response.pendingPurchaseApprovals()),
                row("Overdue Purchase Orders", response.overduePurchaseOrders()),
                row("Total Purchase Value", response.totalPurchaseValue()),
                row("Total Paid Amount", response.totalPaidAmount()),
                row("Critical Alerts", response.criticalAlerts()));
        return export(format, "Executive Dashboard", headers, rows);
    }

    private List<ProductValuationItem> buildValuationItems(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        Map<Long, ReportingDataClient.WarehouseRecord> warehouses = warehouseMap();
        return reportingDataClient.getStocks(request).stream()
                .filter(stock -> includeCategoryBrand(stock.productId(), products, request))
                .map(stock -> {
                    ReportingDataClient.ProductRecord product = products.get(stock.productId());
                    ReportingDataClient.WarehouseRecord warehouse = warehouses.get(stock.warehouseId());
                    BigDecimal quantity = decimal(stock.quantity());
                    BigDecimal unitCost = safe(product != null ? product.costPrice() : null);
                    return new ProductValuationItem(
                            stock.productId(),
                            coalesce(stock.sku(), product != null ? product.sku() : null),
                            coalesce(stock.productName(), product != null ? product.name() : null),
                            product != null ? product.category() : null,
                            stock.warehouseId(),
                            warehouse != null ? warehouse.name() : stock.warehouseName(),
                            quantity,
                            unitCost,
                            quantity.multiply(unitCost));
                })
                .toList();
    }

    private boolean includeCategoryBrand(Long productId, Map<Long, ReportingDataClient.ProductRecord> products, ReportFilterRequest request) {
        ReportingDataClient.ProductRecord product = products.get(productId);
        if (product == null) {
            return true;
        }
        boolean categoryMatches = request.getCategory() == null || request.getCategory().equalsIgnoreCase(product.category());
        boolean brandMatches = request.getBrand() == null || request.getBrand().equalsIgnoreCase(product.brand());
        return categoryMatches && brandMatches;
    }

    private PurchaseSummaryResponse buildPurchaseSummary(List<ReportingDataClient.PurchaseOrderRecord> orders) {
        BigDecimal totalValue = orders.stream().map(order -> safe(order.totalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedValue = orders.stream()
                .filter(order -> "RECEIVED".equalsIgnoreCase(order.status()) || "PARTIALLY_RECEIVED".equalsIgnoreCase(order.status()))
                .map(order -> safe(order.totalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingValue = orders.stream()
                .filter(order -> "PENDING".equalsIgnoreCase(order.status()) || "PENDING_APPROVAL".equalsIgnoreCase(order.status()))
                .map(order -> safe(order.totalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseSummaryResponse(
                orders.size(),
                countStatus(orders, "PENDING", "PENDING_APPROVAL"),
                countStatus(orders, "APPROVED"),
                countStatus(orders, "RECEIVED", "PARTIALLY_RECEIVED"),
                countStatus(orders, "CANCELLED"),
                orders.stream().filter(order -> Boolean.TRUE.equals(order.isOverdue())).count(),
                totalValue,
                receivedValue,
                pendingValue);
    }

    private SupplierPerformanceReportResponse buildSupplierPerformance(Long supplierId, ReportingDataClient.SupplierRecord supplier,
                                                                      List<ReportingDataClient.PurchaseOrderRecord> orders) {
        BigDecimal totalSpend = orders.stream().map(order -> safe(order.totalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long receivedOrders = orders.stream().filter(order -> "RECEIVED".equalsIgnoreCase(order.status()) || "PARTIALLY_RECEIVED".equalsIgnoreCase(order.status())).count();
        long delayedOrders = orders.stream().filter(order -> Boolean.TRUE.equals(order.isOverdue())).count();
        BigDecimal avgLeadTime = average(orders.stream()
                .filter(order -> order.actualDeliveryDate() != null && order.createdAt() != null)
                .map(order -> BigDecimal.valueOf(ChronoUnit.DAYS.between(order.createdAt().toLocalDate(), order.actualDeliveryDate())))
                .toList());
        return new SupplierPerformanceReportResponse(
                supplierId,
                supplier != null ? supplier.name() : "Unknown supplier",
                orders.size(),
                receivedOrders,
                delayedOrders,
                totalSpend,
                avgLeadTime,
                safe(supplier != null ? supplier.rating() : null));
    }

    private List<TopMovingProductResponse> movementSummary(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        return reportingDataClient.searchMovements(request).content().stream()
                .collect(Collectors.groupingBy(ReportingDataClient.MovementRecord::productId))
                .entrySet().stream()
                .map(entry -> {
                    ReportingDataClient.ProductRecord product = products.get(entry.getKey());
                    BigDecimal quantity = entry.getValue().stream().map(movement -> safe(movement.quantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalValue = entry.getValue().stream().map(movement -> safe(movement.totalValue())).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TopMovingProductResponse(entry.getKey(),
                            product != null ? product.sku() : null,
                            product != null ? product.name() : null,
                            quantity,
                            entry.getValue().size(),
                            totalValue);
                })
                .toList();
    }

    private List<SlowMovingProductResponse> movementStaleness(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> products = productMap();
        Map<Long, BigDecimal> currentStock = buildValuationItems(request).stream()
                .collect(Collectors.groupingBy(ProductValuationItem::productId,
                        Collectors.reducing(BigDecimal.ZERO, ProductValuationItem::totalValue, BigDecimal::add)));
        Map<Long, BigDecimal> quantityByProduct = reportingDataClient.getStocks(request).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.StockRecord::productId,
                        Collectors.reducing(BigDecimal.ZERO, stock -> decimal(stock.quantity()), BigDecimal::add)));
        Map<Long, LocalDateTime> lastMovement = reportingDataClient.searchMovements(request).content().stream()
                .collect(Collectors.toMap(ReportingDataClient.MovementRecord::productId, ReportingDataClient.MovementRecord::movementDate,
                        (first, second) -> first.isAfter(second) ? first : second));
        return quantityByProduct.entrySet().stream()
                .map(entry -> {
                    ReportingDataClient.ProductRecord product = products.get(entry.getKey());
                    LocalDateTime movementDate = lastMovement.get(entry.getKey());
                    long days = movementDate == null ? deadStockDays + 1L : ChronoUnit.DAYS.between(movementDate.toLocalDate(), LocalDate.now());
                    return new SlowMovingProductResponse(
                            entry.getKey(),
                            product != null ? product.sku() : null,
                            product != null ? product.name() : null,
                            movementDate,
                            days,
                            entry.getValue(),
                            currentStock.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                })
                .toList();
    }

    private List<TrendPointResponse> buildValuationTrend() {
        List<InventorySnapshot> snapshots = inventorySnapshotRepository.findBySnapshotDateBetween(LocalDate.now().minusDays(6), LocalDate.now());
        Map<LocalDate, BigDecimal> byDate = snapshots.stream().collect(Collectors.groupingBy(InventorySnapshot::getSnapshotDate,
                TreeMap::new, Collectors.reducing(BigDecimal.ZERO, InventorySnapshot::getTotalValue, BigDecimal::add)));
        return toTrend(byDate);
    }

    private List<TrendPointResponse> buildPurchaseTrend() {
        Map<LocalDate, BigDecimal> byDate = reportingDataClient.searchPurchaseOrders(ReportFilterRequest.builder()
                        .fromDate(LocalDate.now().minusDays(6))
                        .toDate(LocalDate.now())
                        .size(500)
                        .build()).stream()
                .filter(order -> order.createdAt() != null)
                .collect(Collectors.groupingBy(order -> order.createdAt().toLocalDate(),
                        TreeMap::new, Collectors.reducing(BigDecimal.ZERO, order -> safe(order.totalAmount()), BigDecimal::add)));
        return toTrend(byDate);
    }

    private List<TrendPointResponse> toTrend(Map<LocalDate, BigDecimal> byDate) {
        List<TrendPointResponse> trend = new ArrayList<>();
        BigDecimal previous = null;
        for (Map.Entry<LocalDate, BigDecimal> entry : byDate.entrySet()) {
            TrendDirection direction = TrendDirection.FLAT;
            if (previous != null) {
                int comparison = entry.getValue().compareTo(previous);
                direction = comparison > 0 ? TrendDirection.UP : comparison < 0 ? TrendDirection.DOWN : TrendDirection.FLAT;
            }
            trend.add(new TrendPointResponse(entry.getKey(), entry.getValue(), direction));
            previous = entry.getValue();
        }
        return trend;
    }

    private ReportFilterRequest normalizeRequest(ReportFilterRequest request) {
        ReportFilterRequest normalized = request != null ? request : ReportFilterRequest.builder().build();
        if (normalized.getPeriod() != null && (normalized.getFromDate() == null || normalized.getToDate() == null)) {
            LocalDate end = LocalDate.now();
            LocalDate start;
            switch (normalized.getPeriod()) {
                case TODAY -> start = end;
                case LAST_7_DAYS -> start = end.minusDays(6);
                case LAST_30_DAYS -> start = end.minusDays(29);
                case THIS_MONTH -> start = end.withDayOfMonth(1);
                case LAST_MONTH -> {
                    LocalDate previousMonth = end.minusMonths(1);
                    start = previousMonth.withDayOfMonth(1);
                    end = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth());
                }
                case CUSTOM -> start = normalized.getFromDate();
                default -> start = end.minusDays(29);
            }
            normalized.setFromDate(start);
            normalized.setToDate(end);
        }
        if (normalized.getFromDate() != null || normalized.getToDate() != null) {
            validateDateRange(normalized.getFromDate(), normalized.getToDate());
        }
        if (normalized.getSize() <= 0) {
            normalized.setSize(10);
        }
        if (normalized.getPage() < 0) {
            normalized.setPage(0);
        }
        return normalized;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required for custom ranges");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
    }

    private LocalDate requiredFromDate(ReportFilterRequest request) {
        if (request.getFromDate() == null) {
            request.setFromDate(LocalDate.now().minusDays(29));
        }
        return request.getFromDate();
    }

    private LocalDate requiredToDate(ReportFilterRequest request) {
        if (request.getToDate() == null) {
            request.setToDate(LocalDate.now());
        }
        return request.getToDate();
    }

    private InventorySnapshotResponse toSnapshotResponse(InventorySnapshot snapshot) {
        return new InventorySnapshotResponse(
                snapshot.getSnapshotId(),
                snapshot.getSnapshotDate(),
                snapshot.getProductId(),
                snapshot.getProductSku(),
                snapshot.getProductName(),
                snapshot.getWarehouseId(),
                snapshot.getWarehouseCode(),
                snapshot.getWarehouseName(),
                snapshot.getQuantity(),
                snapshot.getReservedQuantity(),
                snapshot.getAvailableQuantity(),
                snapshot.getUnitCost(),
                snapshot.getTotalValue(),
                snapshot.getCreatedAt());
    }

    private <T> Page<T> page(List<T> items, ReportFilterRequest request) {
        int start = Math.min(request.getPage() * request.getSize(), items.size());
        int end = Math.min(start + request.getSize(), items.size());
        return new PageImpl<>(items.subList(start, end), PageRequest.of(request.getPage(), request.getSize()), items.size());
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private byte[] export(ExportFormat format, String title, List<String> headers, List<? extends List<?>> rows) {
        publishEvent("export-requested-routing-key", ReportEventType.REPORT_EXPORT_REQUESTED, ReportType.EXECUTIVE_DASHBOARD, "STARTED", Map.of("title", title, "format", format.name()));
        byte[] content = switch (format) {
            case CSV -> reportExportService.exportCsv(headers, rows);
            case EXCEL -> reportExportService.exportExcel(title.replace(" ", "_"), headers, rows);
            case PDF -> reportExportService.exportPdf(title, headers, rows);
        };
        publishEvent("export-completed-routing-key", ReportEventType.REPORT_EXPORT_COMPLETED, ReportType.EXECUTIVE_DASHBOARD, "SUCCESS", Map.of("title", title, "format", format.name()));
        return content;
    }

    private String enumName(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return String.valueOf(value);
    }

    private String coalesce(String first, String second) {
        return first != null ? first : second;
    }

    private BigDecimal decimal(Number value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value.doubleValue());
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private String severity(BigDecimal shortage, BigDecimal reorderLevel) {
        if (reorderLevel.compareTo(BigDecimal.ZERO) == 0) {
            return "LOW";
        }
        BigDecimal ratio = shortage.divide(reorderLevel, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return "CRITICAL";
        }
        if (ratio.compareTo(BigDecimal.valueOf(0.2)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean isLowStock(ReportingDataClient.StockRecord stock, ReportingDataClient.ProductRecord product) {
        if (product == null || product.reorderLevel() == null) {
            return false;
        }
        return decimal(stock.availableQuantity()).compareTo(decimal(product.reorderLevel())) < 0;
    }

    private boolean isOverstock(ReportingDataClient.StockRecord stock, ReportingDataClient.ProductRecord product) {
        if (product == null || product.maxStockLevel() == null) {
            return false;
        }
        return decimal(stock.quantity()).compareTo(decimal(product.maxStockLevel())) > 0;
    }

    private long countStatus(List<ReportingDataClient.PurchaseOrderRecord> orders, String... statuses) {
        List<String> expected = List.of(statuses);
        return orders.stream().filter(order -> expected.contains(order.status().toUpperCase())).count();
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<Long, ReportingDataClient.ProductRecord> productMap() {
        return reportingDataClient.getProducts().stream()
                .collect(Collectors.toMap(ReportingDataClient.ProductRecord::productId, Function.identity(), (first, second) -> first));
    }

    private Map<Long, ReportingDataClient.WarehouseRecord> warehouseMap() {
        return reportingDataClient.getWarehouses().stream()
                .collect(Collectors.toMap(ReportingDataClient.WarehouseRecord::warehouseId, Function.identity(), (first, second) -> first));
    }

    private Map<Long, ReportingDataClient.SupplierRecord> supplierMap() {
        return reportingDataClient.getSuppliers().stream()
                .collect(Collectors.toMap(ReportingDataClient.SupplierRecord::supplierId, Function.identity(), (first, second) -> first));
    }

    private Map<Long, InventorySnapshot> aggregateSnapshotByProduct(List<InventorySnapshot> snapshots) {
        Map<Long, InventorySnapshot> result = new HashMap<>();
        for (InventorySnapshot snapshot : snapshots) {
            result.merge(snapshot.getProductId(), snapshot, (left, right) -> InventorySnapshot.builder()
                    .productId(left.getProductId())
                    .quantity(safe(left.getQuantity()).add(safe(right.getQuantity())))
                    .build());
        }
        return result;
    }

    private List<InventorySnapshot> snapshotRangeForDate(LocalDate date) {
        return inventorySnapshotRepository.findBySnapshotDate(date);
    }

    private <T> T safeCall(ThrowingSupplier<T> supplier, List<String> unavailableSections, String section) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            log.warn("Unable to fetch {} data for dashboard: {}", section, ex.getMessage());
            unavailableSections.add(section);
            return null;
        }
    }

    private void publishEvent(String routingKeyPropertyName, ReportEventType type, ReportType reportType, String status, Map<String, Object> metadata) {
        String routingKey = switch (routingKeyPropertyName) {
            case "snapshot-routing-key" -> "report.snapshot.created";
            case "export-requested-routing-key" -> "report.export.requested";
            case "export-completed-routing-key" -> "report.export.completed";
            default -> "report.generated";
        };
        reportEventPublisher.publish(routingKey, new ReportEvent(
                UUID.randomUUID().toString(),
                type,
                reportType,
                LocalDateTime.now(),
                status,
                metadata));
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
