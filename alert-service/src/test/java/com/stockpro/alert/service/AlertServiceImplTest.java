package com.stockpro.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.alert.client.AuthServiceClient;
import com.stockpro.alert.dto.event.EmailAlertEvent;
import com.stockpro.alert.dto.event.LowStockEvent;
import com.stockpro.alert.dto.event.OverdueReceiptEvent;
import com.stockpro.alert.dto.event.OverstockEvent;
import com.stockpro.alert.dto.event.PoPendingApprovalEvent;
import com.stockpro.alert.dto.request.SendAlertRequest;
import com.stockpro.alert.dto.request.SendBulkAlertRequest;
import com.stockpro.alert.dto.response.AlertResponse;
import com.stockpro.alert.dto.response.AuthUserResponse;
import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import com.stockpro.alert.mapper.AlertMapper;
import com.stockpro.alert.publisher.AlertEventPublisher;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.security.AuthenticatedUser;
import com.stockpro.alert.security.SecurityUtils;
import com.stockpro.alert.service.impl.AlertServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AlertEventPublisher alertEventPublisher;

    @Mock
    private AuthServiceClient authServiceClient;

    private AlertServiceImpl alertService;

    private final AtomicLong idSequence = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        alertService = new AlertServiceImpl(
                alertRepository,
                alertMapper,
                securityUtils,
                alertEventPublisher,
                authServiceClient,
                new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(alertService, "emailLookupEnabled", true);

        lenient().when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            if (alert.getAlertId() == null) {
                alert.setAlertId(idSequence.getAndIncrement());
            }
            if (alert.getCreatedAt() == null) {
                alert.setCreatedAt(LocalDateTime.of(2026, 4, 25, 10, 0));
            }
            if (alert.getIsRead() == null) {
                alert.setIsRead(Boolean.FALSE);
            }
            if (alert.getIsAcknowledged() == null) {
                alert.setIsAcknowledged(Boolean.FALSE);
            }
            return alert;
        });
        lenient().when(alertMapper.toResponse(any(Alert.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));
        lenient().when(authServiceClient.getAllUsers()).thenReturn(List.of(
                AuthUserResponse.builder().userId(101L).email("recipient101@stockpro.com").build(),
                AuthUserResponse.builder().userId(102L).email("recipient102@stockpro.com").build()));
    }

    @Test
    void sendAlertShouldPublishEmailForCriticalAlerts() {
        SendAlertRequest request = SendAlertRequest.builder()
                .recipientId(101L)
                .type(AlertType.SYSTEM)
                .severity(AlertSeverity.CRITICAL)
                .title("Critical outage")
                .message("Warehouse scanners are offline.")
                .channel(AlertChannel.IN_APP)
                .build();

        AlertResponse response = alertService.sendAlert(request);

        assertThat(response.getAlertId()).isEqualTo(1L);
        assertThat(response.getChannel()).isEqualTo(AlertChannel.BOTH);
        ArgumentCaptor<EmailAlertEvent> captor = ArgumentCaptor.forClass(EmailAlertEvent.class);
        verify(alertEventPublisher).publishEmailAlert(captor.capture());
        assertThat(captor.getValue().getToEmail()).isEqualTo("recipient101@stockpro.com");
        assertThat(captor.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void sendAlertShouldNotPublishEmailForInfoInAppAlert() {
        SendAlertRequest request = SendAlertRequest.builder()
                .recipientId(101L)
                .type(AlertType.SYSTEM)
                .severity(AlertSeverity.INFO)
                .title("Dashboard update")
                .message("The dashboard has been refreshed.")
                .channel(AlertChannel.IN_APP)
                .build();

        AlertResponse response = alertService.sendAlert(request);

        assertThat(response.getChannel()).isEqualTo(AlertChannel.IN_APP);
        verify(alertEventPublisher, never()).publishEmailAlert(any(EmailAlertEvent.class));
    }

    @Test
    void sendAlertShouldPublishEmailWhenInfoAlertRequestsEmailChannel() {
        SendAlertRequest request = SendAlertRequest.builder()
                .recipientId(101L)
                .type(AlertType.SYSTEM)
                .severity(AlertSeverity.INFO)
                .title("Broadcast")
                .message("A message for all users.")
                .channel(AlertChannel.EMAIL)
                .build();

        AlertResponse response = alertService.sendAlert(request);

        assertThat(response.getChannel()).isEqualTo(AlertChannel.BOTH);
        verify(alertEventPublisher).publishEmailAlert(any(EmailAlertEvent.class));
    }

    @Test
    void handleLowStockEventShouldCreateAlert() {
        LowStockEvent event = LowStockEvent.builder()
                .recipientId(101L)
                .productId(501L)
                .warehouseId(5L)
                .productName("Industrial Drill")
                .warehouseName("Central Warehouse")
                .availableQuantity(new BigDecimal("3.5000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .severity(AlertSeverity.WARNING)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 15))
                .build();

        AlertResponse response = alertService.handleLowStockEvent(event);

        assertThat(response.getType()).isEqualTo(AlertType.LOW_STOCK);
        assertThat(response.getRelatedProductId()).isEqualTo(501L);
        verify(alertEventPublisher, never()).publishEmailAlert(any(EmailAlertEvent.class));
    }

    @Test
    void handleOverstockEventShouldCreateAlert() {
        OverstockEvent event = OverstockEvent.builder()
                .recipientId(101L)
                .productId(777L)
                .warehouseId(5L)
                .productName("Safety Helmet")
                .warehouseName("Central Warehouse")
                .quantity(new BigDecimal("150.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .severity(AlertSeverity.WARNING)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 20))
                .build();

        AlertResponse response = alertService.handleOverstockEvent(event);

        assertThat(response.getType()).isEqualTo(AlertType.OVERSTOCK);
        assertThat(response.getRelatedWarehouseId()).isEqualTo(5L);
    }

    @Test
    void handlePoPendingApprovalEventShouldCreateAlert() {
        PoPendingApprovalEvent event = PoPendingApprovalEvent.builder()
                .recipientId(101L)
                .purchaseOrderId(7001L)
                .supplierId(8L)
                .warehouseId(5L)
                .poNumber("PO-7001")
                .totalAmount(new BigDecimal("45000.00"))
                .severity(AlertSeverity.INFO)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 30))
                .build();

        AlertResponse response = alertService.handlePoPendingApprovalEvent(event);

        assertThat(response.getType()).isEqualTo(AlertType.PO_PENDING);
        assertThat(response.getRelatedPurchaseOrderId()).isEqualTo(7001L);
    }

    @Test
    void handleOverdueReceiptEventShouldCreateAlert() {
        OverdueReceiptEvent event = OverdueReceiptEvent.builder()
                .recipientId(101L)
                .purchaseOrderId(9001L)
                .supplierId(8L)
                .warehouseId(5L)
                .expectedDate(LocalDate.of(2026, 4, 24))
                .severity(AlertSeverity.WARNING)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 45))
                .build();

        AlertResponse response = alertService.handleOverdueReceiptEvent(event);

        assertThat(response.getType()).isEqualTo(AlertType.OVERDUE_RECEIPT);
        assertThat(response.getRelatedPurchaseOrderId()).isEqualTo(9001L);
    }

    @Test
    void markAsReadShouldUpdateAlertState() {
        Alert alert = storedAlert(1L, 101L);
        when(alertRepository.findById(1L)).thenReturn(java.util.Optional.of(alert));
        when(securityUtils.hasAnyRole(eq("ADMIN"))).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser(101L));

        AlertResponse response = alertService.markAsRead(1L);

        assertThat(response.getIsRead()).isTrue();
        assertThat(response.getReadAt()).isNotNull();
    }

    @Test
    void acknowledgeShouldUpdateAlertState() {
        Alert alert = storedAlert(1L, 101L);
        when(alertRepository.findById(1L)).thenReturn(java.util.Optional.of(alert));
        when(securityUtils.hasAnyRole(eq("ADMIN"))).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser(101L));

        AlertResponse response = alertService.acknowledge(1L);

        assertThat(response.getIsAcknowledged()).isTrue();
        assertThat(response.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void getUnreadCountShouldReturnRepositoryCount() {
        when(securityUtils.hasAnyRole(eq("ADMIN"))).thenReturn(false);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser(101L));
        when(alertRepository.countByRecipientIdAndIsRead(101L, Boolean.FALSE)).thenReturn(4L);

        var response = alertService.getUnreadCount(101L);

        assertThat(response.getUnreadCount()).isEqualTo(4L);
    }

    @Test
    void sendBulkShouldCreateAlertForEachRecipient() {
        SendBulkAlertRequest request = SendBulkAlertRequest.builder()
                .recipientIds(List.of(101L, 102L))
                .type(AlertType.SYSTEM)
                .severity(AlertSeverity.WARNING)
                .title("Bulk alert")
                .message("Bulk message.")
                .channel(AlertChannel.BOTH)
                .build();

        List<AlertResponse> responses = alertService.sendBulk(request);

        assertThat(responses).hasSize(2);
        verify(alertRepository, times(2)).save(any(Alert.class));
        verify(alertEventPublisher, times(2)).publishEmailAlert(any(EmailAlertEvent.class));
    }

    private Alert storedAlert(Long alertId, Long recipientId) {
        return Alert.builder()
                .alertId(alertId)
                .recipientId(recipientId)
                .type(AlertType.SYSTEM)
                .severity(AlertSeverity.INFO)
                .title("Stored alert")
                .message("Stored alert message")
                .channel(AlertChannel.IN_APP)
                .isRead(Boolean.FALSE)
                .isAcknowledged(Boolean.FALSE)
                .createdAt(LocalDateTime.of(2026, 4, 25, 8, 0))
                .build();
    }

    private AuthenticatedUser currentUser(Long userId) {
        return new AuthenticatedUser(userId, "user@stockpro.com", "INVENTORY_MANAGER", List.of());
    }

    private AlertResponse toResponse(Alert alert) {
        return AlertResponse.builder()
                .alertId(alert.getAlertId())
                .recipientId(alert.getRecipientId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .relatedProductId(alert.getRelatedProductId())
                .relatedWarehouseId(alert.getRelatedWarehouseId())
                .relatedPurchaseOrderId(alert.getRelatedPurchaseOrderId())
                .channel(alert.getChannel())
                .isRead(alert.getIsRead())
                .isAcknowledged(alert.getIsAcknowledged())
                .createdAt(alert.getCreatedAt())
                .readAt(alert.getReadAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .build();
    }
}
