package com.stockpro.movementservice.service.impl;

import com.stockpro.movementservice.dto.request.CreateMovementFromEventRequest;
import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.MovementSearchRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse.MovementVolumeItem;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse.TrendPoint;
import com.stockpro.movementservice.dto.response.MovementResponse;
import com.stockpro.movementservice.dto.response.MovementSummaryResponse;
import com.stockpro.movementservice.entity.StockMovement;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementEventType;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import com.stockpro.movementservice.events.MovementEvent;
import com.stockpro.movementservice.exception.InvalidMovementException;
import com.stockpro.movementservice.exception.MovementNotFoundException;
import com.stockpro.movementservice.repository.StockMovementRepository;
import com.stockpro.movementservice.service.MovementEventPublisher;
import com.stockpro.movementservice.service.MovementMapper;
import com.stockpro.movementservice.service.MovementService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovementServiceImpl implements MovementService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "movementDate", "createdAt", "movementNumber", "productId", "warehouseId", "movementType", "totalValue");
    private static final Set<MovementType> REVERSIBLE_MOVEMENT_TYPES = EnumSet.of(
            MovementType.STOCK_IN,
            MovementType.STOCK_OUT,
            MovementType.TRANSFER_IN,
            MovementType.TRANSFER_OUT,
            MovementType.ADJUSTMENT,
            MovementType.WRITE_OFF,
            MovementType.RETURN,
            MovementType.CYCLE_COUNT_CORRECTION);

    private final StockMovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementEventPublisher movementEventPublisher;

    @Override
    @Transactional
    public MovementResponse createMovement(CreateMovementRequest request, Long actorId) {
        validateCreateRequest(request.movementType(), request.direction(), request.quantity(), request.unitCost(), request.balanceAfter());
        StockMovement movement = StockMovement.builder()
                .movementNumber(generateMovementNumber())
                .productId(request.productId())
                .warehouseId(request.warehouseId())
                .movementType(request.movementType())
                .direction(request.direction())
                .quantity(normalizeAmount(request.quantity()))
                .unitCost(normalizeMoney(request.unitCost()))
                .totalValue(calculateTotalValue(request.quantity(), request.unitCost()))
                .balanceAfter(normalizeAmount(request.balanceAfter()))
                .referenceType(request.referenceType())
                .referenceId(trimToNull(request.referenceId()))
                .referenceNumber(trimToNull(request.referenceNumber()))
                .performedBy(actorId)
                .reasonCode(request.reasonCode() != null ? request.reasonCode() : MovementReasonCode.OTHER)
                .notes(trimToNull(request.notes()))
                .movementDate(request.movementDate() != null ? request.movementDate() : LocalDateTime.now())
                .sourceService(request.sourceService() != null ? request.sourceService() : "movement-service")
                .correlationId(trimToNull(request.correlationId()))
                .build();
        StockMovement saved = movementRepository.save(movement);
        log.info("Movement created manually movementId={} movementNumber={}", saved.getMovementId(), saved.getMovementNumber());
        publishMovementEvent(saved, MovementEventType.MOVEMENT_CREATED);
        return movementMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MovementResponse createMovementFromEvent(CreateMovementFromEventRequest request) {
        if (request.eventId() == null && request.correlationId() == null) {
            throw new InvalidMovementException("Event id or correlation id is required for event-driven movements");
        }
        if ("STOCK_TRANSFERRED".equalsIgnoreCase(request.eventType())) {
            return createTransferPair(request);
        }
        MovementType movementType = resolveMovementType(request);
        MovementDirection direction = resolveDirection(request, movementType);
        BigDecimal quantity = normalizeAmount(requiredPositiveQuantity(request.quantity()));
        if (direction == MovementDirection.NEUTRAL && quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidMovementException("Neutral movements still require a positive quantity");
        }
        String idempotencyKey = buildIdempotencyKey(request.eventId(), request.correlationId(), movementType.name(), request.warehouseId());
        if (idempotencyKey != null) {
            var existing = movementRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate movement event skipped idempotencyKey={}", idempotencyKey);
                return movementMapper.toResponse(existing.get());
            }
        }
        validateCreateRequest(movementType, direction, quantity, request.unitCost(), request.balanceAfter());
        StockMovement saved = movementRepository.save(StockMovement.builder()
                .movementNumber(generateMovementNumber())
                .productId(request.productId())
                .productSku(trimToNull(request.productSku()))
                .productName(trimToNull(request.productName()))
                .warehouseId(request.warehouseId())
                .warehouseCode(trimToNull(request.warehouseCode()))
                .warehouseName(trimToNull(request.warehouseName()))
                .movementType(movementType)
                .direction(direction)
                .quantity(quantity)
                .unitCost(normalizeMoney(request.unitCost()))
                .totalValue(calculateTotalValue(quantity, request.unitCost()))
                .balanceAfter(normalizeAmount(request.balanceAfter()))
                .referenceType(request.referenceType() != null ? request.referenceType() : ReferenceType.SYSTEM)
                .referenceId(trimToNull(request.referenceId()))
                .referenceNumber(trimToNull(request.referenceNumber()))
                .performedBy(request.performedBy())
                .performedByName(trimToNull(request.performedByName()))
                .reasonCode(request.reasonCode() != null ? request.reasonCode() : MovementReasonCode.OTHER)
                .notes(trimToNull(request.notes()))
                .movementDate(request.eventTime() != null ? request.eventTime() : LocalDateTime.now())
                .sourceService(request.sourceService() != null ? request.sourceService() : "warehouse-service")
                .correlationId(trimToNull(request.correlationId()))
                .sourceEventId(trimToNull(request.eventId()))
                .idempotencyKey(idempotencyKey)
                .build());
        log.info("Movement created from event movementId={} eventId={} eventType={}",
                saved.getMovementId(), request.eventId(), request.eventType());
        publishMovementEvent(saved, MovementEventType.MOVEMENT_CREATED);
        return movementMapper.toResponse(saved);
    }

    @Override
    public MovementResponse getMovementById(Long movementId) {
        return movementMapper.toResponse(getEntity(movementId));
    }

    @Override
    public MovementResponse getMovementByNumber(String movementNumber) {
        return movementMapper.toResponse(movementRepository.findByMovementNumber(movementNumber)
                .orElseThrow(() -> new MovementNotFoundException("Movement not found with number: " + movementNumber)));
    }

    @Override
    public Page<MovementResponse> getAllMovements(int page, int size, String sortBy, String sortDir) {
        return movementRepository.findAll(pageable(page, size, sortBy, sortDir)).map(movementMapper::toResponse);
    }

    @Override
    public Page<MovementResponse> searchMovements(MovementSearchRequest request) {
        Pageable pageable = pageable(
                valueOrDefault(request.page(), 0),
                valueOrDefault(request.size(), 10),
                request.sortBy(),
                request.sortDir());
        return movementRepository.findAll(buildSpecification(request), pageable).map(movementMapper::toResponse);
    }

    @Override
    public Page<MovementResponse> getMovementsByProduct(Long productId, int page, int size) {
        return movementRepository.findByProductId(productId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "movementDate")))
                .map(movementMapper::toResponse);
    }

    @Override
    public Page<MovementResponse> getMovementsByWarehouse(Long warehouseId, int page, int size) {
        return movementRepository.findByWarehouseId(warehouseId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "movementDate")))
                .map(movementMapper::toResponse);
    }

    @Override
    public Page<MovementResponse> getMovementsByReference(String referenceType, String referenceId, int page, int size) {
        return movementRepository.findByReferenceTypeAndReferenceId(
                        parseReferenceType(referenceType),
                        referenceId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "movementDate")))
                .map(movementMapper::toResponse);
    }

    @Override
    public Page<MovementResponse> getMovementsByUser(Long userId, int page, int size) {
        return movementRepository.findByPerformedBy(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "movementDate")))
                .map(movementMapper::toResponse);
    }

    @Override
    @Transactional
    public MovementResponse reverseMovement(Long movementId, ReverseMovementRequest request, Long actorId) {
        if (request.reasonCode() == null) {
            throw new InvalidMovementException("Reversal reason is required");
        }
        StockMovement original = getEntity(movementId);
        if (Boolean.TRUE.equals(original.getIsReversal())) {
            throw new InvalidMovementException("Cannot reverse an already reversed movement");
        }
        if (movementRepository.existsByRelatedMovementId(movementId)) {
            throw new InvalidMovementException("Movement has already been reversed");
        }
        if (!REVERSIBLE_MOVEMENT_TYPES.contains(original.getMovementType())) {
            throw new InvalidMovementException("This movement type cannot be reversed");
        }
        MovementDirection reverseDirection = oppositeDirection(original.getDirection());
        BigDecimal reverseBalance = calculateReverseBalance(original);
        MovementType reversalMovementType = resolveReversalMovementType();
        log.info(
                "Creating reversal movement movementId={} originalMovementType={} reversalMovementType={} direction={} quantity={} reasonCode={}",
                original.getMovementId(),
                original.getMovementType(),
                reversalMovementType,
                reverseDirection,
                original.getQuantity(),
                request.reasonCode());
        StockMovement reversal = movementRepository.save(StockMovement.builder()
                .movementNumber(generateMovementNumber())
                .productId(original.getProductId())
                .productSku(original.getProductSku())
                .productName(original.getProductName())
                .warehouseId(original.getWarehouseId())
                .warehouseCode(original.getWarehouseCode())
                .warehouseName(original.getWarehouseName())
                .movementType(reversalMovementType)
                .direction(reverseDirection)
                .quantity(original.getQuantity())
                .unitCost(original.getUnitCost())
                .totalValue(original.getTotalValue())
                .balanceAfter(reverseBalance)
                .referenceType(ReferenceType.MANUAL_CORRECTION)
                .referenceId(String.valueOf(original.getMovementId()))
                .referenceNumber(original.getMovementNumber())
                .performedBy(actorId)
                .performedByName(null)
                .reasonCode(request.reasonCode())
                .notes(trimToNull(request.notes()))
                .relatedMovementId(original.getMovementId())
                .isReversal(Boolean.TRUE)
                .movementDate(LocalDateTime.now())
                .sourceService("movement-service")
                .correlationId(UUID.randomUUID().toString())
                .build());
        log.info("Movement reversal created originalMovementId={} reversalMovementId={}",
                original.getMovementId(), reversal.getMovementId());
        publishMovementEvent(reversal, MovementEventType.MOVEMENT_REVERSED);
        return movementMapper.toResponse(reversal);
    }

    @Override
    public MovementSummaryResponse getMovementSummary(LocalDateTime fromDate, LocalDateTime toDate) {
        List<StockMovement> movements = findInRange(fromDate, toDate);
        BigDecimal totalStockIn = sumQuantity(movements, Set.of(MovementType.STOCK_IN));
        BigDecimal totalStockOut = sumQuantity(movements, Set.of(MovementType.STOCK_OUT, MovementType.WRITE_OFF));
        BigDecimal totalTransfer = sumQuantity(movements, Set.of(MovementType.TRANSFER_IN, MovementType.TRANSFER_OUT));
        BigDecimal totalAdjustment = sumQuantity(movements, Set.of(MovementType.ADJUSTMENT, MovementType.CYCLE_COUNT_CORRECTION));
        BigDecimal totalWriteOff = sumQuantity(movements, Set.of(MovementType.WRITE_OFF));
        BigDecimal totalReturn = sumQuantity(movements, Set.of(MovementType.RETURN));
        BigDecimal totalValue = movements.stream().map(StockMovement::getTotalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.now();
        long movementsToday = movements.stream().filter(m -> m.getMovementDate().toLocalDate().isEqual(today)).count();
        long movementsThisMonth = movements.stream().filter(m -> YearMonth.from(m.getMovementDate()).equals(month)).count();
        return new MovementSummaryResponse(
                movements.size(),
                totalStockIn,
                totalStockOut,
                totalTransfer,
                totalAdjustment,
                totalWriteOff,
                totalReturn,
                totalValue,
                movementsToday,
                movementsThisMonth);
    }

    @Override
    public List<MovementResponse> getRecentMovements(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        return movementRepository.findRecentMovements(safeLimit).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    @Override
    public MovementAnalyticsResponse getMovementAnalytics(LocalDateTime fromDate, LocalDateTime toDate) {
        List<StockMovement> movements = findInRange(fromDate, toDate);
        Map<String, Long> byType = new HashMap<>();
        Map<String, Long> byWarehouse = new HashMap<>();
        Map<String, Long> byProduct = new HashMap<>();
        Map<String, BigDecimal> dailyTrend = new HashMap<>();
        Map<Long, MovementVolumeItem> volumeByProduct = new HashMap<>();
        List<TrendPoint> adjustmentTrend = buildTrend(movements, EnumSet.of(MovementType.ADJUSTMENT, MovementType.CYCLE_COUNT_CORRECTION));
        List<TrendPoint> writeOffTrend = buildTrend(movements, EnumSet.of(MovementType.WRITE_OFF));

        for (StockMovement movement : movements) {
            byType.merge(movement.getMovementType().name(), 1L, Long::sum);
            byWarehouse.merge(warehouseLabel(movement), 1L, Long::sum);
            byProduct.merge(productLabel(movement), 1L, Long::sum);
            String dateKey = movement.getMovementDate().toLocalDate().format(DateTimeFormatter.ISO_DATE);
            dailyTrend.merge(dateKey, movement.getQuantity(), BigDecimal::add);
            MovementVolumeItem current = volumeByProduct.get(movement.getProductId());
            if (current == null) {
                volumeByProduct.put(movement.getProductId(), new MovementVolumeItem(
                        movement.getProductId(),
                        productLabel(movement),
                        movement.getQuantity(),
                        movement.getTotalValue()));
            } else {
                volumeByProduct.put(movement.getProductId(), new MovementVolumeItem(
                        current.productId(),
                        current.productName(),
                        current.quantity().add(movement.getQuantity()),
                        current.totalValue().add(movement.getTotalValue())));
            }
        }

        List<MovementVolumeItem> topMovedProducts = volumeByProduct.values().stream()
                .sorted(Comparator.comparing(MovementVolumeItem::quantity).reversed())
                .limit(5)
                .toList();
        List<MovementResponse> highestValue = movements.stream()
                .sorted(Comparator.comparing(StockMovement::getTotalValue).reversed())
                .limit(5)
                .map(movementMapper::toResponse)
                .toList();

        return new MovementAnalyticsResponse(byType, byWarehouse, byProduct, dailyTrend, topMovedProducts, highestValue, adjustmentTrend, writeOffTrend);
    }

    @Override
    public byte[] exportMovementsToCsv(MovementSearchRequest request) {
        Page<MovementResponse> page = searchMovements(MovementSearchRequest.builder()
                .keyword(request.keyword())
                .productId(request.productId())
                .warehouseId(request.warehouseId())
                .movementType(request.movementType())
                .direction(request.direction())
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .performedBy(request.performedBy())
                .fromDate(request.fromDate())
                .toDate(request.toDate())
                .minQuantity(request.minQuantity())
                .maxQuantity(request.maxQuantity())
                .sourceService(request.sourceService())
                .correlationId(request.correlationId())
                .isReversal(request.isReversal())
                .page(0)
                .size(Math.max(valueOrDefault(request.size(), 500), 500))
                .sortBy(request.sortBy())
                .sortDir(request.sortDir())
                .build());
        StringBuilder builder = new StringBuilder();
        builder.append("Movement Number,Date,Product,Warehouse,Type,Direction,Quantity,Unit Cost,Total Value,Reference,Performed By,Reason,Source")
                .append(System.lineSeparator());
        for (MovementResponse movement : page.getContent()) {
            builder.append(csv(movement.movementNumber())).append(',')
                    .append(csv(String.valueOf(movement.movementDate()))).append(',')
                    .append(csv(defaultLabel(movement.productName(), movement.productId(), "Product"))).append(',')
                    .append(csv(defaultLabel(movement.warehouseName(), movement.warehouseId(), "Warehouse"))).append(',')
                    .append(csv(movement.movementType().name())).append(',')
                    .append(csv(movement.direction().name())).append(',')
                    .append(csv(movement.quantity().toPlainString())).append(',')
                    .append(csv(movement.unitCost().toPlainString())).append(',')
                    .append(csv(movement.totalValue().toPlainString())).append(',')
                    .append(csv(movement.referenceNumber() != null ? movement.referenceNumber() : movement.referenceId())).append(',')
                    .append(csv(String.valueOf(movement.performedBy()))).append(',')
                    .append(csv(movement.reasonCode() != null ? movement.reasonCode().name() : "")).append(',')
                    .append(csv(movement.sourceService()))
                    .append(System.lineSeparator());
        }
        log.info("Movement CSV export completed rows={}", page.getNumberOfElements());
        return builder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private MovementResponse createTransferPair(CreateMovementFromEventRequest request) {
        if (request.sourceWarehouseId() == null || request.destinationWarehouseId() == null) {
            throw new InvalidMovementException("Transfer movement requires source and destination warehouses");
        }
        BigDecimal quantity = normalizeAmount(requiredPositiveQuantity(request.quantity()));
        String baseKey = request.eventId() != null ? request.eventId() : request.correlationId();
        StockMovement transferOut = createEventMovementIfAbsent(
                request,
                request.sourceWarehouseId(),
                MovementType.TRANSFER_OUT,
                MovementDirection.OUT,
                quantity,
                request.balanceAfter(),
                buildIdempotencyKey(baseKey, request.correlationId(), "TRANSFER_OUT", request.sourceWarehouseId()));
        createEventMovementIfAbsent(
                request,
                request.destinationWarehouseId(),
                MovementType.TRANSFER_IN,
                MovementDirection.IN,
                quantity,
                request.balanceAfter(),
                buildIdempotencyKey(baseKey, request.correlationId(), "TRANSFER_IN", request.destinationWarehouseId()));
        return movementMapper.toResponse(transferOut);
    }

    private StockMovement createEventMovementIfAbsent(
            CreateMovementFromEventRequest request,
            Long warehouseId,
            MovementType movementType,
            MovementDirection direction,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            String idempotencyKey) {
        var existing = movementRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate transfer event skipped idempotencyKey={}", idempotencyKey);
            return existing.get();
        }
        BigDecimal effectiveBalance = normalizeAmount(balanceAfter != null ? balanceAfter : quantity);
        validateCreateRequest(movementType, direction, quantity, request.unitCost(), effectiveBalance);
        StockMovement saved = movementRepository.save(StockMovement.builder()
                .movementNumber(generateMovementNumber())
                .productId(request.productId())
                .productSku(trimToNull(request.productSku()))
                .productName(trimToNull(request.productName()))
                .warehouseId(warehouseId)
                .warehouseCode(trimToNull(request.warehouseCode()))
                .warehouseName(trimToNull(request.warehouseName()))
                .movementType(movementType)
                .direction(direction)
                .quantity(quantity)
                .unitCost(normalizeMoney(request.unitCost()))
                .totalValue(calculateTotalValue(quantity, request.unitCost()))
                .balanceAfter(effectiveBalance)
                .referenceType(request.referenceType() != null ? request.referenceType() : ReferenceType.TRANSFER)
                .referenceId(trimToNull(request.referenceId()))
                .referenceNumber(trimToNull(request.referenceNumber()))
                .performedBy(request.performedBy())
                .performedByName(trimToNull(request.performedByName()))
                .reasonCode(request.reasonCode() != null ? request.reasonCode() : direction == MovementDirection.OUT
                        ? MovementReasonCode.TRANSFER_OUT
                        : MovementReasonCode.TRANSFER_IN)
                .notes(trimToNull(request.notes()))
                .movementDate(request.eventTime() != null ? request.eventTime() : LocalDateTime.now())
                .sourceService(request.sourceService() != null ? request.sourceService() : "warehouse-service")
                .correlationId(trimToNull(request.correlationId()))
                .sourceEventId(trimToNull(request.eventId()))
                .idempotencyKey(idempotencyKey)
                .build());
        publishMovementEvent(saved, MovementEventType.MOVEMENT_CREATED);
        return saved;
    }

    private void publishMovementEvent(StockMovement movement, MovementEventType eventType) {
        MovementEvent event = new MovementEvent(
                UUID.randomUUID().toString(),
                eventType,
                movement.getMovementId(),
                movement.getMovementNumber(),
                movement.getProductId(),
                movement.getWarehouseId(),
                movement.getMovementType(),
                movement.getDirection(),
                movement.getQuantity(),
                movement.getUnitCost(),
                movement.getTotalValue(),
                movement.getBalanceAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getPerformedBy(),
                movement.getMovementDate(),
                movement.getSourceService(),
                movement.getCorrelationId());
        movementEventPublisher.publish(routingKeyFor(movement, eventType), event);
    }

    private String routingKeyFor(StockMovement movement, MovementEventType eventType) {
        if (eventType == MovementEventType.MOVEMENT_REVERSED) {
            return "movement.reversed";
        }
        return switch (movement.getMovementType()) {
            case STOCK_IN -> "movement.stock-in";
            case STOCK_OUT -> "movement.stock-out";
            case TRANSFER_IN -> "movement.transfer-in";
            case TRANSFER_OUT -> "movement.transfer-out";
            case ADJUSTMENT, CYCLE_COUNT_CORRECTION -> "movement.adjusted";
            case WRITE_OFF -> "movement.write-off";
            case RETURN -> "movement.returned";
            default -> "movement.created";
        };
    }

    private StockMovement getEntity(Long movementId) {
        return movementRepository.findByMovementId(movementId)
                .orElseThrow(() -> new MovementNotFoundException("Movement not found with id: " + movementId));
    }

    private Pageable pageable(int page, int size, String sortBy, String sortDir) {
        String sortField = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "movementDate";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, sortField));
    }

    private Specification<StockMovement> buildSpecification(MovementSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.keyword() != null && !request.keyword().isBlank()) {
                String pattern = "%" + request.keyword().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("movementNumber")), pattern),
                        cb.like(cb.lower(root.get("productSku")), pattern),
                        cb.like(cb.lower(root.get("productName")), pattern),
                        cb.like(cb.lower(root.get("warehouseCode")), pattern),
                        cb.like(cb.lower(root.get("warehouseName")), pattern),
                        cb.like(cb.lower(root.get("referenceNumber")), pattern),
                        cb.like(cb.lower(root.get("notes")), pattern)));
            }
            if (request.productId() != null) predicates.add(cb.equal(root.get("productId"), request.productId()));
            if (request.warehouseId() != null) predicates.add(cb.equal(root.get("warehouseId"), request.warehouseId()));
            if (request.movementType() != null) predicates.add(cb.equal(root.get("movementType"), request.movementType()));
            if (request.direction() != null) predicates.add(cb.equal(root.get("direction"), request.direction()));
            if (request.referenceType() != null) predicates.add(cb.equal(root.get("referenceType"), request.referenceType()));
            if (request.referenceId() != null && !request.referenceId().isBlank()) predicates.add(cb.equal(root.get("referenceId"), request.referenceId()));
            if (request.performedBy() != null) predicates.add(cb.equal(root.get("performedBy"), request.performedBy()));
            if (request.fromDate() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("movementDate"), request.fromDate()));
            if (request.toDate() != null) predicates.add(cb.lessThanOrEqualTo(root.get("movementDate"), request.toDate()));
            if (request.minQuantity() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("quantity"), request.minQuantity()));
            if (request.maxQuantity() != null) predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), request.maxQuantity()));
            if (request.sourceService() != null && !request.sourceService().isBlank()) predicates.add(cb.equal(root.get("sourceService"), request.sourceService()));
            if (request.correlationId() != null && !request.correlationId().isBlank()) predicates.add(cb.equal(root.get("correlationId"), request.correlationId()));
            if (request.isReversal() != null) predicates.add(cb.equal(root.get("isReversal"), request.isReversal()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateCreateRequest(MovementType movementType, MovementDirection direction, BigDecimal quantity, BigDecimal unitCost, BigDecimal balanceAfter) {
        requiredPositiveQuantity(quantity);
        if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Invalid unit cost for movement type={}", movementType);
            throw new InvalidMovementException("Unit cost cannot be negative");
        }
        if (balanceAfter == null || balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMovementException("Balance after cannot be negative");
        }
        if (!isValidCombination(movementType, direction)) {
            log.warn("Invalid movement type/direction type={} direction={}", movementType, direction);
            throw new InvalidMovementException("Invalid movement type or direction");
        }
    }

    private boolean isValidCombination(MovementType movementType, MovementDirection direction) {
        return switch (movementType) {
            case STOCK_IN, TRANSFER_IN -> direction == MovementDirection.IN;
            case STOCK_OUT, TRANSFER_OUT, WRITE_OFF -> direction == MovementDirection.OUT;
            case ADJUSTMENT, RETURN, CYCLE_COUNT_CORRECTION -> direction == MovementDirection.IN
                    || direction == MovementDirection.OUT
                    || direction == MovementDirection.NEUTRAL;
            case RESERVATION, RESERVATION_RELEASE -> direction == MovementDirection.NEUTRAL;
            case REVERSAL -> direction == MovementDirection.IN
                    || direction == MovementDirection.OUT
                    || direction == MovementDirection.NEUTRAL;
        };
    }

    private MovementType resolveMovementType(CreateMovementFromEventRequest request) {
        if (request.movementType() != null) {
            return request.movementType();
        }
        return switch (request.eventType() == null ? "" : request.eventType().toUpperCase(Locale.ROOT)) {
            case "STOCK_RECEIVED" -> MovementType.STOCK_IN;
            case "STOCK_ISSUED" -> MovementType.STOCK_OUT;
            case "STOCK_TRANSFERRED" -> MovementType.TRANSFER_OUT;
            case "STOCK_ADJUSTED" -> MovementType.ADJUSTMENT;
            case "STOCK_RESERVED" -> MovementType.RESERVATION;
            case "STOCK_RESERVATION_RELEASED" -> MovementType.RESERVATION_RELEASE;
            default -> throw new InvalidMovementException("Unsupported stock event type: " + request.eventType());
        };
    }

    private MovementDirection resolveDirection(CreateMovementFromEventRequest request, MovementType movementType) {
        if (request.direction() != null) {
            return request.direction();
        }
        return switch (movementType) {
            case STOCK_IN, TRANSFER_IN -> MovementDirection.IN;
            case STOCK_OUT, TRANSFER_OUT, WRITE_OFF -> MovementDirection.OUT;
            case RESERVATION, RESERVATION_RELEASE -> MovementDirection.NEUTRAL;
            case ADJUSTMENT, RETURN, CYCLE_COUNT_CORRECTION -> {
                BigDecimal quantity = request.quantity() != null ? request.quantity() : BigDecimal.ZERO;
                if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                    yield MovementDirection.IN;
                }
                if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                    yield MovementDirection.OUT;
                }
                yield MovementDirection.NEUTRAL;
            }
            case REVERSAL -> MovementDirection.NEUTRAL;
        };
    }

    private MovementDirection oppositeDirection(MovementDirection direction) {
        return switch (direction) {
            case IN -> MovementDirection.OUT;
            case OUT -> MovementDirection.IN;
            case NEUTRAL -> MovementDirection.NEUTRAL;
        };
    }

    private MovementType resolveReversalMovementType() {
        // Use ADJUSTMENT for reversal persistence so older MySQL enum columns do not reject the write.
        return MovementType.ADJUSTMENT;
    }

    private BigDecimal calculateReverseBalance(StockMovement original) {
        BigDecimal result = switch (original.getDirection()) {
            case IN -> original.getBalanceAfter().subtract(original.getQuantity());
            case OUT -> original.getBalanceAfter().add(original.getQuantity());
            case NEUTRAL -> original.getBalanceAfter();
        };
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMovementException("Reversal would result in a negative balance");
        }
        return result.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal sumQuantity(List<StockMovement> movements, Set<MovementType> types) {
        return movements.stream()
                .filter(m -> types.contains(m.getMovementType()))
                .map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<StockMovement> findInRange(LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate == null && toDate == null) {
            return movementRepository.findAll();
        }
        LocalDateTime start = fromDate != null ? fromDate : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = toDate != null ? toDate : LocalDateTime.now();
        return movementRepository.findByMovementDateBetween(start, end);
    }

    private List<TrendPoint> buildTrend(List<StockMovement> movements, Set<MovementType> types) {
        Map<String, BigDecimal> grouped = new HashMap<>();
        for (StockMovement movement : movements) {
            if (!types.contains(movement.getMovementType())) {
                continue;
            }
            String key = movement.getMovementDate().toLocalDate().format(DateTimeFormatter.ISO_DATE);
            grouped.merge(key, movement.getQuantity(), BigDecimal::add);
        }
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TrendPoint(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String generateMovementNumber() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
        long sequence = movementRepository.countByCreatedAtBetween(start, end) + 1;
        String prefix = "MOV-" + today.format(DateTimeFormatter.BASIC_ISO_DATE);
        String movementNumber = prefix + "-" + String.format("%06d", sequence);
        while (movementRepository.existsByMovementNumber(movementNumber)) {
            sequence++;
            movementNumber = prefix + "-" + String.format("%06d", sequence);
        }
        return movementNumber;
    }

    private BigDecimal calculateTotalValue(BigDecimal quantity, BigDecimal unitCost) {
        return normalizeAmount(quantity).multiply(normalizeMoney(unitCost)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP) : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal requiredPositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMovementException("Quantity must be positive");
        }
        return quantity.abs();
    }

    private String buildIdempotencyKey(String eventId, String correlationId, String action, Long warehouseId) {
        String base = trimToNull(eventId) != null ? trimToNull(eventId) : trimToNull(correlationId);
        if (base == null) {
            return null;
        }
        return base + ":" + action + ":" + warehouseId;
    }

    private String warehouseLabel(StockMovement movement) {
        return defaultLabel(movement.getWarehouseName(), movement.getWarehouseId(), "Warehouse");
    }

    private String productLabel(StockMovement movement) {
        return movement.getProductName() != null && !movement.getProductName().isBlank()
                ? movement.getProductName()
                : "Deleted/Unavailable Product";
    }

    private String defaultLabel(String value, Long id, String prefix) {
        return value != null && !value.isBlank() ? value : prefix + " #" + id;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private ReferenceType parseReferenceType(String value) {
        try {
            return ReferenceType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new InvalidMovementException("Invalid reference type: " + value);
        }
    }
}
