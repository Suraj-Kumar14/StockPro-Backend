package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.dto.request.StockSearchRequest;
import com.stockpro.warehouse.entity.StockLevel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class StockLevelSpecifications {

    private StockLevelSpecifications() {
    }

    public static Specification<StockLevel> withFilters(StockSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (request.getWarehouseId() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("warehouseId"), request.getWarehouseId()));
            }

            if (request.getProductId() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("productId"), request.getProductId()));
            }

            if (StringUtils.hasText(request.getLocation())) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("location")),
                                "%" + request.getLocation().trim().toLowerCase() + "%"));
            }

            return predicate;
        };
    }
}
