package com.stockpro.purchaseservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import org.junit.jupiter.api.Test;

class PurchaseOrderWorkflowTest {

    private final PurchaseOrderWorkflow workflow = new PurchaseOrderWorkflow();

    @Test
    void assertDraftRejectsNonDraftState() {
        PurchaseOrderWorkflow.PurchaseOrderStateSnapshot snapshot =
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.APPROVED);

        assertThrows(InvalidPOStateException.class, () -> workflow.assertDraft(snapshot));
    }

    @Test
    void assertDraftAndApprovalTransitionsAllowExpectedStates() {
        assertDoesNotThrow(() -> workflow.assertDraft(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.DRAFT)));
        assertDoesNotThrow(() -> workflow.assertCanSubmit(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.DRAFT)));
        assertDoesNotThrow(() -> workflow.assertCanApprove(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.PENDING_APPROVAL)));
        assertDoesNotThrow(() -> workflow.assertCanReject(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.PENDING_APPROVAL)));
    }

    @Test
    void assertCanCancelAllowsDraftAndPendingApprovalOnly() {
        assertDoesNotThrow(() -> workflow.assertCanCancel(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.DRAFT)));
        assertDoesNotThrow(() -> workflow.assertCanCancel(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.PENDING_APPROVAL)));
        assertThrows(InvalidPOStateException.class, () -> workflow.assertCanCancel(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.APPROVED)));
    }

    @Test
    void assertCanReceiveAllowsPaidStatesOnly() {
        assertDoesNotThrow(() -> workflow.assertCanReceive(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.PAID)));
        assertDoesNotThrow(() -> workflow.assertCanReceive(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.PARTIALLY_RECEIVED)));
        assertThrows(InvalidPOStateException.class, () -> workflow.assertCanReceive(
                new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(POStatus.APPROVED)));
    }
}
