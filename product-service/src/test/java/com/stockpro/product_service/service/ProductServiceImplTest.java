package com.stockpro.product_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import com.stockpro.product_service.service.impl.ProductServiceImpl;

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

        lenient().when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product.getProductId() == null) {
                product.setProductId(1L);
            }
            if (product.getCreatedAt() == null) {
                LocalDateTime now = LocalDateTime.now();
                product.setCreatedAt(now);
                product.setUpdatedAt(now);
            }
            return product;
        });
        lenient().doNothing().when(productAuditService).record(any(ProductAuditEntry.class));
    }

    @Test
    void createProduct_shouldCreateProduct_whenValidRequest() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-001")).thenReturn(false);

        ProductResponse response = productService.createProduct(buildCreateRequest(), 10L);

        assertEquals("SKU-001", response.getSku());
        assertEquals("Laptop", response.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_shouldPublishProductCreatedEvent() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-001")).thenReturn(false);

        productService.createProduct(buildCreateRequest(), 10L);

        ArgumentCaptor<ProductLifecycleEvent> captor = ArgumentCaptor.forClass(ProductLifecycleEvent.class);
        verify(productEventPublisher).publishProductCreated(captor.capture());
        assertEquals("PRODUCT_CREATED", captor.getValue().eventType());
        assertNotNull(captor.getValue().eventId());
    }

    @Test
    void createProduct_shouldThrowConflict_whenSkuAlreadyExists() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);
        CreateProductRequest request = buildCreateRequest();

        assertThrows(DuplicateSkuException.class, () -> productService.createProduct(request, 10L));
    }

    @Test
    void createProduct_shouldThrowConflict_whenBarcodeAlreadyExists() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("BAR-001")).thenReturn(true);
        CreateProductRequest request = buildCreateRequest();

        assertThrows(DuplicateBarcodeException.class, () -> productService.createProduct(request, 10L));
    }

    @Test
    void createProduct_shouldThrowBadRequest_whenMaxStockLessThanReorderLevel() {
        CreateProductRequest request = buildCreateRequest();
        request.setMaxStockLevel(2);

        assertThrows(InvalidProductDataException.class, () -> productService.createProduct(request, 10L));
    }

    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(buildProduct()));

        ProductResponse response = productService.getProductById(1L);

        assertEquals(1L, response.getProductId());
        assertEquals("SKU-001", response.getSku());
    }

    @Test
    void getProductById_shouldThrowNotFound_whenProductMissing() {
        when(productRepository.findByProductId(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(999L));
    }

    @Test
    void getProductBySku_shouldReturnProduct() {
        Product product = buildProduct();
        product.setSku("SKU-001");
        when(productRepository.findBySkuIgnoreCase("SKU-001")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductBySku("SKU-001");

        assertEquals(1L, response.getProductId());
        assertEquals("SKU-001", response.getSku());
    }

    @Test
    void getProductByBarcode_shouldReturnProduct() {
        when(productRepository.findByBarcode("BAR-001")).thenReturn(Optional.of(buildProduct()));

        ProductResponse response = productService.getProductByBarcode("BAR-001");

        assertEquals("BAR-001", response.getBarcode());
    }

    @Test
    void updateProduct_shouldUpdateProduct_whenValidRequest() {
        Product existing = buildProduct();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("SKU-001", 1L)).thenReturn(false);
        when(productRepository.existsByBarcodeAndProductIdNot("BAR-002", 1L)).thenReturn(false);

        UpdateProductRequest request = buildUpdateRequest();
        request.setBarcode("BAR-002");
        ProductResponse response = productService.updateProduct(1L, request, 10L);

        assertEquals("Laptop Pro", response.getName());
        verify(productEventPublisher).publishProductUpdated(any(ProductLifecycleEvent.class));
    }

    @Test
    void updateProduct_shouldThrowConflict_whenBarcodeAlreadyUsedByAnotherProduct() {
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(buildProduct()));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("SKU-001", 1L)).thenReturn(false);
        when(productRepository.existsByBarcodeAndProductIdNot("BAR-002", 1L)).thenReturn(true);

        UpdateProductRequest request = buildUpdateRequest();
        request.setBarcode("BAR-002");

        assertThrows(DuplicateBarcodeException.class, () -> productService.updateProduct(1L, request, 10L));
    }

    @Test
    void updateProduct_shouldPublishReorderConfigChangedEvent_whenReorderFieldsChanged() {
        Product existing = buildProduct();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("SKU-001", 1L)).thenReturn(false);
        when(productRepository.existsByBarcodeAndProductIdNot("BAR-001", 1L)).thenReturn(false);

        UpdateProductRequest request = buildUpdateRequest();
        request.setReorderLevel(9);

        productService.updateProduct(1L, request, 10L);

        verify(productEventPublisher).publishProductReorderConfigChanged(any(ProductLifecycleEvent.class));
    }

    @Test
    void deactivateProduct_shouldSetIsActiveFalse() {
        Product existing = buildProduct();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.deactivateProduct(1L, 10L);

        assertFalse(response.getIsActive());
        verify(productEventPublisher).publishProductDeactivated(any(ProductLifecycleEvent.class));
    }

    @Test
    void activateProduct_shouldSetIsActiveTrue() {
        Product existing = buildProduct();
        existing.setIsActive(false);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));

        ProductResponse response = productService.activateProduct(1L, 10L);

        assertTrue(response.getIsActive());
        verify(productEventPublisher).publishProductActivated(any(ProductLifecycleEvent.class));
    }

    @Test
    void searchProducts_shouldReturnPagedResult() {
        Page<Product> page = new PageImpl<>(List.of(buildProduct()));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ProductResponse> result = productService.searchProducts(
                "lap",
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
    void deleteProduct_shouldSoftDeleteInactiveProduct() {
        Product existing = buildProduct();
        existing.setIsActive(false);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(currentUserContext.getActorId()).thenReturn(10L);

        productService.deleteProduct(1L);

        assertFalse(existing.getIsActive());
        assertEquals(10L, existing.getUpdatedBy());
        verify(productRepository).save(existing);
        verify(productEventPublisher).publishProductDeleted(any(ProductLifecycleEvent.class));
    }

    @Test
    void deleteProduct_shouldSoftDeleteReferencedProduct() {
        Product existing = buildProduct();
        existing.setIsActive(false);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(currentUserContext.getActorId()).thenReturn(11L);

        productService.deleteProduct(1L);

        assertFalse(existing.getIsActive());
        assertEquals(11L, existing.getUpdatedBy());
        verify(productRepository).save(existing);
        verify(productEventPublisher).publishProductDeleted(any(ProductLifecycleEvent.class));
    }

    private Product buildProduct() {
        return Product.builder()
                .productId(1L)
                .sku("SKU-001")
                .name("Laptop")
                .description("Warehouse laptop")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("Piece")
                .costPrice(BigDecimal.valueOf(750))
                .sellingPrice(BigDecimal.valueOf(999))
                .reorderLevel(5)
                .maxStockLevel(25)
                .leadTimeDays(7)
                .imageUrl("https://example.com/laptop.png")
                .barcode("BAR-001")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(10L)
                .updatedBy(10L)
                .version(1L)
                .build();
    }

    private CreateProductRequest buildCreateRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("SKU-001");
        request.setName("Laptop");
        request.setDescription("Warehouse laptop");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(750));
        request.setSellingPrice(BigDecimal.valueOf(999));
        request.setReorderLevel(5);
        request.setMaxStockLevel(25);
        request.setLeadTimeDays(7);
        request.setImageUrl("https://example.com/laptop.png");
        request.setBarcode("BAR-001");
        return request;
    }

    private UpdateProductRequest buildUpdateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Laptop Pro");
        request.setDescription("Updated laptop");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(800));
        request.setSellingPrice(BigDecimal.valueOf(1099));
        request.setReorderLevel(6);
        request.setMaxStockLevel(30);
        request.setLeadTimeDays(10);
        request.setImageUrl("https://example.com/laptop-pro.png");
        request.setBarcode("BAR-001");
        request.setIsActive(true);
        return request;
    }
}
