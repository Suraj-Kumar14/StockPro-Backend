package com.stockpro.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFormRequest {

    private Long productId;

    @NotBlank(message = "SKU is required.")
    @Size(max = 100, message = "SKU cannot be longer than 100 characters.")
    private String sku;

    @NotBlank(message = "Product name is required.")
    @Size(max = 255, message = "Product name cannot be longer than 255 characters.")
    private String name;

    @Size(max = 1000, message = "Description cannot be longer than 1000 characters.")
    private String description;

    @NotBlank(message = "Category is required.")
    @Size(max = 150, message = "Category cannot be longer than 150 characters.")
    private String category;

    @NotBlank(message = "Brand is required.")
    @Size(max = 150, message = "Brand cannot be longer than 150 characters.")
    private String brand;

    @NotBlank(message = "Unit of measure is required.")
    @Size(max = 100, message = "Unit of measure cannot be longer than 100 characters.")
    private String unitOfMeasure;

    @NotNull(message = "Cost price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative.")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price cannot be negative.")
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level cannot be negative.")
    private BigDecimal reorderLevel;

    @NotNull(message = "Max stock level is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Max stock level cannot be negative.")
    private BigDecimal maxStockLevel;

    @NotNull(message = "Lead time days is required.")
    @PositiveOrZero(message = "Lead time days cannot be negative.")
    private Integer leadTimeDays;

    @URL(message = "Image URL must be valid.")
    private String imageUrl;

    private String barcode;
    private Boolean active = Boolean.TRUE;
}
