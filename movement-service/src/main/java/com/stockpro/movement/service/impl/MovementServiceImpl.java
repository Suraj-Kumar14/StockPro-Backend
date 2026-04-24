package com.stockpro.movement.service.impl;

import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.MovementResponse;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.enums.MovementType;
import com.stockpro.movement.exception.InvalidMovementRequestException;
import com.stockpro.movement.exception.MovementNotFoundException;
import com.stockpro.movement.mapper.MovementMapper;
import com.stockpro.movement.repository.MovementRepository;
import com.stockpro.movement.repository.MovementSpecifications;
import com.stockpro.movement.security.AuthenticatedUser;
import com.stockpro.movement.security.SecurityUtils;
import com.stockpro.movement.service.MovementService;
import com.stockpro.movement.validation.MovementRequestValidator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class MovementServiceImpl implements MovementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MovementServiceImpl.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "movementId",
            "productId",
            "warehouseId",
            "movementType",
            "quantity",
            "referenceId",
            "performedBy",
            "movementDate",
            "balanceAfter",
            "createdAt");
    private static final EnumSet<MovementType> STOCK_IN_TYPES =
            EnumSet.of(MovementType.STOCK_IN, MovementType.TRANSFER_IN, MovementType.RETURN);
    private static final EnumSet<MovementType> STOCK_OUT_TYPES =
            EnumSet.of(MovementType.STOCK_OUT, MovementType.TRANSFER_OUT, MovementType.WRITE_OFF);

    private final MovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final MovementRequestValidator movementRequestValidator;
    private final SecurityUtils securityUtils;
    private final String applicationName;

    public MovementServiceImpl(MovementRepository movementRepository,
            MovementMapper movementMapper,
            MovementRequestValidator movementRequestValidator,
            SecurityUtils securityUtils,
            @Value("${spring.application.name:MOVEMENT-SERVICE}") String applicationName) {
        this.movementRepository = movementRepository;
        this.movementMapper = movementMapper;
        this.movementRequestValidator = movementRequestValidator;
        this.securityUtils = securityUtils;
        this.applicationName = applicationName;
    }

    @Override
    public MovementResponse recordMovement(RecordMovementRequest request) {
        AuthenticatedUser currentUser = securityUtils.getCurrentUser();
        movementRequestValidator.validateRecordRequest(request, currentUser);

        StockMovement movement = movementMapper.toEntity(request, currentUser, resolveSourceService());
        StockMovement savedMovement = movementRepository.save(movement);

        LOGGER.info("Recorded movement {} for productId={} warehouseId={} type={} quantity={}",
                savedMovement.getMovementId(),
                savedMovement.getProductId(),
                savedMovement.getWarehouseId(),
                savedMovement.getMovementType(),
                savedMovement.getQuantity());

        return movementMapper.toResponse(savedMovement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getByProduct(Long productId) {
        validatePositiveId(productId, "productId is required.");
        return mapRequiredList(movementRepository.findByProductId(productId),
                "No stock movements found for productId: " + productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getByWarehouse(Long warehouseId) {
        validatePositiveId(warehouseId, "warehouseId is required.");
        return mapRequiredList(movementRepository.findByWarehouseId(warehouseId),
                "No stock movements found for warehouseId: " + warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getByType(MovementType movementType) {
        if (movementType == null) {
            throw new InvalidMovementRequestException("movementType is required.");
        }
        return mapRequiredList(movementRepository.findByMovementType(movementType),
                "No stock movements found for movementType: " + movementType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        return mapRequiredList(movementRepository.findByMovementDateBetween(startDate, endDate),
                "No stock movements found for the given date range.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getByReference(Long referenceId) {
        validatePositiveId(referenceId, "referenceId is required.");
        return mapRequiredList(movementRepository.findByReferenceId(referenceId),
                "No stock movements found for referenceId: " + referenceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getMovementHistory(Long productId, Long warehouseId) {
        validatePositiveId(productId, "productId is required.");
        validatePositiveId(warehouseId, "warehouseId is required.");

        return mapRequiredList(
                movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(productId, warehouseId),
                "No stock movement history found for productId " + productId + " and warehouseId " + warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getStockIn(Long productId) {
        validatePositiveId(productId, "productId is required.");
        return movementRepository.sumQuantityByProductIdAndMovementTypes(productId, STOCK_IN_TYPES);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getStockOut(Long productId) {
        validatePositiveId(productId, "productId is required.");
        return movementRepository.sumQuantityByProductIdAndMovementTypes(productId, STOCK_OUT_TYPES);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovementResponse> getAllMovements(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return movementRepository.findAll(pageable).map(movementMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovementResponse> searchMovements(MovementSearchRequest request) {
        movementRequestValidator.validateSearchRequest(request);
        Pageable pageable = buildPageable(request.getPage(), request.getSize(), request.getSortBy(), request.getSortDir());

        return movementRepository.findAll(MovementSpecifications.withFilters(request), pageable)
                .map(movementMapper::toResponse);
    }

    private List<MovementResponse> mapRequiredList(List<StockMovement> movements, String notFoundMessage) {
        if (movements == null || movements.isEmpty()) {
            throw new MovementNotFoundException(notFoundMessage);
        }

        return movements.stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy, String sortDir) {
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 20 : size;
        if (safePage < 0 || safeSize <= 0) {
            throw new InvalidMovementRequestException("page must be zero or greater and size must be greater than zero.");
        }

        String resolvedSortBy = StringUtils.hasText(sortBy) ? sortBy.trim() : "movementDate";
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new InvalidMovementRequestException("Unsupported sortBy value: " + resolvedSortBy);
        }

        Sort.Direction direction = parseSortDirection(sortDir);
        return PageRequest.of(safePage, safeSize, Sort.by(direction, resolvedSortBy));
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if (!StringUtils.hasText(sortDir)) {
            return Sort.Direction.DESC;
        }

        try {
            return Sort.Direction.fromString(sortDir.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidMovementRequestException("sortDir must be either asc or desc.");
        }
    }

    private void validatePositiveId(Long value, String message) {
        if (value == null || value <= 0) {
            throw new InvalidMovementRequestException(message);
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidMovementRequestException("Both startDate and endDate are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidMovementRequestException("endDate cannot be before startDate.");
        }
    }

    private String resolveSourceService() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            String sourceServiceHeader = servletRequestAttributes.getRequest().getHeader("X-Source-Service");
            if (StringUtils.hasText(sourceServiceHeader)) {
                return sourceServiceHeader.trim().toUpperCase(Locale.ROOT);
            }
        }

        return applicationName;
    }
}
