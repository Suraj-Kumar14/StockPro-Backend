package com.stockpro.purchase.publisher;

import com.stockpro.purchase.dto.event.OverdueReceiptEvent;
import com.stockpro.purchase.dto.event.PoPendingApprovalEvent;

public interface AlertEventPublisher {

    void publishPoPendingApprovalEvent(PoPendingApprovalEvent event);

    void publishOverdueReceiptEvent(OverdueReceiptEvent event);
}
