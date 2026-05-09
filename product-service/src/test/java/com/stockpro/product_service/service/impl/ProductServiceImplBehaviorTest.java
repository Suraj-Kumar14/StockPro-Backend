package com.stockpro.product_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.dto.response.ProductResponse;
import com.stockpro.product_service.dto.response.ProductSummaryResponse;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.repository.ProductRepository;
import com.stockpro.product_service.service.CurrentUserContext;
import com.stockpro.product_service.service.InventoryAvailabilityGateway;
import com.stockpro.product_service.service.ProductAuditService;
import com.stockpro.product_service.service.ProductEventPublisher;
import com.stockpro.product_service.service.ProductMapper;
import com.stockpro.product_service.service.ProductValidationService;
import com.stockpro.product_service.service.PurchaseOrderUsageGateway;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplBehaviorTest {

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
    void getAllProducts_shouldSanitizePagingAndFallbackSortField() {
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildProduct())));

        ProductResponse response = productService.getAllProducts(-4, 0, "unsupported", "desc")
                .getContent()
                .get(0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("name").getDirection());
        assertEquals("SKU-001", response.getSku());
    }

    @Test
    void searchProducts_shouldCapPageSizeAndNormalizeAscendingSort() {
        when(productRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildProduct())));

        productService.searchProducts(" laptop ", "Electronics", "Dell", true, 3, 500, "sku", "ASC");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(3, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("sku").getDirection());
    }

    @Test
    void getProductBySkuAndBarcode_shouldThrowWhenMissing() {
        when(productRepository.findBySkuIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());
        when(productRepository.findByBarcode("BAR-404")).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductBySku(" unknown "));
        assertThrows(ProductNotFoundException.class, () -> productService.getProductByBarcode(" BAR-404 "));
    }

    @Test
    void getCategoryAndBrandMethods_shouldTrimInputAndMapResults() {
        Product product = buildProduct();
        when(productRepository.findByCategoryIgnoreCaseOrderByNameAsc("Electronics")).thenReturn(List.of(product));
        when(productRepository.findByBrandIgnoreCaseOrderByNameAsc("Dell")).thenReturn(List.of(product));
        when(productRepository.findDistinctCategories()).thenReturn(List.of("Electronics", "Hardware"));
        when(productRepository.findDistinctBrands()).thenReturn(List.of("Dell", "HP"));

        List<ProductResponse> byCategory = productService.getProductsByCategory("  Electronics  ");
        List<ProductResponse> byBrand = productService.getProductsByBrand(" Dell ");

        assertEquals(1, byCategory.size());
        assertEquals("Laptop", byCategory.get(0).getName());
        assertEquals(1, byBrand.size());
        assertEquals(List.of("Electronics", "Hardware"), productService.getCategories());
        assertEquals(List.of("Dell", "HP"), productService.getBrands());
    }

    @Test
    void getProductSummary_shouldAggregateRepositoryCounts() {
        when(productRepository.count()).thenReturn(12L);
        when(productRepository.countByIsActive(true)).thenReturn(9L);
        when(productRepository.countByIsActive(false)).thenReturn(3L);
        when(productRepository.countDistinctCategories()).thenReturn(4L);
        when(productRepository.countDistinctBrands()).thenReturn(5L);

        ProductSummaryResponse summary = productService.getProductSummary();

        assertEquals(12L, summary.getTotalProducts());
        assertEquals(9L, summary.getActiveProducts());
        assertEquals(3L, summary.getInactiveProducts());
        assertEquals(4L, summary.getCategoriesCount());
        assertEquals(5L, summary.getBrandsCount());
    }

    @Test
    void updateProduct_shouldInitializeNullVersionAndSkipReorderEventWhenLevelsUnchanged() {
        Product existing = buildProduct();
        UpdateProductRequest request = buildUpdateRequest();
        request.setReorderLevel(existing.getReorderLevel());
        request.setMaxStockLevel(existing.getMaxStockLevel());
        request.setLeadTimeDays(existing.getLeadTimeDays());
        request.setBarcode(existing.getBarcode());
        request.setIsActive(null);

        when(productRepository.initializeNullVersion(1L)).thenReturn(1);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("SKU-001", 1L)).thenReturn(false);
        when(productRepository.existsByBarcodeAndProductIdNot("BAR-001", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request, 90L);

        verify(productRepository).initializeNullVersion(1L);
        verify(productEventPublisher).publishProductUpdated(any());
        verify(productEventPublisher, never()).publishProductReorderConfigChanged(any());
        assertTrue(response.getIsActive());
        assertEquals(90L, existing.getUpdatedBy());
    }

    @Test
    void updateProduct_shouldRejectDuplicateSkuOnCurrentProductUpdate() {
        when(productRepository.initializeNullVersion(1L)).thenReturn(0);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(buildProduct()));
        when(productRepository.existsBySkuIgnoreCaseAndProductIdNot("SKU-001", 1L)).thenReturn(true);

        assertThrows(DuplicateSkuException.class, () -> productService.updateProduct(1L, buildUpdateRequest(), 12L));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_shouldRejectWhenReferencedByPurchaseOrders() {
        Product inactive = buildProduct();
        inactive.setIsActive(false);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(inactive));
        when(inventoryAvailabilityGateway.hasInventoryUsage(1L)).thenReturn(false);
        when(purchaseOrderUsageGateway.hasPurchaseOrderUsage(1L)).thenReturn(true);

        assertThrows(InvalidProductDataException.class, () -> productService.deleteProduct(1L));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deactivateAndActivateProduct_shouldUpdateStateAndActor() {
        Product active = buildProduct();
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(active));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse deactivated = productService.deactivateProduct(1L, 200L);
        assertFalse(deactivated.getIsActive());
        assertEquals(200L, active.getUpdatedBy());

        active.setIsActive(false);
        ProductResponse activated = productService.activateProduct(1L, 201L);
        assertTrue(activated.getIsActive());
        assertEquals(201L, active.getUpdatedBy());
        verify(productEventPublisher).publishProductDeactivated(any());
        verify(productEventPublisher).publishProductActivated(any());
    }

    @Test
    void deleteProduct_shouldDeleteInactiveUnreferencedProductAndUseCurrentActor() {
        Product inactive = buildProduct();
        inactive.setIsActive(false);
        when(productRepository.findByProductId(1L)).thenReturn(Optional.of(inactive));
        when(inventoryAvailabilityGateway.hasInventoryUsage(1L)).thenReturn(false);
        when(purchaseOrderUsageGateway.hasPurchaseOrderUsage(1L)).thenReturn(false);
        when(currentUserContext.getActorId()).thenReturn(321L);

        productService.deleteProduct(1L);

        verify(productRepository).delete(inactive);
        verify(currentUserContext).getActorId();
        verify(productEventPublisher).publishProductDeleted(any());
    }

    private UpdateProductRequest buildUpdateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Laptop Updated");
        request.setDescription("Updated description");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(800));
        request.setSellingPrice(BigDecimal.valueOf(1100));
        request.setReorderLevel(8);
        request.setMaxStockLevel(35);
        request.setLeadTimeDays(10);
        request.setImageUrl("https://example.com/item.png");
        request.setBarcode("BAR-001");
        request.setIsActive(true);
        return request;
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
                .costPrice(BigDecimal.valueOf(700))
                .sellingPrice(BigDecimal.valueOf(950))
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
}
