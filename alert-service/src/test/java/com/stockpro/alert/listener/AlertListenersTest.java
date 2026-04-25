package com.stockpro.alert.listener;

import static org.mockito.Mockito.verify;

import com.stockpro.alert.dto.event.EmailAlertEvent;
import com.stockpro.alert.dto.event.LowStockEvent;
import com.stockpro.alert.dto.event.OverdueReceiptEvent;
import com.stockpro.alert.dto.event.OverstockEvent;
import com.stockpro.alert.dto.event.PoPendingApprovalEvent;
import com.stockpro.alert.dto.event.SystemAlertEvent;
import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.mail.MailService;
import com.stockpro.alert.service.AlertService;
import com.stockpro.alert.validation.AlertEventValidator;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertListenersTest {

    @Mock
    private AlertService alertService;

    @Mock
    private MailService mailService;

    private AlertEventValidator alertEventValidator;

    @BeforeEach
    void setUp() {
        alertEventValidator = new AlertEventValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void lowStockListenerShouldDelegateToService() {
        LowStockAlertListener listener = new LowStockAlertListener(alertService, alertEventValidator);

        listener.handle(LowStockEvent.builder()
                .recipientId(101L)
                .productId(501L)
                .warehouseId(1L)
                .productName("Industrial Drill")
                .warehouseName("Central Warehouse")
                .availableQuantity(new BigDecimal("3.0000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .severity(AlertSeverity.WARNING)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 0))
                .build());

        verify(alertService).handleLowStockEvent(org.mockito.ArgumentMatchers.any(LowStockEvent.class));
    }

    @Test
    void overstockListenerShouldDelegateToService() {
        OverstockAlertListener listener = new OverstockAlertListener(alertService, alertEventValidator);

        listener.handle(OverstockEvent.builder()
                .recipientId(101L)
                .productId(502L)
                .warehouseId(1L)
                .productName("Safety Helmet")
                .warehouseName("Central Warehouse")
                .quantity(new BigDecimal("120.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .severity(AlertSeverity.WARNING)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 10))
                .build());

        verify(alertService).handleOverstockEvent(org.mockito.ArgumentMatchers.any(OverstockEvent.class));
    }

    @Test
    void poPendingListenerShouldDelegateToService() {
        PoPendingAlertListener listener = new PoPendingAlertListener(alertService, alertEventValidator);

        listener.handle(PoPendingApprovalEvent.builder()
                .recipientId(101L)
                .purchaseOrderId(7001L)
                .supplierId(8L)
                .warehouseId(1L)
                .poNumber("PO-7001")
                .totalAmount(new BigDecimal("55000.00"))
                .severity(AlertSeverity.INFO)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 20))
                .build());

        verify(alertService).handlePoPendingApprovalEvent(org.mockito.ArgumentMatchers.any(PoPendingApprovalEvent.class));
    }

    @Test
    void overdueReceiptListenerShouldDelegateToService() {
        OverdueReceiptAlertListener listener = new OverdueReceiptAlertListener(alertService, alertEventValidator);

        listener.handle(OverdueReceiptEvent.builder()
                .recipientId(101L)
                .purchaseOrderId(8001L)
                .supplierId(8L)
                .warehouseId(1L)
                .expectedDate(LocalDate.of(2026, 4, 24))
                .severity(AlertSeverity.WARNING)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 30))
                .build());

        verify(alertService).handleOverdueReceiptEvent(org.mockito.ArgumentMatchers.any(OverdueReceiptEvent.class));
    }

    @Test
    void systemListenerShouldDelegateToService() {
        SystemAlertListener listener = new SystemAlertListener(alertService, alertEventValidator);

        listener.handle(SystemAlertEvent.builder()
                .recipientId(101L)
                .title("System maintenance")
                .message("Maintenance starts tonight.")
                .severity(AlertSeverity.INFO)
                .channel(AlertChannel.IN_APP)
                .eventTime(LocalDateTime.of(2026, 4, 25, 9, 40))
                .build());

        verify(alertService).handleSystemAlertEvent(org.mockito.ArgumentMatchers.any(SystemAlertEvent.class));
    }

    @Test
    void emailListenerShouldDelegateToMailService() {
        EmailAlertListener listener = new EmailAlertListener(mailService, alertEventValidator);

        listener.handle(EmailAlertEvent.builder()
                .recipientId(101L)
                .toEmail("recipient@stockpro.com")
                .subject("Critical alert")
                .body("A critical alert occurred.")
                .alertId(1L)
                .severity(AlertSeverity.CRITICAL)
                .build());

        verify(mailService).sendEmail("recipient@stockpro.com", "Critical alert", "A critical alert occurred.");
    }
}
