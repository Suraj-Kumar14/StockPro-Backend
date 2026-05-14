package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.request.CreateBroadcastAlertRequest;
import com.stockpro.alertservice.exception.InvalidAlertException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AlertValidationService {

    private static final Set<String> SUPPORTED_BROADCAST_ROLES = Set.of(
            "ALL",
            "ADMIN",
            "MANAGER",
            "OFFICER",
            "STAFF",
            "INVENTORY_MANAGER",
            "PURCHASE_OFFICER",
            "WAREHOUSE_STAFF");

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
        List<String> recipientRoles = request.resolveRecipientRoles();
        if (hasNoBroadcastTargets(recipientRoles, request.getRecipientIds())) {
            throw new InvalidAlertException("Broadcast requires at least one target role or recipient ID");
        }
        if (isBlank(request.getTitle())) {
            throw new InvalidAlertException("Alert title is required");
        }
        if (isBlank(request.getMessage())) {
            throw new InvalidAlertException("Alert message is required");
        }
        if (request.getSeverity() == null) {
            throw new InvalidAlertException("Alert severity is required");
        }
        validateBroadcastRoles(recipientRoles);
    }

    private boolean hasNoBroadcastTargets(List<String> recipientRoles, List<Long> recipientIds) {
        return (recipientRoles == null || recipientRoles.isEmpty())
                && (recipientIds == null || recipientIds.isEmpty());
    }

    private void validateBroadcastRoles(List<String> recipientRoles) {
        if (recipientRoles == null) {
            return;
        }
        for (String role : recipientRoles) {
            if (isBlank(role)) {
                throw new InvalidAlertException("Target role must not be blank");
            }
            if (!SUPPORTED_BROADCAST_ROLES.contains(role.trim().toUpperCase(Locale.ROOT))) {
                throw new InvalidAlertException("Unsupported target role: " + role);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
