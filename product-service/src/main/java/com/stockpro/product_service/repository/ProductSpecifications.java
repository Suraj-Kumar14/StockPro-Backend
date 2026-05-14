package com.stockpro.product_service.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.stockpro.product_service.entity.Product;

import jakarta.persistence.criteria.Predicate;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> search(
            String keyword,
            String category,
            String brand,
            Boolean isActive) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("sku")), pattern),
                        builder.like(builder.lower(builder.coalesce(root.get("barcode").as(String.class), "")), pattern),
                        builder.like(builder.lower(builder.coalesce(root.get("brand").as(String.class), "")), pattern),
                        builder.like(builder.lower(root.get("category")), pattern)));
            }

            if (hasText(category)) {
                predicates.add(builder.equal(builder.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (hasText(brand)) {
                predicates.add(builder.equal(builder.lower(root.get("brand")), brand.trim().toLowerCase()));
            }

            if (isActive != null) {
                predicates.add(builder.equal(root.get("isActive"), isActive));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
