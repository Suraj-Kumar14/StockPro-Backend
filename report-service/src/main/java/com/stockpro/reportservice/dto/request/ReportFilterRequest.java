package com.stockpro.reportservice.dto.request;

import com.stockpro.reportservice.enums.ReportPeriod;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long productId;
    private Long warehouseId;
    private Long supplierId;
    private String category;
    private String brand;
    private String movementType;
    private String poStatus;
    private String paymentStatus;
    private String alertSeverity;
    private ReportPeriod period;
    @Builder.Default
    @Min(0)
    private int page = 0;
    @Builder.Default
    @Min(1)
    private int size = 10;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String sortDir = "desc";
}
