package com.stockpro.alertservice.events;

import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertEvent {
    private String eventId;
    private Long alertId;
    private String alertNumber;
    private Long recipientId;
    private String recipientRole;
    private AlertType type;
    private AlertSeverity severity;
    private AlertChannel channel;
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private String sourceService;
    private String correlationId;
    private LocalDateTime createdAt;
}
