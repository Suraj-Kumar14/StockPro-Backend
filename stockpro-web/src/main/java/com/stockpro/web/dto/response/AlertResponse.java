package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockpro.web.dto.AlertChannel;
import com.stockpro.web.dto.AlertSeverity;
import com.stockpro.web.dto.AlertType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertResponse {

    private Long alertId;
    private Long recipientId;
    private AlertType type;
    private AlertSeverity severity;
    private String title;
    private String message;
    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;
    private AlertChannel channel;

    @JsonAlias({ "read", "isRead" })
    private Boolean read;

    @JsonAlias({ "acknowledged", "isAcknowledged" })
    private Boolean acknowledged;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
}
