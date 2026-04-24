package com.stockpro.product.dto.request;

import com.stockpro.product.validation.ValidProductStockLevels;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidProductStockLevels
@Schema(description = "Request body used to update product catalogue fields.")
public class UpdateProductRequest {

    @Size(max = 255, message = "Product name cannot be longer than 255 characters.")
    @Schema(example = "Industrial Drill Pro")
    private String name;

    @Size(max = 1000, message = "Description cannot be longer than 1000 characters.")
    @Schema(example = "Updated drill model with extended duty cycle.")
    private String description;

    @Size(max = 150, message = "Category cannot be longer than 150 characters.")
    @Schema(example = "Tools")
    private String category;

    @Size(max = 150, message = "Brand cannot be longer than 150 characters.")
    @Schema(example = "Bosch")
    private String brand;

    @Size(max = 100, message = "Unit of measure cannot be longer than 100 characters.")
    @Schema(example = "Piece")
    private String unitOfMeasure;

    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative.")
    @Schema(example = "1550.0000")
    private BigDecimal costPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price cannot be negative.")
    @Schema(example = "2099.9900")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level cannot be negative.")
    @Schema(example = "12.0000")
    private BigDecimal reorderLevel;

    @DecimalMin(value = "0.0", inclusive = true, message = "Max stock level cannot be negative.")
    @Schema(example = "120.0000")
    private BigDecimal maxStockLevel;

    @PositiveOrZero(message = "Lead time days cannot be negative.")
    @Schema(example = "7")
    private Integer leadTimeDays;

    @URL(message = "imageUrl must be a valid URL.")
    @Size(max = 1000, message = "Image URL cannot be longer than 1000 characters.")
    @Schema(example = "https://cdn.stockpro.com/products/drill-pro.png")
    private String imageUrl;

    @Size(max = 150, message = "Barcode cannot be longer than 150 characters.")
    @Schema(example = "8901234567891")
    private String barcode;

    @Schema(example = "true")
    private Boolean isActive;
}
