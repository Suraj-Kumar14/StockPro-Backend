package com.stockpro.alertservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.request.CreateBroadcastAlertRequest;
import com.stockpro.alertservice.dto.request.AlertSearchRequest;
import com.stockpro.alertservice.dto.request.AcknowledgeAlertRequest;
import com.stockpro.alertservice.dto.request.DismissAlertRequest;
import com.stockpro.alertservice.dto.response.AlertAnalyticsResponse;
import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.dto.response.AlertSummaryResponse;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import com.stockpro.alertservice.events.MovementAlertEvent;
import com.stockpro.alertservice.events.PurchaseAlertEvent;
import com.stockpro.alertservice.events.StockAlertEvent;
import com.stockpro.alertservice.events.SupplierAlertEvent;
import com.stockpro.alertservice.exception.InvalidAlertException;
import com.stockpro.alertservice.mail.EmailNotificationService;
import com.stockpro.alertservice.rabbitmq.AlertEventPublisher;
import com.stockpro.alertservice.repository.AlertRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertValidationService validationService;

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private AlertEventPublisher alertEventPublisher;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private AlertService alertService;

    private CreateAlertRequest request;
    private Alert alert;

    @BeforeEach
    void setUp() {
        request = new CreateAlertRequest();
        request.setRecipientRole("MANAGER");
        request.setType(AlertType.LOW_STOCK);
        request.setSeverity(AlertSeverity.WARNING);
        request.setChannel(AlertChannel.IN_APP);
        request.setTitle("Low Stock Alert");
        request.setMessage("Inventory is low");

        alert = Alert.builder()
                .alertId(1L)
                .alertNumber("ALT-20260501-000001")
                .recipientRole("MANAGER")
                .type(AlertType.LOW_STOCK)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.NEW)
                .channel(AlertChannel.IN_APP)
                .title("Low Stock Alert")
                .message("Inventory is low")
                .isRead(false)
                .isAcknowledged(false)
                .isDismissed(false)
                .createdAt(LocalDateTime.now())
                .build();

        org.mockito.Mockito.lenient().when(alertMapper.toResponse(any(Alert.class)))
                .thenAnswer(invocation -> mapToResponse(invocation.getArgument(0)));
    }

    @Test
    void createAlert_shouldCreateAlert_whenValidRequest() {
        doNothing().when(validationService).validateCreateAlert(request);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            saved.setAlertId(1L);
            return saved;
        });
        AlertResponse response = alertService.createAlert(request, 99L);

        assertEquals("Low Stock Alert", response.getTitle());
        verify(alertRepository).save(any(Alert.class));
        verify(alertEventPublisher).publish(eq("alert.created"), any());
        verify(emailNotificationService, never()).sendCriticalAlertEmail(any(), any());
    }

    @Test
    void createBroadcastAlert_shouldCreateAlertsForRolesAndRecipients() {
        CreateBroadcastAlertRequest broadcast = new CreateBroadcastAlertRequest();
        broadcast.setRecipientRoles(List.of("MANAGER", "ADMIN"));
        broadcast.setRecipientIds(List.of(11L));
        broadcast.setSeverity(AlertSeverity.WARNING);
        broadcast.setTitle("System message");
        broadcast.setMessage("Read this");

        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AlertResponse> responses = alertService.createBroadcastAlert(broadcast, 7L);

        assertEquals(3, responses.size());
        verify(validationService).validateBroadcast(broadcast);
        verify(alertRepository, org.mockito.Mockito.times(3)).save(any(Alert.class));
    }

    @Test
    void markAsRead_shouldUpdateStatus() {
        when(alertRepository.findByAlertId(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertResponse response = alertService.markAsRead(1L, 7L, "MANAGER", false);

        assertEquals(AlertStatus.READ, response.getStatus());
        assertFalse(Boolean.FALSE.equals(response.getIsRead()));
        verify(alertEventPublisher).publish(eq("alert.read"), any());
    }

    @Test
    void getAlertByNumber_shouldReturnAlertForAdmin() {
        when(alertRepository.findByAlertNumber("ALT-1")).thenReturn(Optional.of(alert));

        AlertResponse response = alertService.getAlertByNumber("ALT-1", 99L, "ADMIN", true);

        assertEquals(alert.getAlertNumber(), response.getAlertNumber());
    }

    @Test
    void getMyAlertsAndSearchAlerts_shouldDelegateToRepository() {
        AlertSearchRequest request = AlertSearchRequest.builder()
                .keyword("low")
                .recipientRole("MANAGER")
                .page(0)
                .size(5)
                .sortBy("createdAt")
                .sortDir("desc")
                .build();
        when(alertRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(alert)));

        var mine = alertService.getMyAlerts(7L, "MANAGER", request);
        var adminSearch = alertService.searchAlerts(request, 7L, "MANAGER", true);
        var userSearch = alertService.searchAlerts(request, 7L, "MANAGER", false);

        assertEquals(1, mine.getTotalElements());
        assertEquals(1, adminSearch.getTotalElements());
        assertEquals(1, userSearch.getTotalElements());
    }

    @Test
    void acknowledgeDismissResolveAndMarkAll_shouldUpdateLifecycle() {
        Alert active = copyAlert(alert);
        active.setStatus(AlertStatus.NEW);
        active.setIsRead(false);
        when(alertRepository.findByAlertId(1L)).thenReturn(Optional.of(active));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Alert unread = copyAlert(alert);
        unread.setIsRead(false);
        unread.setStatus(AlertStatus.NEW);
        when(alertRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(unread));

        AlertResponse acknowledged = alertService.acknowledgeAlert(1L, new AcknowledgeAlertRequest(), 7L, "MANAGER", false);
        AlertResponse dismissed = alertService.dismissAlert(1L, new DismissAlertRequest(), 7L, "MANAGER", true);
        AlertResponse resolved = alertService.resolveAlert(1L, 8L);
        alertService.markAllAsRead(7L, "MANAGER");

        assertEquals(AlertStatus.ACKNOWLEDGED, acknowledged.getStatus());
        assertEquals(AlertStatus.DISMISSED, dismissed.getStatus());
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
        verify(alertRepository).saveAll(any());
        verify(alertEventPublisher).publish(eq("alert.acknowledged"), any());
        verify(alertEventPublisher).publish(eq("alert.dismissed"), any());
        verify(alertEventPublisher).publish(eq("alert.resolved"), any());
    }

    @Test
    void acknowledgeAlert_shouldRejectDismissedAlert() {
        Alert dismissed = copyAlert(alert);
        dismissed.setStatus(AlertStatus.DISMISSED);
        when(alertRepository.findByAlertId(1L)).thenReturn(Optional.of(dismissed));

        assertThrows(InvalidAlertException.class,
                () -> alertService.acknowledgeAlert(1L, new AcknowledgeAlertRequest(), 7L, "MANAGER", true));
    }

    @Test
    void unreadSummaryAndAnalytics_shouldAggregateCounts() {
        Alert second = copyAlert(alert);
        second.setAlertId(2L);
        second.setType(AlertType.OVERSTOCK);
        second.setSeverity(AlertSeverity.CRITICAL);
        second.setStatus(AlertStatus.ACKNOWLEDGED);
        second.setRecipientRole("ADMIN");
        second.setRelatedProductId(5L);
        second.setRelatedWarehouseId(8L);
        second.setIsRead(true);
        second.setIsAcknowledged(true);
        second.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(alertRepository.countByRecipientIdAndIsReadFalse(7L)).thenReturn(2L);
        when(alertRepository.countByRecipientRoleAndIsReadFalse("MANAGER")).thenReturn(3L);
        when(alertRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(alert, second));
        when(alertRepository.findAll()).thenReturn(List.of(alert, second));

        long unread = alertService.getUnreadCount(7L, "MANAGER");
        AlertSummaryResponse mySummary = alertService.getMyAlertSummary(7L, "MANAGER");
        AlertSummaryResponse systemSummary = alertService.getSystemAlertSummary();
        AlertAnalyticsResponse analytics = alertService.getAlertAnalytics(LocalDateTime.now().minusDays(2), LocalDateTime.now());

        assertEquals(5L, unread);
        assertEquals(2L, mySummary.getTotalAlerts());
        assertEquals(2L, systemSummary.getTotalAlerts());
        assertEquals(2, analytics.getAlertsByType().size());
        assertEquals(1, analytics.getTopAlertedProducts().size());
    }

    @Test
    void getAlertById_shouldRejectUnauthorizedAccess() {
        Alert staffAlert = Alert.builder()
                .alertId(2L)
                .alertNumber("ALT-20260501-000002")
                .recipientRole("STAFF")
                .type(AlertType.GENERAL)
                .severity(AlertSeverity.INFO)
                .status(AlertStatus.NEW)
                .channel(AlertChannel.IN_APP)
                .title("General")
                .message("Message")
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .isAcknowledged(false)
                .isDismissed(false)
                .build();
        when(alertRepository.findByAlertId(2L)).thenReturn(Optional.of(staffAlert));

        assertThrows(AccessDeniedException.class, () -> alertService.getAlertById(2L, 11L, "OFFICER", false));
    }

    @Test
    void createAlertFromStockEvent_shouldCreateLowStockAlert() {
        StockAlertEvent event = new StockAlertEvent();
        event.setEventType("LOW_STOCK_DETECTED");
        event.setProductId(9L);
        event.setWarehouseId(4L);
        event.setProductName("Widget");
        event.setWarehouseName("Central");
        event.setAvailableQuantity(BigDecimal.valueOf(2));
        event.setReorderLevel(BigDecimal.valueOf(10));
        event.setCorrelationId("stock-9-4");

        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("stock-9-4", AlertType.LOW_STOCK, "MANAGER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("stock-9-4:ADMIN", AlertType.LOW_STOCK, "ADMIN")).thenReturn(false);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailNotificationService.sendCriticalAlertEmail(any(), any())).thenReturn(false);

        alertService.createAlertFromStockEvent(event);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals(AlertSeverity.CRITICAL, captor.getAllValues().get(0).getSeverity());
    }

    @Test
    void createAlertFromStockEvent_shouldHandleOverstockAndTransfer() {
        StockAlertEvent overstock = new StockAlertEvent();
        overstock.setEventType("OVERSTOCK_DETECTED");
        overstock.setProductId(10L);
        overstock.setWarehouseId(4L);
        overstock.setProductName("Widget");
        overstock.setWarehouseName("Central");
        overstock.setAvailableQuantity(BigDecimal.valueOf(20));
        overstock.setMaxStockLevel(BigDecimal.valueOf(12));
        overstock.setCorrelationId("over-10-4");

        StockAlertEvent transfer = new StockAlertEvent();
        transfer.setEventType("STOCK_TRANSFER_COMPLETED");
        transfer.setProductId(10L);
        transfer.setWarehouseId(4L);
        transfer.setProductName("Widget");
        transfer.setCorrelationId("transfer-10-4");

        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("over-10-4", AlertType.OVERSTOCK, "MANAGER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("transfer-10-4", AlertType.STOCK_TRANSFER, "STAFF")).thenReturn(false);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.createAlertFromStockEvent(overstock);
        alertService.createAlertFromStockEvent(transfer);

        verify(alertRepository, org.mockito.Mockito.atLeast(2)).save(any(Alert.class));
    }

    @Test
    void createAlertFromPurchaseEvent_shouldCreatePendingApprovalAlertsForAdminAndManager() {
        PurchaseAlertEvent event = new PurchaseAlertEvent();
        event.setEventType("purchase.pending-approval");
        event.setPurchaseOrderId(44L);
        event.setPurchaseOrderNumber("PO-20260505-0001");
        event.setMessage("Purchase Order PO-20260505-0001 is pending approval.");
        event.setCorrelationId("purchase.pending-approval:44:7");
        event.setCreatedBy(7L);

        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("purchase.pending-approval:44:7:MANAGER", AlertType.PO_APPROVAL_PENDING, "MANAGER"))
                .thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("purchase.pending-approval:44:7:ADMIN", AlertType.PO_APPROVAL_PENDING, "ADMIN"))
                .thenReturn(false);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.createAlertFromPurchaseEvent(event);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("MANAGER", captor.getAllValues().get(0).getRecipientRole());
        assertEquals("ADMIN", captor.getAllValues().get(1).getRecipientRole());
        assertEquals("/purchase-orders/44", captor.getAllValues().get(0).getActionUrl());
        assertEquals(AlertType.PO_APPROVAL_PENDING, captor.getAllValues().get(0).getType());
    }

    @Test
    void createAlertFromPurchaseEvent_shouldFallBackToStatusWhenEventTypeMissing() {
        PurchaseAlertEvent event = new PurchaseAlertEvent();
        event.setPurchaseOrderId(45L);
        event.setPurchaseOrderNumber("PO-20260505-0002");
        event.setStatus("PENDING_APPROVAL");

        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("PURCHASE_ORDER_PENDING_APPROVAL:45:-:MANAGER", AlertType.PO_APPROVAL_PENDING, "MANAGER"))
                .thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("PURCHASE_ORDER_PENDING_APPROVAL:45:-:ADMIN", AlertType.PO_APPROVAL_PENDING, "ADMIN"))
                .thenReturn(false);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.createAlertFromPurchaseEvent(event);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("Purchase order PO-20260505-0002 is pending approval.", captor.getAllValues().get(0).getMessage());
    }

    @Test
    void createAlertFromPurchaseEvent_shouldHandleOverdueApprovedRejectedAndReceived() {
        PurchaseAlertEvent overdue = new PurchaseAlertEvent();
        overdue.setEventType("purchase.overdue");
        overdue.setPurchaseOrderId(50L);
        overdue.setPurchaseOrderNumber("PO-50");
        overdue.setDaysOverdue(3);
        overdue.setCorrelationId("po-overdue");

        PurchaseAlertEvent approved = new PurchaseAlertEvent();
        approved.setEventType("purchase.approved");
        approved.setPurchaseOrderId(51L);
        approved.setPurchaseOrderNumber("PO-51");
        approved.setCreatedBy(9L);

        PurchaseAlertEvent rejected = new PurchaseAlertEvent();
        rejected.setEventType("purchase.rejected");
        rejected.setPurchaseOrderId(52L);
        rejected.setPurchaseOrderNumber("PO-52");
        rejected.setCreatedBy(10L);

        PurchaseAlertEvent received = new PurchaseAlertEvent();
        received.setEventType("purchase.received");
        received.setPurchaseOrderId(53L);
        received.setPurchaseOrderNumber("PO-53");

        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("po-overdue:OFFICER", AlertType.PO_OVERDUE_RECEIPT, "OFFICER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("po-overdue:MANAGER", AlertType.PO_OVERDUE_RECEIPT, "MANAGER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("po-overdue:ADMIN", AlertType.PO_OVERDUE_RECEIPT, "ADMIN")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientId("PURCHASE.APPROVED:51:9", AlertType.PO_APPROVED, 9L)).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientId("PURCHASE.REJECTED:52:10", AlertType.PO_REJECTED, 10L)).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("PURCHASE.RECEIVED:53:-:OFFICER", AlertType.PO_RECEIVED, "OFFICER")).thenReturn(false);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailNotificationService.sendCriticalAlertEmail(any(), any())).thenReturn(false);

        alertService.createAlertFromPurchaseEvent(overdue);
        alertService.createAlertFromPurchaseEvent(approved);
        alertService.createAlertFromPurchaseEvent(rejected);
        alertService.createAlertFromPurchaseEvent(received);

        verify(alertRepository, org.mockito.Mockito.atLeast(6)).save(any(Alert.class));
    }

    @Test
    void createAlertFromSupplierMovementAndExpiry_shouldCoverAdditionalBranches() {
        SupplierAlertEvent supplier = new SupplierAlertEvent();
        supplier.setEventType("supplier.blacklisted");
        supplier.setSupplierId(3L);
        supplier.setSupplierName("Bad Supplier");
        supplier.setReason("Repeated failures");
        supplier.setCorrelationId("supplier-blacklisted");

        SupplierAlertEvent deactivated = new SupplierAlertEvent();
        deactivated.setEventType("supplier.deactivated");
        deactivated.setSupplierId(4L);
        deactivated.setSupplierName("Dormant Supplier");
        deactivated.setCorrelationId("supplier-deactivated");

        MovementAlertEvent movement = new MovementAlertEvent();
        movement.setEventType("movement.anomaly");
        movement.setMovementId(88L);
        movement.setProductId(9L);
        movement.setWarehouseId(6L);
        movement.setMessage("Unusual stock drop");
        movement.setCorrelationId("movement-anomaly");

        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("supplier-blacklisted:OFFICER", AlertType.SUPPLIER_BLACKLISTED, "OFFICER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("supplier-blacklisted:ADMIN", AlertType.SUPPLIER_BLACKLISTED, "ADMIN")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("supplier-blacklisted:MANAGER", AlertType.SUPPLIER_BLACKLISTED, "MANAGER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("supplier-deactivated:OFFICER", AlertType.SUPPLIER_DEACTIVATED, "OFFICER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("supplier-deactivated:ADMIN", AlertType.SUPPLIER_DEACTIVATED, "ADMIN")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("supplier-deactivated:MANAGER", AlertType.SUPPLIER_DEACTIVATED, "MANAGER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("movement-anomaly:MANAGER", AlertType.MOVEMENT_ANOMALY, "MANAGER")).thenReturn(false);
        when(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("movement-anomaly:ADMIN", AlertType.MOVEMENT_ANOMALY, "ADMIN")).thenReturn(false);
        when(alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc(any())).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Alert expired = copyAlert(alert);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        expired.setStatus(AlertStatus.NEW);
        when(alertRepository.findByExpiresAtBeforeAndStatusNot(any(), eq(AlertStatus.EXPIRED)))
                .thenReturn(List.of(expired));

        alertService.createAlertFromSupplierEvent(supplier);
        alertService.createAlertFromSupplierEvent(deactivated);
        alertService.createAlertFromMovementEvent(movement);
        alertService.createAlertFromMovementEvent(new MovementAlertEvent());
        alertService.expireOldAlerts();

        verify(alertRepository, org.mockito.Mockito.atLeast(8)).save(any(Alert.class));
        verify(alertRepository).saveAll(any());
    }

    private AlertResponse mapToResponse(Alert saved) {
        return AlertResponse.builder()
                .alertId(saved.getAlertId())
                .alertNumber(saved.getAlertNumber())
                .recipientId(saved.getRecipientId())
                .recipientRole(saved.getRecipientRole())
                .title(saved.getTitle())
                .message(saved.getMessage())
                .type(saved.getType())
                .severity(saved.getSeverity())
                .status(saved.getStatus())
                .channel(saved.getChannel())
                .isRead(saved.getIsRead())
                .isAcknowledged(saved.getIsAcknowledged())
                .isDismissed(saved.getIsDismissed())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private Alert copyAlert(Alert source) {
        return Alert.builder()
                .alertId(source.getAlertId())
                .alertNumber(source.getAlertNumber())
                .recipientId(source.getRecipientId())
                .recipientRole(source.getRecipientRole())
                .type(source.getType())
                .severity(source.getSeverity())
                .status(source.getStatus())
                .channel(source.getChannel())
                .title(source.getTitle())
                .message(source.getMessage())
                .relatedProductId(source.getRelatedProductId())
                .relatedWarehouseId(source.getRelatedWarehouseId())
                .relatedPurchaseOrderId(source.getRelatedPurchaseOrderId())
                .relatedSupplierId(source.getRelatedSupplierId())
                .relatedMovementId(source.getRelatedMovementId())
                .referenceType(source.getReferenceType())
                .referenceId(source.getReferenceId())
                .referenceNumber(source.getReferenceNumber())
                .isRead(source.getIsRead())
                .isAcknowledged(source.getIsAcknowledged())
                .isDismissed(source.getIsDismissed())
                .readAt(source.getReadAt())
                .acknowledgedAt(source.getAcknowledgedAt())
                .dismissedAt(source.getDismissedAt())
                .acknowledgedBy(source.getAcknowledgedBy())
                .dismissedBy(source.getDismissedBy())
                .createdAt(source.getCreatedAt())
                .expiresAt(source.getExpiresAt())
                .sourceService(source.getSourceService())
                .correlationId(source.getCorrelationId())
                .priority(source.getPriority())
                .actionUrl(source.getActionUrl())
                .metadataJson(source.getMetadataJson())
                .version(source.getVersion())
                .build();
    }
}
