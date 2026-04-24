package com.stockpro.product.mapper;

import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.request.UpdateProductRequest;
import com.stockpro.product.dto.response.LowStockProductResponse;
import com.stockpro.product.dto.response.ProductResponse;
import com.stockpro.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest request) {
        return Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .brand(request.getBrand())
                .unitOfMeasure(request.getUnitOfMeasure())
                .costPrice(request.getCostPrice())
                .sellingPrice(request.getSellingPrice())
                .reorderLevel(request.getReorderLevel())
                .maxStockLevel(request.getMaxStockLevel())
                .leadTimeDays(request.getLeadTimeDays())
                .imageUrl(request.getImageUrl())
                .barcode(request.getBarcode())
                .isActive(Boolean.TRUE)
                .build();
    }

    public void updateEntity(UpdateProductRequest request, Product product) {
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getUnitOfMeasure() != null) {
            product.setUnitOfMeasure(request.getUnitOfMeasure());
        }
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        if (request.getSellingPrice() != null) {
            product.setSellingPrice(request.getSellingPrice());
        }
        if (request.getReorderLevel() != null) {
            product.setReorderLevel(request.getReorderLevel());
        }
        if (request.getMaxStockLevel() != null) {
            product.setMaxStockLevel(request.getMaxStockLevel());
        }
        if (request.getLeadTimeDays() != null) {
            product.setLeadTimeDays(request.getLeadTimeDays());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getBarcode() != null) {
            product.setBarcode(request.getBarcode());
        }
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
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
                .isActive(product.getIsActive())
                .barcode(product.getBarcode())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public LowStockProductResponse toLowStockResponse(Product product) {
        return LowStockProductResponse.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .name(product.getName())
                .reorderLevel(product.getReorderLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .barcode(product.getBarcode())
                .category(product.getCategory())
                .brand(product.getBrand())
                .build();
    }
}
