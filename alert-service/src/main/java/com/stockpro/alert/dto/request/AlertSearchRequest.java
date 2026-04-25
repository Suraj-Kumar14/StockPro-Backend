package com.stockpro.alert.dto.request;

import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import com.stockpro.alert.validation.ValidAlertSearchDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
@ValidAlertSearchDateRange
@Schema(description = "Filtered and pageable alert search request.")
public class AlertSearchRequest {

    @Positive(message = "recipientId must be greater than zero.")
    @Schema(example = "101")
    private Long recipientId;

    @Schema(example = "LOW_STOCK")
    private AlertType type;

    @Schema(example = "WARNING")
    private AlertSeverity severity;

    @Schema(example = "false")
    private Boolean isRead;

    @Schema(example = "false")
    private Boolean isAcknowledged;

    @Schema(example = "2026-04-24T00:00:00")
    private LocalDateTime startDate;

    @Schema(example = "2026-04-25T23:59:59")
    private LocalDateTime endDate;

    @Min(value = 0, message = "page must be zero or greater.")
    @Builder.Default
    @Schema(example = "0")
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than zero.")
    @Builder.Default
    @Schema(example = "20")
    private Integer size = 20;

    @Builder.Default
    @Schema(example = "createdAt")
    private String sortBy = "createdAt";

    @Builder.Default
    @Schema(example = "desc")
    private String sortDir = "desc";
}
