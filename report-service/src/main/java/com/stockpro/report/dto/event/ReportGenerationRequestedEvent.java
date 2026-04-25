package com.stockpro.report.dto.event;

import com.stockpro.report.export.ReportFormat;
import com.stockpro.report.export.ReportType;
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
public class ReportGenerationRequestedEvent {

    private ReportType reportType;
    private ReportFormat format;
    private String requestedBy;
    private LocalDateTime requestedAt;
}
