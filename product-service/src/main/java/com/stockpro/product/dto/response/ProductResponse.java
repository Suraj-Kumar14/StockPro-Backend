package com.stockpro.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Schema(description = "Full product catalogue response.")
public class ProductResponse extends ProductSummaryResponse {

    @Schema(example = "Heavy-duty industrial drill for warehouse maintenance.")
    private String description;

    @Schema(example = "1499.5000")
    private BigDecimal costPrice;

    @Schema(example = "1999.9000")
    private BigDecimal sellingPrice;

    @Schema(example = "10.0000")
    private BigDecimal reorderLevel;

    @Schema(example = "100.0000")
    private BigDecimal maxStockLevel;

    @Schema(example = "5")
    private Integer leadTimeDays;

    @Schema(example = "https://cdn.stockpro.com/products/drill.png")
    private String imageUrl;

    @Schema(example = "2026-04-24T09:30:00")
    private LocalDateTime createdAt;

    @Schema(example = "2026-04-24T09:30:00")
    private LocalDateTime updatedAt;
}
