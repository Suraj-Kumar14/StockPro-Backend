package com.stockpro.web.dto.request;

import com.stockpro.web.dto.AlertSeverity;
import com.stockpro.web.dto.AlertType;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSearchRequest {

    private Long recipientId;
    private AlertType type;
    private AlertSeverity severity;
    private Boolean read;
    private Boolean acknowledged;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer size = 20;

    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
