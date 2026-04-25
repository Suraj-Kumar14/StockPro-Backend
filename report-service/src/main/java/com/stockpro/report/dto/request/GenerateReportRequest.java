package com.stockpro.report.dto.request;

import com.stockpro.report.export.ReportFormat;
import com.stockpro.report.export.ReportType;
import com.stockpro.report.validation.ValidReportDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
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
@ValidReportDateRange
@Schema(description = "Request used to generate and export an analytical report.")
public class GenerateReportRequest {

    @NotNull(message = "reportType is required.")
    private ReportType reportType;

    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    @Positive(message = "productId must be greater than zero.")
    private Long productId;

    @Positive(message = "supplierId must be greater than zero.")
    private Long supplierId;

    private LocalDate fromDate;

    private LocalDate toDate;

    @NotNull(message = "format is required.")
    private ReportFormat format;

    private String requestedBy;
}
