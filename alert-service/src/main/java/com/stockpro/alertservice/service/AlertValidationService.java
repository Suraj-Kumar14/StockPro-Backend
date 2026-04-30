package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import com.stockpro.alertservice.exception.InvalidAlertException;
import org.springframework.stereotype.Component;

@Component
public class AlertValidationService {

    public void validateRequest(AlertRequestDTO request) {
        if (request.getRecipientId() == null) {
            throw new InvalidAlertException("Recipient ID is required");
        }
        if (request.getType() == null) {
            throw new InvalidAlertException("Alert type is required");
        }
        if (request.getSeverity() == null) {
            throw new InvalidAlertException("Severity is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new InvalidAlertException("Title is required");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new InvalidAlertException("Message is required");
        }

        validateDomainRules(request);
    }

    private void validateDomainRules(AlertRequestDTO request) {
        if (request.getType() == AlertType.LOW_STOCK && request.getRelatedProductId() == null) {
            throw new InvalidAlertException("Low stock alerts must include relatedProductId");
        }
        if (request.getType() == AlertType.LOW_STOCK && request.getRelatedWarehouseId() == null) {
            throw new InvalidAlertException("Low stock alerts must include relatedWarehouseId");
        }
        if (request.getType() == AlertType.OVERSTOCK && request.getRelatedWarehouseId() == null) {
            throw new InvalidAlertException("Overstock alerts must include relatedWarehouseId");
        }
        if ((request.getType() == AlertType.PO_PENDING || request.getType() == AlertType.PO_PENDING_APPROVAL
                || request.getType() == AlertType.OVERDUE_RECEIPT)
                && request.getRelatedPurchaseOrderId() == null) {
            throw new InvalidAlertException("PO-related alerts must include relatedPurchaseOrderId");
        }
        if (request.getSeverity() == Severity.CRITICAL
                && request.getType() == AlertType.SYSTEM
                && (request.getChannel() == null || request.getChannel().trim().isEmpty())) {
            throw new InvalidAlertException("Critical system alerts must define a notification channel");
        }
    }
}
