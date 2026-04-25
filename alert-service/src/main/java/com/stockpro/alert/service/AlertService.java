package com.stockpro.alert.service;

import com.stockpro.alert.dto.event.LowStockEvent;
import com.stockpro.alert.dto.event.OverdueReceiptEvent;
import com.stockpro.alert.dto.event.OverstockEvent;
import com.stockpro.alert.dto.event.PoPendingApprovalEvent;
import com.stockpro.alert.dto.event.SystemAlertEvent;
import com.stockpro.alert.dto.request.AlertSearchRequest;
import com.stockpro.alert.dto.request.SendAlertRequest;
import com.stockpro.alert.dto.request.SendBulkAlertRequest;
import com.stockpro.alert.dto.response.AlertResponse;
import com.stockpro.alert.dto.response.UnreadCountResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface AlertService {

    AlertResponse sendAlert(SendAlertRequest request);

    List<AlertResponse> sendBulk(SendBulkAlertRequest request);

    AlertResponse handleLowStockEvent(LowStockEvent event);

    AlertResponse handleOverstockEvent(OverstockEvent event);

    AlertResponse handlePoPendingApprovalEvent(PoPendingApprovalEvent event);

    AlertResponse handleOverdueReceiptEvent(OverdueReceiptEvent event);

    AlertResponse handleSystemAlertEvent(SystemAlertEvent event);

    AlertResponse markAsRead(Long alertId);

    void markAllRead(Long recipientId);

    AlertResponse acknowledge(Long alertId);

    List<AlertResponse> getByRecipient(Long recipientId);

    Page<AlertResponse> searchAlerts(AlertSearchRequest request);

    UnreadCountResponse getUnreadCount(Long recipientId);

    List<AlertResponse> getUnacknowledged(Long recipientId);

    void deleteAlert(Long alertId);
}
