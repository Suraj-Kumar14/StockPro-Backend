package com.stockpro.reportservice.service.impl;

import com.stockpro.reportservice.client.ReportingDataClient;
import com.stockpro.reportservice.dto.request.ReportFilterRequest;
import com.stockpro.reportservice.dto.response.AlertSummaryReportResponse;
import com.stockpro.reportservice.dto.response.CancelledPurchaseOrderReportResponse;
import com.stockpro.reportservice.dto.response.CancelledPurchaseOrderRowResponse;
import com.stockpro.reportservice.dto.response.DashboardAlertItem;
import com.stockpro.reportservice.dto.response.DeadStockResponse;
import com.stockpro.reportservice.dto.response.ExecutiveDashboardResponse;
import com.stockpro.reportservice.dto.response.GeneratedInventoryReportResponse;
import com.stockpro.reportservice.dto.response.InventorySnapshotResponse;
import com.stockpro.reportservice.dto.response.InventoryTurnoverProductItem;
import com.stockpro.reportservice.dto.response.InventoryTurnoverReportResponse;
import com.stockpro.reportservice.dto.response.InventoryTurnoverResponse;
import com.stockpro.reportservice.dto.response.InventoryValuationResponse;
import com.stockpro.reportservice.dto.response.LowStockReportItem;
import com.stockpro.reportservice.dto.response.OverstockReportItem;
import com.stockpro.reportservice.dto.response.PaymentBillingRowResponse;
import com.stockpro.reportservice.dto.response.PaymentLineItemResponse;
import com.stockpro.reportservice.dto.response.PaymentSummaryReportResponse;
import com.stockpro.reportservice.dto.response.PaymentSupplierBreakdownItem;
import com.stockpro.reportservice.dto.response.ProductMovementSummaryResponse;
import com.stockpro.reportservice.dto.response.ProductValuationItem;
import com.stockpro.reportservice.dto.response.PurchaseOrderDetailItemResponse;
import com.stockpro.reportservice.dto.response.PurchaseOrderDetailReportResponse;
import com.stockpro.reportservice.dto.response.PurchaseOrderPaymentSummaryResponse;
import com.stockpro.reportservice.dto.response.PurchaseOrderTimelineItemResponse;
import com.stockpro.reportservice.dto.response.PurchaseSpendBreakdownItem;
import com.stockpro.reportservice.dto.response.PurchaseSummaryResponse;
import com.stockpro.reportservice.dto.response.RejectedPurchaseOrderReportResponse;
import com.stockpro.reportservice.dto.response.SlowMovingProductResponse;
import com.stockpro.reportservice.dto.response.StockMovementReportItem;
import com.stockpro.reportservice.dto.response.StockSummaryResponse;
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
import com.stockpro.reportservice.exception.ReportGenerationException;
import com.stockpro.reportservice.export.ReportExportService;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import com.stockpro.reportservice.security.AuthenticatedUser;
import com.stockpro.reportservice.service.ReportEventPublisher;
import com.stockpro.reportservice.service.ReportService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final LocalDate EARLIEST_DATE = LocalDate.of(2000, 1, 1);
    private static final int DEFAULT_TOP_MOVING_LIMIT = 10;
    private static final String UNKNOWN_SUPPLIER = "Unknown supplier";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final String SECTION_INVENTORY = "inventory";
    private static final String SECTION_PURCHASE = "purchase";
    private static final String SECTION_ALERTS = "alerts";
    private static final String SECTION_MOVEMENTS = "movements";
    private static final String VALUE_HEADER = "Value";

    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ReportingDataClient reportingDataClient;
    private final ReportExportService reportExportService;
    private final ReportEventPublisher reportEventPublisher;
    private final ObjectProvider<ReportService> selfProvider;

    @Value("${report.dead-stock-days:90}")
    private int deadStockDays;

    @Value("${report.slow-moving-days:30}")
    private int slowMovingDays;

    @Override
    public void takeSnapshot(LocalDate date) {
        selfProvider.getObject().createInventorySnapshotForDate(date != null ? date : LocalDate.now());
    }

    @Override
    public InventoryValuationResponse getInventoryValuation(ReportFilterRequest request) {
        return buildInventoryValuationReport(request);
    }

    @Override
    public InventoryValuationResponse getTotalStockValue(Long warehouseId, LocalDate asOfDate) {
        ReportFilterRequest request = ReportFilterRequest.builder()
                .warehouseId(warehouseId)
                .fromDate(asOfDate)
                .toDate(asOfDate)
                .size(Integer.MAX_VALUE)
                .build();
        return buildInventoryValuationReport(request);
    }

    @Override
    public List<WarehouseValuationItem> getStockValueByWarehouse(LocalDate asOfDate) {
        return getTotalStockValue(null, asOfDate).warehouseBreakdown();
    }

    @Override
    public StockSummaryResponse getStockSummary(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        List<ReportingDataClient.StockRecord> stocks = reportingDataClient.getStocks(normalized);
        BigDecimal totalQuantity = sumStocks(stocks, ReportingDataClient.StockRecord::quantity);
        BigDecimal totalReserved = sumStocks(stocks, ReportingDataClient.StockRecord::reservedQuantity);
        BigDecimal totalAvailable = sumStocks(stocks, ReportingDataClient.StockRecord::availableQuantity);
        long lowStockCount = stocks.stream().filter(stock -> isLowStock(stock, productMap.get(stock.productId()))).count();
        long overstockCount = stocks.stream().filter(stock -> isOverstock(stock, productMap.get(stock.productId()))).count();
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
        return page(buildValuationItems(normalizeRequest(request), null), normalizeRequest(request));
    }

    @Override
    public Page<WarehouseValuationItem> getWarehouseStockReport(ReportFilterRequest request) {
        return page(groupWarehouseValuation(buildValuationItems(normalizeRequest(request), null)), normalizeRequest(request));
    }

    @Override
    public Page<LowStockReportItem> getLowStockReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        List<LowStockReportItem> items = reportingDataClient.getStocks(normalized).stream()
                .map(stock -> buildLowStockItem(stock, productMap.get(stock.productId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(LowStockReportItem::severity).reversed()
                        .thenComparing(LowStockReportItem::productName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        return page(items, normalized);
    }

    @Override
    public Page<OverstockReportItem> getOverstockReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        List<OverstockReportItem> items = reportingDataClient.getStocks(normalized).stream()
                .map(stock -> buildOverstockItem(stock, productMap.get(stock.productId())))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OverstockReportItem::availableQuantity).reversed())
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
    public Page<ProductMovementSummaryResponse> getStockMovementSummary(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<ProductMovementSummaryResponse> items = movementSummaryByProductWarehouse(normalized);
        return page(items, normalized);
    }

    @Override
    public List<InventoryTurnoverResponse> getInventoryTurnoverReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        InventoryTurnoverReportResponse report = getInventoryTurnover(
                requiredFromDate(normalized),
                requiredToDate(normalized),
                normalized.getWarehouseId());
        return report.productTurnover().stream()
                .map(item -> new InventoryTurnoverResponse(
                        item.productId(),
                        item.sku(),
                        item.productName(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        item.averageInventoryValue(),
                        item.cogs(),
                        item.turnoverRate()))
                .toList();
    }

    @Override
    public InventoryTurnoverReportResponse getInventoryTurnover(LocalDate from, LocalDate to, Long warehouseId) {
        validateDateRange(from, to);
        ReportFilterRequest movementFilter = ReportFilterRequest.builder()
                .fromDate(from)
                .toDate(to)
                .warehouseId(warehouseId)
                .size(Integer.MAX_VALUE)
                .build();
        List<ReportingDataClient.MovementRecord> movements = reportingDataClient.searchAllMovements(movementFilter);
        List<ReportingDataClient.MovementRecord> stockOutMovements = movements.stream()
                .filter(this::isStockOut)
                .toList();
        Map<Long, List<InventorySnapshot>> snapshotsByProduct = snapshotsBetween(from, to, warehouseId).stream()
                .collect(Collectors.groupingBy(InventorySnapshot::getProductId));
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        Set<Long> productIds = new LinkedHashSet<>();
        productIds.addAll(stockOutMovements.stream().map(ReportingDataClient.MovementRecord::productId).filter(Objects::nonNull).toList());
        productIds.addAll(snapshotsByProduct.keySet());

        List<InventoryTurnoverProductItem> productTurnover = productIds.stream()
                .map(productId -> {
                    BigDecimal cogs = stockOutMovements.stream()
                            .filter(movement -> Objects.equals(productId, movement.productId()))
                            .map(this::movementValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal averageInventoryValue = average(
                            snapshotsByProduct.getOrDefault(productId, List.of()).stream()
                                    .map(InventorySnapshot::getTotalValue)
                                    .map(this::safe)
                                    .toList());
                    BigDecimal turnoverRate = averageInventoryValue.compareTo(BigDecimal.ZERO) > 0
                            ? cogs.divide(averageInventoryValue, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    ReportingDataClient.ProductRecord product = productMap.get(productId);
                    return new InventoryTurnoverProductItem(
                            productId,
                            product != null ? product.name() : null,
                            product != null ? product.sku() : null,
                            cogs,
                            averageInventoryValue,
                            turnoverRate);
                })
                .sorted(Comparator.comparing(InventoryTurnoverProductItem::turnoverRate).reversed())
                .toList();

        BigDecimal totalCogs = productTurnover.stream().map(InventoryTurnoverProductItem::cogs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAverageInventory = average(
                snapshotsBetween(from, to, warehouseId).stream()
                        .collect(Collectors.groupingBy(InventorySnapshot::getSnapshotDate,
                                Collectors.reducing(BigDecimal.ZERO, InventorySnapshot::getTotalValue, BigDecimal::add)))
                        .values().stream().toList());
        BigDecimal turnoverRate = totalAverageInventory.compareTo(BigDecimal.ZERO) > 0
                ? totalCogs.divide(totalAverageInventory, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new InventoryTurnoverReportResponse(
                from,
                to,
                warehouseId,
                totalCogs,
                totalAverageInventory,
                turnoverRate,
                "COGS is estimated from STOCK_OUT movements.",
                productTurnover);
    }

    @Override
    public List<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        int limit = normalized.getSize() > 0 ? normalized.getSize() : DEFAULT_TOP_MOVING_LIMIT;
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        return reportingDataClient.searchAllMovements(normalized).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.MovementRecord::productId))
                .entrySet().stream()
                .map(entry -> {
                    ReportingDataClient.ProductRecord product = productMap.get(entry.getKey());
                    BigDecimal unitsIn = sumMovements(entry.getValue(), this::isStockIn);
                    BigDecimal unitsOut = sumMovements(entry.getValue(), this::isStockOut);
                    BigDecimal totalMoved = unitsIn.add(unitsOut);
                    BigDecimal movementValue = entry.getValue().stream().map(this::movementValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TopMovingProductResponse(
                            entry.getKey(),
                            product != null ? product.name() : entry.getValue().get(0).productName(),
                            product != null ? product.sku() : entry.getValue().get(0).productSku(),
                            unitsIn,
                            unitsOut,
                            totalMoved,
                            entry.getValue().size(),
                            movementValue);
                })
                .sorted(Comparator.comparing(TopMovingProductResponse::totalMoved).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<SlowMovingProductResponse> getSlowMovingProducts(ReportFilterRequest request) {
        return getSlowMovingProducts(request, null);
    }

    @Override
    public List<SlowMovingProductResponse> getSlowMovingProducts(ReportFilterRequest request, Integer threshold) {
        ReportFilterRequest normalized = normalizeRequest(request);
        BigDecimal thresholdValue = threshold != null ? BigDecimal.valueOf(threshold) : resolveSlowMovingThreshold(request);
        Map<Long, ProductActivity> activity = buildProductActivity(normalized);
        return activity.values().stream()
                .filter(item -> item.totalMoved().compareTo(thresholdValue) <= 0)
                .map(ProductActivity::toSlowMovingResponse)
                .sorted(Comparator.comparing(SlowMovingProductResponse::totalMoved)
                        .thenComparing(SlowMovingProductResponse::daysSinceLastMovement).reversed())
                .toList();
    }

    @Override
    public List<DeadStockResponse> getDeadStockReport(ReportFilterRequest request) {
        return getDeadStockReport(request, null);
    }

    @Override
    public List<DeadStockResponse> getDeadStockReport(ReportFilterRequest request, Long thresholdDays) {
        ReportFilterRequest normalized = normalizeRequest(request);
        long effectiveThresholdDays = thresholdDays != null && thresholdDays > 0
                ? thresholdDays
                : deadStockDays;
        Map<Long, ProductActivity> activity = buildProductActivity(normalized);
        Map<Long, ReportingDataClient.WarehouseRecord> warehouseMap = warehouseMap();
        return reportingDataClient.getStocks(normalized).stream()
                .map(stock -> {
                    ProductActivity productActivity = activity.get(stock.productId());
                    if (productActivity == null || productActivity.daysSinceLastMovement() <= effectiveThresholdDays) {
                        return null;
                    }
                    ReportingDataClient.WarehouseRecord warehouse = warehouseMap.get(stock.warehouseId());
                    return new DeadStockResponse(
                            stock.productId(),
                            coalesce(stock.productName(), productActivity.productName()),
                            coalesce(stock.sku(), productActivity.sku()),
                            stock.warehouseId(),
                            warehouse != null ? warehouse.name() : stock.warehouseName(),
                            decimal(stock.quantity()),
                            decimal(stock.quantity()).multiply(productActivity.costPrice()),
                            productActivity.lastMovementDate(),
                            productActivity.daysSinceLastMovement());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DeadStockResponse::daysWithoutMovement).reversed())
                .toList();
    }

    @Override
    public PurchaseSummaryResponse getPurchaseSummary(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        LocalDate fromDate = requiredFromDate(normalized);
        LocalDate toDate = requiredToDate(normalized);
        log.info(
                "Generating poSummary role={} from={} to={} warehouseId={} supplierId={}",
                currentUserRole(),
                fromDate,
                toDate,
                normalized.getWarehouseId(),
                normalized.getSupplierId());
        try {
            List<ReportingDataClient.PurchaseOrderRecord> orders = reportingDataClient.searchPurchaseOrders(normalized);
            log.info(
                    "Generated poSummary role={} from={} to={} warehouseId={} supplierId={} orderCount={}",
                    currentUserRole(),
                    fromDate,
                    toDate,
                    normalized.getWarehouseId(),
                    normalized.getSupplierId(),
                    orders.size());
            return buildPurchaseSummary(fromDate, toDate, orders);
        } catch (ReportGenerationException ex) {
            throw new ReportGenerationException("Purchase summary data is temporarily unavailable.", ex);
        }
    }

    @Override
    public PurchaseSummaryResponse getPurchaseOrderReport(LocalDate from, LocalDate to, Long warehouseId, Long supplierId) {
        ReportFilterRequest request = normalizeRequest(ReportFilterRequest.builder()
                .fromDate(from)
                .toDate(to)
                .warehouseId(warehouseId)
                .supplierId(supplierId)
                .size(Integer.MAX_VALUE)
                .build());
        LocalDate normalizedFrom = requiredFromDate(request);
        LocalDate normalizedTo = requiredToDate(request);
        log.info(
                "Generating poSummary role={} from={} to={} warehouseId={} supplierId={}",
                currentUserRole(),
                normalizedFrom,
                normalizedTo,
                request.getWarehouseId(),
                request.getSupplierId());
        try {
            List<ReportingDataClient.PurchaseOrderRecord> orders = reportingDataClient.searchPurchaseOrders(request);
            log.info(
                    "Generated poSummary role={} from={} to={} warehouseId={} supplierId={} orderCount={}",
                    currentUserRole(),
                    normalizedFrom,
                    normalizedTo,
                    request.getWarehouseId(),
                    request.getSupplierId(),
                    orders.size());
            return buildPurchaseSummary(normalizedFrom, normalizedTo, orders);
        } catch (ReportGenerationException ex) {
            throw new ReportGenerationException("Purchase summary data is temporarily unavailable.", ex);
        }
    }

    @Override
    public PurchaseOrderDetailReportResponse getPurchaseOrderDetailReport(Long purchaseOrderId) {
        ReportingDataClient.PurchaseOrderDetailRecord order = reportingDataClient.getPurchaseOrder(purchaseOrderId);
        List<ReportingDataClient.PaymentRecord> payments = safeCall(
                () -> reportingDataClient.getPaymentsByPurchaseOrder(purchaseOrderId),
                "payment-service");
        ReportingDataClient.RemainingAmountRecord remaining = safeCall(
                () -> reportingDataClient.getRemainingAmount(purchaseOrderId),
                "payment-service");

        List<PurchaseOrderDetailItemResponse> items = order.lineItems() == null ? List.<PurchaseOrderDetailItemResponse>of() : order.lineItems().stream()
                .map(item -> new PurchaseOrderDetailItemResponse(
                        item.productId(),
                        item.productName(),
                        item.productSku(),
                        item.orderedQuantity(),
                        item.receivedQuantity(),
                        item.pendingQuantity(),
                        safe(item.unitCost()),
                        safe(item.lineTotal())))
                .toList();

        List<PurchaseOrderTimelineItemResponse> timeline = order.history() == null ? List.<PurchaseOrderTimelineItemResponse>of() : order.history().stream()
                .map(history -> new PurchaseOrderTimelineItemResponse(
                        coalesce(history.newStatus(), history.action()),
                        history.actionAt(),
                        history.actorId(),
                        null,
                        history.remarks()))
                .sorted(Comparator.comparing(PurchaseOrderTimelineItemResponse::changedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();

        List<PaymentLineItemResponse> paymentLines = payments == null ? List.<PaymentLineItemResponse>of() : payments.stream()
                .map(payment -> new PaymentLineItemResponse(
                        payment.paymentId(),
                        payment.paymentNumber(),
                        safe(payment.paymentAmount()),
                        payment.paymentMethod(),
                        payment.razorpayOrderId(),
                        payment.razorpayPaymentId(),
                        payment.status(),
                        payment.paymentDate(),
                        payment.paidAt(),
                        payment.paidBy()))
                .toList();

        BigDecimal totalPaid = paymentLines.stream().map(PaymentLineItemResponse::paymentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingAmount = remaining != null ? safe(remaining.remainingAmount())
                : safe(order.totalAmount()).subtract(totalPaid).max(BigDecimal.ZERO);

        return new PurchaseOrderDetailReportResponse(
                order.purchaseOrderId(),
                order.poNumber(),
                order.supplierName(),
                order.warehouseName(),
                order.status(),
                order.createdBy(),
                order.createdByName(),
                order.approvedBy(),
                order.approvedByName(),
                order.createdAt(),
                order.approvedAt(),
                order.expectedDeliveryDate(),
                Boolean.TRUE.equals(order.isOverdue()),
                safe(order.totalAmount()),
                order.paymentStatus(),
                items,
                timeline,
                new PurchaseOrderPaymentSummaryResponse(totalPaid, remainingAmount, paymentLines),
                order.cancellationReason(),
                order.rejectionReason(),
                order.receivedAt());
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
        Map<Long, ReportingDataClient.SupplierRecord> supplierMap = safeSupplierMap();
        List<SupplierPerformanceReportResponse> items = reportingDataClient.searchPurchaseOrders(normalized).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.PurchaseOrderRecord::supplierId))
                .entrySet().stream()
                .map(entry -> {
                    ReportingDataClient.SupplierRecord supplier = supplierMap.get(entry.getKey());
                    BigDecimal totalSpend = entry.getValue().stream()
                            .map(ReportingDataClient.PurchaseOrderRecord::totalAmount)
                            .map(this::safe)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal averageLeadTime = average(entry.getValue().stream()
                            .filter(order -> order.createdAt() != null && order.actualDeliveryDate() != null)
                            .map(order -> BigDecimal.valueOf(
                                    ChronoUnit.DAYS.between(order.createdAt().toLocalDate(), order.actualDeliveryDate())))
                            .toList());
                    return new SupplierPerformanceReportResponse(
                            entry.getKey(),
                            supplier != null ? supplier.name() : UNKNOWN_SUPPLIER,
                            entry.getValue().size(),
                            countStatuses(entry.getValue(), "RECEIVED", "PARTIALLY_RECEIVED"),
                            entry.getValue().stream().filter(order -> Boolean.TRUE.equals(order.isOverdue())).count(),
                            totalSpend,
                            averageLeadTime,
                            supplier != null ? safe(supplier.rating()) : BigDecimal.ZERO);
                })
                .sorted(Comparator.comparing(SupplierPerformanceReportResponse::totalSpend).reversed())
                .toList();
        return page(items, normalized);
    }

    @Override
    public PaymentSummaryReportResponse getPaymentSummary(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        ReportingDataClient.PaymentSummaryRecord summary = reportingDataClient.getPaymentSummary();
        List<ReportingDataClient.PaymentRecord> payments = reportingDataClient.searchPayments(normalized);

        Map<String, Long> statusBreakdown = payments.stream()
                .collect(Collectors.groupingBy(payment -> enumName(payment.status()), TreeMap::new, Collectors.counting()));
        Map<String, Long> methodBreakdown = payments.stream()
                .collect(Collectors.groupingBy(payment -> enumName(payment.paymentMethod()), TreeMap::new, Collectors.counting()));
        List<PaymentSupplierBreakdownItem> supplierBreakdown = payments.stream()
                .collect(Collectors.groupingBy(ReportingDataClient.PaymentRecord::supplierId))
                .entrySet().stream()
                .map(entry -> new PaymentSupplierBreakdownItem(
                        entry.getKey(),
                        entry.getValue().stream().map(ReportingDataClient.PaymentRecord::supplierName).filter(Objects::nonNull).findFirst().orElse(UNKNOWN_SUPPLIER),
                        entry.getValue().stream().map(ReportingDataClient.PaymentRecord::paymentAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(PaymentSupplierBreakdownItem::totalPaid).reversed())
                .toList();

        BigDecimal pendingAmount = payments.stream()
                .filter(payment -> !isTerminalPaymentStatus(payment.status()))
                .map(payment -> safe(payment.remainingAmount()).compareTo(BigDecimal.ZERO) > 0
                        ? safe(payment.remainingAmount())
                        : safe(payment.poTotalAmount()).subtract(safe(payment.previouslyPaidAmount())).subtract(safe(payment.paymentAmount())).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal razorpayAmount = payments.stream()
                .filter(payment -> "RAZORPAY".equalsIgnoreCase(payment.paymentMethod()) || payment.razorpayPaymentId() != null)
                .map(ReportingDataClient.PaymentRecord::paymentAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentSummaryReportResponse(
                safeLong(summary.totalPayments()),
                safe(summary.totalPaidAmount()),
                pendingAmount.max(safe(summary.pendingPaymentAmount())),
                razorpayAmount,
                statusBreakdown,
                methodBreakdown,
                supplierBreakdown,
                List.of());
    }

    @Override
    public Page<PaymentBillingRowResponse> getPaymentBillingReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<PaymentBillingRowResponse> rows = reportingDataClient.searchPayments(normalized).stream()
                .map(payment -> new PaymentBillingRowResponse(
                        payment.paymentId(),
                        payment.paymentNumber(),
                        payment.poNumber(),
                        payment.supplierName(),
                        safe(payment.paymentAmount()),
                        payment.paymentMethod(),
                        payment.razorpayPaymentId(),
                        payment.paymentDate(),
                        payment.paidAt(),
                        payment.status(),
                        payment.paidBy(),
                        payment.createdBy()))
                .sorted(Comparator.comparing(PaymentBillingRowResponse::paidAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();
        return page(rows, normalized);
    }

    @Override
    public CancelledPurchaseOrderReportResponse getCancelledPurchaseOrderReport(ReportFilterRequest request) {
        return buildPurchaseOrderStatusReport(normalizeRequest(withStatus(request, STATUS_CANCELLED)), true);
    }

    @Override
    public RejectedPurchaseOrderReportResponse getRejectedPurchaseOrderReport(ReportFilterRequest request) {
        CancelledPurchaseOrderReportResponse report = buildPurchaseOrderStatusReport(normalizeRequest(withStatus(request, STATUS_REJECTED)), false);
        return new RejectedPurchaseOrderReportResponse(
                report.cancelledCount(),
                report.cancelledValue(),
                report.supplierBreakdown(),
                report.warehouseBreakdown(),
                report.cancelledOrders());
    }

    @Override
    public AlertSummaryReportResponse getAlertSummary(ReportFilterRequest request) {
        ReportingDataClient.AlertSummaryRecord summary = reportingDataClient.getMyAlertSummary();
        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("LOW_STOCK", safeLong(summary.lowStockCount()));
        byType.put("OVERSTOCK", safeLong(summary.overstockCount()));
        byType.put("PO_APPROVAL_PENDING", safeLong(summary.pendingPoApprovalCount()));
        byType.put("OVERDUE_RECEIPT", safeLong(summary.overduePoCount()));
        return new AlertSummaryReportResponse(
                safeLong(summary.totalAlerts()),
                safeLong(summary.unreadCount()),
                safeLong(summary.criticalCount()),
                safeLong(summary.warningCount()),
                byType);
    }

    @Override
    public InventoryValuationResponse generateInventoryReport(ReportFilterRequest request) {
        return buildInventoryValuationReport(request);
    }

    @Override
    public GeneratedInventoryReportResponse generateInventoryReport(
            ReportFilterRequest request,
            Integer slowMovingThreshold,
            Long deadStockThresholdDays) {
        ReportFilterRequest normalized = normalizeRequest(request);
        List<String> warnings = new ArrayList<>();
        InventoryValuationResponse valuation = buildInventoryValuationReport(normalized);
        warnings.addAll(valuation.warnings());
        InventoryTurnoverReportResponse turnover = getInventoryTurnover(
                requiredFromDate(normalized),
                requiredToDate(normalized),
                normalized.getWarehouseId());
        List<LowStockReportItem> lowStock = getLowStockReport(normalized).getContent();
        List<ProductMovementSummaryResponse> movementVelocity = getStockMovementSummary(normalized).getContent();
        List<TopMovingProductResponse> topMovingProducts = getTopMovingProducts(normalized);
        List<SlowMovingProductResponse> slowMovingProducts = getSlowMovingProducts(normalized, slowMovingThreshold);
        List<DeadStockResponse> deadStock = getDeadStockReport(normalized, deadStockThresholdDays);
        PurchaseSummaryResponse poSummary = getPurchaseOrderReport(
                requiredFromDate(normalized),
                requiredToDate(normalized),
                normalized.getWarehouseId(),
                normalized.getSupplierId());
        return new GeneratedInventoryReportResponse(
                valuation,
                valuation.warehouseBreakdown(),
                turnover,
                lowStock,
                movementVelocity,
                topMovingProducts,
                slowMovingProducts,
                deadStock,
                poSummary,
                warnings);
    }

    private InventoryValuationResponse buildInventoryValuationReport(ReportFilterRequest request) {
        ReportFilterRequest normalized = normalizeRequest(request);
        LocalDate asOfDate = normalized.getToDate() != null ? normalized.getToDate() : latestSnapshotDate().orElse(LocalDate.now());
        List<String> warnings = new ArrayList<>();
        List<ProductValuationItem> items = buildValuationItems(normalized, warnings);
        BigDecimal totalValue = items.stream().map(ProductValuationItem::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalQuantity = items.stream().map(ProductValuationItem::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new InventoryValuationResponse(
                asOfDate,
                totalValue,
                totalQuantity,
                items.stream().map(ProductValuationItem::productId).distinct().count(),
                items.stream().map(ProductValuationItem::warehouseId).distinct().count(),
                groupWarehouseValuation(items),
                items,
                warnings);
    }

    @Override
    public ExecutiveDashboardResponse getExecutiveDashboardReport() {
        List<String> warnings = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();

        InventoryValuationResponse valuation = dashboardSection(() -> generateInventoryReport(ReportFilterRequest.builder().size(Integer.MAX_VALUE).build()),
                unavailable, warnings, SECTION_INVENTORY);
        StockSummaryResponse stockSummary = dashboardSection(() -> getStockSummary(ReportFilterRequest.builder().size(Integer.MAX_VALUE).build()),
                unavailable, warnings, SECTION_INVENTORY);
        PurchaseSummaryResponse purchaseSummary = dashboardSection(() -> getPurchaseSummary(ReportFilterRequest.builder()
                .fromDate(LocalDate.now().minusDays(29))
                .toDate(LocalDate.now())
                .size(Integer.MAX_VALUE)
                .build()), unavailable, warnings, SECTION_PURCHASE);
        PaymentSummaryReportResponse paymentSummary = dashboardSection(() -> getPaymentSummary(ReportFilterRequest.builder()
                .fromDate(LocalDate.now().minusDays(29))
                .toDate(LocalDate.now())
                .size(Integer.MAX_VALUE)
                .build()), unavailable, warnings, "payments");
        ReportingDataClient.AlertSummaryRecord alertSummary = dashboardSection(reportingDataClient::getSystemAlertSummary, unavailable, warnings, SECTION_ALERTS);
        List<ReportingDataClient.AlertRecord> alerts = dashboardSection(() -> reportingDataClient.getRecentAlerts(true), unavailable, warnings, SECTION_ALERTS);
        List<TopMovingProductResponse> topMoving = dashboardSection(() -> getTopMovingProducts(ReportFilterRequest.builder().size(5).build()),
                unavailable, warnings, SECTION_MOVEMENTS);
        List<TrendPointResponse> valuationTrend = dashboardSection(this::buildValuationTrend, unavailable, warnings, SECTION_INVENTORY);
        List<TrendPointResponse> purchaseTrend = dashboardSection(this::buildPurchaseTrend, unavailable, warnings, SECTION_PURCHASE);
        Long movementToday = dashboardSection(() -> (long) reportingDataClient.searchAllMovements(ReportFilterRequest.builder()
                        .fromDate(LocalDate.now())
                        .toDate(LocalDate.now())
                        .size(Integer.MAX_VALUE)
                        .build()).size(),
                unavailable, warnings, SECTION_MOVEMENTS);

        return buildDashboardResponse(
                new DashboardData(
                        valuation,
                        stockSummary,
                        purchaseSummary,
                        paymentSummary,
                        alertSummary != null ? safeLong(alertSummary.criticalCount()) : 0,
                        movementToday != null ? movementToday : 0,
                        topMoving,
                        alerts != null ? alerts.stream()
                                .map(alert -> new DashboardAlertItem(alert.alertId(), alert.title(), alert.severity(), alert.type(), String.valueOf(alert.createdAt())))
                                .toList() : List.of(),
                        valuationTrend,
                        purchaseTrend),
                warnings,
                unavailable);
    }

    @Override
    public ExecutiveDashboardResponse getExecutiveDashboard() {
        return getExecutiveDashboardReport();
    }

    @Override
    public ExecutiveDashboardResponse getRoleDashboard(String role, Long userId) {
        List<String> warnings = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();

        InventoryValuationResponse valuation = dashboardSection(() -> generateInventoryReport(ReportFilterRequest.builder().size(Integer.MAX_VALUE).build()),
                unavailable, warnings, SECTION_INVENTORY);
        StockSummaryResponse stockSummary = dashboardSection(() -> getStockSummary(ReportFilterRequest.builder().size(Integer.MAX_VALUE).build()),
                unavailable, warnings, SECTION_INVENTORY);
        PurchaseSummaryResponse purchaseSummary = dashboardSection(() -> getPurchaseSummary(ReportFilterRequest.builder()
                .fromDate(LocalDate.now().minusDays(29))
                .toDate(LocalDate.now())
                .size(Integer.MAX_VALUE)
                .build()), unavailable, warnings, SECTION_PURCHASE);
        PaymentSummaryReportResponse paymentSummary = dashboardSection(() -> getPaymentSummary(ReportFilterRequest.builder()
                .fromDate(LocalDate.now().minusDays(29))
                .toDate(LocalDate.now())
                .size(Integer.MAX_VALUE)
                .build()), unavailable, warnings, "payments");
        ReportingDataClient.AlertSummaryRecord myAlerts = dashboardSection(reportingDataClient::getMyAlertSummary, unavailable, warnings, SECTION_ALERTS);
        List<TopMovingProductResponse> topMoving = dashboardSection(() -> getTopMovingProducts(ReportFilterRequest.builder().size(5).build()),
                unavailable, warnings, SECTION_MOVEMENTS);
        List<TrendPointResponse> valuationTrend = dashboardSection(this::buildValuationTrend, unavailable, warnings, SECTION_INVENTORY);
        List<TrendPointResponse> purchaseTrend = dashboardSection(this::buildPurchaseTrend, unavailable, warnings, SECTION_PURCHASE);

        return buildDashboardResponse(
                new DashboardData(
                        valuation,
                        stockSummary,
                        purchaseSummary,
                        paymentSummary,
                        myAlerts != null ? safeLong(myAlerts.criticalCount()) : 0,
                        0,
                        topMoving,
                        List.of(),
                        valuationTrend,
                        purchaseTrend),
                warnings,
                unavailable);
    }

    @Override
    public void createDailyInventorySnapshot() {
        selfProvider.getObject().createInventorySnapshotForDate(LocalDate.now());
    }

    @Override
    @Transactional
    public void createInventorySnapshotForDate(LocalDate date) {
        LocalDate snapshotDate = date != null ? date : LocalDate.now();
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        Map<Long, ReportingDataClient.WarehouseRecord> warehouseMap = warehouseMap();
        List<ReportingDataClient.StockRecord> stocks = reportingDataClient.getStocks(ReportFilterRequest.builder().size(Integer.MAX_VALUE).build());
        for (ReportingDataClient.StockRecord stock : stocks) {
            Optional<InventorySnapshot> existing = inventorySnapshotRepository
                    .findBySnapshotDateAndProductIdAndWarehouseId(snapshotDate, stock.productId(), stock.warehouseId());
            if (existing.isPresent()) {
                continue;
            }
            ReportingDataClient.ProductRecord product = productMap.get(stock.productId());
            ReportingDataClient.WarehouseRecord warehouse = warehouseMap.get(stock.warehouseId());
            BigDecimal unitCost = product != null ? safe(product.costPrice()) : BigDecimal.ZERO;
            BigDecimal quantity = decimal(stock.quantity());
            InventorySnapshot snapshot = InventorySnapshot.builder()
                    .snapshotDate(snapshotDate)
                    .productId(stock.productId())
                    .productSku(coalesce(stock.sku(), product != null ? product.sku() : null))
                    .productName(coalesce(stock.productName(), product != null ? product.name() : null))
                    .warehouseId(stock.warehouseId())
                    .warehouseCode(warehouse != null ? warehouse.code() : null)
                    .warehouseName(coalesce(stock.warehouseName(), warehouse != null ? warehouse.name() : null))
                    .quantity(quantity)
                    .reservedQuantity(decimal(stock.reservedQuantity()))
                    .availableQuantity(decimal(stock.availableQuantity()))
                    .unitCost(unitCost)
                    .totalValue(quantity.multiply(unitCost))
                    .createdAt(LocalDateTime.now())
                    .build();
            inventorySnapshotRepository.save(snapshot);
        }
        publishEvent("snapshot-routing-key", ReportEventType.SNAPSHOT_CREATED, ReportType.INVENTORY_VALUATION, "SUCCESS",
                Map.of("snapshotDate", snapshotDate.toString()));
    }

    @Override
    public Page<InventorySnapshotResponse> getInventorySnapshots(LocalDate date, int page, int size) {
        LocalDate snapshotDate = date != null ? date : latestSnapshotDate().orElse(LocalDate.now());
        ReportFilterRequest request = ReportFilterRequest.builder().page(page).size(size).build();
        return page(
                inventorySnapshotRepository.findBySnapshotDateOrderByWarehouseIdAscProductIdAsc(snapshotDate).stream()
                        .map(this::toSnapshotResponse)
                        .toList(),
                request);
    }

    @Override
    public Page<InventorySnapshotResponse> getInventorySnapshots(Long warehouseId, Long productId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        LocalDate from = fromDate != null ? fromDate : latestSnapshotDate().orElse(LocalDate.now());
        LocalDate to = toDate != null ? toDate : from;
        validateDateRange(from, to);
        ReportFilterRequest request = ReportFilterRequest.builder().page(page).size(size).build();
        List<InventorySnapshotResponse> items = inventorySnapshotRepository.findBySnapshotDateBetween(from, to).stream()
                .filter(snapshot -> warehouseId == null || Objects.equals(snapshot.getWarehouseId(), warehouseId))
                .filter(snapshot -> productId == null || Objects.equals(snapshot.getProductId(), productId))
                .sorted(Comparator.comparing(InventorySnapshot::getSnapshotDate)
                        .thenComparing(InventorySnapshot::getWarehouseId)
                        .thenComparing(InventorySnapshot::getProductId))
                .map(this::toSnapshotResponse)
                .toList();
        return page(items, request);
    }

    @Override
    public List<InventorySnapshotResponse> getSnapshotTrend(Long productId, Long warehouseId, LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        return inventorySnapshotRepository.findBySnapshotDateBetween(fromDate, toDate).stream()
                .filter(snapshot -> productId == null || Objects.equals(snapshot.getProductId(), productId))
                .filter(snapshot -> warehouseId == null || Objects.equals(snapshot.getWarehouseId(), warehouseId))
                .sorted(Comparator.comparing(InventorySnapshot::getSnapshotDate))
                .map(this::toSnapshotResponse)
                .toList();
    }

    @Override
    public byte[] exportInventoryValuation(ReportFilterRequest request, ExportFormat format) {
        InventoryValuationResponse report = generateInventoryReport(request);
        List<String> headers = List.of("Warehouse", "Product", "SKU", "Quantity", "Cost Price", "Stock Value");
        List<List<?>> rows = new ArrayList<>();
        report.productBreakdown().forEach(item ->
                rows.add(row(item.warehouseName(), item.productName(), item.sku(), item.quantity(), item.costPrice(), item.stockValue())));
        return export(format, "Inventory Valuation", headers, rows);
    }

    @Override
    public byte[] exportStockMovementReport(ReportFilterRequest request, ExportFormat format) {
        List<String> headers = List.of("Movement No", "Product", "SKU", "Warehouse", "Type", "Direction", "Quantity", VALUE_HEADER, "Date");
        List<List<?>> rows = new ArrayList<>();
        getStockMovementReport(request).getContent().forEach(item ->
                rows.add(row(item.movementNumber(), item.productName(), item.sku(), item.warehouseName(),
                        item.movementType(), item.direction(), item.quantity(), item.totalValue(), item.movementDate())));
        return export(format, "Stock Movements", headers, rows);
    }

    @Override
    public byte[] exportPurchaseSummary(ReportFilterRequest request, ExportFormat format) {
        PurchaseSummaryResponse report = getPurchaseSummary(request);
        List<String> headers = List.of("Metric", VALUE_HEADER);
        List<List<?>> rows = List.of(
                row("Total Purchase Orders", report.totalPurchaseOrders()),
                row("Total Spend", report.totalSpend()),
                row("Pending Approval", report.pendingApprovalCount()),
                row("Approved", report.approvedCount()),
                row("Partially Received", report.partiallyReceivedCount()),
                row("Fully Received", report.fullyReceivedCount()),
                row("Overdue", report.overdueCount()),
                row("Cancelled", report.cancelledCount()),
                row("Rejected", report.rejectedCount()));
        return export(format, "Purchase Summary", headers, rows);
    }

    @Override
    public byte[] exportSupplierPerformance(ReportFilterRequest request, ExportFormat format) {
        List<String> headers = List.of("Supplier", "Orders", "Received", "Delayed", "Spend", "Avg Lead Time", "Rating");
        List<List<?>> rows = new ArrayList<>();
        getSupplierPerformanceReport(request).getContent().forEach(item ->
                rows.add(row(item.supplierName(), item.totalOrders(), item.receivedOrders(), item.delayedOrders(),
                        item.totalSpend(), item.averageLeadTimeDays(), item.rating())));
        return export(format, "Supplier Performance", headers, rows);
    }

    @Override
    public byte[] exportExecutiveDashboard(ExportFormat format) {
        ExecutiveDashboardResponse dashboard = getExecutiveDashboardReport();
        List<String> headers = List.of("Metric", VALUE_HEADER);
        List<List<?>> rows = List.of(
                row("Total Products", dashboard.totalProducts()),
                row("Active Products", dashboard.activeProducts()),
                row("Total Warehouses", dashboard.totalWarehouses()),
                row("Inventory Value", dashboard.inventoryValue()),
                row("Low Stock Items", dashboard.lowStockItems()),
                row("Overstock Items", dashboard.overstockItems()),
                row("Pending PO Approvals", dashboard.pendingPoApprovals()),
                row("Overdue Purchase Orders", dashboard.overduePurchaseOrders()),
                row("Total Purchase Value", dashboard.totalPurchaseValue()),
                row("Total Paid Amount", dashboard.totalPaidAmount()),
                row("Payable Purchase Orders", dashboard.payablePurchaseOrders()),
                row("Cancelled Purchase Orders", dashboard.cancelledPurchaseOrders()));
        return export(format, "Executive Dashboard", headers, rows);
    }

    private PurchaseSummaryResponse buildPurchaseSummary(LocalDate from, LocalDate to, List<ReportingDataClient.PurchaseOrderRecord> orders) {
        Map<String, Long> statusBreakdown = orders.stream()
                .collect(Collectors.groupingBy(order -> coalesce(enumName(order.status()), "UNKNOWN"), TreeMap::new, Collectors.counting()));
        List<PurchaseSpendBreakdownItem> supplierBreakdown = orders.stream()
                .collect(Collectors.groupingBy(ReportingDataClient.PurchaseOrderRecord::supplierId))
                .entrySet().stream()
                .map(entry -> new PurchaseSpendBreakdownItem(
                        entry.getKey(),
                        entry.getValue().stream().map(ReportingDataClient.PurchaseOrderRecord::supplierName).filter(Objects::nonNull).findFirst().orElse(UNKNOWN_SUPPLIER),
                        entry.getValue().size(),
                        entry.getValue().stream().map(ReportingDataClient.PurchaseOrderRecord::totalAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(PurchaseSpendBreakdownItem::totalSpend).reversed())
                .toList();
        List<PurchaseSpendBreakdownItem> warehouseBreakdown = orders.stream()
                .collect(Collectors.groupingBy(ReportingDataClient.PurchaseOrderRecord::warehouseId))
                .entrySet().stream()
                .map(entry -> new PurchaseSpendBreakdownItem(
                        entry.getKey(),
                        entry.getValue().stream().map(ReportingDataClient.PurchaseOrderRecord::warehouseName).filter(Objects::nonNull).findFirst().orElse("Unknown warehouse"),
                        entry.getValue().size(),
                        entry.getValue().stream().map(ReportingDataClient.PurchaseOrderRecord::totalAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(PurchaseSpendBreakdownItem::totalSpend).reversed())
                .toList();
        return new PurchaseSummaryResponse(
                from,
                to,
                orders.size(),
                orders.stream().map(ReportingDataClient.PurchaseOrderRecord::totalAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add),
                statusBreakdown,
                supplierBreakdown,
                warehouseBreakdown,
                countStatuses(orders, "PENDING_APPROVAL", "PENDING"),
                countStatuses(orders, "APPROVED"),
                countStatuses(orders, "PARTIALLY_RECEIVED"),
                countStatuses(orders, "RECEIVED"),
                orders.stream().filter(order -> Boolean.TRUE.equals(order.isOverdue())).count(),
                countStatuses(orders, STATUS_CANCELLED),
                countStatuses(orders, STATUS_REJECTED));
    }

    private CancelledPurchaseOrderReportResponse buildPurchaseOrderStatusReport(ReportFilterRequest request, boolean cancelled) {
        List<ReportingDataClient.PurchaseOrderRecord> orders = reportingDataClient.searchPurchaseOrders(request);
        List<CancelledPurchaseOrderRowResponse> rows = orders.stream()
                .map(order -> safeCall(() -> reportingDataClient.getPurchaseOrder(order.purchaseOrderId()), "purchase-service"))
                .filter(Objects::nonNull)
                .map(detail -> new CancelledPurchaseOrderRowResponse(
                        detail.purchaseOrderId(),
                        detail.poNumber(),
                        detail.supplierName(),
                        detail.warehouseName(),
                        safe(detail.totalAmount()),
                        cancelled ? detail.approvedBy() : detail.createdBy(),
                        cancelled ? detail.approvedByName() : detail.createdByName(),
                        cancelled ? detail.cancelledAt() : detail.rejectedAt(),
                        cancelled ? detail.cancellationReason() : detail.rejectionReason()))
                .toList();

        List<PurchaseSpendBreakdownItem> supplierBreakdown = rows.stream()
                .collect(Collectors.groupingBy(CancelledPurchaseOrderRowResponse::supplierName))
                .entrySet().stream()
                .map(entry -> new PurchaseSpendBreakdownItem(
                        null,
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(CancelledPurchaseOrderRowResponse::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
        List<PurchaseSpendBreakdownItem> warehouseBreakdown = rows.stream()
                .collect(Collectors.groupingBy(CancelledPurchaseOrderRowResponse::warehouseName))
                .entrySet().stream()
                .map(entry -> new PurchaseSpendBreakdownItem(
                        null,
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(CancelledPurchaseOrderRowResponse::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();

        return new CancelledPurchaseOrderReportResponse(
                rows.size(),
                rows.stream().map(CancelledPurchaseOrderRowResponse::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                supplierBreakdown,
                warehouseBreakdown,
                rows);
    }

    private ReportFilterRequest withStatus(ReportFilterRequest request, String status) {
        ReportFilterRequest source = request != null ? request : new ReportFilterRequest();
        return ReportFilterRequest.builder()
                .fromDate(source.getFromDate())
                .toDate(source.getToDate())
                .productId(source.getProductId())
                .warehouseId(source.getWarehouseId())
                .supplierId(source.getSupplierId())
                .category(source.getCategory())
                .brand(source.getBrand())
                .movementType(source.getMovementType())
                .poStatus(status)
                .paymentStatus(source.getPaymentStatus())
                .alertSeverity(source.getAlertSeverity())
                .period(source.getPeriod())
                .page(source.getPage())
                .size(source.getSize())
                .sortBy(source.getSortBy())
                .sortDir(source.getSortDir())
                .build();
    }

    private List<ProductValuationItem> buildValuationItems(ReportFilterRequest request, List<String> warnings) {
        Optional<LocalDate> snapshotDate = resolveSnapshotDate(request);
        if (snapshotDate.isPresent()) {
            List<InventorySnapshot> snapshots = snapshotsForDate(snapshotDate.get(), request.getWarehouseId()).stream()
                    .filter(snapshot -> request.getProductId() == null || Objects.equals(snapshot.getProductId(), request.getProductId()))
                    .toList();
            if (!snapshots.isEmpty()) {
                return snapshots.stream()
                        .map(snapshot -> new ProductValuationItem(
                                snapshot.getProductId(),
                                snapshot.getProductName(),
                                snapshot.getProductSku(),
                                snapshot.getWarehouseId(),
                                snapshot.getWarehouseName(),
                                safe(snapshot.getQuantity()),
                                safe(snapshot.getUnitCost()),
                                safe(snapshot.getTotalValue()),
                                null))
                        .toList();
            }
        }
        if (warnings != null) {
            warnings.add("Snapshot data unavailable for requested date. Falling back to live stock data.");
        }
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        Map<Long, ReportingDataClient.WarehouseRecord> warehouseMap = warehouseMap();
        return reportingDataClient.getStocks(request).stream()
                .filter(stock -> includeCategoryBrand(stock.productId(), productMap, request))
                .map(stock -> {
                    ReportingDataClient.ProductRecord product = productMap.get(stock.productId());
                    ReportingDataClient.WarehouseRecord warehouse = warehouseMap.get(stock.warehouseId());
                    BigDecimal quantity = decimal(stock.quantity());
                    BigDecimal costPrice = product != null ? safe(product.costPrice()) : BigDecimal.ZERO;
                    return new ProductValuationItem(
                            stock.productId(),
                            coalesce(stock.productName(), product != null ? product.name() : null),
                            coalesce(stock.sku(), product != null ? product.sku() : null),
                            stock.warehouseId(),
                            warehouse != null ? warehouse.name() : stock.warehouseName(),
                            quantity,
                            costPrice,
                            quantity.multiply(costPrice),
                            product != null ? product.category() : null);
                })
                .toList();
    }

    private Optional<LocalDate> resolveSnapshotDate(ReportFilterRequest request) {
        if (request.getToDate() != null && inventorySnapshotRepository.existsBySnapshotDate(request.getToDate())) {
            return Optional.of(request.getToDate());
        }
        return latestSnapshotDate();
    }

    private Optional<LocalDate> latestSnapshotDate() {
        return Optional.ofNullable(inventorySnapshotRepository.findLatestSnapshotDate());
    }

    private List<InventorySnapshot> snapshotsForDate(LocalDate date, Long warehouseId) {
        if (warehouseId != null) {
            return inventorySnapshotRepository.findByWarehouseIdAndSnapshotDate(warehouseId, date);
        }
        return inventorySnapshotRepository.findBySnapshotDate(date);
    }

    private List<InventorySnapshot> snapshotsBetween(LocalDate from, LocalDate to, Long warehouseId) {
        if (warehouseId != null) {
            return inventorySnapshotRepository.findByWarehouseIdAndSnapshotDateBetween(warehouseId, from, to);
        }
        return inventorySnapshotRepository.findBySnapshotDateBetween(from, to);
    }

    private List<WarehouseValuationItem> groupWarehouseValuation(List<ProductValuationItem> items) {
        return items.stream()
                .collect(Collectors.groupingBy(ProductValuationItem::warehouseId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(group -> new WarehouseValuationItem(
                        group.get(0).warehouseId(),
                        group.get(0).warehouseName(),
                        group.stream().map(ProductValuationItem::quantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                        group.stream().map(ProductValuationItem::stockValue).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .toList();
    }

    private LowStockReportItem buildLowStockItem(ReportingDataClient.StockRecord stock, ReportingDataClient.ProductRecord product) {
        if (!isLowStock(stock, product)) {
            return null;
        }
        BigDecimal available = decimal(stock.availableQuantity());
        BigDecimal reorderLevel = decimal(product.reorderLevel());
        BigDecimal maxStockLevel = decimal(product.maxStockLevel());
        String severity = available.compareTo(BigDecimal.ZERO) <= 0 ? "CRITICAL" : "WARNING";
        return new LowStockReportItem(
                stock.productId(),
                coalesce(stock.productName(), product.name()),
                coalesce(stock.sku(), product.sku()),
                stock.warehouseId(),
                stock.warehouseName(),
                available,
                reorderLevel,
                maxStockLevel,
                severity,
                available.compareTo(BigDecimal.ZERO) <= 0 ? "Raise urgent replenishment request" : "Review reorder and create replenishment PO");
    }

    private OverstockReportItem buildOverstockItem(ReportingDataClient.StockRecord stock, ReportingDataClient.ProductRecord product) {
        if (!isOverstock(stock, product)) {
            return null;
        }
        BigDecimal quantity = decimal(stock.availableQuantity());
        BigDecimal reorderLevel = decimal(product.reorderLevel());
        BigDecimal maxStockLevel = decimal(product.maxStockLevel());
        return new OverstockReportItem(
                stock.productId(),
                coalesce(stock.productName(), product.name()),
                coalesce(stock.sku(), product.sku()),
                stock.warehouseId(),
                stock.warehouseName(),
                quantity,
                reorderLevel,
                maxStockLevel,
                "WARNING",
                "Review purchasing pace or transfer excess inventory");
    }

    private List<ProductMovementSummaryResponse> movementSummaryByProductWarehouse(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        return reportingDataClient.searchAllMovements(request).stream()
                .collect(Collectors.groupingBy(movement -> movement.productId() + ":" + movement.warehouseId()))
                .values().stream()
                .map(group -> {
                    ReportingDataClient.MovementRecord first = group.get(0);
                    ReportingDataClient.ProductRecord product = productMap.get(first.productId());
                    BigDecimal unitsIn = sumMovements(group, this::isStockIn);
                    BigDecimal unitsOut = sumMovements(group, this::isStockOut);
                    return new ProductMovementSummaryResponse(
                            first.productId(),
                            product != null ? product.name() : first.productName(),
                            product != null ? product.sku() : first.productSku(),
                            first.warehouseId(),
                            first.warehouseName(),
                            unitsIn,
                            unitsOut,
                            unitsIn.add(unitsOut),
                            group.size(),
                            group.stream().map(this::movementValue).reduce(BigDecimal.ZERO, BigDecimal::add));
                })
                .sorted(Comparator.comparing(ProductMovementSummaryResponse::totalMoved).reversed())
                .toList();
    }

    private Map<Long, ProductActivity> buildProductActivity(ReportFilterRequest request) {
        Map<Long, ReportingDataClient.ProductRecord> productMap = productMap();
        Map<Long, BigDecimal> stockByProduct = reportingDataClient.getStocks(request).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.StockRecord::productId,
                        Collectors.reducing(BigDecimal.ZERO, stock -> decimal(stock.quantity()), BigDecimal::add)));
        ReportFilterRequest totalMovementFilter = movementFilter(request, request.getFromDate(), request.getToDate());
        ReportFilterRequest lastMovementFilter = movementFilter(request, EARLIEST_DATE, movementEndDate(request));
        Map<Long, BigDecimal> movementByProduct = reportingDataClient.searchAllMovements(totalMovementFilter).stream()
                .collect(Collectors.groupingBy(ReportingDataClient.MovementRecord::productId,
                        Collectors.reducing(BigDecimal.ZERO, movement -> safe(movement.quantity()), BigDecimal::add)));
        Map<Long, LocalDate> lastMovementByProduct = reportingDataClient.searchAllMovements(lastMovementFilter).stream()
                .collect(Collectors.toMap(
                        ReportingDataClient.MovementRecord::productId,
                        movement -> movement.movementDate() != null ? movement.movementDate().toLocalDate() : null,
                        (first, second) -> first != null && (second == null || first.isAfter(second)) ? first : second));

        Map<Long, ProductActivity> activity = new LinkedHashMap<>();
        for (Long productId : unionKeys(productMap.keySet(), stockByProduct.keySet(), movementByProduct.keySet())) {
            ReportingDataClient.ProductRecord product = productMap.get(productId);
            LocalDate lastMovementDate = lastMovementByProduct.get(productId);
            long daysSinceLastMovement = lastMovementDate == null
                    ? deadStockDays + 1L
                    : ChronoUnit.DAYS.between(lastMovementDate, LocalDate.now());
            activity.put(productId, new ProductActivity(
                    productId,
                    product != null ? product.name() : null,
                    product != null ? product.sku() : null,
                    product != null ? safe(product.costPrice()) : BigDecimal.ZERO,
                    movementByProduct.getOrDefault(productId, BigDecimal.ZERO),
                    lastMovementDate,
                    daysSinceLastMovement,
                    stockByProduct.getOrDefault(productId, BigDecimal.ZERO)));
        }
        return activity;
    }

    private List<TrendPointResponse> buildValuationTrend() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        Map<LocalDate, BigDecimal> byDate = inventorySnapshotRepository.findBySnapshotDateBetween(from, to).stream()
                .collect(Collectors.groupingBy(InventorySnapshot::getSnapshotDate,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, InventorySnapshot::getTotalValue, BigDecimal::add)));
        return toTrend(byDate, from, to);
    }

    private List<TrendPointResponse> buildPurchaseTrend() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        Map<LocalDate, BigDecimal> byDate = reportingDataClient.searchPurchaseOrders(ReportFilterRequest.builder()
                        .fromDate(from)
                        .toDate(to)
                        .size(Integer.MAX_VALUE)
                        .build()).stream()
                .filter(order -> order.createdAt() != null)
                .collect(Collectors.groupingBy(order -> order.createdAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, order -> safe(order.totalAmount()), BigDecimal::add)));
        return toTrend(byDate, from, to);
    }

    private List<TrendPointResponse> toTrend(Map<LocalDate, BigDecimal> byDate, LocalDate from, LocalDate to) {
        List<TrendPointResponse> trend = new ArrayList<>();
        BigDecimal previous = null;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            BigDecimal value = byDate.getOrDefault(date, BigDecimal.ZERO);
            TrendDirection direction = previous == null ? TrendDirection.FLAT : resolveTrendDirection(previous, value);
            trend.add(new TrendPointResponse(date, value, direction));
            previous = value;
        }
        return trend;
    }

    private ExecutiveDashboardResponse buildDashboardResponse(
            DashboardData data,
            List<String> warnings,
            List<String> unavailable) {
        return new ExecutiveDashboardResponse(
                data.valuation() != null ? data.valuation().totalProducts() : 0,
                countActiveProductsSafe(unavailable, warnings),
                data.valuation() != null ? data.valuation().totalWarehouses() : 0,
                data.valuation() != null ? data.valuation().totalInventoryValue() : BigDecimal.ZERO,
                data.stockSummary() != null ? data.stockSummary().lowStockCount() : 0,
                data.stockSummary() != null ? data.stockSummary().overstockCount() : 0,
                data.purchaseSummary() != null ? data.purchaseSummary().pendingApprovalCount() : 0,
                data.purchaseSummary() != null ? data.purchaseSummary().overdueCount() : 0,
                data.purchaseSummary() != null ? data.purchaseSummary().totalSpend() : BigDecimal.ZERO,
                data.paymentSummary() != null ? data.paymentSummary().totalPaidAmount() : BigDecimal.ZERO,
                data.paymentSummary() != null ? countPayablePurchaseOrders() : 0,
                data.purchaseSummary() != null ? data.purchaseSummary().cancelledCount() : 0,
                null,
                data.criticalAlerts(),
                data.movementToday(),
                data.topMoving() != null ? data.topMoving() : List.of(),
                data.alerts() != null ? data.alerts() : List.of(),
                data.valuationTrend() != null ? data.valuationTrend() : List.of(),
                data.purchaseTrend() != null ? data.purchaseTrend() : List.of(),
                warnings,
                unavailable);
    }

    private ReportFilterRequest movementFilter(ReportFilterRequest request, LocalDate fromDate, LocalDate toDate) {
        return ReportFilterRequest.builder()
                .warehouseId(request.getWarehouseId())
                .productId(request.getProductId())
                .fromDate(fromDate)
                .toDate(toDate)
                .size(Integer.MAX_VALUE)
                .build();
    }

    private LocalDate movementEndDate(ReportFilterRequest request) {
        return request.getToDate() != null ? request.getToDate() : LocalDate.now();
    }

    private TrendDirection resolveTrendDirection(BigDecimal previous, BigDecimal current) {
        int comparison = current.compareTo(previous);
        if (comparison > 0) {
            return TrendDirection.UP;
        }
        if (comparison < 0) {
            return TrendDirection.DOWN;
        }
        return TrendDirection.FLAT;
    }

    private <T> T dashboardSection(Supplier<T> supplier, List<String> unavailable, List<String> warnings, String section) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            log.warn("Dashboard section {} unavailable: {}", section, ex.getMessage());
            unavailable.add(section);
            warnings.add(section + " data temporarily unavailable");
            return null;
        }
    }

    private long countActiveProductsSafe(List<String> unavailable, List<String> warnings) {
        Long count = dashboardSection(() -> reportingDataClient.getProducts().stream()
                .filter(product -> !Boolean.FALSE.equals(product.isActive()))
                .count(), unavailable, warnings, "product-service");
        return count != null ? count : 0L;
    }

    private long countPayablePurchaseOrders() {
        return reportingDataClient.searchPayments(ReportFilterRequest.builder().size(Integer.MAX_VALUE).build()).stream()
                .filter(payment -> safe(payment.remainingAmount()).compareTo(BigDecimal.ZERO) > 0)
                .map(ReportingDataClient.PaymentRecord::purchaseOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private ReportFilterRequest normalizeRequest(ReportFilterRequest request) {
        ReportFilterRequest normalized = request != null ? request : ReportFilterRequest.builder().build();
        if (needsDerivedDateRange(normalized)) {
            DateWindow dateWindow = resolveDateWindow(normalized);
            normalized.setFromDate(dateWindow.fromDate());
            normalized.setToDate(dateWindow.toDate());
        }
        if (normalized.getFromDate() != null || normalized.getToDate() != null) {
            validateDateRange(requiredFromDate(normalized), requiredToDate(normalized));
        }
        if (normalized.getSize() <= 0) {
            normalized.setSize(10);
        }
        if (normalized.getPage() < 0) {
            normalized.setPage(0);
        }
        return normalized;
    }

    private boolean needsDerivedDateRange(ReportFilterRequest request) {
        return request.getPeriod() != null && (request.getFromDate() == null || request.getToDate() == null);
    }

    private DateWindow resolveDateWindow(ReportFilterRequest request) {
        LocalDate end = LocalDate.now();
        ReportPeriod period = request.getPeriod();
        if (period == ReportPeriod.TODAY) {
            return new DateWindow(end, end);
        }
        if (period == ReportPeriod.LAST_7_DAYS) {
            return new DateWindow(end.minusDays(6), end);
        }
        if (period == ReportPeriod.LAST_30_DAYS) {
            return new DateWindow(end.minusDays(29), end);
        }
        if (period == ReportPeriod.THIS_MONTH) {
            return new DateWindow(end.withDayOfMonth(1), end);
        }
        if (period == ReportPeriod.LAST_MONTH) {
            LocalDate previousMonth = end.minusMonths(1);
            return new DateWindow(previousMonth.withDayOfMonth(1), previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()));
        }
        if (period == ReportPeriod.CUSTOM) {
            return new DateWindow(request.getFromDate(), end);
        }
        return new DateWindow(end.minusDays(29), end);
    }

    private LocalDate requiredFromDate(ReportFilterRequest request) {
        if (request.getFromDate() == null && request.getToDate() != null) {
            request.setFromDate(request.getToDate().minusDays(29));
        } else if (request.getFromDate() == null) {
            request.setFromDate(LocalDate.now().minusDays(29));
        }
        return request.getFromDate();
    }

    private LocalDate requiredToDate(ReportFilterRequest request) {
        if (request.getToDate() == null && request.getFromDate() != null) {
            request.setToDate(LocalDate.now());
        } else if (request.getToDate() == null) {
            request.setToDate(LocalDate.now());
        }
        return request.getToDate();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }
    }

    private BigDecimal resolveSlowMovingThreshold(ReportFilterRequest request) {
        if (request != null && request.getSortBy() != null && request.getSortBy().matches("\\d+")) {
            return new BigDecimal(request.getSortBy());
        }
        return BigDecimal.valueOf(5L);
    }

    private InventorySnapshotResponse toSnapshotResponse(InventorySnapshot snapshot) {
        return new InventorySnapshotResponse(
                snapshot.getSnapshotId(),
                snapshot.getSnapshotDate(),
                snapshot.getWarehouseId(),
                snapshot.getWarehouseName(),
                snapshot.getProductId(),
                snapshot.getProductName(),
                snapshot.getProductSku(),
                safe(snapshot.getQuantity()),
                safe(snapshot.getUnitCost()),
                safe(snapshot.getTotalValue()),
                snapshot.getCreatedAt(),
                snapshot.getWarehouseCode(),
                safe(snapshot.getReservedQuantity()),
                safe(snapshot.getAvailableQuantity()),
                safe(snapshot.getUnitCost()),
                safe(snapshot.getTotalValue()));
    }

    private <T> Page<T> page(List<T> items, ReportFilterRequest request) {
        int start = Math.min(request.getPage() * request.getSize(), items.size());
        int end = Math.min(start + request.getSize(), items.size());
        return new PageImpl<>(items.subList(start, end), PageRequest.of(request.getPage(), request.getSize()), items.size());
    }

    private byte[] export(ExportFormat format, String title, List<String> headers, List<? extends List<?>> rows) {
        publishEvent("export-requested-routing-key", ReportEventType.REPORT_EXPORT_REQUESTED, ReportType.EXECUTIVE_DASHBOARD, "STARTED",
                Map.of("title", title, "format", format.name()));
        byte[] content;
        if (format == ExportFormat.CSV) {
            content = reportExportService.exportCsv(headers, rows);
        } else if (format == ExportFormat.EXCEL) {
            content = reportExportService.exportExcel(title.replace(" ", "_"), headers, rows);
        } else {
            content = reportExportService.exportPdf(title, headers, rows);
        }
        publishEvent("export-completed-routing-key", ReportEventType.REPORT_EXPORT_COMPLETED, ReportType.EXECUTIVE_DASHBOARD, "SUCCESS",
                Map.of("title", title, "format", format.name()));
        return content;
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

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
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

    private boolean isLowStock(ReportingDataClient.StockRecord stock, ReportingDataClient.ProductRecord product) {
        return product != null
                && product.reorderLevel() != null
                && decimal(stock.availableQuantity()).compareTo(decimal(product.reorderLevel())) < 0;
    }

    private boolean isOverstock(ReportingDataClient.StockRecord stock, ReportingDataClient.ProductRecord product) {
        return product != null
                && product.maxStockLevel() != null
                && decimal(stock.availableQuantity()).compareTo(decimal(product.maxStockLevel())) > 0;
    }

    private boolean isStockIn(ReportingDataClient.MovementRecord movement) {
        return "IN".equalsIgnoreCase(enumName(movement.direction()));
    }

    private boolean isStockOut(ReportingDataClient.MovementRecord movement) {
        return "OUT".equalsIgnoreCase(enumName(movement.direction()));
    }

    private BigDecimal movementValue(ReportingDataClient.MovementRecord movement) {
        if (safe(movement.totalValue()).compareTo(BigDecimal.ZERO) > 0) {
            return safe(movement.totalValue());
        }
        return safe(movement.quantity()).multiply(safe(movement.unitCost()));
    }

    private BigDecimal sumMovements(List<ReportingDataClient.MovementRecord> movements, Predicate<ReportingDataClient.MovementRecord> predicate) {
        return movements.stream()
                .filter(predicate)
                .map(ReportingDataClient.MovementRecord::quantity)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumStocks(List<ReportingDataClient.StockRecord> stocks, Function<ReportingDataClient.StockRecord, Integer> mapper) {
        return stocks.stream().map(mapper).map(this::decimal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isTerminalPaymentStatus(String status) {
        return status != null && List.of(STATUS_PAID, STATUS_CANCELLED, STATUS_FAILED, STATUS_REJECTED).contains(status.toUpperCase());
    }

    private String enumName(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Enum<?> enumValue ? enumValue.name() : String.valueOf(value);
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

    private BigDecimal average(Collection<BigDecimal> values) {
        List<BigDecimal> safeValues = values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
        if (safeValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = safeValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(safeValues.size()), 4, RoundingMode.HALF_UP);
    }

    private String currentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.role();
        }
        return "UNKNOWN";
    }

    @SafeVarargs
    private final <T> Set<T> unionKeys(Set<T>... sets) {
        Set<T> result = new LinkedHashSet<>();
        for (Set<T> set : sets) {
            result.addAll(set);
        }
        return result;
    }

    private long countStatuses(List<ReportingDataClient.PurchaseOrderRecord> orders, String... statuses) {
        Set<String> expected = Arrays.stream(statuses).map(String::toUpperCase).collect(Collectors.toSet());
        return orders.stream()
                .filter(order -> order.status() != null && expected.contains(order.status().toUpperCase()))
                .count();
    }

    private Map<Long, ReportingDataClient.ProductRecord> productMap() {
        return reportingDataClient.getProducts().stream()
                .collect(Collectors.toMap(ReportingDataClient.ProductRecord::productId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, ReportingDataClient.WarehouseRecord> warehouseMap() {
        return reportingDataClient.getWarehouses().stream()
                .collect(Collectors.toMap(ReportingDataClient.WarehouseRecord::warehouseId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, ReportingDataClient.SupplierRecord> safeSupplierMap() {
        try {
            return reportingDataClient.getSuppliers().stream()
                    .collect(Collectors.toMap(ReportingDataClient.SupplierRecord::supplierId, Function.identity(), (left, right) -> left));
        } catch (RuntimeException ex) {
            log.warn("Supplier lookup unavailable for report generation. reason={}", ex.getMessage());
            return Map.of();
        }
    }

    private <T> T safeCall(ThrowingSupplier<T> supplier, String serviceName) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            log.warn("Unable to fetch {} data: {}", serviceName, ex.getMessage());
            return null;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }

    private record DashboardData(
            InventoryValuationResponse valuation,
            StockSummaryResponse stockSummary,
            PurchaseSummaryResponse purchaseSummary,
            PaymentSummaryReportResponse paymentSummary,
            long criticalAlerts,
            long movementToday,
            List<TopMovingProductResponse> topMoving,
            List<DashboardAlertItem> alerts,
            List<TrendPointResponse> valuationTrend,
            List<TrendPointResponse> purchaseTrend) {
    }

    private record DateWindow(LocalDate fromDate, LocalDate toDate) {
    }

    private record ProductActivity(
            Long productId,
            String productName,
            String sku,
            BigDecimal costPrice,
            BigDecimal totalMoved,
            LocalDate lastMovementDate,
            long daysSinceLastMovement,
            BigDecimal currentQuantity) {

        private SlowMovingProductResponse toSlowMovingResponse() {
            return new SlowMovingProductResponse(
                    productId,
                    productName,
                    sku,
                    totalMoved,
                    lastMovementDate,
                    daysSinceLastMovement,
                    currentQuantity);
        }
    }
}
