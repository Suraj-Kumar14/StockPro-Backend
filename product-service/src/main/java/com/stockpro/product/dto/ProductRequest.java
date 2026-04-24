package com.stockpro.product.dto;

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

/**
 * Request body used for create and update product operations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "SKU is required.")
    @Size(max = 50, message = "SKU cannot be longer than 50 characters.")
    private String sku;

    @NotBlank(message = "Product name is required.")
    @Size(max = 150, message = "Product name cannot be longer than 150 characters.")
    private String name;

    @Size(max = 1000, message = "Description cannot be longer than 1000 characters.")
    private String description;

    @NotBlank(message = "Category is required.")
    @Size(max = 100, message = "Category cannot be longer than 100 characters.")
    private String category;

    @NotBlank(message = "Brand is required.")
    @Size(max = 100, message = "Brand cannot be longer than 100 characters.")
    private String brand;

    @NotBlank(message = "Unit of measure is required.")
    @Size(max = 30, message = "Unit of measure cannot be longer than 30 characters.")
    private String unitOfMeasure;

    @NotNull(message = "Cost price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative.")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price cannot be negative.")
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required.")
    @PositiveOrZero(message = "Reorder level cannot be negative.")
    private Integer reorderLevel;

    @NotNull(message = "Max stock level is required.")
    @PositiveOrZero(message = "Max stock level cannot be negative.")
    private Integer maxStockLevel;

    @NotNull(message = "Lead time is required.")
    @PositiveOrZero(message = "Lead time days cannot be negative.")
    private Integer leadTimeDays;

    @Size(max = 500, message = "Image URL cannot be longer than 500 characters.")
    private String imageUrl;

    private Boolean isActive;

    @NotBlank(message = "Barcode is required.")
    @Size(max = 100, message = "Barcode cannot be longer than 100 characters.")
    private String barcode;
}
