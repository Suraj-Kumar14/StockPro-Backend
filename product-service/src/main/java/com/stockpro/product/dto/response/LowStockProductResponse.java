package com.stockpro.product.dto.response;

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
@Schema(description = "Low-stock product metadata enriched from the product master catalogue.")
public class LowStockProductResponse {

    @Schema(example = "1")
    private Long productId;

    @Schema(example = "SKU-1001")
    private String sku;

    @Schema(example = "Industrial Drill")
    private String name;

    @Schema(example = "10.0000")
    private BigDecimal reorderLevel;

    @Schema(example = "100.0000")
    private BigDecimal maxStockLevel;

    @Schema(example = "8901234567890")
    private String barcode;

    @Schema(example = "Tools")
    private String category;

    @Schema(example = "Bosch")
    private String brand;
}
