package com.stockpro.alert.controller;

import com.stockpro.alert.dto.request.AlertSearchRequest;
import com.stockpro.alert.dto.request.SendAlertRequest;
import com.stockpro.alert.dto.request.SendBulkAlertRequest;
import com.stockpro.alert.dto.response.AlertActionResponse;
import com.stockpro.alert.dto.response.AlertResponse;
import com.stockpro.alert.dto.response.UnreadCountResponse;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import com.stockpro.alert.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({ "/api/v1/alerts", "/alerts" })
@Tag(name = "Alert Management", description = "In-app alert center, bulk/system alerts, unread state, acknowledgement and search APIs.")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private static final String SEND_ALERT_EXAMPLE = """
            {
              "recipientId": 101,
              "type": "SYSTEM",
              "severity": "INFO",
              "title": "Scheduled maintenance",
              "message": "StockPro maintenance starts tonight at 11 PM.",
              "channel": "IN_APP"
            }
            """;

    private static final String SEND_BULK_ALERT_EXAMPLE = """
            {
              "recipientIds": [101, 102, 103],
              "type": "SYSTEM",
              "severity": "WARNING",
              "title": "Cycle count freeze",
              "message": "Inventory transactions are frozen during cycle counting.",
              "channel": "BOTH"
            }
            """;

    private static final String SEARCH_ALERT_EXAMPLE = """
            {
              "recipientId": 101,
              "type": "LOW_STOCK",
              "severity": "CRITICAL",
              "isRead": false,
              "isAcknowledged": false,
              "startDate": "2026-04-24T00:00:00",
              "endDate": "2026-04-25T23:59:59",
              "page": 0,
              "size": 20,
              "sortBy": "createdAt",
              "sortDir": "desc"
            }
            """;

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'PURCHASE_OFFICER')")
    @Operation(
            summary = "Send single alert",
            description = "Creates a single in-app alert and asynchronously publishes email delivery for CRITICAL or BOTH-channel alerts.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SendAlert", value = SEND_ALERT_EXAMPLE))))
    public ResponseEntity<AlertResponse> sendAlert(@Valid @RequestBody SendAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.sendAlert(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Send bulk alert",
            description = "Creates the same alert for multiple recipients. Intended for admin/system broadcasts.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SendBulkAlert", value = SEND_BULK_ALERT_EXAMPLE))))
    public ResponseEntity<List<AlertResponse>> sendBulk(@Valid @RequestBody SendBulkAlertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.sendBulk(request));
    }

    @GetMapping("/recipient/{recipientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get alerts by recipient", description = "Returns alert center data for a recipient. Non-admin users can only access their own alerts.")
    public ResponseEntity<List<AlertResponse>> getByRecipient(@PathVariable Long recipientId) {
        return ResponseEntity.ok(alertService.getByRecipient(recipientId));
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Search alerts",
            description = "Searches alerts with recipient, status, severity and date range filters. Non-admin users are automatically scoped to their own alerts.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SearchAlerts", value = SEARCH_ALERT_EXAMPLE))))
    public ResponseEntity<Page<AlertResponse>> searchAlerts(@Valid @RequestBody AlertSearchRequest request) {
        return ResponseEntity.ok(alertService.searchAlerts(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get or search alerts", description = "Query-parameter variant of alert search for gateway or frontend integrations.")
    public ResponseEntity<Page<AlertResponse>> getAlerts(
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isAcknowledged,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        AlertSearchRequest request = AlertSearchRequest.builder()
                .recipientId(recipientId)
                .type(type)
                .severity(severity)
                .isRead(isRead)
                .isAcknowledged(isAcknowledged)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();

        return ResponseEntity.ok(alertService.searchAlerts(request));
    }

    @GetMapping("/unread-count/{recipientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Returns the unread alert count for a recipient.")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(alertService.getUnreadCount(recipientId));
    }

    @GetMapping("/unacknowledged/{recipientId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unacknowledged alerts", description = "Returns alerts that have not yet been acknowledged.")
    public ResponseEntity<List<AlertResponse>> getUnacknowledged(@PathVariable Long recipientId) {
        return ResponseEntity.ok(alertService.getUnacknowledged(recipientId));
    }

    @PutMapping("/{alertId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark alert as read", description = "Marks a single alert as read and sets readAt.")
    public ResponseEntity<AlertResponse> markAsRead(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.markAsRead(alertId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all alerts as read", description = "Marks all unread alerts for a recipient as read.")
    public ResponseEntity<AlertActionResponse> markAllRead(@PathVariable Long recipientId) {
        alertService.markAllRead(recipientId);
        return ResponseEntity.ok(AlertActionResponse.builder()
                .alertId(null)
                .message("All unread alerts marked as read.")
                .build());
    }

    @PutMapping("/{alertId}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Acknowledge alert", description = "Acknowledges a single alert and sets acknowledgedAt.")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.acknowledge(alertId));
    }

    @DeleteMapping("/{alertId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete alert", description = "Deletes an alert if the current user owns it or is an admin.")
    public ResponseEntity<AlertActionResponse> deleteAlert(@PathVariable Long alertId) {
        alertService.deleteAlert(alertId);
        return ResponseEntity.ok(AlertActionResponse.builder()
                .alertId(alertId)
                .message("Alert deleted successfully.")
                .build());
    }
}
