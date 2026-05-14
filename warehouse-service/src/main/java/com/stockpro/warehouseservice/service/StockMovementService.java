package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.StockMovementResponseDTO;
import com.stockpro.warehouseservice.entity.StockMovement;
import com.stockpro.warehouseservice.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;

    public void recordReceipt(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason) {
        recordMovement(warehouseId, productId, "RECEIPT",
                quantityChanged, previousQuantity, newQuantity, null, reason);
    }

    public void recordIssue(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason) {
        recordMovement(warehouseId, productId, "ISSUE",
                quantityChanged, previousQuantity, newQuantity, null, reason);
    }

    public void recordAdjustment(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason) {
        recordMovement(warehouseId, productId, "ADJUSTMENT",
                quantityChanged, previousQuantity, newQuantity, null, reason);
    }

    public void recordReservation(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason) {
        recordMovement(warehouseId, productId, "RESERVATION",
                quantityChanged, previousQuantity, newQuantity, null, reason);
    }

    public void recordRelease(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason) {
        recordMovement(warehouseId, productId, "RELEASE",
                quantityChanged, previousQuantity, newQuantity, null, reason);
    }

    public void recordTransferOut(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason, Long relatedWarehouseId) {
        recordMovement(warehouseId, productId, "TRANSFER_OUT",
                quantityChanged, previousQuantity, newQuantity,
                relatedWarehouseId, reason);
    }

    public void recordTransferIn(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason, Long relatedWarehouseId) {
        recordMovement(warehouseId, productId, "TRANSFER_IN",
                quantityChanged, previousQuantity, newQuantity,
                relatedWarehouseId, reason);
    }

    public void recordAudit(Long warehouseId, Long productId,
            Integer quantityChanged, Integer previousQuantity,
            Integer newQuantity, String reason) {
        recordMovement(warehouseId, productId, "AUDIT",
                quantityChanged, previousQuantity, newQuantity, null, reason);
    }

    public List<StockMovementResponseDTO> getMovementHistory(
            Long warehouseId, Long productId) {
        List<StockMovement> movements;
        if (warehouseId != null && productId != null) {
            movements = stockMovementRepository
                    .findByWarehouseIdAndProductIdOrderByCreatedAtDesc(
                            warehouseId, productId);
        } else if (warehouseId != null) {
            movements = stockMovementRepository
                    .findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
        } else if (productId != null) {
            movements = stockMovementRepository
                    .findByProductIdOrderByCreatedAtDesc(productId);
        } else {
            movements = stockMovementRepository.findAll()
                    .stream()
                    .sorted((left, right) -> right.getCreatedAt()
                            .compareTo(left.getCreatedAt()))
                    .toList();
        }

        return movements.stream().map(this::mapToDto).toList();
    }

    private void recordMovement(Long warehouseId, Long productId,
            String movementType, Integer quantityChanged,
            Integer previousQuantity, Integer newQuantity,
            Long relatedWarehouseId, String reason) {
        StockMovement movement = StockMovement.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .movementType(movementType)
                .quantityChanged(quantityChanged)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .relatedWarehouseId(relatedWarehouseId)
                .reason(reason)
                .build();
        stockMovementRepository.save(movement);
        log.info("Recorded stock movement: operationType={}, warehouseId={}, productId={}",
                movementType, warehouseId, productId);
    }

    private StockMovementResponseDTO mapToDto(StockMovement movement) {
        return StockMovementResponseDTO.builder()
                .movementId(movement.getMovementId())
                .warehouseId(movement.getWarehouseId())
                .productId(movement.getProductId())
                .movementType(movement.getMovementType())
                .quantityChanged(movement.getQuantityChanged())
                .previousQuantity(movement.getPreviousQuantity())
                .newQuantity(movement.getNewQuantity())
                .relatedWarehouseId(movement.getRelatedWarehouseId())
                .reason(movement.getReason())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
