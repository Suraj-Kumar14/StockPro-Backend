package com.stockpro.movement.repository;

import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.entity.StockMovement;
import org.springframework.data.jpa.domain.Specification;

public final class MovementSpecifications {

    private MovementSpecifications() {
    }

    public static Specification<StockMovement> withFilters(MovementSearchRequest request) {
        return Specification.where(hasProductId(request.getProductId()))
                .and(hasWarehouseId(request.getWarehouseId()))
                .and(hasMovementType(request.getMovementType()))
                .and(hasReferenceId(request.getReferenceId()))
                .and(hasPerformedBy(request.getPerformedBy()))
                .and(isOnOrAfter(request.getStartDate()))
                .and(isOnOrBefore(request.getEndDate()));
    }

    private static Specification<StockMovement> hasProductId(Long productId) {
        return (root, query, criteriaBuilder) -> productId == null
                ? null
                : criteriaBuilder.equal(root.get("productId"), productId);
    }

    private static Specification<StockMovement> hasWarehouseId(Long warehouseId) {
        return (root, query, criteriaBuilder) -> warehouseId == null
                ? null
                : criteriaBuilder.equal(root.get("warehouseId"), warehouseId);
    }

    private static Specification<StockMovement> hasMovementType(Object movementType) {
        return (root, query, criteriaBuilder) -> movementType == null
                ? null
                : criteriaBuilder.equal(root.get("movementType"), movementType);
    }

    private static Specification<StockMovement> hasReferenceId(Long referenceId) {
        return (root, query, criteriaBuilder) -> referenceId == null
                ? null
                : criteriaBuilder.equal(root.get("referenceId"), referenceId);
    }

    private static Specification<StockMovement> hasPerformedBy(Long performedBy) {
        return (root, query, criteriaBuilder) -> performedBy == null
                ? null
                : criteriaBuilder.equal(root.get("performedBy"), performedBy);
    }

    private static Specification<StockMovement> isOnOrAfter(java.time.LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> startDate == null
                ? null
                : criteriaBuilder.greaterThanOrEqualTo(root.get("movementDate"), startDate);
    }

    private static Specification<StockMovement> isOnOrBefore(java.time.LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> endDate == null
                ? null
                : criteriaBuilder.lessThanOrEqualTo(root.get("movementDate"), endDate);
    }
}
