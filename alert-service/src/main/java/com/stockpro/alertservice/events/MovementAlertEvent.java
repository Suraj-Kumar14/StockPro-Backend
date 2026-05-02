package com.stockpro.alertservice.events;

import lombok.Data;

@Data
public class MovementAlertEvent {
    private String eventId;
    private String eventType;
    private Long movementId;
    private String movementNumber;
    private Long productId;
    private Long warehouseId;
    private String message;
    private String sourceService;
    private String correlationId;
}
