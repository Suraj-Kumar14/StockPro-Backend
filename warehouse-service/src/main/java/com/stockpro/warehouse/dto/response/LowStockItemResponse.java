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
@Schema(description = "Low stock warehouse-product response for alerting workflows.")
public class LowStockItemResponse {

    @Schema(example = "1")
    private Long warehouseId;

    @Schema(example = "501")
    private Long productId;

    @Schema(example = "10.0000")
    private BigDecimal quantity;

    @Schema(example = "2.0000")
    private BigDecimal reservedQuantity;

    @Schema(example = "8.0000")
    private BigDecimal availableQuantity;

    @Schema(example = "A-01-RACK-03")
    private String location;
}
