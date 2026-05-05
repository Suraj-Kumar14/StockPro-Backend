package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import org.springframework.stereotype.Component;

@Component
public class PurchaseOrderWorkflow {

    public void assertDraft(PurchaseOrderStateSnapshot snapshot) {
        assertState(snapshot.status(), POStatus.DRAFT,
                "Only DRAFT POs can be modified");
    }

    public void assertCanSubmit(PurchaseOrderStateSnapshot snapshot) {
        assertState(snapshot.status(), POStatus.DRAFT,
                "Only DRAFT POs can be submitted");
    }

    public void assertCanApprove(PurchaseOrderStateSnapshot snapshot) {
        assertState(snapshot.status(), POStatus.PENDING_APPROVAL,
                "Only PENDING_APPROVAL POs can be approved");
    }

    public void assertCanReject(PurchaseOrderStateSnapshot snapshot) {
        assertState(snapshot.status(), POStatus.PENDING_APPROVAL,
                "Only PENDING_APPROVAL POs can be rejected");
    }

    public void assertCanCancel(PurchaseOrderStateSnapshot snapshot) {
        if (snapshot.status() != POStatus.DRAFT && snapshot.status() != POStatus.PENDING_APPROVAL) {
            throw new InvalidPOStateException(
                    "Only DRAFT or PENDING_APPROVAL POs can be cancelled. Current: " + snapshot.status());
        }
    }

    public void assertCanReceive(PurchaseOrderStateSnapshot snapshot) {
        if (snapshot.status() != POStatus.APPROVED
                && snapshot.status() != POStatus.PARTIALLY_RECEIVED) {
            throw new InvalidPOStateException(
                    "Goods can only be received for APPROVED or PARTIALLY_RECEIVED POs. Current: "
                            + snapshot.status());
        }
    }

    private void assertState(POStatus actualStatus, POStatus expectedStatus, String message) {
        if (actualStatus != expectedStatus) {
            throw new InvalidPOStateException(message + ". Current: " + actualStatus);
        }
    }

    public record PurchaseOrderStateSnapshot(POStatus status) {
    }
}
