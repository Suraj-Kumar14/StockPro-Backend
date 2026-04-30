package com.stockpro.product_service;

import com.stockpro.product_service.dto.ProductRequestDTO;
import com.stockpro.product_service.dto.ProductResponseDTO;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.DuplicateBarcodeException;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.repository.ProductRepository;
import com.stockpro.product_service.service.InventoryAvailabilityGateway;
import com.stockpro.product_service.service.ProductMapper;
import com.stockpro.product_service.service.ProductService;
import com.stockpro.product_service.service.ProductValidationService;
import com.stockpro.product_service.service.PurchaseOrderUsageGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryAvailabilityGateway inventoryAvailabilityGateway;

    @Mock
    private PurchaseOrderUsageGateway purchaseOrderUsageGateway;

    @Spy
    private ProductMapper productMapper;

    private ProductValidationService productValidationService;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;
    private ProductRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        productValidationService = new ProductValidationService(productRepository);
        productService = new ProductService(
                productRepository,
                productMapper,
                productValidationService,
                inventoryAvailabilityGateway,
                purchaseOrderUsageGateway
        );

        mockProduct = Product.builder()
                .productId(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("Test Description")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("piece")
                .costPrice(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("150.00"))
                .reorderLevel(5)
                .maxStockLevel(100)
                .leadTimeDays(7)
                .barcode("1234567890")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDTO = new ProductRequestDTO();
        requestDTO.setSku(" sku-001 ");
        requestDTO.setName(" Test Product ");
        requestDTO.setDescription("Test Description");
        requestDTO.setCategory(" Electronics ");
        requestDTO.setBrand(" Dell ");
        requestDTO.setUnitOfMeasure(" piece ");
        requestDTO.setCostPrice(new BigDecimal("100.00"));
        requestDTO.setSellingPrice(new BigDecimal("150.00"));
        requestDTO.setReorderLevel(5);
        requestDTO.setMaxStockLevel(100);
        requestDTO.setLeadTimeDays(7);
        requestDTO.setBarcode("1234567890");
    }

    @Test
    void createProduct_success() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("1234567890")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setProductId(1L);
            return product;
        });

        ProductResponseDTO result = productService.createProduct(requestDTO);

        assertEquals("SKU-001", result.getSku());
        assertEquals("Test Product", result.getName());
        verify(productRepository).save(argThat(product ->
                "SKU-001".equals(product.getSku())
                        && "Electronics".equals(product.getCategory())
                        && "Dell".equals(product.getBrand())
                        && "piece".equals(product.getUnitOfMeasure())));
    }

    @Test
    void createProduct_duplicateSku_throwsException() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.createProduct(requestDTO));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_duplicateBarcode_throwsException() {
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("1234567890")).thenReturn(true);

        assertThrows(DuplicateBarcodeException.class,
                () -> productService.createProduct(requestDTO));
    }

    @Test
    void createProduct_invalidPricing_throwsException() {
        requestDTO.setSellingPrice(new BigDecimal("90.00"));
        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(false);

        InvalidProductDataException exception = assertThrows(
                InvalidProductDataException.class,
                () -> productService.createProduct(requestDTO));

        assertEquals("Selling price must be greater than or equal to cost price",
                exception.getMessage());
    }

    @Test
    void getProductById_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        ProductResponseDTO result = productService.getProductById(1L);

        assertEquals(1L, result.getProductId());
        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    @Test
    void getProductBySku_success() {
        when(productRepository.findBySkuIgnoreCase("SKU-001"))
                .thenReturn(Optional.of(mockProduct));

        ProductResponseDTO result = productService.getProductBySku("sku-001");

        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void getProductBySku_notFound_throwsException() {
        when(productRepository.findBySkuIgnoreCase("INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductBySku("INVALID"));
    }

    @Test
    void getProductByBarcode_success() {
        when(productRepository.findByBarcode("1234567890"))
                .thenReturn(Optional.of(mockProduct));
        when(inventoryAvailabilityGateway.getAvailableQuantity(1L))
                .thenReturn(Optional.of(25));

        ProductResponseDTO result = productService.getProductByBarcode("1234567890");

        assertEquals("1234567890", result.getBarcode());
        verify(inventoryAvailabilityGateway).getAvailableQuantity(1L);
    }

    @Test
    void getProductByBarcode_notFound_throwsException() {
        when(productRepository.findByBarcode("000"))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductByBarcode("000"));
    }

    @Test
    void getAllProducts_returnsList() {
        when(productRepository.findAll()).thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getAllProducts();

        assertEquals(1, result.size());
    }

    @Test
    void getActiveProducts_returnsOnlyActive() {
        when(productRepository.findByIsActive(true)).thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getActiveProducts();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void getProductsByCategory_returnsList() {
        when(productRepository.findByCategoryIgnoreCaseOrderByNameAsc("Electronics"))
                .thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getProductsByCategory("Electronics");

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategory());
    }

    @Test
    void getProductsByBrand_returnsList() {
        when(productRepository.findByBrandIgnoreCaseOrderByNameAsc("Dell"))
                .thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getProductsByBrand("Dell");

        assertEquals(1, result.size());
        assertEquals("Dell", result.get(0).getBrand());
    }

    @Test
    void searchProducts_keywordSearch_returnsPagedContent() {
        when(productRepository.searchProducts(eq("laptop"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));

        List<ProductResponseDTO> result = productService.searchProducts("laptop");

        assertEquals(1, result.size());
    }

    @Test
    void searchProducts_filteredSearch_returnsPagedContent() {
        when(productRepository.searchByFilters(
                eq("lap"),
                eq("Electronics"),
                eq("Dell"),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct)));

        List<ProductResponseDTO> result = productService.searchProducts(
                null, "Lap", "Electronics", "Dell", 0, 20);

        assertEquals(1, result.size());
    }

    @Test
    void updateProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.existsByBarcodeAndProductIdNot("1234567890", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponseDTO result = productService.updateProduct(1L, requestDTO);

        assertEquals("SKU-001", result.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_skuChangeRejected() {
        ProductRequestDTO updateRequest = new ProductRequestDTO();
        updateRequest.setSku("NEW-SKU");
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated");
        updateRequest.setCategory("Electronics");
        updateRequest.setBrand("Dell");
        updateRequest.setUnitOfMeasure("piece");
        updateRequest.setCostPrice(new BigDecimal("100.00"));
        updateRequest.setSellingPrice(new BigDecimal("150.00"));
        updateRequest.setReorderLevel(5);
        updateRequest.setMaxStockLevel(100);
        updateRequest.setLeadTimeDays(7);

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        InvalidProductDataException exception = assertThrows(
                InvalidProductDataException.class,
                () -> productService.updateProduct(1L, updateRequest));

        assertEquals("SKU cannot be changed once the product is created", exception.getMessage());
    }

    @Test
    void updateProduct_duplicateBarcode_throwsException() {
        ProductRequestDTO updateRequest = new ProductRequestDTO();
        updateRequest.setSku("SKU-001");
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated");
        updateRequest.setCategory("Electronics");
        updateRequest.setBrand("Dell");
        updateRequest.setUnitOfMeasure("piece");
        updateRequest.setCostPrice(new BigDecimal("100.00"));
        updateRequest.setSellingPrice(new BigDecimal("150.00"));
        updateRequest.setReorderLevel(5);
        updateRequest.setMaxStockLevel(100);
        updateRequest.setLeadTimeDays(7);
        updateRequest.setBarcode("222");

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.existsByBarcodeAndProductIdNot("222", 1L)).thenReturn(true);

        assertThrows(DuplicateBarcodeException.class,
                () -> productService.updateProduct(1L, updateRequest));
    }

    @Test
    void updateProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(99L, requestDTO));
    }

    @Test
    void deactivateProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.deactivateProduct(1L));
        verify(productRepository).save(argThat(product -> !product.getIsActive()));
    }

    @Test
    void deleteProduct_softDeletesWhenUnused() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(inventoryAvailabilityGateway.hasInventoryUsage(1L)).thenReturn(false);
        when(purchaseOrderUsageGateway.hasPurchaseOrderUsage(1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository, never()).deleteById(any());
        verify(productRepository).save(argThat(product -> !product.getIsActive()));
    }

    @Test
    void deleteProduct_rejectsWhenUsedInStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(inventoryAvailabilityGateway.hasInventoryUsage(1L)).thenReturn(true);

        InvalidProductDataException exception = assertThrows(
                InvalidProductDataException.class,
                () -> productService.deleteProduct(1L));

        assertEquals("Product cannot be deleted because it is used in warehouse stock",
                exception.getMessage());
    }

    @Test
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(99L));
    }

    @Test
    void getLowStockProducts_filtersUsingInventoryGateway() {
        Product secondProduct = Product.builder()
                .productId(2L)
                .sku("SKU-002")
                .name("Second Product")
                .category("Electronics")
                .unitOfMeasure("piece")
                .costPrice(new BigDecimal("50.00"))
                .sellingPrice(new BigDecimal("70.00"))
                .reorderLevel(10)
                .maxStockLevel(50)
                .leadTimeDays(5)
                .isActive(true)
                .build();

        when(productRepository.findByIsActiveTrue(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(mockProduct, secondProduct)));
        when(inventoryAvailabilityGateway.getAvailableQuantity(1L)).thenReturn(Optional.of(3));
        when(inventoryAvailabilityGateway.getAvailableQuantity(2L)).thenReturn(Optional.of(20));

        List<ProductResponseDTO> result = productService.getLowStockProducts(0, 20);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProductId());
    }
}
