package com.stockpro.alertservice.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentAlertEvent {
    private String eventId;
    private String eventType;
    private Long paymentId;
    private String paymentNumber;
    private String paymentReference;
    private Long purchaseOrderId;
    private String purchaseOrderNumber;
    private Long supplierId;
    private String supplierName;
    private Long actorId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String currency;
    private String message;
    private String actionUrl;
    private String sourceService;
    private String correlationId;
    private LocalDateTime eventTime;
}
