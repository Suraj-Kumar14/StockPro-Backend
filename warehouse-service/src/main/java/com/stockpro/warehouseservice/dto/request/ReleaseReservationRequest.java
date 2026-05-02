package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReleaseReservationRequest {
    @NotNull private Long warehouseId;
    @NotNull private Long productId;
    @Min(1) private Integer quantity;
    private String referenceId;
    private String referenceType;
    private String reason;
}
