package com.stockpro.reportservice.service;

import com.stockpro.reportservice.dto.DeadStockDTO;
import com.stockpro.reportservice.dto.InventorySnapshotDTO;
import com.stockpro.reportservice.dto.POSummaryDTO;
import com.stockpro.reportservice.dto.StockValuationDTO;
import com.stockpro.reportservice.dto.TopMovingProductDTO;
import com.stockpro.reportservice.entity.InventorySnapshot;
import com.stockpro.reportservice.exception.ReportGenerationException;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private static final int DEFAULT_TOP_MOVERS_LIMIT = 10;

    private final InventorySnapshotRepository snapshotRepository;
    private final InventorySnapshotMapper snapshotMapper;
    private final ReportDataGateway reportDataGateway;

    @Value("${report.dead-stock-days:90}")
    private int deadStockDays;

    @Value("${report.slow-moving-days:30}")
    private int slowMovingDays;

    @Transactional
    public InventorySnapshotDTO takeSnapshot(
            Long warehouseId, Long productId,
            Integer quantity, BigDecimal stockValue) {

        validateManualSnapshot(warehouseId, productId, quantity, stockValue);

        LocalDate today = LocalDate.now();

        InventorySnapshot snapshot = snapshotRepository
                .findByWarehouseIdAndProductIdAndSnapshotDate(warehouseId, productId, today)
                .orElseGet(() -> InventorySnapshot.builder()
                        .warehouseId(warehouseId)
                        .productId(productId)
                        .snapshotDate(today)
                        .build());

        snapshot.setQuantity(quantity);
        snapshot.setStockValue(stockValue);

        InventorySnapshot saved = saveSnapshot(snapshot);

        log.info("Saved manual snapshot {} for warehouse {} product {}",
                saved.getSnapshotId(), warehouseId, productId);

        return snapshotMapper.toDto(saved);
    }

    @Transactional
    public List<InventorySnapshotDTO> takeDailySnapshot() {
        LocalDate snapshotDate = LocalDate.now();
        log.info("Taking daily inventory snapshot for {}", snapshotDate);

        List<ReportDataGateway.ProductView> products = reportDataGateway.fetchProducts().stream()
                .filter(product -> !Boolean.FALSE.equals(product.isActive()))
                .toList();

        Map<Long, ReportDataGateway.ProductView> productsById = products.stream()
                .collect(Collectors.toMap(
                        ReportDataGateway.ProductView::productId,
                        product -> product,
                        (existing, duplicate) -> existing
                ));

        List<InventorySnapshot> savedSnapshots = new ArrayList<>();

        for (ReportDataGateway.WarehouseView warehouse : reportDataGateway.fetchWarehouses()) {
            if (Boolean.FALSE.equals(warehouse.isActive())) {
                continue;
            }

            List<ReportDataGateway.StockLevelView> stockLevels =
                    reportDataGateway.fetchStockByWarehouse(warehouse.warehouseId());

            for (ReportDataGateway.StockLevelView stockLevel : stockLevels) {
                ReportDataGateway.ProductView product = productsById.get(stockLevel.productId());

                if (product == null || product.costPrice() == null) {
                    continue;
                }

                int availableQuantity = valueOrZero(stockLevel.availableQuantity());

                BigDecimal stockValue = product.costPrice()
                        .multiply(BigDecimal.valueOf(Math.max(availableQuantity, 0)));

                InventorySnapshot snapshot = snapshotRepository
                        .findByWarehouseIdAndProductIdAndSnapshotDate(
                                warehouse.warehouseId(),
                                stockLevel.productId(),
                                snapshotDate
                        )
                        .orElseGet(() -> InventorySnapshot.builder()
                                .warehouseId(warehouse.warehouseId())
                                .productId(stockLevel.productId())
                                .snapshotDate(snapshotDate)
                                .build());

                snapshot.setQuantity(availableQuantity);
                snapshot.setStockValue(stockValue);

                savedSnapshots.add(saveSnapshot(snapshot));
            }
        }

        log.info("Daily inventory snapshot complete. {} records stored", savedSnapshots.size());

        return savedSnapshots.stream()
                .map(snapshotMapper::toDto)
                .toList();
    }

    public List<InventorySnapshotDTO> getSnapshotsByDate(LocalDate date) {
        return snapshotRepository.findBySnapshotDate(date).stream()
                .map(snapshotMapper::toDto)
                .toList();
    }

    public List<InventorySnapshotDTO> getSnapshotsByWarehouse(Long warehouseId) {
        return snapshotRepository.findByWarehouseId(warehouseId).stream()
                .sorted(Comparator.comparing(InventorySnapshot::getSnapshotDate).reversed())
                .map(snapshotMapper::toDto)
                .toList();
    }

    public List<InventorySnapshotDTO> getSnapshotsByDateRange(LocalDate start, LocalDate end) {
        validateDateRange(start, end);

        return snapshotRepository.findBySnapshotDateBetween(start, end).stream()
                .sorted(Comparator.comparing(InventorySnapshot::getSnapshotDate).reversed())
                .map(snapshotMapper::toDto)
                .toList();
    }

    public List<InventorySnapshotDTO> getLatestSnapshot() {
        LocalDate latestSnapshotDate = snapshotRepository.findLatestSnapshotDate();

        if (latestSnapshotDate == null) {
            log.warn("No inventory snapshots available. Returning empty latest snapshot list.");
            return List.of();
        }

        return snapshotRepository.findBySnapshotDateOrderByWarehouseIdAscProductIdAsc(latestSnapshotDate).stream()
                .map(snapshotMapper::toDto)
                .toList();
    }

    public StockValuationDTO getTotalStockValue() {
        LocalDate asOfDate = snapshotRepository.findLatestSnapshotDate();

        if (asOfDate == null) {
            log.warn("No inventory snapshots available. Returning zero stock valuation.");
            return StockValuationDTO.builder()
                    .totalValue(BigDecimal.ZERO)
                    .asOfDate(null)
                    .totalProducts(0)
                    .build();
        }

        List<InventorySnapshot> latestSnapshots = snapshotsForDate(asOfDate);

        BigDecimal totalValue = latestSnapshots.stream()
                .map(InventorySnapshot::getStockValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StockValuationDTO.builder()
                .totalValue(totalValue)
                .asOfDate(asOfDate)
                .totalProducts(latestSnapshots.size())
                .build();
    }

    public StockValuationDTO getStockValueByWarehouse(Long warehouseId) {
        LocalDate asOfDate = snapshotRepository.findLatestSnapshotDate();

        if (asOfDate == null) {
            log.warn("No inventory snapshots available. Returning zero warehouse stock valuation.");
            return StockValuationDTO.builder()
                    .warehouseId(warehouseId)
                    .totalValue(BigDecimal.ZERO)
                    .asOfDate(null)
                    .totalProducts(0)
                    .build();
        }

        List<InventorySnapshot> warehouseSnapshots =
                snapshotRepository.findByWarehouseIdAndSnapshotDate(warehouseId, asOfDate);

        BigDecimal totalValue = warehouseSnapshots.stream()
                .map(InventorySnapshot::getStockValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StockValuationDTO.builder()
                .warehouseId(warehouseId)
                .totalValue(totalValue)
                .asOfDate(asOfDate)
                .totalProducts(warehouseSnapshots.size())
                .build();
    }

    public Map<String, Object> getInventoryTurnover(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        log.info("Generating inventory turnover report from {} to {}", startDate, endDate);

        List<ReportDataGateway.StockMovementView> movements =
                reportDataGateway.fetchMovements(startDate, endDate);

        BigDecimal cogs = movements.stream()
                .filter(movement -> "STOCK_OUT".equalsIgnoreCase(movement.movementType()))
                .map(movement -> {
                    BigDecimal unitCost = movement.unitCost() == null
                            ? BigDecimal.ZERO
                            : movement.unitCost();

                    return unitCost.multiply(
                            BigDecimal.valueOf(Math.abs(valueOrZero(movement.quantity())))
                    );
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<InventorySnapshot> snapshotsInRange =
                snapshotRepository.findBySnapshotDateBetween(startDate, endDate);

        Map<LocalDate, BigDecimal> valueByDate = snapshotsInRange.stream()
                .collect(Collectors.groupingBy(
                        InventorySnapshot::getSnapshotDate,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                snapshot -> snapshot.getStockValue() == null
                                        ? BigDecimal.ZERO
                                        : snapshot.getStockValue(),
                                BigDecimal::add
                        )
                ));

        BigDecimal averageInventory = valueByDate.isEmpty()
                ? BigDecimal.ZERO
                : valueByDate.values().stream()
                  .reduce(BigDecimal.ZERO, BigDecimal::add)
                  .divide(BigDecimal.valueOf(valueByDate.size()), 4, RoundingMode.HALF_UP);

        BigDecimal turnover = averageInventory.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : cogs.divide(averageInventory, 4, RoundingMode.HALF_UP);

        return Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "cogs", cogs,
                "averageInventoryValue", averageInventory,
                "inventoryTurnover", turnover
        );
    }

    public List<InventorySnapshotDTO> getLowStockReport(Integer threshold) {
        log.info("Generating low stock report");

        Map<Long, ReportDataGateway.ProductView> productById =
                reportDataGateway.fetchProducts().stream()
                        .collect(Collectors.toMap(
                                ReportDataGateway.ProductView::productId,
                                product -> product,
                                (existing, duplicate) -> existing
                        ));

        List<InventorySnapshotDTO> report = new ArrayList<>();

        List<ReportDataGateway.StockLevelView> lowStockLevels =
                reportDataGateway.fetchLowStockItems(threshold != null ? threshold : 10);

        LocalDate snapshotDate = LocalDate.now();

        for (ReportDataGateway.StockLevelView stockLevel : lowStockLevels) {
            ReportDataGateway.ProductView product = productById.get(stockLevel.productId());

            if (product == null || product.costPrice() == null) {
                continue;
            }

            int availableQuantity = valueOrZero(stockLevel.availableQuantity());
            Integer reorderLevel = product.reorderLevel();

            if (reorderLevel != null && availableQuantity >= reorderLevel) {
                continue;
            }

            report.add(InventorySnapshotDTO.builder()
                    .warehouseId(stockLevel.warehouseId())
                    .productId(stockLevel.productId())
                    .quantity(availableQuantity)
                    .stockValue(product.costPrice().multiply(BigDecimal.valueOf(availableQuantity)))
                    .snapshotDate(snapshotDate)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return report;
    }

    public List<DeadStockDTO> getDeadStock(Integer days) {
        int threshold = days != null ? days : deadStockDays;
        LocalDate cutoffDate = LocalDate.now().minusDays(threshold);

        log.info("Generating dead stock report for {} day threshold", threshold);

        LocalDate latestSnapshotDate = snapshotRepository.findLatestSnapshotDate();

        if (latestSnapshotDate == null) {
            log.warn("No inventory snapshots available. Returning empty dead stock report.");
            return List.of();
        }

        List<InventorySnapshot> latestSnapshots = snapshotsForDate(latestSnapshotDate).stream()
                .filter(snapshot -> valueOrZero(snapshot.getQuantity()) > 0)
                .toList();

        if (latestSnapshots.isEmpty()) {
            return List.of();
        }

        Set<String> movedKeys = reportDataGateway.fetchMovements(cutoffDate, LocalDate.now()).stream()
                .map(movement -> movement.productId() + ":" + movement.warehouseId())
                .collect(Collectors.toSet());

        return latestSnapshots.stream()
                .filter(snapshot -> !movedKeys.contains(snapshot.getProductId() + ":" + snapshot.getWarehouseId()))
                .map(snapshot -> DeadStockDTO.builder()
                        .productId(snapshot.getProductId())
                        .warehouseId(snapshot.getWarehouseId())
                        .currentQuantity(snapshot.getQuantity())
                        .lastMovementDate(cutoffDate)
                        .daysWithoutMovement(threshold)
                        .build())
                .toList();
    }

    public POSummaryDTO getPOSummary(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        log.info("Generating PO summary from {} to {}", startDate, endDate);

        List<ReportDataGateway.PurchaseOrderView> purchaseOrders =
                reportDataGateway.fetchPurchaseOrders(startDate, endDate);

        long totalPos = purchaseOrders.size();

        BigDecimal totalSpend = purchaseOrders.stream()
                .map(po -> po.totalAmount() == null ? BigDecimal.ZERO : po.totalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> spendBySupplier = purchaseOrders.stream()
                .collect(Collectors.groupingBy(
                        ReportDataGateway.PurchaseOrderView::supplierId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                po -> po.totalAmount() == null ? BigDecimal.ZERO : po.totalAmount(),
                                BigDecimal::add
                        )
                ));

        Map<Long, BigDecimal> spendByWarehouse = purchaseOrders.stream()
                .collect(Collectors.groupingBy(
                        ReportDataGateway.PurchaseOrderView::warehouseId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                po -> po.totalAmount() == null ? BigDecimal.ZERO : po.totalAmount(),
                                BigDecimal::add
                        )
                ));

        log.info("PO summary supplier groups={}, warehouse groups={}",
                spendBySupplier.size(), spendByWarehouse.size());

        return POSummaryDTO.builder()
                .fromDate(startDate)
                .toDate(endDate)
                .totalPOs(totalPos)
                .totalSpend(totalSpend)
                .approvedPOs(countByStatus(purchaseOrders, "APPROVED"))
                .pendingPOs(countByStatus(purchaseOrders, "PENDING"))
                .cancelledPOs(countByStatus(purchaseOrders, "CANCELLED"))
                .fullyReceivedPOs(countByReceivedStatus(purchaseOrders))
                .build();
    }

    public Map<String, Object> getStockMovementSummary(
            Long warehouseId, LocalDate startDate, LocalDate endDate) {

        validateDateRange(startDate, endDate);

        log.info("Generating movement summary from {} to {} for warehouse {}",
                startDate, endDate, warehouseId);

        List<ReportDataGateway.StockMovementView> movements =
                reportDataGateway.fetchMovements(startDate, endDate);

        if (warehouseId != null) {
            movements = movements.stream()
                    .filter(movement -> warehouseId.equals(movement.warehouseId()))
                    .toList();
        }

        int stockIn = sumMovementQuantity(movements, Set.of("STOCK_IN"));
        int stockOut = sumMovementQuantity(movements, Set.of("STOCK_OUT"));
        int adjustments = sumMovementQuantity(movements, Set.of("ADJUSTMENT"));
        int transfers = sumMovementQuantity(movements, Set.of("TRANSFER_IN", "TRANSFER_OUT"));

        return Map.of(
                "warehouseId", warehouseId != null ? warehouseId : "ALL",
                "fromDate", startDate,
                "toDate", endDate,
                "totalStockIn", stockIn,
                "totalStockOut", stockOut,
                "totalAdjustments", adjustments,
                "totalTransfers", transfers
        );
    }

    public List<TopMovingProductDTO> getTopMovingProducts(Integer limit) {
        int requestedLimit = limit != null && limit > 0
                ? limit
                : DEFAULT_TOP_MOVERS_LIMIT;

        log.info("Generating top moving products report limit={}", requestedLimit);

        Map<Long, ReportDataGateway.ProductView> productsById =
                reportDataGateway.fetchProducts().stream()
                        .collect(Collectors.toMap(
                                ReportDataGateway.ProductView::productId,
                                product -> product,
                                (existing, duplicate) -> existing
                        ));

        Map<String, MovementAggregate> aggregateByProductWarehouse =
                aggregateMovements(reportDataGateway.fetchMovements(
                        LocalDate.now().minusDays(slowMovingDays),
                        LocalDate.now()
                ));

        return aggregateByProductWarehouse.values().stream()
                .sorted(Comparator.comparing(MovementAggregate::totalMovement).reversed())
                .limit(requestedLimit)
                .map(aggregate -> toTopMovingDto(
                        aggregate,
                        productsById.get(aggregate.productId())
                ))
                .toList();
    }

    public List<TopMovingProductDTO> getSlowMovingProducts(Integer days) {
        int threshold = days != null ? days : slowMovingDays;

        log.info("Generating slow moving products report days={}", threshold);

        Map<Long, ReportDataGateway.ProductView> productsById =
                reportDataGateway.fetchProducts().stream()
                        .collect(Collectors.toMap(
                                ReportDataGateway.ProductView::productId,
                                product -> product,
                                (existing, duplicate) -> existing
                        ));

        Map<String, MovementAggregate> aggregates =
                aggregateMovements(reportDataGateway.fetchMovements(
                        LocalDate.now().minusDays(threshold),
                        LocalDate.now()
                ));

        List<TopMovingProductDTO> result = new ArrayList<>(
                aggregates.values().stream()
                        .sorted(Comparator.comparing(MovementAggregate::totalMovement))
                        .map(aggregate -> toTopMovingDto(
                                aggregate,
                                productsById.get(aggregate.productId())
                        ))
                        .toList()
        );

        LocalDate latestSnapshotDate = snapshotRepository.findLatestSnapshotDate();

        if (latestSnapshotDate == null) {
            log.warn("No inventory snapshots available. Returning slow moving report using movement data only.");
            return result;
        }

        Set<String> seenKeys = new HashSet<>(aggregates.keySet());

        for (InventorySnapshot snapshot : snapshotsForDate(latestSnapshotDate)) {
            String key = movementKey(snapshot.getProductId(), snapshot.getWarehouseId());

            if (seenKeys.contains(key)) {
                continue;
            }

            ReportDataGateway.ProductView productView = productsById.get(snapshot.getProductId());

            result.add(TopMovingProductDTO.builder()
                    .productId(snapshot.getProductId())
                    .productName(productView != null ? productView.name() : null)
                    .warehouseId(snapshot.getWarehouseId())
                    .totalUnitsIn(0)
                    .totalUnitsOut(0)
                    .totalMovement(0)
                    .build());
        }

        return result;
    }

    public String exportReport(String type) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase();

        return switch (normalizedType) {
            case "valuation" -> exportValuationReport();
            case "movement" -> exportMovementSummary();
            default -> throw new IllegalArgumentException("Unsupported export type: " + type);
        };
    }

    private String exportValuationReport() {
        StockValuationDTO valuation = getTotalStockValue();

        return String.join("\n",
                "reportType,totalValue,asOfDate,totalProducts",
                String.format("valuation,%s,%s,%s",
                        valuation.getTotalValue(),
                        valuation.getAsOfDate(),
                        valuation.getTotalProducts()));
    }

    private String exportMovementSummary() {
        Map<String, Object> summary = getStockMovementSummary(
                null,
                LocalDate.now().minusDays(slowMovingDays),
                LocalDate.now()
        );

        return String.join("\n",
                "reportType,warehouseId,fromDate,toDate,generatedAt,totalStockIn,totalStockOut,totalAdjustments,totalTransfers",
                String.format("movement,%s,%s,%s,%s,%s,%s,%s,%s",
                        summary.get("warehouseId"),
                        summary.get("fromDate"),
                        summary.get("toDate"),
                        LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                        summary.get("totalStockIn"),
                        summary.get("totalStockOut"),
                        summary.get("totalAdjustments"),
                        summary.get("totalTransfers")));
    }

    private void validateManualSnapshot(
            Long warehouseId,
            Long productId,
            Integer quantity,
            BigDecimal stockValue) {

        if (warehouseId == null || productId == null) {
            throw new IllegalArgumentException("warehouseId and productId are required");
        }

        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }

        if (stockValue == null || stockValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("stockValue cannot be negative");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    private InventorySnapshot saveSnapshot(InventorySnapshot snapshot) {
        try {
            return snapshotRepository.save(snapshot);
        } catch (DataIntegrityViolationException ex) {
            throw new ReportGenerationException("Failed to persist inventory snapshot", ex);
        }
    }

    private List<InventorySnapshot> snapshotsForDate(LocalDate date) {
        if (date == null) {
            return List.of();
        }

        return snapshotRepository.findBySnapshotDateOrderByWarehouseIdAscProductIdAsc(date);
    }

    private long countByStatus(
            List<ReportDataGateway.PurchaseOrderView> purchaseOrders,
            String status) {

        return purchaseOrders.stream()
                .filter(po -> status.equalsIgnoreCase(po.status()))
                .count();
    }

    private long countByReceivedStatus(List<ReportDataGateway.PurchaseOrderView> purchaseOrders) {
        return purchaseOrders.stream()
                .filter(po -> "RECEIVED".equalsIgnoreCase(po.status())
                        || "FULLY_RECEIVED".equalsIgnoreCase(po.status()))
                .count();
    }

    private int sumMovementQuantity(
            List<ReportDataGateway.StockMovementView> movements,
            Set<String> movementTypes) {

        return movements.stream()
                .filter(movement -> movement.movementType() != null)
                .filter(movement -> movementTypes.contains(movement.movementType().toUpperCase()))
                .mapToInt(movement -> Math.abs(valueOrZero(movement.quantity())))
                .sum();
    }

    private Map<String, MovementAggregate> aggregateMovements(
            List<ReportDataGateway.StockMovementView> movements) {

        Map<String, MovementAggregate> aggregates = new HashMap<>();

        for (ReportDataGateway.StockMovementView movement : movements) {
            String key = movementKey(movement.productId(), movement.warehouseId());

            MovementAggregate aggregate = aggregates.getOrDefault(
                    key,
                    new MovementAggregate(movement.productId(), movement.warehouseId(), 0, 0)
            );

            int quantity = Math.abs(valueOrZero(movement.quantity()));

            if (isInboundMovement(movement.movementType(), movement.referenceType(), movement.quantity())) {
                aggregate = aggregate.withInbound(aggregate.totalUnitsIn + quantity);
            } else {
                aggregate = aggregate.withOutbound(aggregate.totalUnitsOut + quantity);
            }

            aggregates.put(key, aggregate);
        }

        return aggregates;
    }

    private boolean isInboundMovement(String movementType, String referenceType, Integer quantity) {
        if (movementType == null) {
            return false;
        }

        if ("RETURN".equalsIgnoreCase(movementType)) {
            return !"SUPPLIER_RETURN".equalsIgnoreCase(referenceType);
        }

        if ("ADJUSTMENT".equalsIgnoreCase(movementType)) {
            return valueOrZero(quantity) >= 0;
        }

        return Set.of("STOCK_IN", "TRANSFER_IN").contains(movementType.toUpperCase());
    }

    private TopMovingProductDTO toTopMovingDto(
            MovementAggregate aggregate,
            ReportDataGateway.ProductView productView) {

        return TopMovingProductDTO.builder()
                .productId(aggregate.productId())
                .productName(productView != null ? productView.name() : null)
                .warehouseId(aggregate.warehouseId())
                .totalUnitsIn(aggregate.totalUnitsIn())
                .totalUnitsOut(aggregate.totalUnitsOut())
                .totalMovement(aggregate.totalMovement())
                .build();
    }

    private String movementKey(Long productId, Long warehouseId) {
        return productId + ":" + warehouseId;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record MovementAggregate(
            Long productId,
            Long warehouseId,
            int totalUnitsIn,
            int totalUnitsOut
    ) {
        private int totalMovement() {
            return totalUnitsIn + totalUnitsOut;
        }

        private MovementAggregate withInbound(int inbound) {
            return new MovementAggregate(productId, warehouseId, inbound, totalUnitsOut);
        }

        private MovementAggregate withOutbound(int outbound) {
            return new MovementAggregate(productId, warehouseId, totalUnitsIn, outbound);
        }
    }
}