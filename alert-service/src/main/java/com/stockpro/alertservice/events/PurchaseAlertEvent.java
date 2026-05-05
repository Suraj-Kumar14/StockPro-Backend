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
    @JsonAlias({"requestedByUserId", "actorId", "createdById"})
    private Long createdBy;
    private LocalDate expectedDate;
    @JsonAlias({"daysOverdue"})
    private Integer daysOverdue;
    @JsonAlias({"reason"})
    private String message;
    private String sourceService;
    private String correlationId;
    private String status;
    private String oldStatus;
    private String newStatus;
}
