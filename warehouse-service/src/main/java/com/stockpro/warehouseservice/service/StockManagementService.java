package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.client.ProductCatalogClient;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.dto.request.AdjustStockRequest;
import com.stockpro.warehouseservice.dto.request.CreateStockLevelRequest;
import com.stockpro.warehouseservice.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouseservice.dto.request.ReserveStockRequest;
import com.stockpro.warehouseservice.dto.request.StockIssueRequest;
import com.stockpro.warehouseservice.dto.request.StockReceiptRequest;
import com.stockpro.warehouseservice.dto.request.TransferStockRequest;
import com.stockpro.warehouseservice.dto.request.UpdateStockRequest;
import com.stockpro.warehouseservice.dto.response.StockLevelResponse;
import com.stockpro.warehouseservice.dto.response.StockSummaryResponse;
import com.stockpro.warehouseservice.dto.response.TransferStockResponse;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.enums.TransferStatus;
import com.stockpro.warehouseservice.events.StockEvent;
import com.stockpro.warehouseservice.exception.InvalidOperationException;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import com.stockpro.warehouseservice.exception.StockLevelNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockManagementService {

    private static final String WAREHOUSE_ID_REQUIRED = "Warehouse ID is required";
    private static final String PRODUCT_ID_REQUIRED = "Product ID is required";
    private static final String SOURCE_WAREHOUSE_ID_REQUIRED = "Source warehouse ID is required";
    private static final String TARGET_WAREHOUSE_ID_REQUIRED = "Target warehouse ID is required";

    private final StockLevelRepository stockLevelRepository;
    private final InventoryOperationService inventoryOperationService;
    private final WarehouseManagementService warehouseManagementService;
    private final ProductCatalogClient productCatalogClient;
    private final WarehouseEventPublisher warehouseEventPublisher;

    @Value("${stockpro.rabbitmq.warehouse.routing.stock-created}") private String stockCreatedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-updated}") private String stockUpdatedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-received}") private String stockReceivedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-issued}") private String stockIssuedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-reserved}") private String stockReservedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-reservation-released}") private String stockReleasedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-transfer-initiated}") private String stockTransferInitiatedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-transferred}") private String stockTransferredRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-adjusted}") private String stockAdjustedRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-low}") private String stockLowRouting;
    @Value("${stockpro.rabbitmq.warehouse.routing.stock-overstock}") private String stockOverstockRouting;

    @Transactional
    public StockLevelResponse createStockLevel(CreateStockLevelRequest request, Long actorId) {
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(request.getWarehouseId());
        ensureActiveWarehouse(warehouse);
        ProductLookupResponseDTO product = validateProduct(request.getProductId(), true);
        if (stockLevelRepository.existsByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId())) {
            throw new IllegalArgumentException("Stock level already exists for this warehouse and product");
        }
        StockLevel stockLevel = StockLevel.builder()
                .warehouseId(request.getWarehouseId())
                .productId(request.getProductId())
                .quantity(defaultIfNull(request.getQuantity()))
                .reservedQuantity(defaultIfNull(request.getReservedQuantity()))
                .reorderLevel(product.getReorderLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .binLocation(request.getLocationCode())
                .build();
        validateStockState(stockLevel);
        warehouse.setUsedCapacity(defaultIfNull(warehouse.getUsedCapacity()) + stockLevel.getQuantity());
        inventoryOperationService.saveWarehouse(warehouse);
        StockLevel saved = stockLevelRepository.save(stockLevel);
        publishStockEvent(stockCreatedRouting, "STOCK_LEVEL_CREATED", saved, actorId, saved.getQuantity(), null, null, "Initial stock level created", null, null, null);
        publishThresholdEvents(saved, actorId);
        return toResponse(saved);
    }

    public StockLevelResponse getStockLevel(Long warehouseId, Long productId) {
        return toResponse(getStockEntity(warehouseId, productId));
    }

    public Page<StockLevelResponse> getStockByWarehouse(Long warehouseId, int page, int size) {
        return stockLevelRepository.findByWarehouseId(warehouseId, pageRequest(page, size)).map(this::toResponse);
    }

    public Page<StockLevelResponse> getStockByProduct(Long productId, int page, int size) {
        return stockLevelRepository.findByProductId(productId, pageRequest(page, size)).map(this::toResponse);
    }

    public Page<StockLevelResponse> searchStock(Long warehouseId, Long productId, String locationCode, Boolean lowStockOnly, Boolean overstockOnly, int page, int size) {
        Specification<StockLevel> specification = Specification.where(null);
        if (warehouseId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("warehouseId"), warehouseId));
        }
        if (productId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("productId"), productId));
        }
        if (locationCode != null && !locationCode.isBlank()) {
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("locationCode")), "%" + locationCode.toLowerCase() + "%"));
        }
        if (Boolean.TRUE.equals(lowStockOnly)) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(cb.diff(root.get("quantity"), root.get("reservedQuantity")), cb.coalesce(root.get("reorderLevel"), 0)));
        }
        if (Boolean.TRUE.equals(overstockOnly)) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("quantity"), cb.coalesce(root.get("maxStockLevel"), Integer.MAX_VALUE)));
        }
        return stockLevelRepository.findAll(specification, pageRequest(page, size)).map(this::toResponse);
    }

    @Transactional
    public StockLevelResponse updateStock(UpdateStockRequest request, Long actorId) {
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(request.getWarehouseId());
        ensureActiveWarehouse(warehouse);
        validateProduct(request.getProductId(), true);
        StockLevel stock = getStockEntity(request.getWarehouseId(), request.getProductId());
        if (request.getQuantity() < defaultIfNull(stock.getReservedQuantity())) {
            throw new InvalidOperationException("Quantity cannot be lower than reserved quantity");
        }
        int previousQuantity = defaultIfNull(stock.getQuantity());
        int delta = request.getQuantity() - previousQuantity;
        stock.setQuantity(request.getQuantity());
        validateStockState(stock);
        warehouse.setUsedCapacity(defaultIfNull(warehouse.getUsedCapacity()) + delta);
        inventoryOperationService.saveWarehouse(warehouse);
        StockLevel saved = stockLevelRepository.save(stock);
        publishStockEvent(stockUpdatedRouting, "STOCK_UPDATED", saved, actorId, delta, null, null, request.getReason(), request.getNotes(), null, null);
        publishThresholdEvents(saved, actorId);
        return toResponse(saved);
    }

    @Transactional
    public StockLevelResponse receiveStock(StockReceiptRequest request, Long actorId) {
        log.info("Stock receive request started. warehouseId={}, productId={}, quantity={}, referenceType={}, referenceId={}, movementType={}",
                request.getWarehouseId(), request.getProductId(), request.getQuantity(),
                request.getReferenceType(), request.getReferenceId(), request.getMovementType());
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(request.getWarehouseId());
        ensureActiveWarehouse(warehouse);
        validateProduct(request.getProductId(), true);
        InventoryOperationService.ThresholdSettings thresholds = inventoryOperationService.resolveThresholds(
                request.getProductId(),
                request.getReorderLevel(),
                request.getMaxStockLevel());
        boolean stockRowExists = stockLevelRepository.findByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId()).isPresent();
        StockLevel stock = stockLevelRepository.findByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId())
                .orElseGet(() -> newStockLevel(request.getWarehouseId(), request.getProductId(), thresholds));
        if (stockRowExists) {
            log.info("Existing stock row found for receipt. warehouseId={}, productId={}, currentQuantity={}",
                    request.getWarehouseId(), request.getProductId(), stock.getQuantity());
        } else {
            log.info("Creating stock row during receipt. warehouseId={}, productId={}, reorderLevel={}, maxStockLevel={}",
                    request.getWarehouseId(), request.getProductId(), thresholds.reorderLevel(), thresholds.maxStockLevel());
        }
        if (stock.getReorderLevel() == null && thresholds.reorderLevel() != null) {
            stock.setReorderLevel(thresholds.reorderLevel());
        }
        if (stock.getMaxStockLevel() == null && thresholds.maxStockLevel() != null) {
            stock.setMaxStockLevel(thresholds.maxStockLevel());
        }
        inventoryOperationService.handleReceipt(warehouse, stock, request.getQuantity(),
                request.getReason() != null && !request.getReason().isBlank() ? request.getReason() : "Stock received");
        StockLevel saved = inventoryOperationService.saveStockLevel(stock);
        inventoryOperationService.saveWarehouse(warehouse);
        publishStockEvent(stockReceivedRouting, "STOCK_RECEIVED", saved, actorId, request.getQuantity(), null, null,
                request.getReason() != null && !request.getReason().isBlank() ? request.getReason() : "Stock received",
                request.getNotes(), request.getReferenceId(), request.getReferenceType());
        publishThresholdEvents(saved, actorId);
        log.info("Stock receive request completed. warehouseId={}, productId={}, stockRowCreated={}, finalQuantity={}",
                request.getWarehouseId(), request.getProductId(), !stockRowExists, saved.getQuantity());
        return toResponse(saved);
    }

    @Transactional
    public StockLevelResponse issueStock(StockIssueRequest request, Long actorId) {
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(request.getWarehouseId());
        ensureActiveWarehouse(warehouse);
        validateProduct(request.getProductId(), true);
        StockLevel stock = getStockEntity(request.getWarehouseId(), request.getProductId());
        inventoryOperationService.handleIssue(warehouse, stock, request.getQuantity(), request.getReason());
        StockLevel saved = inventoryOperationService.saveStockLevel(stock);
        inventoryOperationService.saveWarehouse(warehouse);
        publishStockEvent(stockIssuedRouting, "STOCK_ISSUED", saved, actorId, request.getQuantity(), null, null, request.getReason(), request.getNotes(), request.getReferenceId(), request.getReferenceType());
        publishThresholdEvents(saved, actorId);
        return toResponse(saved);
    }

    @Transactional
    public StockLevelResponse reserveStock(ReserveStockRequest request, Long actorId) {
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        validateProduct(request.getProductId(), true);
        StockLevel stock = getStockEntity(request.getWarehouseId(), request.getProductId());
        inventoryOperationService.reserveStock(stock, request.getQuantity());
        StockLevel saved = inventoryOperationService.saveStockLevel(stock);
        publishStockEvent(stockReservedRouting, "STOCK_RESERVED", saved, actorId, request.getQuantity(), null, null, request.getReason(), null, request.getReferenceId(), request.getReferenceType());
        return toResponse(saved);
    }

    @Transactional
    public StockLevelResponse releaseReservation(ReleaseReservationRequest request, Long actorId) {
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        validateProduct(request.getProductId(), true);
        StockLevel stock = getStockEntity(request.getWarehouseId(), request.getProductId());
        inventoryOperationService.releaseReservation(stock, request.getQuantity());
        StockLevel saved = inventoryOperationService.saveStockLevel(stock);
        publishStockEvent(stockReleasedRouting, "STOCK_RESERVATION_RELEASED", saved, actorId, request.getQuantity(), null, null, request.getReason(), null, request.getReferenceId(), request.getReferenceType());
        return toResponse(saved);
    }

    @Transactional
    public TransferStockResponse transferStock(TransferStockRequest request, Long actorId) {
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        validateRequiredId(request.getSourceWarehouseId(), SOURCE_WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getDestinationWarehouseId(), TARGET_WAREHOUSE_ID_REQUIRED);
        log.info("Stock transfer request started. productId={}, sourceWarehouseId={}, targetWarehouseId={}, quantity={}, reasonCode={}",
                request.getProductId(), request.getSourceWarehouseId(), request.getDestinationWarehouseId(),
                request.getQuantity(), request.getReasonCode());
        if (Objects.equals(request.getSourceWarehouseId(), request.getDestinationWarehouseId())) {
            throw new InvalidOperationException("Source and destination warehouse cannot be same");
        }
        Warehouse source = warehouseManagementService.getWarehouseEntity(request.getSourceWarehouseId());
        Warehouse destination = warehouseManagementService.getWarehouseEntity(request.getDestinationWarehouseId());
        ensureActiveWarehouse(source);
        ensureActiveWarehouse(destination);
        ProductLookupResponseDTO product = validateProduct(request.getProductId(), true);
        InventoryOperationService.ThresholdSettings thresholds = new InventoryOperationService.ThresholdSettings(
                product.getReorderLevel(),
                product.getMaxStockLevel());
        StockLevel sourceStock = getStockEntity(request.getSourceWarehouseId(), request.getProductId());
        StockLevel destinationStock = stockLevelRepository.findByWarehouseIdAndProductId(request.getDestinationWarehouseId(), request.getProductId())
                .orElseGet(() -> newStockLevel(request.getDestinationWarehouseId(), request.getProductId(), thresholds));
        publishStockEvent(stockTransferInitiatedRouting, "STOCK_TRANSFER_INITIATED", sourceStock, actorId, request.getQuantity(),
                request.getSourceWarehouseId(), request.getDestinationWarehouseId(), request.getReasonCode(), request.getNotes(), null, null);
        inventoryOperationService.handleIssue(source, sourceStock, request.getQuantity(), request.getReasonCode());
        inventoryOperationService.handleReceipt(destination, destinationStock, request.getQuantity(), request.getReasonCode());
        StockLevel savedSource = inventoryOperationService.saveStockLevel(sourceStock);
        StockLevel savedDestination = inventoryOperationService.saveStockLevel(destinationStock);
        inventoryOperationService.saveWarehouse(source);
        inventoryOperationService.saveWarehouse(destination);
        publishStockEvent(stockTransferredRouting, "STOCK_TRANSFERRED", savedSource, actorId, request.getQuantity(), request.getSourceWarehouseId(), request.getDestinationWarehouseId(), request.getReasonCode(), request.getNotes(), null, null);
        publishThresholdEvents(savedSource, actorId);
        publishThresholdEvents(savedDestination, actorId);
        log.info("Stock transfer request completed. productId={}, sourceWarehouseId={}, targetWarehouseId={}, quantity={}, sourceBalanceAfter={}, targetBalanceAfter={}",
                request.getProductId(), request.getSourceWarehouseId(), request.getDestinationWarehouseId(),
                request.getQuantity(), savedSource.getQuantity(), savedDestination.getQuantity());
        return TransferStockResponse.builder()
                .transferId(null)
                .productId(request.getProductId())
                .sourceWarehouseId(request.getSourceWarehouseId())
                .destinationWarehouseId(request.getDestinationWarehouseId())
                .quantity(request.getQuantity())
                .sourceBalanceAfter(savedSource.getQuantity())
                .destinationBalanceAfter(savedDestination.getQuantity())
                .status(TransferStatus.COMPLETED)
                .message("Stock transferred successfully")
                .transferredAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public StockLevelResponse adjustStock(AdjustStockRequest request, Long actorId) {
        validateRequiredId(request.getWarehouseId(), WAREHOUSE_ID_REQUIRED);
        validateRequiredId(request.getProductId(), PRODUCT_ID_REQUIRED);
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(request.getWarehouseId());
        ensureActiveWarehouse(warehouse);
        validateProduct(request.getProductId(), true);
        StockLevel stock = getStockEntity(request.getWarehouseId(), request.getProductId());
        if (request.getNewQuantity() < defaultIfNull(stock.getReservedQuantity())) {
            throw new InvalidOperationException("New quantity cannot be lower than reserved quantity");
        }
        int previousQuantity = defaultIfNull(stock.getQuantity());
        int delta = request.getNewQuantity() - previousQuantity;
        stock.setQuantity(request.getNewQuantity());
        warehouse.setUsedCapacity(defaultIfNull(warehouse.getUsedCapacity()) + delta);
        validateStockState(stock);
        inventoryOperationService.saveWarehouse(warehouse);
        StockLevel saved = stockLevelRepository.save(stock);
        publishStockEvent(stockAdjustedRouting, "STOCK_ADJUSTED", saved, actorId, delta, null, null, request.getReason(), request.getNotes(), null, null);
        publishThresholdEvents(saved, actorId);
        return toResponse(saved);
    }

    public StockSummaryResponse getStockSummary() {
        List<StockLevel> items = stockLevelRepository.findAll();
        return StockSummaryResponse.builder()
                .totalStockItems(items.size())
                .totalQuantity(items.stream().mapToLong(s -> defaultIfNull(s.getQuantity())).sum())
                .totalReservedQuantity(items.stream().mapToLong(s -> defaultIfNull(s.getReservedQuantity())).sum())
                .totalAvailableQuantity(items.stream().mapToLong(StockLevel::getAvailableQuantity).sum())
                .lowStockItemsCount(items.stream().filter(s -> s.getReorderLevel() != null && s.getAvailableQuantity() <= s.getReorderLevel()).count())
                .overstockItemsCount(items.stream().filter(s -> s.getMaxStockLevel() != null && s.getQuantity() >= s.getMaxStockLevel()).count())
                .build();
    }

    public List<StockLevelResponse> getLowStockItems() {
        return stockLevelRepository.findAll().stream()
                .filter(s -> s.getReorderLevel() != null && s.getAvailableQuantity() <= s.getReorderLevel())
                .map(this::toResponse)
                .toList();
    }

    public List<StockLevelResponse> getOverstockItems() {
        return stockLevelRepository.findAll().stream()
                .filter(s -> s.getMaxStockLevel() != null && s.getQuantity() >= s.getMaxStockLevel())
                .map(this::toResponse)
                .toList();
    }

    private ProductLookupResponseDTO validateProduct(Long productId, boolean mustBeActive) {
        validateRequiredId(productId, PRODUCT_ID_REQUIRED);
        ProductLookupResponseDTO product = productCatalogClient.getProductById(productId);
        if (mustBeActive && Boolean.FALSE.equals(product.getIsActive())) {
            throw new ProductLookupException("Product not found with ID: " + productId);
        }
        return product;
    }

    private void publishThresholdEvents(StockLevel stock, Long actorId) {
        if (stock.getReorderLevel() != null && stock.getAvailableQuantity() <= stock.getReorderLevel()) {
            publishStockEvent(stockLowRouting, "LOW_STOCK_DETECTED", stock, actorId, 0, null, null, "Low stock threshold reached", null, null, null);
        }
        if (stock.getMaxStockLevel() != null && stock.getQuantity() >= stock.getMaxStockLevel()) {
            publishStockEvent(stockOverstockRouting, "OVERSTOCK_DETECTED", stock, actorId, 0, null, null, "Overstock threshold reached", null, null, null);
        }
    }

    private void publishStockEvent(String routingKey, String eventType, StockLevel stock, Long actorId, Integer operationQuantity, Long sourceWarehouseId, Long destinationWarehouseId, String reason, String notes, String referenceId, String referenceType) {
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(stock.getWarehouseId());
        ProductLookupResponseDTO product = null;
        try {
            product = productCatalogClient.getProductById(stock.getProductId());
        } catch (Exception ex) {
            log.warn("Product lookup failed for stock event: productId={}, error={}", stock.getProductId(), ex.getMessage());
        }
        warehouseEventPublisher.publishStockEvent(routingKey, StockEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .stockId(stock.getStockId())
                .warehouseId(stock.getWarehouseId())
                .warehouseName(warehouse.getName())
                .productId(stock.getProductId())
                .productName(product != null ? product.getName() : null)
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(stock.getAvailableQuantity())
                .operationQuantity(operationQuantity)
                .sourceWarehouseId(sourceWarehouseId)
                .destinationWarehouseId(destinationWarehouseId)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .reason(reason)
                .notes(notes)
                .actorId(actorId)
                .eventTime(LocalDateTime.now())
                .balanceAfter(stock.getQuantity())
                .newValue(toResponse(stock))
                .build());
    }

    private StockLevel getStockEntity(Long warehouseId, Long productId) {
        return stockLevelRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new StockLevelNotFoundException(
                        "Stock level not found for warehouse ID: " + warehouseId + " and product ID: " + productId));
    }

    private void validateRequiredId(Long value, String message) {
        if (value == null) {
            throw new InvalidOperationException(message);
        }
    }

    private StockLevel newStockLevel(Long warehouseId, Long productId, InventoryOperationService.ThresholdSettings thresholds) {
        return StockLevel.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .reorderLevel(thresholds.reorderLevel())
                .maxStockLevel(thresholds.maxStockLevel())
                .build();
    }

    private StockLevelResponse toResponse(StockLevel stock) {
        Warehouse warehouse = warehouseManagementService.getWarehouseEntity(stock.getWarehouseId());
        ProductLookupResponseDTO product = null;
        try {
            product = productCatalogClient.getProductById(stock.getProductId());
        } catch (Exception ex) {
            log.warn("Product lookup failed for stock response: productId={}, error={}", stock.getProductId(), ex.getMessage());
        }
        return StockLevelResponse.builder()
                .stockId(stock.getStockId())
                .warehouseId(stock.getWarehouseId())
                .warehouseName(warehouse.getName())
                .productId(stock.getProductId())
                .productName(product != null ? product.getName() : null)
                .sku(product != null ? product.getSku() : null)
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(stock.getAvailableQuantity())
                .locationCode(stock.getLocationCode())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastUpdated"));
    }

    private void ensureActiveWarehouse(Warehouse warehouse) {
        if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
            throw new InvalidOperationException("Inactive warehouse operation is not allowed");
        }
    }

    private void validateStockState(StockLevel stock) {
        if (defaultIfNull(stock.getQuantity()) < 0) {
            throw new InvalidOperationException("Stock quantity cannot be negative");
        }
        if (defaultIfNull(stock.getReservedQuantity()) < 0) {
            throw new InvalidOperationException("Reserved quantity cannot be negative");
        }
        if (defaultIfNull(stock.getReservedQuantity()) > defaultIfNull(stock.getQuantity())) {
            throw new InvalidOperationException("Reserved quantity cannot exceed quantity");
        }
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
