package com.stockpro.product_service.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.exception.InvalidProductDataException;

@Component
public class ProductValidationService {

    public CreateProductRequest sanitize(CreateProductRequest request) {
        CreateProductRequest sanitized = new CreateProductRequest();
        sanitized.setSku(normalizeSku(request.getSku()));
        sanitized.setName(trimToNull(request.getName()));
        sanitized.setDescription(trimToNull(request.getDescription()));
        sanitized.setCategory(trimToNull(request.getCategory()));
        sanitized.setBrand(trimToNull(request.getBrand()));
        sanitized.setUnitOfMeasure(trimToNull(request.getUnitOfMeasure()));
        sanitized.setCostPrice(request.getCostPrice());
        sanitized.setSellingPrice(request.getSellingPrice());
        sanitized.setReorderLevel(request.getReorderLevel());
        sanitized.setMaxStockLevel(request.getMaxStockLevel());
        sanitized.setLeadTimeDays(request.getLeadTimeDays());
        sanitized.setImageUrl(trimToNull(request.getImageUrl()));
        sanitized.setBarcode(trimToNull(request.getBarcode()));
        return sanitized;
    }

    public UpdateProductRequest sanitize(UpdateProductRequest request) {
        UpdateProductRequest sanitized = new UpdateProductRequest();
        sanitized.setName(trimToNull(request.getName()));
        sanitized.setDescription(trimToNull(request.getDescription()));
        sanitized.setCategory(trimToNull(request.getCategory()));
        sanitized.setBrand(trimToNull(request.getBrand()));
        sanitized.setUnitOfMeasure(trimToNull(request.getUnitOfMeasure()));
        sanitized.setCostPrice(request.getCostPrice());
        sanitized.setSellingPrice(request.getSellingPrice());
        sanitized.setReorderLevel(request.getReorderLevel());
        sanitized.setMaxStockLevel(request.getMaxStockLevel());
        sanitized.setLeadTimeDays(request.getLeadTimeDays());
        sanitized.setImageUrl(trimToNull(request.getImageUrl()));
        sanitized.setBarcode(trimToNull(request.getBarcode()));
        sanitized.setIsActive(request.getIsActive());
        return sanitized;
    }

    public void validateBusinessRules(
            BigDecimal costPrice,
            BigDecimal sellingPrice,
            Integer reorderLevel,
            Integer maxStockLevel,
            Integer leadTimeDays) {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Cost price cannot be negative");
        }
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Selling price cannot be negative");
        }
        if (reorderLevel == null || reorderLevel < 0) {
            throw new InvalidProductDataException("Reorder level cannot be negative");
        }
        if (maxStockLevel == null || maxStockLevel < 0) {
            throw new InvalidProductDataException("Max stock level cannot be negative");
        }
        if (maxStockLevel <= reorderLevel) {
            throw new InvalidProductDataException("Max stock level must be greater than reorder level");
        }
        if (leadTimeDays == null || leadTimeDays < 0) {
            throw new InvalidProductDataException("Lead time days cannot be negative");
        }
    }

    public String normalizeSku(String sku) {
        String trimmedSku = trimToNull(sku);
        return trimmedSku == null ? null : trimmedSku.toUpperCase(Locale.ROOT);
    }

    public String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
