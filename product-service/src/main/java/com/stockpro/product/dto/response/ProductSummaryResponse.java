package com.stockpro.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Compact product catalogue response.")
public class ProductSummaryResponse {

    @Schema(example = "1")
    private Long productId;

    @Schema(example = "SKU-1001")
    private String sku;

    @Schema(example = "Industrial Drill")
    private String name;

    @Schema(example = "Tools")
    private String category;

    @Schema(example = "Bosch")
    private String brand;

    @Schema(example = "Piece")
    private String unitOfMeasure;

    @Schema(example = "true")
    private Boolean isActive;

    @Schema(example = "8901234567890")
    private String barcode;
}
