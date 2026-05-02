package com.stockpro.product_service.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.dto.response.ProductResponse;
import com.stockpro.product_service.dto.response.ProductSummaryResponse;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request, Long actorId);

    ProductResponse getProductById(Long productId);

    ProductResponse getProductBySku(String sku);

    ProductResponse getProductByBarcode(String barcode);

    Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    Page<ProductResponse> searchProducts(
            String keyword,
            String category,
            String brand,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir);

    ProductResponse updateProduct(Long productId, UpdateProductRequest request, Long actorId);

    ProductResponse deactivateProduct(Long productId, Long actorId);

    ProductResponse activateProduct(Long productId, Long actorId);

    void deleteProduct(Long productId);

    List<ProductResponse> getProductsByCategory(String category);

    List<ProductResponse> getProductsByBrand(String brand);

    List<String> getCategories();

    List<String> getBrands();

    ProductSummaryResponse getProductSummary();
}
