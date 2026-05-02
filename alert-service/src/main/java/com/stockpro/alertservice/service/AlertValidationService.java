package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.request.CreateBroadcastAlertRequest;
import com.stockpro.alertservice.exception.InvalidAlertException;
import org.springframework.stereotype.Component;

@Component
public class AlertValidationService {

    public void validateCreateAlert(CreateAlertRequest request) {
        if (isBlank(request.getTitle())) {
            throw new InvalidAlertException("Alert title is required");
        }
        if (isBlank(request.getMessage())) {
            throw new InvalidAlertException("Alert message is required");
        }
        if (request.getRecipientId() == null && isBlank(request.getRecipientRole())) {
            throw new InvalidAlertException("Recipient ID or recipient role is required");
        }
    }

    public void validateBroadcast(CreateBroadcastAlertRequest request) {
        if ((request.getRecipientRoles() == null || request.getRecipientRoles().isEmpty())
                && (request.getRecipientIds() == null || request.getRecipientIds().isEmpty())) {
            throw new InvalidAlertException("Broadcast requires at least one recipient");
        }
        if (isBlank(request.getTitle())) {
            throw new InvalidAlertException("Alert title is required");
        }
        if (isBlank(request.getMessage())) {
            throw new InvalidAlertException("Alert message is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
