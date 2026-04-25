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
@Schema(description = "Product metadata response consumed from product-service.")
public class ProductSummaryResponse {

    @Schema(example = "501")
    private Long productId;

    @Schema(example = "SKU-501")
    private String sku;

    @Schema(example = "Industrial Drill")
    private String name;

    @Schema(example = "15.0000")
    private BigDecimal reorderLevel;

    @Schema(example = "500.0000")
    private BigDecimal maxStockLevel;

    @Schema(example = "true")
    private Boolean isActive;
}
