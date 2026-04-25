package com.stockpro.web.dto.request;

import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilterRequest {

    private Long warehouseId;
    private Long productId;
    private Long supplierId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    private Integer size = 20;

    private String sortBy = "productId";
    private String sortDir = "asc";
}
