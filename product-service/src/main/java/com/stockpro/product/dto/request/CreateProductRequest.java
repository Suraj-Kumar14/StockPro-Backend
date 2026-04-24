package com.stockpro.product.dto.request;

import com.stockpro.product.validation.ValidProductStockLevels;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request body used to create a product in the master catalogue.")
public class CreateProductRequest {

    @NotBlank(message = "SKU is required.")
    @Size(max = 100, message = "SKU cannot be longer than 100 characters.")
    @Schema(example = "SKU-1001")
    private String sku;

    @NotBlank(message = "Product name is required.")
    @Size(max = 255, message = "Product name cannot be longer than 255 characters.")
    @Schema(example = "Industrial Drill")
    private String name;

    @Size(max = 1000, message = "Description cannot be longer than 1000 characters.")
    @Schema(example = "Heavy-duty industrial drill for warehouse maintenance.")
    private String description;

    @NotBlank(message = "Category is required.")
    @Size(max = 150, message = "Category cannot be longer than 150 characters.")
    @Schema(example = "Tools")
    private String category;

    @NotBlank(message = "Brand is required.")
    @Size(max = 150, message = "Brand cannot be longer than 150 characters.")
    @Schema(example = "Bosch")
    private String brand;

    @NotBlank(message = "Unit of measure is required.")
    @Size(max = 100, message = "Unit of measure cannot be longer than 100 characters.")
    @Schema(example = "Piece")
    private String unitOfMeasure;

    @NotNull(message = "Cost price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative.")
    @Schema(example = "1499.5000")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price cannot be negative.")
    @Schema(example = "1999.9000")
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level cannot be negative.")
    @Schema(example = "10.0000")
    private BigDecimal reorderLevel;

    @NotNull(message = "Max stock level is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Max stock level cannot be negative.")
    @Schema(example = "100.0000")
    private BigDecimal maxStockLevel;

    @NotNull(message = "Lead time days is required.")
    @PositiveOrZero(message = "Lead time days cannot be negative.")
    @Schema(example = "5")
    private Integer leadTimeDays;

    @URL(message = "imageUrl must be a valid URL.")
    @Size(max = 1000, message = "Image URL cannot be longer than 1000 characters.")
    @Schema(example = "https://cdn.stockpro.com/products/drill.png")
    private String imageUrl;

    @Size(max = 150, message = "Barcode cannot be longer than 150 characters.")
    @Schema(example = "8901234567890")
    private String barcode;
}
