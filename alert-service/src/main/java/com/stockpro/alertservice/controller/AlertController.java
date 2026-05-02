package com.stockpro.alertservice.controller;

import com.stockpro.alertservice.dto.request.AcknowledgeAlertRequest;
import com.stockpro.alertservice.dto.request.AlertSearchRequest;
import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.request.CreateBroadcastAlertRequest;
import com.stockpro.alertservice.dto.request.DismissAlertRequest;
import com.stockpro.alertservice.dto.response.AlertAnalyticsResponse;
import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.dto.response.AlertSummaryResponse;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import com.stockpro.alertservice.security.AuthenticatedUser;
import com.stockpro.alertservice.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Management", description = "Role-based alert center, broadcast, acknowledgement, and analytics APIs")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a manual alert")
    public ResponseEntity<AlertResponse> createAlert(@Valid @RequestBody CreateAlertRequest request, Authentication authentication) {
        return ResponseEntity.status(201).body(alertService.createAlert(request, actorId(authentication)));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a broadcast alert")
    public ResponseEntity<List<AlertResponse>> createBroadcastAlert(
            @Valid @RequestBody CreateBroadcastAlertRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(alertService.createBroadcastAlert(request, actorId(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get an alert by id")
    public ResponseEntity<AlertResponse> getAlertById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(alertService.getAlertById(id, actorId(authentication), role(authentication), isAdmin(authentication)));
    }

    @GetMapping("/number/{alertNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get an alert by alert number")
    public ResponseEntity<AlertResponse> getAlertByNumber(@PathVariable String alertNumber, Authentication authentication) {
        return ResponseEntity.ok(alertService.getAlertByNumber(alertNumber, actorId(authentication), role(authentication), isAdmin(authentication)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get current user's alerts")
    public ResponseEntity<Page<AlertResponse>> getMyAlerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isAcknowledged,
            @RequestParam(required = false) Boolean isDismissed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        AlertSearchRequest request = AlertSearchRequest.builder()
                .keyword(keyword)
                .type(type)
                .severity(severity)
                .status(status)
                .isRead(isRead)
                .isAcknowledged(isAcknowledged)
                .isDismissed(isDismissed)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
        return ResponseEntity.ok(alertService.getMyAlerts(actorId(authentication), role(authentication), request));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Search alerts")
    public ResponseEntity<Page<AlertResponse>> searchAlerts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) String recipientRole,
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isAcknowledged,
            @RequestParam(required = false) Boolean isDismissed,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) String sourceService,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        AlertSearchRequest request = AlertSearchRequest.builder()
                .keyword(keyword)
                .recipientId(recipientId)
                .recipientRole(recipientRole)
                .type(type)
                .severity(severity)
                .status(status)
                .isRead(isRead)
                .isAcknowledged(isAcknowledged)
                .isDismissed(isDismissed)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .sourceService(sourceService)
                .fromDate(fromDate)
                .toDate(toDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
        return ResponseEntity.ok(alertService.searchAlerts(request, actorId(authentication), role(authentication), isAdmin(authentication)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Mark alert as read")
    public ResponseEntity<AlertResponse> markAsRead(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(alertService.markAsRead(id, actorId(authentication), role(authentication), isAdmin(authentication)));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Mark all alerts as read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        alertService.markAllAsRead(actorId(authentication), role(authentication));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<AlertResponse> acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody(required = false) AcknowledgeAlertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(
                id,
                request != null ? request : new AcknowledgeAlertRequest(),
                actorId(authentication),
                role(authentication),
                isAdmin(authentication)));
    }

    @PatchMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Dismiss an alert")
    public ResponseEntity<AlertResponse> dismissAlert(
            @PathVariable Long id,
            @RequestBody(required = false) DismissAlertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(alertService.dismissAlert(
                id,
                request != null ? request : new DismissAlertRequest(),
                actorId(authentication),
                role(authentication),
                isAdmin(authentication)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve an alert")
    public ResponseEntity<AlertResponse> resolveAlert(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(alertService.resolveAlert(id, actorId(authentication)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get unread alert count for current user")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(alertService.getUnreadCount(actorId(authentication), role(authentication)));
    }

    @GetMapping("/summary/my")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get current user's alert summary")
    public ResponseEntity<AlertSummaryResponse> getMySummary(Authentication authentication) {
        return ResponseEntity.ok(alertService.getMyAlertSummary(actorId(authentication), role(authentication)));
    }

    @GetMapping("/summary/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system-wide alert summary")
    public ResponseEntity<AlertSummaryResponse> getSystemSummary() {
        return ResponseEntity.ok(alertService.getSystemAlertSummary());
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get alert analytics")
    public ResponseEntity<AlertAnalyticsResponse> getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(alertService.getAlertAnalytics(toStart(fromDate), toEnd(toDate)));
    }

    private Long actorId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal.userId();
    }

    private String role(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return "";
        }
        return principal.role();
    }

    private boolean isAdmin(Authentication authentication) {
        return "ADMIN".equalsIgnoreCase(role(authentication));
    }

    private LocalDateTime toStart(LocalDate value) {
        return value != null ? value.atStartOfDay() : null;
    }

    private LocalDateTime toEnd(LocalDate value) {
        return value != null ? value.plusDays(1).atStartOfDay().minusNanos(1) : null;
    }
}
