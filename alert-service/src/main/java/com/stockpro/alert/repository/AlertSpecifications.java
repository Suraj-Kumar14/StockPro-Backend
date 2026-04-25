package com.stockpro.alert.repository;

import com.stockpro.alert.dto.request.AlertSearchRequest;
import com.stockpro.alert.entity.Alert;
import org.springframework.data.jpa.domain.Specification;

public final class AlertSpecifications {

    private AlertSpecifications() {
    }

    public static Specification<Alert> withFilters(AlertSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (request.getRecipientId() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("recipientId"), request.getRecipientId()));
            }

            if (request.getType() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("type"), request.getType()));
            }

            if (request.getSeverity() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("severity"), request.getSeverity()));
            }

            if (request.getIsRead() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("isRead"), request.getIsRead()));
            }

            if (request.getIsAcknowledged() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("isAcknowledged"), request.getIsAcknowledged()));
            }

            if (request.getStartDate() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.getStartDate()));
            }

            if (request.getEndDate() != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), request.getEndDate()));
            }

            return predicate;
        };
    }
}
