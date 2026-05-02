package com.stockpro.supplierservice.repository;

import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.entity.SupplierStatus;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

public final class SupplierSpecifications {
    private SupplierSpecifications() {}

    public static Specification<Supplier> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("supplierCode")), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("contactPerson")), pattern),
                cb.like(cb.lower(root.get("city")), pattern),
                cb.like(cb.lower(root.get("country")), pattern)
        );
    }

    public static Specification<Supplier> status(SupplierStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Supplier> isActive(Boolean isActive) {
        return isActive == null ? null : (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<Supplier> city(String city) {
        if (city == null || city.isBlank()) return null;
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    public static Specification<Supplier> country(String country) {
        if (country == null || country.isBlank()) return null;
        return (root, query, cb) -> cb.equal(cb.lower(root.get("country")), country.toLowerCase());
    }

    public static Specification<Supplier> minRating(BigDecimal minRating) {
        return minRating == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    public static Specification<Supplier> maxLeadTime(Integer maxLeadTimeDays) {
        return maxLeadTimeDays == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("leadTimeDays"), maxLeadTimeDays);
    }
}
