package com.stockpro.warehouse.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Internal stock quantity response consumed by product-service.")
public class StockLevelQuantityResponse {

    @Schema(example = "501")
    private Long productId;

    @Schema(example = "100")
    private Integer currentQuantity;
}
