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
import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import com.stockpro.alertservice.events.StockAlertEvent;
import com.stockpro.alertservice.mail.EmailNotificationService;
import com.stockpro.alertservice.rabbitmq.AlertEventPublisher;
import com.stockpro.alertservice.repository.AlertRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        when(alertMapper.toResponse(any(Alert.class))).thenReturn(AlertResponse.builder()
                .alertId(1L)
                .alertNumber("ALT-20260501-000001")
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .severity(request.getSeverity())
                .status(AlertStatus.NEW)
                .channel(AlertChannel.IN_APP)
                .isRead(false)
                .isAcknowledged(false)
                .isDismissed(false)
                .build());

        AlertResponse response = alertService.createAlert(request, 99L);

        assertEquals("Low Stock Alert", response.getTitle());
        verify(alertRepository).save(any(Alert.class));
        verify(alertEventPublisher).publish(eq("alert.created"), any());
        verify(emailNotificationService, never()).sendCriticalAlertEmail(any(), any());
    }

    @Test
    void markAsRead_shouldUpdateStatus() {
        when(alertRepository.findByAlertId(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alertMapper.toResponse(any(Alert.class))).thenAnswer(invocation -> {
            Alert saved = invocation.getArgument(0);
            return AlertResponse.builder()
                    .alertId(saved.getAlertId())
                    .alertNumber(saved.getAlertNumber())
                    .title(saved.getTitle())
                    .message(saved.getMessage())
                    .type(saved.getType())
                    .severity(saved.getSeverity())
                    .status(saved.getStatus())
                    .channel(saved.getChannel())
                    .isRead(saved.getIsRead())
                    .isAcknowledged(saved.getIsAcknowledged())
                    .isDismissed(saved.getIsDismissed())
                    .build();
        });

        AlertResponse response = alertService.markAsRead(1L, 7L, "MANAGER", false);

        assertEquals(AlertStatus.READ, response.getStatus());
        assertFalse(Boolean.FALSE.equals(response.getIsRead()));
        verify(alertEventPublisher).publish(eq("alert.read"), any());
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
}
