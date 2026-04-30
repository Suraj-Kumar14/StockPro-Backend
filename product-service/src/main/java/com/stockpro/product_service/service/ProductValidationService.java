package com.stockpro.product_service.service;

import com.stockpro.product_service.dto.ProductRequestDTO;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.DuplicateBarcodeException;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ProductValidationService {

    private final ProductRepository productRepository;

    public SanitizedProductInput sanitize(ProductRequestDTO dto) {
        return new SanitizedProductInput(
                normalizeSku(dto.getSku()),
                trimToNull(dto.getName()),
                trimToNull(dto.getDescription()),
                trimToNull(dto.getCategory()),
                trimToNull(dto.getBrand()),
                trimToNull(dto.getUnitOfMeasure()),
                dto.getCostPrice(),
                dto.getSellingPrice(),
                dto.getReorderLevel(),
                dto.getMaxStockLevel(),
                dto.getLeadTimeDays(),
                trimToNull(dto.getImageUrl()),
                trimToNull(dto.getBarcode())
        );
    }

    public void validateForCreate(SanitizedProductInput input) {
        validateRequiredTextFields(input);
        validateBusinessRules(input);
        if (productRepository.existsBySkuIgnoreCase(input.sku())) {
            throw new DuplicateSkuException("Product with SKU " + input.sku() + " already exists");
        }
        if (input.barcode() != null && productRepository.existsByBarcode(input.barcode())) {
            throw new DuplicateBarcodeException(
                    "Product with barcode " + input.barcode() + " already exists");
        }
    }

    public void validateForUpdate(Product existing, SanitizedProductInput input) {
        SanitizedProductInput mergedInput = mergeWithExisting(existing, input);
        validateRequiredTextFields(mergedInput);
        validateBusinessRules(mergedInput);

        if (input.sku() != null && !existing.getSku().equals(normalizeSku(input.sku()))) {
            throw new InvalidProductDataException("SKU cannot be changed once the product is created");
        }

        if (mergedInput.barcode() != null
                && !mergedInput.barcode().equals(existing.getBarcode())
                && productRepository.existsByBarcodeAndProductIdNot(
                        mergedInput.barcode(), existing.getProductId())) {
            throw new DuplicateBarcodeException(
                    "Product with barcode " + mergedInput.barcode() + " already exists");
        }
    }

    public SanitizedProductInput mergeWithExisting(Product existing, SanitizedProductInput input) {
        return new SanitizedProductInput(
                existing.getSku(),
                input.name() != null ? input.name() : existing.getName(),
                input.description() != null ? input.description() : existing.getDescription(),
                input.category() != null ? input.category() : existing.getCategory(),
                input.brand() != null ? input.brand() : existing.getBrand(),
                input.unitOfMeasure() != null ? input.unitOfMeasure() : existing.getUnitOfMeasure(),
                input.costPrice() != null ? input.costPrice() : existing.getCostPrice(),
                input.sellingPrice() != null ? input.sellingPrice() : existing.getSellingPrice(),
                input.reorderLevel() != null ? input.reorderLevel() : existing.getReorderLevel(),
                input.maxStockLevel() != null ? input.maxStockLevel() : existing.getMaxStockLevel(),
                input.leadTimeDays() != null ? input.leadTimeDays() : existing.getLeadTimeDays(),
                input.imageUrl() != null ? input.imageUrl() : existing.getImageUrl(),
                input.barcode() != null ? input.barcode() : existing.getBarcode()
        );
    }

    private void validateRequiredTextFields(SanitizedProductInput input) {
        if (input.sku() == null) {
            throw new InvalidProductDataException("SKU is required");
        }
        if (input.name() == null) {
            throw new InvalidProductDataException("Product name is required");
        }
        if (input.category() == null) {
            throw new InvalidProductDataException("Category is required");
        }
        if (input.unitOfMeasure() == null) {
            throw new InvalidProductDataException("Unit of measure is required");
        }
    }

    private void validateBusinessRules(SanitizedProductInput input) {
        if (input.costPrice() == null || input.costPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Cost price cannot be negative");
        }
        if (input.sellingPrice() == null || input.sellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Selling price cannot be negative");
        }
        if (input.sellingPrice().compareTo(input.costPrice()) < 0) {
            throw new InvalidProductDataException(
                    "Selling price must be greater than or equal to cost price");
        }
        if (input.reorderLevel() == null || input.reorderLevel() < 0) {
            throw new InvalidProductDataException("Reorder level cannot be negative");
        }
        if (input.maxStockLevel() == null || input.maxStockLevel() < 0) {
            throw new InvalidProductDataException("Max stock level cannot be negative");
        }
        if (input.maxStockLevel() <= input.reorderLevel()) {
            throw new InvalidProductDataException(
                    "Max stock level must be greater than reorder level");
        }
        if (input.leadTimeDays() == null || input.leadTimeDays() < 0) {
            throw new InvalidProductDataException("Lead time cannot be negative");
        }
    }

    private String normalizeSku(String sku) {
        String trimmedSku = trimToNull(sku);
        return trimmedSku == null ? null : trimmedSku.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    public record SanitizedProductInput(
            String sku,
            String name,
            String description,
            String category,
            String brand,
            String unitOfMeasure,
            BigDecimal costPrice,
            BigDecimal sellingPrice,
            Integer reorderLevel,
            Integer maxStockLevel,
            Integer leadTimeDays,
            String imageUrl,
            String barcode) {
    }
}
