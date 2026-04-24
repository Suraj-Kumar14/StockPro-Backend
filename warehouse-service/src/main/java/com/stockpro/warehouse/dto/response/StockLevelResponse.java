package com.stockpro.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
@Schema(description = "Current stock position for a product in a warehouse.")
public class StockLevelResponse {

    @Schema(example = "10")
    private Long stockId;

    @Schema(example = "1")
    private Long warehouseId;

    @Schema(example = "501")
    private Long productId;

    @Schema(example = "120.0000")
    private BigDecimal quantity;

    @Schema(example = "20.0000")
    private BigDecimal reservedQuantity;

    @Schema(example = "100.0000")
    private BigDecimal availableQuantity;

    @Schema(example = "A-01-RACK-03")
    private String location;

    @Schema(example = "2026-04-24T10:00:00")
    private LocalDateTime lastUpdated;
}
