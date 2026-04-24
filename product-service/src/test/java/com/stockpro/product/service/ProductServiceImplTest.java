package com.stockpro.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.product.client.WarehouseClient;
import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.request.UpdateProductRequest;
import com.stockpro.product.dto.response.ProductResponse;
import com.stockpro.product.dto.response.StockLevelQuantityResponse;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateBarcodeException;
import com.stockpro.product.mapper.ProductMapper;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.service.impl.ProductServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseClient warehouseClient;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, new ProductMapper(), warehouseClient);
    }

    @Test
    void createProductShouldNormalizeSkuAndSaveProduct() {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("sku-1001")
                .name("Industrial Drill")
                .description("Heavy-duty drill")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("1499.5000"))
                .sellingPrice(new BigDecimal("1999.9000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .imageUrl("https://cdn.stockpro.com/products/drill.png")
                .barcode("8901234567890")
                .build();

        when(productRepository.existsBySkuIgnoreCase("SKU-1001")).thenReturn(false);
        when(productRepository.findByBarcode("8901234567890")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductId(1L);
            return product;
        });

        ProductResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-1001");
        assertThat(response.getIsActive()).isTrue();
        assertThat(productCaptor.getValue().getCostPrice()).isEqualByComparingTo("1499.5000");
    }

    @Test
    void updateProductShouldRejectDuplicateBarcode() {
        Product existingProduct = Product.builder()
                .productId(1L)
                .sku("SKU-1001")
                .name("Industrial Drill")
                .description("Heavy-duty drill")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("1499.5000"))
                .sellingPrice(new BigDecimal("1999.9000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .barcode("8901234567890")
                .isActive(Boolean.TRUE)
                .build();

        Product duplicateBarcodeProduct = Product.builder()
                .productId(2L)
                .sku("SKU-2002")
                .name("Angle Grinder")
                .category("Tools")
                .brand("Makita")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("900.0000"))
                .sellingPrice(new BigDecimal("1299.0000"))
                .reorderLevel(new BigDecimal("8.0000"))
                .maxStockLevel(new BigDecimal("80.0000"))
                .leadTimeDays(4)
                .barcode("8901234567899")
                .isActive(Boolean.TRUE)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.findByBarcode("8901234567899")).thenReturn(Optional.of(duplicateBarcodeProduct));

        assertThatThrownBy(() -> productService.updateProduct(1L, UpdateProductRequest.builder()
                .barcode("8901234567899")
                .build()))
                .isInstanceOf(DuplicateBarcodeException.class)
                .hasMessageContaining("barcode");
    }

    @Test
    void getLowStockProductsShouldReturnOnlyProductsAtOrBelowReorderLevel() {
        Product lowStockProduct = Product.builder()
                .productId(1L)
                .sku("SKU-LOW")
                .name("Low Stock Item")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("15.0000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("40.0000"))
                .leadTimeDays(2)
                .barcode("LOW-001")
                .isActive(Boolean.TRUE)
                .build();

        Product healthyStockProduct = Product.builder()
                .productId(2L)
                .sku("SKU-OK")
                .name("Healthy Stock Item")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("15.0000"))
                .reorderLevel(new BigDecimal("5.0000"))
                .maxStockLevel(new BigDecimal("50.0000"))
                .leadTimeDays(2)
                .barcode("OK-001")
                .isActive(Boolean.TRUE)
                .build();

        when(productRepository.findByIsActive(Boolean.TRUE)).thenReturn(List.of(lowStockProduct, healthyStockProduct));
        when(warehouseClient.getStockLevels(anyList())).thenReturn(List.of(
                StockLevelQuantityResponse.builder().productId(1L).currentQuantity(8).build(),
                StockLevelQuantityResponse.builder().productId(2L).currentQuantity(12).build()));

        var lowStockProducts = productService.getLowStockProducts();

        assertThat(lowStockProducts).hasSize(1);
        assertThat(lowStockProducts.get(0).getSku()).isEqualTo("SKU-LOW");
        assertThat(lowStockProducts.get(0).getReorderLevel()).isEqualByComparingTo("10.0000");
    }

    @Test
    void deleteProductShouldSoftDeleteByDeactivatingRecord() {
        Product existingProduct = Product.builder()
                .productId(1L)
                .sku("SKU-1001")
                .name("Industrial Drill")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("1499.5000"))
                .sellingPrice(new BigDecimal("1999.9000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .isActive(Boolean.TRUE)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        productService.deleteProduct(1L);

        verify(productRepository).save(existingProduct);
        assertThat(existingProduct.getIsActive()).isFalse();
    }
}
