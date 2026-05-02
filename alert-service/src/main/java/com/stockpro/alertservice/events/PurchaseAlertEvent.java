package com.stockpro.alertservice.events;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PurchaseAlertEvent {
    private String eventId;
    private String eventType;
    @JsonAlias({"poId"})
    private Long purchaseOrderId;
    @JsonAlias({"poNumber"})
    private String purchaseOrderNumber;
    private Long recipientId;
    @JsonAlias({"requestedByUserId"})
    private Long createdBy;
    private LocalDate expectedDate;
    @JsonAlias({"daysOverdue"})
    private Integer daysOverdue;
    private String message;
    private String sourceService;
    private String correlationId;
}
