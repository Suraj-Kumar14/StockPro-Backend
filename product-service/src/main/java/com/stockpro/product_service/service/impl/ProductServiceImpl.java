package com.stockpro.product_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.dto.response.ProductResponse;
import com.stockpro.product_service.dto.response.ProductSummaryResponse;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.DuplicateBarcodeException;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.repository.ProductRepository;
import com.stockpro.product_service.repository.ProductSpecifications;
import com.stockpro.product_service.service.CurrentUserContext;
import com.stockpro.product_service.service.InventoryAvailabilityGateway;
import com.stockpro.product_service.service.ProductAuditEntry;
import com.stockpro.product_service.service.ProductAuditService;
import com.stockpro.product_service.service.ProductEventPublisher;
import com.stockpro.product_service.service.ProductLifecycleEvent;
import com.stockpro.product_service.service.ProductMapper;
import com.stockpro.product_service.service.ProductService;
import com.stockpro.product_service.service.ProductValidationService;
import com.stockpro.product_service.service.PurchaseOrderUsageGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "productId",
            "sku",
            "name",
            "category",
            "brand",
            "costPrice",
            "sellingPrice",
            "reorderLevel",
            "maxStockLevel",
            "leadTimeDays",
            "isActive",
            "createdAt",
            "updatedAt");

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ProductValidationService productValidationService;
    private final InventoryAvailabilityGateway inventoryAvailabilityGateway;
    private final PurchaseOrderUsageGateway purchaseOrderUsageGateway;
    private final ProductAuditService productAuditService;
    private final ProductEventPublisher productEventPublisher;
    private final CurrentUserContext currentUserContext;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, Long actorId) {
        log.info("Product creation started for sku={}", request.getSku());
        CreateProductRequest sanitizedRequest = productValidationService.sanitize(request);
        productValidationService.validateBusinessRules(
                sanitizedRequest.getCostPrice(),
                sanitizedRequest.getSellingPrice(),
                sanitizedRequest.getReorderLevel(),
                sanitizedRequest.getMaxStockLevel(),
                sanitizedRequest.getLeadTimeDays());
        validateSkuAndBarcodeUniqueness(sanitizedRequest.getSku(), sanitizedRequest.getBarcode(), null);

        Product product = Product.builder()
                .sku(sanitizedRequest.getSku())
                .name(sanitizedRequest.getName())
                .description(sanitizedRequest.getDescription())
                .category(sanitizedRequest.getCategory())
                .brand(sanitizedRequest.getBrand())
                .unitOfMeasure(sanitizedRequest.getUnitOfMeasure())
                .costPrice(sanitizedRequest.getCostPrice())
                .sellingPrice(sanitizedRequest.getSellingPrice())
                .reorderLevel(sanitizedRequest.getReorderLevel())
                .maxStockLevel(sanitizedRequest.getMaxStockLevel())
                .leadTimeDays(sanitizedRequest.getLeadTimeDays())
                .imageUrl(sanitizedRequest.getImageUrl())
                .barcode(sanitizedRequest.getBarcode())
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with productId={}", savedProduct.getProductId());
        audit(actorId, "PRODUCT_CREATED", savedProduct.getProductId(), null, snapshot(savedProduct));
        productEventPublisher.publishProductCreated(buildEvent(
                savedProduct,
                actorId,
                "PRODUCT_CREATED",
                null,
                snapshot(savedProduct)));
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        return productMapper.toResponse(findProduct(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        String normalizedSku = productValidationService.normalizeSku(sku);
        Product product = productRepository.findBySkuIgnoreCase(normalizedSku)
                .orElseThrow(() -> {
                    log.warn("Product not found for sku={}", normalizedSku);
                    return new ProductNotFoundException("Product not found with SKU: " + normalizedSku);
                });
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByBarcode(String barcode) {
        String normalizedBarcode = productValidationService.trimToNull(barcode);
        Product product = productRepository.findByBarcode(normalizedBarcode)
                .orElseThrow(() -> {
                    log.warn("Product not found for barcode={}", normalizedBarcode);
                    return new ProductNotFoundException("Product not found with barcode: " + normalizedBarcode);
                });
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            String keyword,
            String category,
            String brand,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return productRepository.findAll(
                        ProductSpecifications.search(keyword, category, brand, isActive),
                        pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request, Long actorId) {
        log.info("[PRODUCT UPDATE SERVICE START] id={}", productId);
        int initializedVersions = productRepository.initializeNullVersion(productId);
        if (initializedVersions > 0) {
            log.info("Initialized missing product version before update. productId={}", productId);
        }
        Product product = findProduct(productId);
        log.info("[PRODUCT UPDATE EXISTING] id={}, sku={}, barcode={}",
                product.getProductId(), product.getSku(), product.getBarcode());
        String oldSnapshot = snapshot(product);

        UpdateProductRequest sanitizedRequest = productValidationService.sanitize(request);
        boolean reorderChanged = hasReorderConfigChanged(product, sanitizedRequest);
        productValidationService.validateBusinessRules(
                sanitizedRequest.getCostPrice(),
                sanitizedRequest.getSellingPrice(),
                sanitizedRequest.getReorderLevel(),
                sanitizedRequest.getMaxStockLevel(),
                sanitizedRequest.getLeadTimeDays());
        validateSkuAndBarcodeUniqueness(product.getSku(), sanitizedRequest.getBarcode(), productId);

        product.setName(sanitizedRequest.getName());
        product.setDescription(sanitizedRequest.getDescription());
        product.setCategory(sanitizedRequest.getCategory());
        product.setBrand(sanitizedRequest.getBrand());
        product.setUnitOfMeasure(sanitizedRequest.getUnitOfMeasure());
        product.setCostPrice(sanitizedRequest.getCostPrice());
        product.setSellingPrice(sanitizedRequest.getSellingPrice());
        product.setReorderLevel(sanitizedRequest.getReorderLevel());
        product.setMaxStockLevel(sanitizedRequest.getMaxStockLevel());
        product.setLeadTimeDays(sanitizedRequest.getLeadTimeDays());
        product.setImageUrl(sanitizedRequest.getImageUrl());
        product.setBarcode(sanitizedRequest.getBarcode());
        if (sanitizedRequest.getIsActive() != null) {
            product.setIsActive(sanitizedRequest.getIsActive());
        }
        product.setUpdatedBy(actorId);

        log.info("[PRODUCT UPDATE BEFORE SAVE] id={}, sku={}, name={}",
                product.getProductId(), product.getSku(), product.getName());
        Product updatedProduct = productRepository.save(product);
        log.info("[PRODUCT UPDATE SAVED] id={}", updatedProduct.getProductId());
        audit(actorId, "PRODUCT_UPDATED", productId, oldSnapshot, snapshot(updatedProduct));
        productEventPublisher.publishProductUpdated(buildEvent(
                updatedProduct,
                actorId,
                "PRODUCT_UPDATED",
                oldSnapshot,
                snapshot(updatedProduct)));
        if (reorderChanged) {
            productEventPublisher.publishProductReorderConfigChanged(
                    buildEvent(
                            updatedProduct,
                            actorId,
                            "PRODUCT_REORDER_CONFIG_CHANGED",
                            oldSnapshot,
                            snapshot(updatedProduct)));
        }
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse deactivateProduct(Long productId, Long actorId) {
        Product product = findProduct(productId);
        String oldSnapshot = snapshot(product);
        product.setIsActive(false);
        product.setUpdatedBy(actorId);
        Product updatedProduct = productRepository.save(product);
        log.info("Product deactivated for productId={}", productId);
        audit(actorId, "PRODUCT_DEACTIVATED", productId, oldSnapshot, snapshot(updatedProduct));
        productEventPublisher.publishProductDeactivated(buildEvent(
                updatedProduct,
                actorId,
                "PRODUCT_DEACTIVATED",
                oldSnapshot,
                snapshot(updatedProduct)));
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse activateProduct(Long productId, Long actorId) {
        Product product = findProduct(productId);
        String oldSnapshot = snapshot(product);
        product.setIsActive(true);
        product.setUpdatedBy(actorId);
        Product updatedProduct = productRepository.save(product);
        log.info("Product activated for productId={}", productId);
        audit(actorId, "PRODUCT_ACTIVATED", productId, oldSnapshot, snapshot(updatedProduct));
        productEventPublisher.publishProductActivated(buildEvent(
                updatedProduct,
                actorId,
                "PRODUCT_ACTIVATED",
                oldSnapshot,
                snapshot(updatedProduct)));
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);
        String oldSnapshot = snapshot(product);
        Long actorId = currentUserContext.getActorId();
        product.setIsActive(false);
        product.setUpdatedBy(actorId);
        productRepository.save(product);
        log.info("Product soft deleted for productId={}", productId);
        audit(actorId, "PRODUCT_DELETED", productId, oldSnapshot, null);
        productEventPublisher.publishProductDeleted(buildEvent(
                product,
                actorId,
                "PRODUCT_DELETED",
                oldSnapshot,
                null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        String normalizedCategory = productValidationService.trimToNull(category);
        return productRepository.findByCategoryIgnoreCaseOrderByNameAsc(normalizedCategory)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByBrand(String brand) {
        String normalizedBrand = productValidationService.trimToNull(brand);
        return productRepository.findByBrandIgnoreCaseOrderByNameAsc(normalizedBrand)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getBrands() {
        return productRepository.findDistinctBrands();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSummaryResponse getProductSummary() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByIsActive(true);
        long inactiveProducts = productRepository.countByIsActive(false);
        return ProductSummaryResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(inactiveProducts)
                .categoriesCount(productRepository.countDistinctCategories())
                .brandsCount(productRepository.countDistinctBrands())
                .build();
    }

    private Product findProduct(Long productId) {
        return productRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found for productId={}", productId);
                    return new ProductNotFoundException("Product not found with id: " + productId);
                });
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        String sortField = sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = size <= 0 ? 10 : Math.min(size, 100);
        return PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(direction, sortField));
    }

    private void validateSkuAndBarcodeUniqueness(String sku, String barcode, Long productId) {
        if (productId == null && productRepository.existsBySkuIgnoreCase(sku)) {
            log.warn("Duplicate SKU detected: {}", sku);
            throw new DuplicateSkuException("Duplicate SKU number");
        }
        if (productId != null && productRepository.existsBySkuIgnoreCaseAndProductIdNot(sku, productId)) {
            log.warn("Duplicate SKU detected during update: {}", sku);
            throw new DuplicateSkuException("Duplicate SKU number");
        }
        if (barcode != null && productId == null && productRepository.existsByBarcode(barcode)) {
            log.warn("Duplicate barcode detected: {}", barcode);
            throw new DuplicateBarcodeException("Duplicate barcode");
        }
        if (barcode != null && productId != null && productRepository.existsByBarcodeAndProductIdNot(barcode, productId)) {
            log.warn("Duplicate barcode detected during update: {}", barcode);
            throw new DuplicateBarcodeException("Duplicate barcode");
        }
    }

    private boolean hasReorderConfigChanged(Product product, UpdateProductRequest request) {
        return !product.getReorderLevel().equals(request.getReorderLevel())
                || !product.getMaxStockLevel().equals(request.getMaxStockLevel())
                || !product.getLeadTimeDays().equals(request.getLeadTimeDays());
    }

    private void audit(Long actorId, String action, Long entityId, String oldValue, String newValue) {
        productAuditService.record(ProductAuditEntry.builder()
                .actorId(actorId)
                .action(action)
                .entityType("PRODUCT")
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .timestamp(LocalDateTime.now())
                .serviceName("product-service")
                .build());
    }

    private ProductLifecycleEvent buildEvent(
            Product product,
            Long actorId,
            String eventType,
            String oldValue,
            String newValue) {
        return ProductLifecycleEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .productId(product.getProductId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .brand(product.getBrand())
                .barcode(product.getBarcode())
                .reorderLevel(product.getReorderLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .leadTimeDays(product.getLeadTimeDays())
                .isActive(product.getIsActive())
                .actorId(actorId)
                .eventTime(LocalDateTime.now())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }

    private String snapshot(Product product) {
        return "Product{productId=%d, sku='%s', name='%s', category='%s', brand='%s', isActive=%s, reorderLevel=%d, maxStockLevel=%d}"
                .formatted(
                        product.getProductId(),
                        product.getSku(),
                        product.getName(),
                        product.getCategory(),
                        product.getBrand(),
                        product.getIsActive(),
                        product.getReorderLevel(),
                        product.getMaxStockLevel());
    }
}
