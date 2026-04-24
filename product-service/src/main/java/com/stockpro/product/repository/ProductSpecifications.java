package com.stockpro.product.repository;

import com.stockpro.product.dto.request.ProductSearchRequest;
import com.stockpro.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> withFilters(ProductSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (StringUtils.hasText(request.getName())) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                                "%" + request.getName().trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(request.getCategory())) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")),
                                request.getCategory().trim().toLowerCase()));
            }

            if (StringUtils.hasText(request.getBrand())) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("brand")),
                                request.getBrand().trim().toLowerCase()));
            }

            if (StringUtils.hasText(request.getSku())) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("sku")),
                                request.getSku().trim().toLowerCase()));
            }

            if (StringUtils.hasText(request.getBarcode())) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.get("barcode")),
                                request.getBarcode().trim().toLowerCase()));
            }

            if (request.getIsActive() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("isActive"), request.getIsActive()));
            }

            return predicate;
        };
    }
}
