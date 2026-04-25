package com.stockpro.report.dto.request;

import com.stockpro.report.validation.ValidReportDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidReportDateRange
@Schema(description = "Generic report filter request with optional warehouse, product, supplier and date filters.")
public class ReportFilterRequest {

    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    @Positive(message = "productId must be greater than zero.")
    private Long productId;

    @Positive(message = "supplierId must be greater than zero.")
    private Long supplierId;

    @Schema(example = "2026-04-01")
    private LocalDate fromDate;

    @Schema(example = "2026-04-30")
    private LocalDate toDate;

    @Min(value = 0, message = "page must be zero or greater.")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than zero.")
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "productId";

    @Builder.Default
    private String sortDir = "asc";
}
