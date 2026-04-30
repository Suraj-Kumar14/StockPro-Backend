package com.stockpro.product_service.service;

import com.stockpro.product_service.dto.ProductRequestDTO;
import com.stockpro.product_service.dto.ProductResponseDTO;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private static final int DEFAULT_SEARCH_PAGE = 0;
    private static final int DEFAULT_SEARCH_SIZE = 50;
    private static final int MAX_SEARCH_SIZE = 200;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidationService productValidationService;
    private final InventoryAvailabilityGateway inventoryAvailabilityGateway;
    private final PurchaseOrderUsageGateway purchaseOrderUsageGateway;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        ProductValidationService.SanitizedProductInput input =
                productValidationService.sanitize(dto);
        log.info("Creating product with SKU: {}", input.sku());

        productValidationService.validateForCreate(input);

        Product product = toNewEntity(input);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {}", savedProduct.getProductId());
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        return productMapper.toResponse(findProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductBySku(String sku) {
        String normalizedSku = normalizeSku(sku);
        log.info("Fetching product with SKU: {}", normalizedSku);
        Product product = productRepository.findBySkuIgnoreCase(normalizedSku)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with SKU: " + normalizedSku));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductByBarcode(String barcode) {
        String normalizedBarcode = trimToNull(barcode);
        log.info("Fetching product with barcode: {}", normalizedBarcode);
        Product product = productRepository.findByBarcode(normalizedBarcode)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with barcode: " + normalizedBarcode));

        inventoryAvailabilityGateway.getAvailableQuantity(product.getProductId())
                .ifPresent(availableQuantity -> log.debug(
                        "Barcode lookup stock hint for product {}: availableQuantity={}",
                        product.getProductId(), availableQuantity));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getActiveProducts() {
        log.info("Fetching active products");
        return productRepository.findByIsActive(true)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategory(String category) {
        String normalizedCategory = trimToNull(category);
        log.info("Fetching products by category: {}", normalizedCategory);
        return productRepository.findByCategoryIgnoreCaseOrderByNameAsc(normalizedCategory)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByBrand(String brand) {
        String normalizedBrand = trimToNull(brand);
        log.info("Fetching products by brand: {}", normalizedBrand);
        return productRepository.findByBrandIgnoreCaseOrderByNameAsc(normalizedBrand)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(String keyword) {
        return searchProducts(keyword, null, null, null,
                DEFAULT_SEARCH_PAGE, DEFAULT_SEARCH_SIZE);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(
            String keyword, String name, String category, String brand, int page, int size) {
        ProductSearchCriteria criteria = new ProductSearchCriteria(
                trimToNull(keyword),
                trimToNull(name),
                trimToNull(category),
                trimToNull(brand),
                Math.max(page, 0),
                sanitizePageSize(size)
        );
        log.info("Searching products with criteria: keyword={}, name={}, category={}, brand={}",
                criteria.keyword(), criteria.name(), criteria.category(), criteria.brand());

        Pageable pageable = PageRequest.of(criteria.page(), criteria.size());

        if (criteria.name() != null || criteria.category() != null || criteria.brand() != null) {
            return productRepository.searchByFilters(
                            normalizeNamePrefix(criteria.name()),
                            criteria.category(),
                            criteria.brand(),
                            pageable)
                    .stream()
                    .map(productMapper::toResponse)
                    .toList();
        }

        String effectiveKeyword = Optional.ofNullable(criteria.keyword()).orElse("");
        return productRepository.searchProducts(effectiveKeyword, pageable)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        log.info("Updating product with ID: {}", id);

        Product product = findProduct(id);
        ProductValidationService.SanitizedProductInput input =
                productValidationService.sanitize(dto);
        ProductValidationService.SanitizedProductInput mergedInput =
                productValidationService.mergeWithExisting(product, input);
        productValidationService.validateForUpdate(product, input);

        applyUpdatableFields(product, mergedInput);
        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully with ID: {}", id);
        return productMapper.toResponse(updatedProduct);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        log.info("Deactivating product with ID: {}", id);

        Product product = findProduct(id);
        product.setIsActive(false);
        productRepository.save(product);

        log.info("Product deactivated successfully with ID: {}", id);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with ID: {}", id);

        Product product = findProduct(id);
        validateDeletionAllowed(product);
        product.setIsActive(false);
        productRepository.save(product);

        log.info("Product soft deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getLowStockProducts(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), sanitizePageSize(size));
        return productRepository.findByIsActiveTrue(pageable)
                .stream()
                .filter(product -> inventoryAvailabilityGateway.getAvailableQuantity(
                                product.getProductId())
                        .map(availableQuantity -> availableQuantity < product.getReorderLevel())
                        .orElse(false))
                .map(productMapper::toResponse)
                .toList();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
    }

    private Product toNewEntity(ProductValidationService.SanitizedProductInput input) {
        return Product.builder()
                .sku(input.sku())
                .name(input.name())
                .description(input.description())
                .category(input.category())
                .brand(input.brand())
                .unitOfMeasure(input.unitOfMeasure())
                .costPrice(input.costPrice())
                .sellingPrice(input.sellingPrice())
                .reorderLevel(input.reorderLevel())
                .maxStockLevel(input.maxStockLevel())
                .leadTimeDays(input.leadTimeDays())
                .imageUrl(input.imageUrl())
                .barcode(input.barcode())
                .isActive(true)
                .build();
    }

    private void applyUpdatableFields(
            Product product, ProductValidationService.SanitizedProductInput input) {
        // Keep SKU immutable; remaining fields are merged safely for future partial-update use.
        if (input.name() != null) {
            product.setName(input.name());
        }
        product.setDescription(input.description());
        if (input.category() != null) {
            product.setCategory(input.category());
        }
        product.setBrand(input.brand());
        if (input.unitOfMeasure() != null) {
            product.setUnitOfMeasure(input.unitOfMeasure());
        }
        if (input.costPrice() != null) {
            product.setCostPrice(input.costPrice());
        }
        if (input.sellingPrice() != null) {
            product.setSellingPrice(input.sellingPrice());
        }
        if (input.reorderLevel() != null) {
            product.setReorderLevel(input.reorderLevel());
        }
        if (input.maxStockLevel() != null) {
            product.setMaxStockLevel(input.maxStockLevel());
        }
        if (input.leadTimeDays() != null) {
            product.setLeadTimeDays(input.leadTimeDays());
        }
        product.setImageUrl(input.imageUrl());
        product.setBarcode(input.barcode());
    }

    private void validateDeletionAllowed(Product product) {
        if (inventoryAvailabilityGateway.hasInventoryUsage(product.getProductId())) {
            throw new InvalidProductDataException(
                    "Product cannot be deleted because it is used in warehouse stock");
        }
        if (purchaseOrderUsageGateway.hasPurchaseOrderUsage(product.getProductId())) {
            throw new InvalidProductDataException(
                    "Product cannot be deleted because it is referenced by purchase orders");
        }
    }

    private int sanitizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_SEARCH_SIZE;
        }
        return Math.min(size, MAX_SEARCH_SIZE);
    }

    private String normalizeSku(String sku) {
        String trimmedSku = trimToNull(sku);
        return trimmedSku == null ? null : trimmedSku.toUpperCase();
    }

    private String normalizeNamePrefix(String name) {
        String trimmedName = trimToNull(name);
        return trimmedName == null ? null : trimmedName.toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
