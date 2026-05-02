package com.stockpro.product_service.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be less than or equal to 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Name must be less than or equal to 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than or equal to 1000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must be less than or equal to 100 characters")
    private String category;

    @Size(max = 100, message = "Brand must be less than or equal to 100 characters")
    private String brand;

    @NotBlank(message = "Unit of measure is required")
    @Size(max = 50, message = "Unit of measure must be less than or equal to 50 characters")
    private String unitOfMeasure;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @NotNull(message = "Max stock level is required")
    @Min(value = 0, message = "Max stock level cannot be negative")
    private Integer maxStockLevel;

    @NotNull(message = "Lead time days is required")
    @Min(value = 0, message = "Lead time days cannot be negative")
    private Integer leadTimeDays;

    @Size(max = 500, message = "Image URL must be less than or equal to 500 characters")
    private String imageUrl;

    @Size(max = 100, message = "Barcode must be less than or equal to 100 characters")
    private String barcode;
}
