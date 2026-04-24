package com.stockpro.product.service;

import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.request.ProductSearchRequest;
import com.stockpro.product.dto.request.UpdateProductRequest;
import com.stockpro.product.dto.response.LowStockProductResponse;
import com.stockpro.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getById(Long productId);

    ProductResponse getBySku(String sku);

    List<ProductResponse> getByCategory(String category);

    List<ProductResponse> getByBrand(String brand);

    Page<ProductResponse> searchProducts(ProductSearchRequest request);

    List<ProductResponse> searchProducts(String keyword);

    ProductResponse updateProduct(Long productId, UpdateProductRequest request);

    ProductResponse deactivateProduct(Long productId);

    void deleteProduct(Long productId);

    Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    List<ProductResponse> getAllProducts();

    ProductResponse getByBarcode(String barcode);

    List<LowStockProductResponse> getLowStockProducts();
}
