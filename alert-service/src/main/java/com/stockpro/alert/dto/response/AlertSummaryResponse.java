package com.stockpro.alert.dto.response;

import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Compact alert response for list views.")
public class AlertSummaryResponse {

    @Schema(example = "1")
    private Long alertId;

    @Schema(example = "LOW_STOCK")
    private AlertType type;

    @Schema(example = "WARNING")
    private AlertSeverity severity;

    @Schema(example = "Low stock detected")
    private String title;

    @Schema(example = "Industrial Drill is below reorder level in Central Warehouse.")
    private String message;

    @Schema(example = "false")
    private Boolean isRead;

    @Schema(example = "false")
    private Boolean isAcknowledged;

    @Schema(example = "2026-04-25T09:30:00")
    private LocalDateTime createdAt;
}
