package com.stockpro.product.service;

import com.stockpro.product.dto.LowStockProductResponse;
import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.dto.ProductResponse;
import java.util.List;

/**
 * Service contract for product master operations.
 */
public interface ProductService {

    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse updateProduct(Long productId, ProductRequest productRequest);

    ProductResponse getById(Long productId);

    ProductResponse getBySku(String sku);

    List<ProductResponse> getByCategory(String category);

    List<ProductResponse> getByBrand(String brand);

    List<ProductResponse> searchProducts(String keyword);

    List<ProductResponse> getAllProducts();

    ProductResponse getByBarcode(String barcode);

    List<LowStockProductResponse> getLowStockProducts();

    void deactivateProduct(Long productId);

    void deleteProduct(Long productId);
}
