package com.stockpro.product_service.service;

import com.stockpro.product_service.dto.ProductResponseDTO;
import com.stockpro.product_service.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponseDTO toResponse(Product product) {
        return ProductResponseDTO.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .unitOfMeasure(product.getUnitOfMeasure())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .reorderLevel(product.getReorderLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .leadTimeDays(product.getLeadTimeDays())
                .imageUrl(product.getImageUrl())
                .barcode(product.getBarcode())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
