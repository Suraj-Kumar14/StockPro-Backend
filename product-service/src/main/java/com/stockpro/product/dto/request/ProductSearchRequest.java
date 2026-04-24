package com.stockpro.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Advanced product search and filter request.")
public class ProductSearchRequest {

    @Size(max = 255, message = "name cannot be longer than 255 characters.")
    @Schema(example = "drill")
    private String name;

    @Size(max = 150, message = "category cannot be longer than 150 characters.")
    @Schema(example = "Tools")
    private String category;

    @Size(max = 150, message = "brand cannot be longer than 150 characters.")
    @Schema(example = "Bosch")
    private String brand;

    @Size(max = 100, message = "sku cannot be longer than 100 characters.")
    @Schema(example = "SKU-1001")
    private String sku;

    @Size(max = 150, message = "barcode cannot be longer than 150 characters.")
    @Schema(example = "8901234567890")
    private String barcode;

    @Schema(example = "true")
    private Boolean isActive;

    @Min(value = 0, message = "page must be zero or greater.")
    @Builder.Default
    @Schema(example = "0")
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than zero.")
    @Builder.Default
    @Schema(example = "20")
    private Integer size = 20;

    @Builder.Default
    @Schema(example = "productId")
    private String sortBy = "productId";

    @Builder.Default
    @Schema(example = "asc")
    private String sortDir = "asc";
}
