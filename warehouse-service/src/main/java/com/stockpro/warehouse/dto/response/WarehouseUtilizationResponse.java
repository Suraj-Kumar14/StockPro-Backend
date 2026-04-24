package com.stockpro.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
@Schema(description = "Warehouse capacity utilization response.")
public class WarehouseUtilizationResponse {

    @Schema(example = "1")
    private Long warehouseId;

    @Schema(example = "5000")
    private Integer capacity;

    @Schema(example = "1200")
    private Integer usedCapacity;

    @Schema(example = "24.00")
    private BigDecimal utilizationPercentage;
}
