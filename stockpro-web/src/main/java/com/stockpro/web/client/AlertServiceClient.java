package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.AlertSeverity;
import com.stockpro.web.dto.AlertType;
import com.stockpro.web.dto.request.PlatformAlertRequest;
import com.stockpro.web.dto.response.AlertResponse;
import com.stockpro.web.dto.response.ApiPageResponse;
import com.stockpro.web.dto.response.UnreadCountResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "alertServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface AlertServiceClient {

    @GetMapping("/api/v1/alerts")
    ApiPageResponse<AlertResponse> getAlerts(
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false, name = "isRead") Boolean isRead,
            @RequestParam(required = false, name = "isAcknowledged") Boolean isAcknowledged,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @GetMapping("/api/v1/alerts/recipient/{recipientId}")
    List<AlertResponse> getAlertsByRecipient(@PathVariable("recipientId") Long recipientId);

    @GetMapping("/api/v1/alerts/unread-count/{recipientId}")
    UnreadCountResponse getUnreadCount(@PathVariable("recipientId") Long recipientId);

    @PutMapping("/api/v1/alerts/{alertId}/read")
    AlertResponse markAsRead(@PathVariable("alertId") Long alertId);

    @PutMapping("/api/v1/alerts/{alertId}/acknowledge")
    AlertResponse acknowledge(@PathVariable("alertId") Long alertId);

    @PostMapping("/api/v1/alerts")
    AlertResponse sendAlert(@RequestBody PlatformAlertRequest request);
}
