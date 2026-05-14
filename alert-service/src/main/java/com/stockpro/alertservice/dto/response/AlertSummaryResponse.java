package com.stockpro.alertservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertSummaryResponse {
    private long totalAlerts;
    private long unreadCount;
    private long acknowledgedCount;
    private long dismissedCount;
    private long criticalCount;
    private long warningCount;
    private long infoCount;
    private long lowStockCount;
    private long overstockCount;
    private long pendingPoApprovalCount;
    private long overduePoCount;
}
