package com.stockpro.alertservice.dto.request;

import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlertSearchRequest {
    private String keyword;
    private Long recipientId;
    private String recipientRole;
    private AlertType type;
    private AlertSeverity severity;
    private AlertStatus status;
    private Boolean isRead;
    private Boolean isAcknowledged;
    private Boolean isDismissed;
    private String referenceType;
    private String referenceId;
    private String sourceService;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDir;
}
