package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockpro.web.dto.ReportFormat;
import com.stockpro.web.dto.ReportType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedReportResponse {

    private ReportType reportType;
    private ReportFormat format;
    private String fileName;
    private String fileUrl;
    private LocalDateTime generatedAt;
}
