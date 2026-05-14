package com.stockpro.product_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.dto.response.ProductResponse;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.DuplicateBarcodeException;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.repository.ProductRepository;
import com.stockpro.product_service.service.CurrentUserContext;
import com.stockpro.product_service.service.InventoryAvailabilityGateway;
import com.stockpro.product_service.service.ProductAuditEntry;
import com.stockpro.product_service.service.ProductAuditService;
import com.stockpro.product_service.service.ProductEventPublisher;
import com.stockpro.product_service.service.ProductLifecycleEvent;
import com.stockpro.product_service.service.ProductMapper;
import com.stockpro.product_service.service.ProductValidationService;
import com.stockpro.product_service.service.PurchaseOrderUsageGateway;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryAvailabilityGateway inventoryAvailabilityGateway;

    @Mock
    private PurchaseOrderUsageGateway purchaseOrderUsageGateway;

    @Mock
    private ProductAuditService productAuditService;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @Mock
    private CurrentUserContext currentUserContext;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productRepository,
                new ProductMapper(),
                new ProductValidationService(),
                inventoryAvailabilityGateway,
                purchaseOrderUsageGateway,
                productAuditService,
                productEventPublisher,
                currentUserContext);
    }

    @Test
    void createProduct_shouldCreateProduct_whenValidRequest() {
        CreateProductRequest request = buildCreateRequest();
        when(productRepository.existsBySkuIgnoreCase("CAN-INK-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductId(1L);
            return product;
        });

        ProductResponse result = productService.createProduct(request, 101L);

        assertEquals(1L, result.getProductId());
        assertEquals("CAN-INK-001", result.getSku());
        assertEquals("Laptop", result.getName());
        verify(productRepository).save(any(Product.class));
        verify(productAuditService).record(any(ProductAuditEntry.class));
        verify(productEventPublisher).publishProductCreated(any(ProductLifecycleEvent.class));
    }

    @Test
    void createProduct_shouldCreateProduct_whenSimpleSkuFormatIsValid() {
        CreateProductRequest request = buildCreateRequest();
        request.setSku("SKU-001");
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductId(2L);
            return product;
        });

        ProductResponse result = productService.createProduct(request, 101L);

        assertEquals(2L, result.getProductId());
        assertEquals("SKU-001", result.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrowConflict_whenSkuAlreadyExists() {
        CreateProductRequest request = buildCreateRequest();
        when(productRepository.existsBySkuIgnoreCase("CAN-INK-001")).thenReturn(true);

        assertThrows(DuplicateSkuException.class, () -> productService.createProduct(request, 101L));
    }

    @Test
    void createProduct_shouldThrowConflict_whenBarcodeAlreadyExists() {
        CreateProductRequest request = buildCreateRequest();
        when(productRepository.existsBySkuIgnoreCase("CAN-INK-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-001")).thenReturn(true);

        assertThrows(DuplicateBarcodeException.class, () -> productService.createProduct(request, 101L));
    }

    @Test
    void createProduct_shouldThrowBadRequest_whenMaxStockLessThanReorderLevel() {
        CreateProductRequest request = buildCreateRequest();
        request.setReorderLevel(20);
        request.setMaxStockLevel(10);

        assertThrows(InvalidProductDataException.class, () -> productService.createProduct(request, 101L));
    }

    @Test
    void createProduct_shouldRejectLowercaseSku() {
        CreateProductRequest request = buildCreateRequest();
        request.setSku("sku-001");

        InvalidProductDataException exception = assertThrows(
                InvalidProductDataException.class,
                () -> productService.createProduct(request, 101L));

        assertEquals("SKU must be valid format like SKU-001 or CAN-INK-001", exception.getMessage());
    }

    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(buildProduct()));

        ProductResponse result = productService.getProductById(1L);

        assertEquals(1L, result.getProductId());
        assertEquals("CAN-INK-001", result.getSku());
    }

    @Test
    void getProductById_shouldThrowNotFound_whenProductMissing() {
        when(productRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getProductBySku_shouldReturnProduct() {
        Product product = buildProduct();
        product.setSku("SKU-001");
        when(productRepository.findBySkuIgnoreCase("SKU-001")).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductBySku(" SKU-001 ");

        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void getProductByBarcode_shouldReturnProduct() {
        when(productRepository.findByBarcode("BAR-001")).thenReturn(Optional.of(buildProduct()));

        ProductResponse result = productService.getProductByBarcode(" BAR-001 ");

        assertEquals("BAR-001", result.getBarcode());
    }

    @Test
    void updateProduct_shouldUpdateProduct_whenValidRequest() {
        Product existingProduct = buildProduct();
        UpdateProductRequest request = buildUpdateRequest();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("CAN-INK-001", 1L)).thenReturn(false);
        when(productRepository.existsByBarcodeAndProductIdNot("BAR-002", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request, 202L);

        assertEquals("Updated Laptop", result.getName());
        assertEquals("BAR-002", result.getBarcode());
        assertEquals(202L, existingProduct.getUpdatedBy());
        verify(productAuditService).record(any(ProductAuditEntry.class));
        verify(productEventPublisher).publishProductUpdated(any(ProductLifecycleEvent.class));
        verify(productEventPublisher).publishProductReorderConfigChanged(any(ProductLifecycleEvent.class));
    }

    @Test
    void updateProduct_shouldThrowConflict_whenBarcodeAlreadyUsedByAnotherProduct() {
        Product existingProduct = buildProduct();
        UpdateProductRequest request = buildUpdateRequest();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("CAN-INK-001", 1L)).thenReturn(false);
        when(productRepository.existsByBarcodeAndProductIdNot("BAR-002", 1L)).thenReturn(true);

        assertThrows(DuplicateBarcodeException.class, () -> productService.updateProduct(1L, request, 202L));
    }

    @Test
    void deactivateProduct_shouldSetIsActiveFalse() {
        Product existingProduct = buildProduct();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.deactivateProduct(1L, 303L);

        assertFalse(result.getIsActive());
        assertFalse(existingProduct.getIsActive());
        verify(productEventPublisher).publishProductDeactivated(any(ProductLifecycleEvent.class));
    }

    @Test
    void activateProduct_shouldSetIsActiveTrue() {
        Product existingProduct = buildProduct();
        existingProduct.setIsActive(false);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse result = productService.activateProduct(1L, 404L);

        assertTrue(result.getIsActive());
        assertTrue(existingProduct.getIsActive());
    }

    @Test
    void searchProducts_shouldReturnPagedResult() {
        Page<Product> productPage = new PageImpl<>(List.of(buildProduct()));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(productPage);

        Page<ProductResponse> result = productService.searchProducts(
                "laptop",
                "Electronics",
                "Dell",
                true,
                0,
                10,
                "name",
                "asc");

        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());
    }

    @Test
    void deleteProduct_shouldSoftDeleteProduct() {
        Product product = buildProduct();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(product));
        when(currentUserContext.getActorId()).thenReturn(505L);

        productService.deleteProduct(1L);

        assertFalse(product.getIsActive());
        verify(productRepository).save(product);
        verify(productAuditService).record(any(ProductAuditEntry.class));
    }

    private CreateProductRequest buildCreateRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("CAN-INK-001");
        request.setName("Laptop");
        request.setDescription("Warehouse laptop");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(750.00));
        request.setSellingPrice(BigDecimal.valueOf(999.00));
        request.setReorderLevel(5);
        request.setMaxStockLevel(25);
        request.setLeadTimeDays(7);
        request.setImageUrl("https://example.com/laptop.png");
        request.setBarcode("BAR-001");
        return request;
    }

    private UpdateProductRequest buildUpdateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Laptop");
        request.setDescription("Updated warehouse laptop");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(800.00));
        request.setSellingPrice(BigDecimal.valueOf(1099.00));
        request.setReorderLevel(8);
        request.setMaxStockLevel(30);
        request.setLeadTimeDays(10);
        request.setImageUrl("https://example.com/laptop-v2.png");
        request.setBarcode("BAR-002");
        request.setIsActive(true);
        return request;
    }

    private Product buildProduct() {
        return Product.builder()
                .productId(1L)
                .sku("CAN-INK-001")
                .name("Laptop")
                .description("Warehouse laptop")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("Piece")
                .costPrice(BigDecimal.valueOf(750.00))
                .sellingPrice(BigDecimal.valueOf(999.00))
                .reorderLevel(5)
                .maxStockLevel(25)
                .leadTimeDays(7)
                .imageUrl("https://example.com/laptop.png")
                .barcode("BAR-001")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(101L)
                .updatedBy(101L)
                .version(1L)
                .build();
    }
}
