package com.stockpro.web.dto.request;

import com.stockpro.web.dto.ReportFormat;
import com.stockpro.web.dto.ReportType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportRequest {

    @NotNull(message = "Report type is required.")
    private ReportType reportType;

    private Long warehouseId;
    private Long productId;
    private Long supplierId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @NotNull(message = "Format is required.")
    private ReportFormat format;

    private String requestedBy;
}
