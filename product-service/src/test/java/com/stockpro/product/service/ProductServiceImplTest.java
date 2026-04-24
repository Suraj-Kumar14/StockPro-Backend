package com.stockpro.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.product.client.WarehouseClient;
import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.dto.ProductResponse;
import com.stockpro.product.dto.StockLevelResponse;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateResourceException;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.service.impl.ProductServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ProductServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseClient warehouseClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        productRequest = ProductRequest.builder()
                .sku("sku-1001")
                .name("Wireless Mouse")
                .description("Simple office mouse")
                .category("Accessories")
                .brand("LogiTech")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("250.00"))
                .sellingPrice(new BigDecimal("400.00"))
                .reorderLevel(10)
                .maxStockLevel(100)
                .leadTimeDays(5)
                .imageUrl("https://example.com/mouse.png")
                .barcode("BAR-1001")
                .build();
    }

    @Test
    void createProductShouldSaveProductWhenSkuAndBarcodeAreUnique() {
        when(productRepository.existsBySkuIgnoreCase("SKU-1001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-1001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductId(1L);
            return product;
        });

        ProductResponse response = productService.createProduct(productRequest);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-1001");
        assertThat(productCaptor.getValue().getIsActive()).isTrue();
    }

    @Test
    void createProductShouldThrowExceptionWhenSkuAlreadyExists() {
        when(productRepository.existsBySkuIgnoreCase("SKU-1001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(productRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void getLowStockProductsShouldReturnOnlyProductsAtOrBelowReorderLevel() {
        Product lowStockProduct = Product.builder()
                .productId(1L)
                .sku("SKU-LOW")
                .name("Low Stock Item")
                .category("Category")
                .brand("Brand")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("15.00"))
                .reorderLevel(10)
                .maxStockLevel(40)
                .leadTimeDays(2)
                .barcode("LOW-001")
                .isActive(true)
                .build();

        Product healthyStockProduct = Product.builder()
                .productId(2L)
                .sku("SKU-OK")
                .name("Healthy Stock Item")
                .category("Category")
                .brand("Brand")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("10.00"))
                .sellingPrice(new BigDecimal("15.00"))
                .reorderLevel(5)
                .maxStockLevel(50)
                .leadTimeDays(2)
                .barcode("OK-001")
                .isActive(true)
                .build();

        when(productRepository.findByIsActive(true))
                .thenReturn(List.of(lowStockProduct, healthyStockProduct));
        when(warehouseClient.getStockLevels(anyList()))
                .thenReturn(List.of(
                        StockLevelResponse.builder().productId(1L).currentQuantity(8).build(),
                        StockLevelResponse.builder().productId(2L).currentQuantity(12).build()));

        var lowStockProducts = productService.getLowStockProducts();

        assertThat(lowStockProducts).hasSize(1);
        assertThat(lowStockProducts.get(0).getSku()).isEqualTo("SKU-LOW");
        assertThat(lowStockProducts.get(0).getCurrentQuantity()).isEqualTo(8);
    }
}
