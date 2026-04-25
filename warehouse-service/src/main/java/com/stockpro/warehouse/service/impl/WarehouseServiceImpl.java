package com.stockpro.warehouse.service.impl;

import com.stockpro.warehouse.client.MovementServiceClient;
import com.stockpro.warehouse.client.ProductServiceClient;
import com.stockpro.warehouse.dto.event.LowStockEvent;
import com.stockpro.warehouse.dto.event.OverstockEvent;
import com.stockpro.warehouse.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouse.dto.request.RecordMovementRequest;
import com.stockpro.warehouse.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouse.dto.request.ReserveStockRequest;
import com.stockpro.warehouse.dto.request.StockSearchRequest;
import com.stockpro.warehouse.dto.request.TransferStockRequest;
import com.stockpro.warehouse.dto.request.UpdateStockRequest;
import com.stockpro.warehouse.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouse.dto.response.LowStockItemResponse;
import com.stockpro.warehouse.dto.response.ProductSummaryResponse;
import com.stockpro.warehouse.dto.response.StockLevelQuantityResponse;
import com.stockpro.warehouse.dto.response.StockLevelResponse;
import com.stockpro.warehouse.dto.response.TransferStockResponse;
import com.stockpro.warehouse.dto.response.WarehouseResponse;
import com.stockpro.warehouse.dto.response.WarehouseUtilizationResponse;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.enums.AlertSeverity;
import com.stockpro.warehouse.exception.ExternalServiceException;
import com.stockpro.warehouse.exception.InsufficientStockException;
import com.stockpro.warehouse.exception.InvalidStockOperationException;
import com.stockpro.warehouse.exception.InvalidWarehouseTransferException;
import com.stockpro.warehouse.exception.MovementRecordingException;
import com.stockpro.warehouse.exception.ProductValidationException;
import com.stockpro.warehouse.exception.StockLevelNotFoundException;
import com.stockpro.warehouse.exception.WarehouseNotFoundException;
import com.stockpro.warehouse.mapper.StockLevelMapper;
import com.stockpro.warehouse.mapper.WarehouseMapper;
import com.stockpro.warehouse.publisher.AlertEventPublisher;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.StockLevelSpecifications;
import com.stockpro.warehouse.repository.WarehouseQuantityProjection;
import com.stockpro.warehouse.repository.WarehouseRepository;
import com.stockpro.warehouse.security.AuthenticatedUser;
import com.stockpro.warehouse.security.SecurityUtils;
import com.stockpro.warehouse.service.WarehouseService;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WarehouseServiceImpl.class);
    private static final int SCALE = 4;
    private static final Set<String> WAREHOUSE_SORT_FIELDS = Set.of(
            "warehouseId",
            "name",
            "location",
            "managerId",
            "capacity",
            "usedCapacity",
            "isActive",
            "createdAt",
            "updatedAt");
    private static final Set<String> STOCK_SORT_FIELDS = Set.of(
            "stockId",
            "warehouseId",
            "productId",
            "quantity",
            "reservedQuantity",
            "location",
            "lastUpdated",
            "createdAt",
            "updatedAt");

    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final WarehouseMapper warehouseMapper;
    private final StockLevelMapper stockLevelMapper;
    private final ProductServiceClient productServiceClient;
    private final MovementServiceClient movementServiceClient;
    private final AlertEventPublisher alertEventPublisher;
    private final SecurityUtils securityUtils;
    private final boolean productValidationEnabled;
    private final String applicationName;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository,
            StockLevelRepository stockLevelRepository,
            WarehouseMapper warehouseMapper,
            StockLevelMapper stockLevelMapper,
            ProductServiceClient productServiceClient,
            MovementServiceClient movementServiceClient,
            AlertEventPublisher alertEventPublisher,
            SecurityUtils securityUtils,
            @Value("${warehouse.validation.product-enabled:true}") boolean productValidationEnabled,
            @Value("${spring.application.name:WAREHOUSE-SERVICE}") String applicationName) {
        this.warehouseRepository = warehouseRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.warehouseMapper = warehouseMapper;
        this.stockLevelMapper = stockLevelMapper;
        this.productServiceClient = productServiceClient;
        this.movementServiceClient = movementServiceClient;
        this.alertEventPublisher = alertEventPublisher;
        this.securityUtils = securityUtils;
        this.productValidationEnabled = productValidationEnabled;
        this.applicationName = applicationName;
    }

    @Override
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        Warehouse warehouse = warehouseMapper.toEntity(request);
        validateWarehouseFields(warehouse);

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        LOGGER.info("Created warehouse warehouseId={} name={}", savedWarehouse.getWarehouseId(), savedWarehouse.getName());
        return warehouseMapper.toResponse(savedWarehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long warehouseId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, WAREHOUSE_SORT_FIELDS, "warehouseId");
        Page<Warehouse> warehousePage = warehouseRepository.findAll(pageable);
        Map<Long, Integer> usedCapacityByWarehouseId = resolveUsedCapacityMap(warehousePage.getContent().stream()
                .map(Warehouse::getWarehouseId)
                .toList());

        return warehousePage.map(warehouse -> warehouseMapper.toResponse(
                warehouse,
                usedCapacityByWarehouseId.getOrDefault(warehouse.getWarehouseId(), warehouse.getUsedCapacity())));
    }

    @Override
    public WarehouseResponse updateWarehouse(Long warehouseId, UpdateWarehouseRequest request) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        warehouseMapper.updateEntity(request, warehouse);
        validateWarehouseFields(warehouse);

        int actualUsedCapacity = resolveUsedCapacity(warehouseId);
        if (warehouse.getCapacity() < actualUsedCapacity) {
            throw new InvalidStockOperationException("Warehouse capacity cannot be less than current used capacity.");
        }

        warehouse.setUsedCapacity(actualUsedCapacity);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        LOGGER.info("Updated warehouse warehouseId={}", savedWarehouse.getWarehouseId());
        return warehouseMapper.toResponse(savedWarehouse);
    }

    @Override
    public WarehouseResponse deactivateWarehouse(Long warehouseId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        warehouse.setIsActive(Boolean.FALSE);
        warehouse.setUsedCapacity(resolveUsedCapacity(warehouseId));

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        LOGGER.info("Deactivated warehouse warehouseId={}", savedWarehouse.getWarehouseId());
        return warehouseMapper.toResponse(savedWarehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public StockLevelResponse getStockLevel(Long warehouseId, Long productId) {
        getWarehouseEntity(warehouseId);
        return stockLevelMapper.toResponse(getRequiredStockLevel(warehouseId, productId));
    }

    @Override
    public StockLevelResponse updateStock(UpdateStockRequest request) {
        Warehouse warehouse = getActiveWarehouseEntity(request.getWarehouseId());
        ProductSummaryResponse product = validateProductExists(request.getProductId());
        AuthenticatedUser currentUser = securityUtils.getCurrentUser();

        BigDecimal quantityChange = normalizeQuantity(request.getQuantity());
        BigDecimal unitCost = normalizeNullableDecimal(request.getUnitCost());

        StockLevel stockLevel = stockLevelRepository.findByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId())
                .orElseGet(() -> newStockLevel(request.getWarehouseId(), request.getProductId(), request.getLocation()));

        BigDecimal previousQuantity = safeDecimal(stockLevel.getQuantity());
        BigDecimal previousReservedQuantity = safeDecimal(stockLevel.getReservedQuantity());
        BigDecimal newQuantity = previousQuantity.add(quantityChange);
        BigDecimal reservedQuantity = safeDecimal(stockLevel.getReservedQuantity());

        validateStockState(newQuantity, reservedQuantity);
        validateWarehouseCapacity(warehouse, previousQuantity, newQuantity);

        stockLevel.setQuantity(newQuantity);
        if (StringUtils.hasText(request.getLocation())) {
            stockLevel.setLocation(request.getLocation().trim());
        }

        StockLevel savedStockLevel = stockLevelRepository.saveAndFlush(stockLevel);
        warehouseRepository.saveAndFlush(warehouse);

        recordMovement(savedStockLevel,
                resolveMovementType(request.getReferenceType(), quantityChange),
                quantityChange.abs(),
                unitCost,
                request.getReferenceId(),
                resolveReferenceType(request.getReferenceType(), "MANUAL_ADJUSTMENT"),
                request.getNotes(),
                currentUser);

        LOGGER.info("Updated stock warehouseId={} productId={} quantityChange={} balance={}",
                savedStockLevel.getWarehouseId(),
                savedStockLevel.getProductId(),
                quantityChange,
                savedStockLevel.getQuantity());
        publishThresholdAlertsAfterCommit(
                warehouse,
                product,
                currentUser.userId(),
                previousQuantity,
                previousReservedQuantity,
                savedStockLevel);

        return stockLevelMapper.toResponse(savedStockLevel);
    }

    @Override
    public StockLevelResponse reserveStock(ReserveStockRequest request) {
        Warehouse warehouse = getActiveWarehouseEntity(request.getWarehouseId());
        AuthenticatedUser currentUser = securityUtils.getCurrentUser();
        BigDecimal reserveQuantity = normalizePositiveQuantity(request.getQuantity(), "Reservation quantity must be greater than zero.");

        StockLevel stockLevel = getRequiredStockLevel(request.getWarehouseId(), request.getProductId());
        BigDecimal previousQuantity = safeDecimal(stockLevel.getQuantity());
        BigDecimal previousReservedQuantity = safeDecimal(stockLevel.getReservedQuantity());
        if (stockLevel.getAvailableQuantity().compareTo(reserveQuantity) < 0) {
            throw new InsufficientStockException("Insufficient available stock to reserve.");
        }

        stockLevel.setReservedQuantity(safeDecimal(stockLevel.getReservedQuantity()).add(reserveQuantity));
        StockLevel savedStockLevel = stockLevelRepository.saveAndFlush(stockLevel);
        ProductSummaryResponse product = resolveProductSummaryForAlert(savedStockLevel.getProductId());

        LOGGER.info("Reserved stock warehouseId={} productId={} quantity={}",
                savedStockLevel.getWarehouseId(),
                savedStockLevel.getProductId(),
                reserveQuantity);
        publishThresholdAlertsAfterCommit(
                warehouse,
                product,
                currentUser.userId(),
                previousQuantity,
                previousReservedQuantity,
                savedStockLevel);

        return stockLevelMapper.toResponse(savedStockLevel);
    }

    @Override
    public StockLevelResponse releaseReservation(ReleaseReservationRequest request) {
        Warehouse warehouse = getActiveWarehouseEntity(request.getWarehouseId());
        AuthenticatedUser currentUser = securityUtils.getCurrentUser();
        BigDecimal releaseQuantity = normalizePositiveQuantity(request.getQuantity(), "Release quantity must be greater than zero.");

        StockLevel stockLevel = getRequiredStockLevel(request.getWarehouseId(), request.getProductId());
        BigDecimal previousQuantity = safeDecimal(stockLevel.getQuantity());
        BigDecimal previousReservedQuantity = safeDecimal(stockLevel.getReservedQuantity());
        if (safeDecimal(stockLevel.getReservedQuantity()).compareTo(releaseQuantity) < 0) {
            throw new InvalidStockOperationException("Cannot release more stock than is currently reserved.");
        }

        stockLevel.setReservedQuantity(safeDecimal(stockLevel.getReservedQuantity()).subtract(releaseQuantity));
        StockLevel savedStockLevel = stockLevelRepository.saveAndFlush(stockLevel);
        ProductSummaryResponse product = resolveProductSummaryForAlert(savedStockLevel.getProductId());

        LOGGER.info("Released reservation warehouseId={} productId={} quantity={}",
                savedStockLevel.getWarehouseId(),
                savedStockLevel.getProductId(),
                releaseQuantity);
        publishThresholdAlertsAfterCommit(
                warehouse,
                product,
                currentUser.userId(),
                previousQuantity,
                previousReservedQuantity,
                savedStockLevel);

        return stockLevelMapper.toResponse(savedStockLevel);
    }

    @Override
    public TransferStockResponse transferStock(TransferStockRequest request) {
        if (request.getSourceWarehouseId().equals(request.getDestinationWarehouseId())) {
            throw new InvalidWarehouseTransferException("Source warehouse must not equal destination warehouse.");
        }

        Warehouse sourceWarehouse = getActiveWarehouseEntity(request.getSourceWarehouseId());
        Warehouse destinationWarehouse = getActiveWarehouseEntity(request.getDestinationWarehouseId());
        ProductSummaryResponse product = validateProductExists(request.getProductId());
        AuthenticatedUser currentUser = securityUtils.getCurrentUser();

        BigDecimal transferQuantity = normalizePositiveQuantity(request.getQuantity(), "Transfer quantity must be greater than zero.");
        BigDecimal unitCost = normalizeNullableDecimal(request.getUnitCost());

        StockLevel sourceStockLevel = getRequiredStockLevel(request.getSourceWarehouseId(), request.getProductId());
        if (sourceStockLevel.getAvailableQuantity().compareTo(transferQuantity) < 0) {
            throw new InsufficientStockException("Insufficient available stock in the source warehouse.");
        }

        StockLevel destinationStockLevel = stockLevelRepository
                .findByWarehouseIdAndProductId(request.getDestinationWarehouseId(), request.getProductId())
                .orElseGet(() -> newStockLevel(request.getDestinationWarehouseId(), request.getProductId(), null));

        BigDecimal sourcePreviousQuantity = safeDecimal(sourceStockLevel.getQuantity());
        BigDecimal sourcePreviousReservedQuantity = safeDecimal(sourceStockLevel.getReservedQuantity());
        BigDecimal sourceNewQuantity = sourcePreviousQuantity.subtract(transferQuantity);
        validateStockState(sourceNewQuantity, safeDecimal(sourceStockLevel.getReservedQuantity()));
        validateWarehouseCapacity(sourceWarehouse, sourcePreviousQuantity, sourceNewQuantity);

        BigDecimal destinationPreviousQuantity = safeDecimal(destinationStockLevel.getQuantity());
        BigDecimal destinationPreviousReservedQuantity = safeDecimal(destinationStockLevel.getReservedQuantity());
        BigDecimal destinationNewQuantity = destinationPreviousQuantity.add(transferQuantity);
        validateStockState(destinationNewQuantity, safeDecimal(destinationStockLevel.getReservedQuantity()));
        validateWarehouseCapacity(destinationWarehouse, destinationPreviousQuantity, destinationNewQuantity);

        sourceStockLevel.setQuantity(sourceNewQuantity);
        destinationStockLevel.setQuantity(destinationNewQuantity);

        StockLevel savedSourceStock = stockLevelRepository.saveAndFlush(sourceStockLevel);
        StockLevel savedDestinationStock = stockLevelRepository.saveAndFlush(destinationStockLevel);
        warehouseRepository.saveAndFlush(sourceWarehouse);
        warehouseRepository.saveAndFlush(destinationWarehouse);

        String referenceType = resolveReferenceType(request.getReferenceType(), "WAREHOUSE_TRANSFER");
        recordMovement(savedSourceStock,
                "TRANSFER_OUT",
                transferQuantity,
                unitCost,
                request.getReferenceId(),
                referenceType,
                request.getNotes(),
                currentUser);
        recordMovement(savedDestinationStock,
                "TRANSFER_IN",
                transferQuantity,
                unitCost,
                request.getReferenceId(),
                referenceType,
                request.getNotes(),
                currentUser);

        LOGGER.info("Transferred stock productId={} sourceWarehouseId={} destinationWarehouseId={} quantity={}",
                request.getProductId(),
                request.getSourceWarehouseId(),
                request.getDestinationWarehouseId(),
                transferQuantity);
        publishThresholdAlertsAfterCommit(
                sourceWarehouse,
                product,
                currentUser.userId(),
                sourcePreviousQuantity,
                sourcePreviousReservedQuantity,
                savedSourceStock);
        publishThresholdAlertsAfterCommit(
                destinationWarehouse,
                product,
                currentUser.userId(),
                destinationPreviousQuantity,
                destinationPreviousReservedQuantity,
                savedDestinationStock);

        return TransferStockResponse.builder()
                .productId(request.getProductId())
                .sourceWarehouseId(request.getSourceWarehouseId())
                .destinationWarehouseId(request.getDestinationWarehouseId())
                .transferredQuantity(transferQuantity)
                .sourceBalance(savedSourceStock.getQuantity())
                .destinationBalance(savedDestinationStock.getQuantity())
                .message("Stock transferred successfully.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> getStockByWarehouse(Long warehouseId, int page, int size, String sortBy, String sortDir) {
        getWarehouseEntity(warehouseId);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, STOCK_SORT_FIELDS, "lastUpdated");
        return stockLevelRepository.findByWarehouseId(warehouseId, pageable).map(stockLevelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> getStockByProduct(Long productId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir, STOCK_SORT_FIELDS, "lastUpdated");
        return stockLevelRepository.findByProductId(productId, pageable).map(stockLevelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LowStockItemResponse> getLowStockItems(int page, int size) {
        List<LowStockItemResponse> lowStockItems = loadLowStockItems();
        return buildPage(lowStockItems, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseUtilizationResponse getWarehouseUtilization(Long warehouseId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        int usedCapacity = resolveUsedCapacity(warehouseId);
        BigDecimal utilizationPercentage = warehouse.getCapacity() == null || warehouse.getCapacity() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(usedCapacity)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(warehouse.getCapacity()), 2, RoundingMode.HALF_UP);

        return WarehouseUtilizationResponse.builder()
                .warehouseId(warehouseId)
                .capacity(warehouse.getCapacity())
                .usedCapacity(usedCapacity)
                .utilizationPercentage(utilizationPercentage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockLevelResponse> searchStock(StockSearchRequest request) {
        int page = request.getPage() == null ? 0 : request.getPage();
        int size = request.getSize() == null ? 20 : request.getSize();

        if (request.getWarehouseId() != null) {
            getWarehouseEntity(request.getWarehouseId());
        }

        Specification<StockLevel> specification = StockLevelSpecifications.withFilters(request);
        Sort sort = buildSort(request.getSortBy(), request.getSortDir(), STOCK_SORT_FIELDS, "lastUpdated");

        if (Boolean.TRUE.equals(request.getLowStockOnly())) {
            Set<Long> activeWarehouseIds = activeWarehouseIds();
            Map<Long, ProductSummaryResponse> productMap = fetchProductMap();

            List<StockLevelResponse> lowStockResponses = stockLevelRepository.findAll(specification, sort).stream()
                    .filter(stockLevel -> activeWarehouseIds.contains(stockLevel.getWarehouseId()))
                    .filter(stockLevel -> isLowStock(stockLevel, productMap.get(stockLevel.getProductId())))
                    .sorted(lowStockComparator())
                    .map(stockLevelMapper::toResponse)
                    .toList();

            return buildPage(lowStockResponses, page, size);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return stockLevelRepository.findAll(specification, pageable).map(stockLevelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockItemResponse> getLowStockItemsForAlert() {
        return loadLowStockItems();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockLevelQuantityResponse> getStockLevels(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctProductIds = new ArrayList<>(new LinkedHashSet<>(productIds));
        Set<Long> activeWarehouseIds = activeWarehouseIds();

        Map<Long, BigDecimal> availableQuantityByProductId = stockLevelRepository.findByProductIdIn(distinctProductIds).stream()
                .filter(stockLevel -> activeWarehouseIds.contains(stockLevel.getWarehouseId()))
                .collect(Collectors.toMap(
                        StockLevel::getProductId,
                        StockLevel::getAvailableQuantity,
                        BigDecimal::add));

        return distinctProductIds.stream()
                .map(productId -> stockLevelMapper.toQuantityResponse(productId,
                        toCurrentQuantityInteger(availableQuantityByProductId.getOrDefault(productId, BigDecimal.ZERO))))
                .toList();
    }

    private Warehouse getWarehouseEntity(Long warehouseId) {
        if (warehouseId == null || warehouseId <= 0) {
            throw new InvalidStockOperationException("warehouseId is required.");
        }

        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found with id: " + warehouseId));
    }

    private Warehouse getActiveWarehouseEntity(Long warehouseId) {
        Warehouse warehouse = getWarehouseEntity(warehouseId);
        if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
            throw new InvalidStockOperationException("Warehouse is inactive and cannot be used for stock operations.");
        }
        return warehouse;
    }

    private StockLevel getRequiredStockLevel(Long warehouseId, Long productId) {
        return stockLevelRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new StockLevelNotFoundException(
                        "Stock level not found for warehouseId " + warehouseId + " and productId " + productId));
    }

    private void validateWarehouseFields(Warehouse warehouse) {
        if (!StringUtils.hasText(warehouse.getName())) {
            throw new InvalidStockOperationException("Warehouse name is required.");
        }
        if (!StringUtils.hasText(warehouse.getLocation())) {
            throw new InvalidStockOperationException("Warehouse location is required.");
        }
        if (!StringUtils.hasText(warehouse.getAddress())) {
            throw new InvalidStockOperationException("Warehouse address is required.");
        }
        if (warehouse.getCapacity() == null || warehouse.getCapacity() <= 0) {
            throw new InvalidStockOperationException("Warehouse capacity must be greater than zero.");
        }
        if (warehouse.getUsedCapacity() == null || warehouse.getUsedCapacity() < 0) {
            throw new InvalidStockOperationException("Warehouse used capacity cannot be negative.");
        }
        if (warehouse.getUsedCapacity() > warehouse.getCapacity()) {
            throw new InvalidStockOperationException("Warehouse used capacity cannot exceed total capacity.");
        }
        warehouse.setName(warehouse.getName().trim());
        warehouse.setLocation(warehouse.getLocation().trim());
        warehouse.setAddress(warehouse.getAddress().trim());
        warehouse.setPhone(normalizeOptionalText(warehouse.getPhone()));
    }

    private ProductSummaryResponse validateProductExists(Long productId) {
        if (productId == null || productId <= 0) {
            throw new InvalidStockOperationException("productId is required.");
        }
        if (!productValidationEnabled) {
            return resolveProductSummaryForAlert(productId);
        }

        try {
            ProductSummaryResponse product = productServiceClient.getProductById(productId);
            if (product.getIsActive() != null && !product.getIsActive()) {
                throw new ProductValidationException("Product is inactive: " + productId);
            }
            return product;
        } catch (FeignException.NotFound exception) {
            throw new ProductValidationException("Product not found with id: " + productId);
        } catch (ProductValidationException exception) {
            throw exception;
        } catch (FeignException exception) {
            throw new ExternalServiceException("Unable to validate product using product-service.", exception);
        }
    }

    private ProductSummaryResponse resolveProductSummaryForAlert(Long productId) {
        try {
            return productServiceClient.getProductById(productId);
        } catch (Exception exception) {
            LOGGER.warn("Unable to fetch product metadata for alert publication productId={}: {}", productId, exception.getMessage());
            return null;
        }
    }

    private List<ProductSummaryResponse> fetchAllProducts() {
        try {
            List<ProductSummaryResponse> products = productServiceClient.getAllProducts();
            return products == null ? List.of() : products;
        } catch (FeignException exception) {
            throw new ExternalServiceException("Unable to fetch product metadata using product-service.", exception);
        }
    }

    private Map<Long, ProductSummaryResponse> fetchProductMap() {
        return fetchAllProducts().stream()
                .filter(product -> Boolean.TRUE.equals(product.getIsActive()))
                .collect(Collectors.toMap(ProductSummaryResponse::getProductId, product -> product, (first, second) -> first,
                        LinkedHashMap::new));
    }

    private StockLevel newStockLevel(Long warehouseId, Long productId, String location) {
        return StockLevel.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                .reservedQuantity(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP))
                .location(normalizeOptionalText(location))
                .build();
    }

    private void validateStockState(BigDecimal quantity, BigDecimal reservedQuantity) {
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidStockOperationException("Stock quantity cannot become negative.");
        }
        if (reservedQuantity.compareTo(quantity) > 0) {
            throw new InvalidStockOperationException("Reserved quantity cannot exceed total quantity.");
        }
    }

    private void validateWarehouseCapacity(Warehouse warehouse, BigDecimal previousQuantity, BigDecimal newQuantity) {
        BigDecimal currentWarehouseTotal = safeDecimal(stockLevelRepository.sumQuantityByWarehouseId(warehouse.getWarehouseId()));
        BigDecimal projectedWarehouseTotal = currentWarehouseTotal.subtract(safeDecimal(previousQuantity)).add(safeDecimal(newQuantity));

        if (projectedWarehouseTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidStockOperationException("Warehouse used capacity cannot become negative.");
        }
        if (projectedWarehouseTotal.compareTo(BigDecimal.valueOf(warehouse.getCapacity())) > 0) {
            throw new InvalidStockOperationException("Warehouse capacity would be exceeded by this operation.");
        }

        warehouse.setUsedCapacity(toUsedCapacity(projectedWarehouseTotal));
    }

    private void recordMovement(StockLevel stockLevel,
            String movementType,
            BigDecimal movementQuantity,
            BigDecimal unitCost,
            Long referenceId,
            String referenceType,
            String notes,
            AuthenticatedUser currentUser) {
        try {
            movementServiceClient.recordMovement(RecordMovementRequest.builder()
                    .productId(stockLevel.getProductId())
                    .warehouseId(stockLevel.getWarehouseId())
                    .movementType(movementType)
                    .quantity(normalizePositiveQuantity(movementQuantity, "Movement quantity must be greater than zero."))
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .unitCost(unitCost)
                    .performedBy(currentUser.userId())
                    .notes(resolveMovementNotes(movementType, notes, stockLevel.getWarehouseId(), stockLevel.getProductId()))
                    .movementDate(LocalDateTime.now())
                    .balanceAfter(stockLevel.getQuantity())
                    .build(), applicationName);
        } catch (FeignException exception) {
            throw new MovementRecordingException("Unable to record stock movement using movement-service.", exception);
        }
    }

    private String resolveMovementType(String referenceType, BigDecimal quantityChange) {
        String normalizedReferenceType = normalizeOptionalText(referenceType);
        if (normalizedReferenceType != null) {
            String upperReferenceType = normalizedReferenceType.toUpperCase(Locale.ROOT);
            if (upperReferenceType.contains("RETURN")) {
                return "RETURN";
            }
            if (upperReferenceType.contains("WRITE_OFF") || upperReferenceType.contains("DAMAGE")) {
                return "WRITE_OFF";
            }
            if (upperReferenceType.contains("ADJUST")) {
                return "ADJUSTMENT";
            }
        }
        return quantityChange.signum() > 0 ? "STOCK_IN" : "STOCK_OUT";
    }

    private String resolveReferenceType(String referenceType, String defaultReferenceType) {
        return StringUtils.hasText(referenceType) ? referenceType.trim() : defaultReferenceType;
    }

    private String resolveMovementNotes(String movementType, String notes, Long warehouseId, Long productId) {
        if (StringUtils.hasText(notes)) {
            return notes.trim();
        }

        return switch (movementType) {
            case "STOCK_IN" -> "Stock received for productId " + productId + " in warehouseId " + warehouseId;
            case "STOCK_OUT" -> "Stock issued for productId " + productId + " from warehouseId " + warehouseId;
            case "TRANSFER_OUT" -> "Transfer issued for productId " + productId + " from warehouseId " + warehouseId;
            case "TRANSFER_IN" -> "Transfer received for productId " + productId + " into warehouseId " + warehouseId;
            case "RETURN" -> "Return recorded for productId " + productId + " in warehouseId " + warehouseId;
            case "WRITE_OFF" -> "Write-off recorded for productId " + productId + " in warehouseId " + warehouseId;
            default -> "Stock adjustment recorded for productId " + productId + " in warehouseId " + warehouseId;
        };
    }

    private List<LowStockItemResponse> loadLowStockItems() {
        Map<Long, ProductSummaryResponse> productMap = fetchProductMap();
        if (productMap.isEmpty()) {
            return List.of();
        }

        Set<Long> activeWarehouseIds = activeWarehouseIds();
        return stockLevelRepository.findAll().stream()
                .filter(stockLevel -> activeWarehouseIds.contains(stockLevel.getWarehouseId()))
                .filter(stockLevel -> isLowStock(stockLevel, productMap.get(stockLevel.getProductId())))
                .sorted(lowStockComparator())
                .map(stockLevelMapper::toLowStockResponse)
                .toList();
    }

    private Set<Long> activeWarehouseIds() {
        return warehouseRepository.findWarehouseIdsByIsActive(Boolean.TRUE).stream()
                .collect(Collectors.toSet());
    }

    private boolean isLowStock(StockLevel stockLevel, ProductSummaryResponse product) {
        return product != null
                && product.getReorderLevel() != null
                && stockLevel.getAvailableQuantity().compareTo(product.getReorderLevel()) <= 0;
    }

    private void publishThresholdAlertsAfterCommit(Warehouse warehouse,
            ProductSummaryResponse product,
            Long fallbackRecipientId,
            BigDecimal previousQuantity,
            BigDecimal previousReservedQuantity,
            StockLevel currentStockLevel) {
        if (product == null) {
            return;
        }

        runAfterCommit(() -> publishThresholdAlerts(
                warehouse,
                product,
                fallbackRecipientId,
                previousQuantity,
                previousReservedQuantity,
                currentStockLevel));
    }

    private void publishThresholdAlerts(Warehouse warehouse,
            ProductSummaryResponse product,
            Long fallbackRecipientId,
            BigDecimal previousQuantity,
            BigDecimal previousReservedQuantity,
            StockLevel currentStockLevel) {
        Long recipientId = resolveAlertRecipient(warehouse, fallbackRecipientId);
        if (recipientId == null) {
            LOGGER.warn("Skipping threshold alert publication because no recipient could be resolved for warehouseId={}",
                    warehouse.getWarehouseId());
            return;
        }

        BigDecimal priorQuantity = safeDecimal(previousQuantity);
        BigDecimal priorReservedQuantity = safeDecimal(previousReservedQuantity);
        BigDecimal priorAvailableQuantity = priorQuantity.subtract(priorReservedQuantity);
        BigDecimal currentQuantity = safeDecimal(currentStockLevel.getQuantity());
        BigDecimal currentAvailableQuantity = currentStockLevel.getAvailableQuantity();

        if (crossedIntoLowStock(priorAvailableQuantity, currentAvailableQuantity, product.getReorderLevel())) {
            alertEventPublisher.publishLowStockEvent(LowStockEvent.builder()
                    .recipientId(recipientId)
                    .productId(currentStockLevel.getProductId())
                    .warehouseId(currentStockLevel.getWarehouseId())
                    .productName(product.getName())
                    .warehouseName(warehouse.getName())
                    .availableQuantity(currentAvailableQuantity)
                    .reorderLevel(product.getReorderLevel())
                    .severity(resolveLowStockSeverity(currentAvailableQuantity, product.getReorderLevel()))
                    .eventTime(LocalDateTime.now())
                    .build());
        }

        if (crossedIntoOverstock(priorQuantity, currentQuantity, product.getMaxStockLevel())) {
            alertEventPublisher.publishOverstockEvent(OverstockEvent.builder()
                    .recipientId(recipientId)
                    .productId(currentStockLevel.getProductId())
                    .warehouseId(currentStockLevel.getWarehouseId())
                    .productName(product.getName())
                    .warehouseName(warehouse.getName())
                    .quantity(currentQuantity)
                    .maxStockLevel(product.getMaxStockLevel())
                    .severity(resolveOverstockSeverity(currentQuantity, product.getMaxStockLevel()))
                    .eventTime(LocalDateTime.now())
                    .build());
        }
    }

    private boolean crossedIntoLowStock(BigDecimal previousAvailableQuantity,
            BigDecimal currentAvailableQuantity,
            BigDecimal reorderLevel) {
        return reorderLevel != null
                && previousAvailableQuantity.compareTo(reorderLevel) > 0
                && currentAvailableQuantity.compareTo(reorderLevel) <= 0;
    }

    private boolean crossedIntoOverstock(BigDecimal previousQuantity,
            BigDecimal currentQuantity,
            BigDecimal maxStockLevel) {
        return maxStockLevel != null
                && previousQuantity.compareTo(maxStockLevel) <= 0
                && currentQuantity.compareTo(maxStockLevel) > 0;
    }

    private AlertSeverity resolveLowStockSeverity(BigDecimal availableQuantity, BigDecimal reorderLevel) {
        if (availableQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return AlertSeverity.CRITICAL;
        }

        BigDecimal criticalThreshold = reorderLevel
                .divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        return availableQuantity.compareTo(criticalThreshold) <= 0 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }

    private AlertSeverity resolveOverstockSeverity(BigDecimal quantity, BigDecimal maxStockLevel) {
        BigDecimal criticalThreshold = maxStockLevel.multiply(new BigDecimal("1.25"));
        return quantity.compareTo(criticalThreshold) > 0 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }

    private Long resolveAlertRecipient(Warehouse warehouse, Long fallbackRecipientId) {
        if (warehouse.getManagerId() != null && warehouse.getManagerId() > 0) {
            return warehouse.getManagerId();
        }
        return fallbackRecipientId;
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }

        action.run();
    }

    private Comparator<StockLevel> lowStockComparator() {
        return Comparator.comparing(StockLevel::getAvailableQuantity)
                .thenComparing(StockLevel::getWarehouseId)
                .thenComparing(StockLevel::getProductId);
    }

    private int resolveUsedCapacity(Long warehouseId) {
        return toUsedCapacity(safeDecimal(stockLevelRepository.sumQuantityByWarehouseId(warehouseId)));
    }

    private Map<Long, Integer> resolveUsedCapacityMap(List<Long> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return Map.of();
        }

        return stockLevelRepository.sumQuantityByWarehouseIds(warehouseIds).stream()
                .collect(Collectors.toMap(
                        WarehouseQuantityProjection::getWarehouseId,
                        projection -> toUsedCapacity(projection.getTotalQuantity())));
    }

    private int toUsedCapacity(BigDecimal totalQuantity) {
        BigDecimal normalized = safeDecimal(totalQuantity);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal rounded = normalized.setScale(0, RoundingMode.CEILING);
        if (rounded.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new InvalidStockOperationException("Warehouse used capacity exceeds supported integer range.");
        }

        return rounded.intValueExact();
    }

    private int toCurrentQuantityInteger(BigDecimal quantity) {
        BigDecimal normalized = safeDecimal(quantity);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal rounded = normalized.setScale(0, RoundingMode.DOWN);
        if (rounded.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }

        return rounded.intValue();
    }

    private BigDecimal normalizeQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidStockOperationException("quantity must be non-zero.");
        }
        return quantity.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePositiveQuantity(BigDecimal quantity, String message) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStockOperationException(message);
        }
        return quantity.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullableDecimal(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP) : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir, Set<String> allowedSortFields,
            String defaultSortBy) {
        if (page < 0 || size <= 0) {
            throw new InvalidStockOperationException("page must be zero or greater and size must be greater than zero.");
        }
        return PageRequest.of(page, size, buildSort(sortBy, sortDir, allowedSortFields, defaultSortBy));
    }

    private Sort buildSort(String sortBy, String sortDir, Set<String> allowedSortFields, String defaultSortBy) {
        String resolvedSortBy = StringUtils.hasText(sortBy) ? sortBy.trim() : defaultSortBy;
        if (!allowedSortFields.contains(resolvedSortBy)) {
            throw new InvalidStockOperationException("Unsupported sortBy value: " + resolvedSortBy);
        }

        return Sort.by(parseSortDirection(sortDir), resolvedSortBy);
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if (!StringUtils.hasText(sortDir)) {
            return Sort.Direction.DESC;
        }

        try {
            return Sort.Direction.fromString(sortDir.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidStockOperationException("sortDir must be either asc or desc.");
        }
    }

    private <T> Page<T> buildPage(List<T> items, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidStockOperationException("page must be zero or greater and size must be greater than zero.");
        }

        int start = Math.min(page * size, items.size());
        int end = Math.min(start + size, items.size());
        List<T> content = start >= end ? List.of() : items.subList(start, end);
        return new PageImpl<>(content, PageRequest.of(page, size), items.size());
    }
}
