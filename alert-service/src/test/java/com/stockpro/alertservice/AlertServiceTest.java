package com.stockpro.alertservice;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.dto.AlertResponseDTO;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import com.stockpro.alertservice.exception.AlertNotFoundException;
import com.stockpro.alertservice.exception.InvalidAlertException;
import com.stockpro.alertservice.repository.AlertRepository;
import com.stockpro.alertservice.service.AlertMapper;
import com.stockpro.alertservice.service.AlertService;
import com.stockpro.alertservice.service.AlertValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private JavaMailSender mailSender;

    private AlertService alertService;

    private Alert mockAlert;
    private AlertRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(
                alertRepository,
                new AlertValidationService(),
                new AlertMapper(),
                mailSender);
        ReflectionTestUtils.setField(alertService, "fromEmail", "noreply@stockpro.com");
        ReflectionTestUtils.setField(alertService, "defaultRecipientId", 1L);

        mockAlert = Alert.builder()
                .alertId(1L)
                .recipientId(1L)
                .type(AlertType.LOW_STOCK)
                .severity(Severity.WARNING)
                .title("Low Stock Alert")
                .message("Product A is running low")
                .relatedProductId(1L)
                .relatedWarehouseId(1L)
                .channel("IN_APP")
                .isRead(false)
                .isAcknowledged(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        requestDTO = new AlertRequestDTO();
        requestDTO.setRecipientId(1L);
        requestDTO.setType(AlertType.LOW_STOCK);
        requestDTO.setSeverity(Severity.WARNING);
        requestDTO.setTitle("Low Stock Alert");
        requestDTO.setMessage("Product A is running low");
        requestDTO.setRelatedProductId(1L);
        requestDTO.setRelatedWarehouseId(1L);
        requestDTO.setChannel("IN_APP");
    }

    @Test
    void createAlert_Success() {
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        AlertResponseDTO result = alertService.createAlert(requestDTO);

        assertNotNull(result);
        assertEquals(AlertType.LOW_STOCK, result.getType());
        verify(alertRepository).save(any(Alert.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void createAlert_CriticalAlert_SendsEmail() {
        requestDTO.setSeverity(Severity.CRITICAL);
        requestDTO.setType(AlertType.OVERDUE_RECEIPT);
        requestDTO.setRelatedPurchaseOrderId(22L);

        Alert criticalAlert = Alert.builder()
                .alertId(2L)
                .recipientId(1L)
                .type(AlertType.OVERDUE_RECEIPT)
                .severity(Severity.CRITICAL)
                .title("Critical Alert")
                .message("PO overdue!")
                .channel("IN_APP")
                .isRead(false)
                .isAcknowledged(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(alertRepository.save(any(Alert.class))).thenReturn(criticalAlert);

        AlertResponseDTO result = alertService.createAlert(requestDTO);

        assertNotNull(result);
        assertEquals(Severity.CRITICAL, result.getSeverity());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void createAlert_InvalidPoAlertWithoutReference_ThrowsException() {
        requestDTO.setType(AlertType.PO_PENDING);
        requestDTO.setRelatedProductId(null);
        requestDTO.setRelatedWarehouseId(null);
        requestDTO.setRelatedPurchaseOrderId(null);

        assertThrows(InvalidAlertException.class, () -> alertService.createAlert(requestDTO));
    }

    @Test
    void buildPendingPoAlert_UsesCaseStudyType() {
        AlertRequestDTO result = alertService.buildPendingPoAlert(5L, 99L, "PO pending");

        assertEquals(AlertType.PO_PENDING, result.getType());
        assertEquals(99L, result.getRelatedPurchaseOrderId());
        assertEquals(5L, result.getRecipientId());
    }

    @Test
    void sendBulkAlerts_CreatesMultipleAlerts() {
        Alert secondAlert = Alert.builder()
                .alertId(2L)
                .recipientId(1L)
                .type(AlertType.OVERSTOCK)
                .severity(Severity.INFO)
                .title("Overstock")
                .message("Too much stock")
                .relatedProductId(2L)
                .relatedWarehouseId(1L)
                .channel("IN_APP")
                .isRead(false)
                .isAcknowledged(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .build();

        AlertRequestDTO secondRequest = new AlertRequestDTO();
        secondRequest.setRecipientId(1L);
        secondRequest.setType(AlertType.OVERSTOCK);
        secondRequest.setSeverity(Severity.INFO);
        secondRequest.setTitle("Overstock");
        secondRequest.setMessage("Too much stock");
        secondRequest.setRelatedProductId(2L);
        secondRequest.setRelatedWarehouseId(1L);
        secondRequest.setChannel("IN_APP");

        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert, secondAlert);

        List<AlertResponseDTO> result = alertService.sendBulkAlerts(List.of(requestDTO, secondRequest));

        assertEquals(2, result.size());
    }

    @Test
    void getAlertById_NotFound_ThrowsException() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () -> alertService.getAlertById(99L));
    }

    @Test
    void getAlertsByRecipient_ReturnsNonArchivedAlerts() {
        when(alertRepository.findByRecipientIdAndIsArchivedFalse(1L)).thenReturn(List.of(mockAlert));

        List<AlertResponseDTO> result = alertService.getAlertsByRecipient(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getUnreadCount_ReturnsCount() {
        when(alertRepository.countByRecipientIdAndIsReadAndIsArchivedFalse(1L, false)).thenReturn(5L);

        Long count = alertService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        assertDoesNotThrow(() -> alertService.markAsRead(1L));

        verify(alertRepository).save(argThat(a -> a.getIsRead() && a.getReadAt() != null));
    }

    @Test
    void acknowledge_SetsReadAndAcknowledged() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        alertService.acknowledge(1L);

        verify(alertRepository).save(argThat(a ->
                a.getIsRead() && a.getIsAcknowledged()
                        && a.getReadAt() != null && a.getAcknowledgedAt() != null));
    }

    @Test
    void deleteAlert_ArchivesInsteadOfDeleting() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        alertService.deleteAlert(1L);

        verify(alertRepository).save(argThat(Alert::getIsArchived));
        verify(alertRepository, never()).deleteById(any());
    }

    @Test
    void getRecentAlerts_InvalidDays_ThrowsException() {
        assertThrows(InvalidAlertException.class, () -> alertService.getRecentAlerts(0));
    }
}
