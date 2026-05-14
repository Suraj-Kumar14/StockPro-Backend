package com.stockpro.alertservice.events;

import com.stockpro.alertservice.enums.AlertSeverity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SystemAlertEvent {
    private String eventId;
    private String eventType;
    private AlertSeverity severity;
    private String title;
    private String message;
    private String userMessage;
    private String technicalDetails;
    private List<String> recipientRoles;
    private List<Long> recipientIds;
    private String actionUrl;
    private String referenceType;
    private String referenceId;
    private LocalDateTime expiresAt;
    private String sourceService;
    private String correlationId;
}
