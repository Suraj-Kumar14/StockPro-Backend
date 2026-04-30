package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.StockMovementRequestDTO;
import com.stockpro.movementservice.dto.StockMovementResponseDTO;
import com.stockpro.movementservice.entity.MovementType;
import com.stockpro.movementservice.entity.StockMovement;
import com.stockpro.movementservice.exception.InvalidMovementException;
import com.stockpro.movementservice.exception.MovementNotFoundException;
import com.stockpro.movementservice.exception.NegativeStockException;
import com.stockpro.movementservice.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final StockMovementMapper movementMapper;
    private final StockMovementValidationService validationService;

    @Transactional
    public StockMovementResponseDTO recordMovement(StockMovementRequestDTO dto) {
        validationService.validateForRecord(dto);

        int previousBalance = getPreviousBalance(dto.getProductId(), dto.getWarehouseId());
        int computedBalanceAfter = calculateBalanceAfter(dto, previousBalance);
        validationService.validateBalanceAfter(computedBalanceAfter, dto.getBalanceAfter());

        StockMovement movement = StockMovement.builder()
                .productId(dto.getProductId())
                .warehouseId(dto.getWarehouseId())
                .movementType(dto.getMovementType())
                .quantity(dto.getQuantity())
                .referenceId(dto.getReferenceId())
                .referenceType(trimToNull(dto.getReferenceType()))
                .unitCost(dto.getUnitCost())
                .performedBy(dto.getPerformedBy())
                .notes(trimToNull(dto.getNotes()))
                .balanceAfter(computedBalanceAfter)
                .build();

        StockMovement saved = movementRepository.save(movement);
        log.info("Recorded movement {} for product {} warehouse {} type {} balanceAfter {}",
                saved.getMovementId(), saved.getProductId(), saved.getWarehouseId(),
                saved.getMovementType(), saved.getBalanceAfter());
        return movementMapper.toResponse(saved);
    }

    @Transactional
    public List<StockMovementResponseDTO> recordTransferPair(
            StockMovementRequestDTO transferOutRequest,
            StockMovementRequestDTO transferInRequest) {
        if (transferOutRequest.getMovementType() != MovementType.TRANSFER_OUT
                || transferInRequest.getMovementType() != MovementType.TRANSFER_IN) {
            throw new InvalidMovementException("Transfer pair must contain TRANSFER_OUT and TRANSFER_IN movements");
        }
        if (!transferOutRequest.getReferenceId().equals(transferInRequest.getReferenceId())) {
            throw new InvalidMovementException("Transfer pair must share the same referenceId");
        }
        if (!transferOutRequest.getQuantity().equals(transferInRequest.getQuantity())) {
            throw new InvalidMovementException("Transfer pair quantities must match");
        }

        return List.of(recordMovement(transferOutRequest), recordMovement(transferInRequest));
    }

    public StockMovementResponseDTO getMovementById(Long id) {
        return movementMapper.toResponse(findMovementById(id));
    }

    public List<StockMovementResponseDTO> getAllMovements() {
        return movementRepository.findAllByOrderByMovementDateDescMovementIdDesc().stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getByProduct(Long productId) {
        return movementRepository.findByProductId(productId).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getByWarehouse(Long warehouseId) {
        return movementRepository.findByWarehouseId(warehouseId).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getByType(String type) {
        MovementType movementType = parseMovementType(type);
        return movementRepository.findByMovementType(movementType).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getByReference(Long referenceId) {
        return movementRepository.findByReferenceId(referenceId).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getByPerformedBy(Long userId) {
        return movementRepository.findByPerformedBy(userId).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getByDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new InvalidMovementException("Start date cannot be after end date");
        }
        return movementRepository.findByMovementDateBetween(start, end).stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public List<StockMovementResponseDTO> getMovementHistory(Long productId, Long warehouseId) {
        return movementRepository
                .findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(productId, warehouseId)
                .stream()
                .map(movementMapper::toResponse)
                .toList();
    }

    public Integer getTotalStockIn(Long productId, Long warehouseId) {
        return getMovementHistory(productId, warehouseId).stream()
                .filter(this::isInboundMovement)
                .mapToInt(StockMovementResponseDTO::getQuantity)
                .sum();
    }

    public Integer getTotalStockOut(Long productId, Long warehouseId) {
        return getMovementHistory(productId, warehouseId).stream()
                .filter(this::isOutboundMovement)
                .mapToInt(response -> Math.abs(response.getQuantity()))
                .sum();
    }

    private StockMovement findMovementById(Long id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new MovementNotFoundException("Movement not found with ID: " + id));
    }

    private int getPreviousBalance(Long productId, Long warehouseId) {
        Pageable firstRecord = PageRequest.of(0, 1);
        return movementRepository.findLatestBalanceCandidates(productId, warehouseId, firstRecord).stream()
                .findFirst()
                .map(StockMovement::getBalanceAfter)
                .orElse(0);
    }

    private int calculateBalanceAfter(StockMovementRequestDTO request, int previousBalance) {
        int quantity = request.getQuantity();
        int balanceAfter;

        switch (request.getMovementType()) {
            case STOCK_IN, TRANSFER_IN -> balanceAfter = previousBalance + quantity;
            case STOCK_OUT, TRANSFER_OUT, WRITE_OFF -> balanceAfter = previousBalance - quantity;
            case RETURN -> balanceAfter = resolveReturnBalance(previousBalance, request);
            case ADJUSTMENT -> balanceAfter = previousBalance + quantity;
            default -> throw new InvalidMovementException("Unsupported movement type: " + request.getMovementType());
        }

        if (balanceAfter < 0) {
            throw new NegativeStockException(
                    "Movement would result in negative stock for product "
                            + request.getProductId() + " in warehouse " + request.getWarehouseId());
        }
        return balanceAfter;
    }

    private int resolveReturnBalance(int previousBalance, StockMovementRequestDTO request) {
        String referenceType = trimToNull(request.getReferenceType());
        if (referenceType != null && referenceType.equalsIgnoreCase("SUPPLIER_RETURN")) {
            return previousBalance - Math.abs(request.getQuantity());
        }
        return previousBalance + Math.abs(request.getQuantity());
    }

    private MovementType parseMovementType(String type) {
        try {
            return MovementType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidMovementException("Invalid movement type: " + type);
        }
    }

    private boolean isInboundMovement(StockMovementResponseDTO response) {
        return response.getMovementType() == MovementType.STOCK_IN
                || response.getMovementType() == MovementType.TRANSFER_IN
                || (response.getMovementType() == MovementType.RETURN
                && !"SUPPLIER_RETURN".equalsIgnoreCase(response.getReferenceType()))
                || (response.getMovementType() == MovementType.ADJUSTMENT && response.getQuantity() > 0);
    }

    private boolean isOutboundMovement(StockMovementResponseDTO response) {
        return response.getMovementType() == MovementType.STOCK_OUT
                || response.getMovementType() == MovementType.TRANSFER_OUT
                || response.getMovementType() == MovementType.WRITE_OFF
                || (response.getMovementType() == MovementType.RETURN
                && "SUPPLIER_RETURN".equalsIgnoreCase(response.getReferenceType()))
                || (response.getMovementType() == MovementType.ADJUSTMENT && response.getQuantity() < 0);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
