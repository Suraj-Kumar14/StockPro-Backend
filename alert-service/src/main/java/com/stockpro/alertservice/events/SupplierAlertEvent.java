package com.stockpro.alertservice.events;

import lombok.Data;

@Data
public class SupplierAlertEvent {
    private String eventId;
    private String eventType;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String reason;
    private String sourceService;
    private String correlationId;
}
