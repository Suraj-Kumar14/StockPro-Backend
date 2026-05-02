package com.stockpro.alertservice.dto.response;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertAnalyticsResponse {
    private Map<String, Long> alertsByType;
    private Map<String, Long> alertsBySeverity;
    private Map<String, Long> alertsByStatus;
    private Map<String, Long> alertsByRole;
    private Map<String, Long> dailyAlertTrend;
    private List<AlertEntityCount> topAlertedProducts;
    private List<AlertEntityCount> topAlertedWarehouses;

    @Data
    @Builder
    public static class AlertEntityCount {
        private Long id;
        private long count;
    }
}
