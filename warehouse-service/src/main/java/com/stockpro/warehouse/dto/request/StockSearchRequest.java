package com.stockpro.warehouse.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
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
@Schema(description = "Advanced stock search request.")
public class StockSearchRequest {

    @Positive(message = "warehouseId must be greater than zero.")
    @Schema(example = "1")
    private Long warehouseId;

    @Positive(message = "productId must be greater than zero.")
    @Schema(example = "501")
    private Long productId;

    @Schema(example = "A-01")
    private String location;

    @Schema(example = "false")
    private Boolean lowStockOnly;

    @Min(value = 0, message = "page must be zero or greater.")
    @Builder.Default
    @Schema(example = "0")
    private Integer page = 0;

    @Min(value = 1, message = "size must be greater than zero.")
    @Builder.Default
    @Schema(example = "20")
    private Integer size = 20;

    @Builder.Default
    @Schema(example = "lastUpdated")
    private String sortBy = "lastUpdated";

    @Builder.Default
    @Schema(example = "desc")
    private String sortDir = "desc";
}
