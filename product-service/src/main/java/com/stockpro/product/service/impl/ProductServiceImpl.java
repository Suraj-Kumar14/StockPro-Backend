package com.stockpro.product.service.impl;

import com.stockpro.product.client.WarehouseClient;
import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.request.ProductSearchRequest;
import com.stockpro.product.dto.request.UpdateProductRequest;
import com.stockpro.product.dto.response.LowStockProductResponse;
import com.stockpro.product.dto.response.ProductResponse;
import com.stockpro.product.dto.response.StockLevelQuantityResponse;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateBarcodeException;
import com.stockpro.product.exception.DuplicateSkuException;
import com.stockpro.product.exception.InvalidProductRequestException;
import com.stockpro.product.exception.ProductNotFoundException;
import com.stockpro.product.exception.WarehouseServiceException;
import com.stockpro.product.mapper.ProductMapper;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.repository.ProductSpecifications;
import com.stockpro.product.service.ProductService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final int SCALE = 4;
    private static final Set<String> SORT_FIELDS = Set.of(
            "productId",
            "sku",
            "name",
            "category",
            "brand",
            "unitOfMeasure",
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
    private final WarehouseClient warehouseClient;

    public ProductServiceImpl(ProductRepository productRepository,
            ProductMapper productMapper,
            WarehouseClient warehouseClient) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.warehouseClient = warehouseClient;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = productMapper.toEntity(request);
        normalizeAndValidateProduct(product);
        validateUniqueSku(product.getSku());
        validateUniqueBarcode(product.getBarcode(), null);

        Product savedProduct = productRepository.save(product);
        LOGGER.info("Created product productId={} sku={}", savedProduct.getProductId(), savedProduct.getSku());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long productId) {
        return productMapper.toResponse(getProductEntity(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getBySku(String sku) {
        Product product = productRepository.findBySkuIgnoreCase(normalizeSku(sku))
                .orElseThrow(() -> new ProductNotFoundException("Product not found for SKU: " + sku));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCaseOrderByNameAsc(normalizeRequiredText(category, "category is required."))
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getByBrand(String brand) {
        return productRepository.findByBrandIgnoreCaseOrderByNameAsc(normalizeRequiredText(brand, "brand is required."))
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest request) {
        Pageable pageable = buildPageable(
                request.getPage() == null ? 0 : request.getPage(),
                request.getSize() == null ? 20 : request.getSize(),
                request.getSortBy(),
                request.getSortDir());

        return productRepository.findAll(ProductSpecifications.withFilters(request), pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
                        normalizeRequiredText(keyword, "keyword is required."))
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request) {
        Product product = getProductEntity(productId);
        productMapper.updateEntity(request, product);
        normalizeAndValidateProduct(product);
        validateUniqueBarcode(product.getBarcode(), product.getProductId());

        Product savedProduct = productRepository.save(product);
        LOGGER.info("Updated product productId={} sku={}", savedProduct.getProductId(), savedProduct.getSku());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse deactivateProduct(Long productId) {
        Product product = getProductEntity(productId);
        product.setIsActive(Boolean.FALSE);

        Product savedProduct = productRepository.save(product);
        LOGGER.info("Deactivated product productId={} sku={}", savedProduct.getProductId(), savedProduct.getSku());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = getProductEntity(productId);
        if (!Boolean.FALSE.equals(product.getIsActive())) {
            product.setIsActive(Boolean.FALSE);
            productRepository.save(product);
        }
        LOGGER.info("Soft deleted product productId={} sku={}", product.getProductId(), product.getSku());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        return productRepository.findAll(buildPageable(page, size, sortBy, sortDir)).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        String normalizedBarcode = normalizeRequiredText(barcode, "Barcode is required.");
        Product product = productRepository.findByBarcode(normalizedBarcode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found for barcode: " + barcode));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockProductResponse> getLowStockProducts() {
        List<Product> activeProducts = productRepository.findByIsActive(Boolean.TRUE).stream()
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (activeProducts.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> quantityByProductId = fetchCurrentQuantities(activeProducts);

        return activeProducts.stream()
                .filter(product -> BigDecimal.valueOf(quantityByProductId.getOrDefault(product.getProductId(), 0))
                        .compareTo(product.getReorderLevel()) <= 0)
                .map(productMapper::toLowStockResponse)
                .toList();
    }

    private Product getProductEntity(Long productId) {
        if (productId == null || productId <= 0) {
            throw new InvalidProductRequestException("productId must be greater than zero.");
        }

        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));
    }

    private void normalizeAndValidateProduct(Product product) {
        product.setSku(normalizeSku(product.getSku()));
        product.setName(normalizeRequiredText(product.getName(), "Product name is required."));
        product.setDescription(normalizeOptionalText(product.getDescription()));
        product.setCategory(normalizeRequiredText(product.getCategory(), "Category is required."));
        product.setBrand(normalizeRequiredText(product.getBrand(), "Brand is required."));
        product.setUnitOfMeasure(normalizeRequiredText(product.getUnitOfMeasure(), "Unit of measure is required."));
        product.setCostPrice(normalizeRequiredDecimal(product.getCostPrice(), "Cost price is required.", "Cost price cannot be negative."));
        product.setSellingPrice(normalizeRequiredDecimal(product.getSellingPrice(), "Selling price is required.", "Selling price cannot be negative."));
        product.setReorderLevel(normalizeRequiredDecimal(product.getReorderLevel(), "Reorder level is required.", "Reorder level cannot be negative."));
        product.setMaxStockLevel(normalizeRequiredDecimal(product.getMaxStockLevel(), "Max stock level is required.", "Max stock level cannot be negative."));
        product.setLeadTimeDays(normalizeRequiredInteger(product.getLeadTimeDays(), "Lead time days is required."));
        product.setImageUrl(normalizeOptionalText(product.getImageUrl()));
        product.setBarcode(normalizeOptionalText(product.getBarcode()));
        if (product.getIsActive() == null) {
            product.setIsActive(Boolean.TRUE);
        }

        if (product.getMaxStockLevel().compareTo(product.getReorderLevel()) < 0) {
            throw new InvalidProductRequestException("maxStockLevel must be greater than or equal to reorderLevel.");
        }
    }

    private void validateUniqueSku(String sku) {
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException("Product SKU already exists: " + sku);
        }
    }

    private void validateUniqueBarcode(String barcode, Long currentProductId) {
        if (!StringUtils.hasText(barcode)) {
            return;
        }

        productRepository.findByBarcode(barcode).ifPresent(existingProduct -> {
            if (currentProductId == null || !existingProduct.getProductId().equals(currentProductId)) {
                throw new DuplicateBarcodeException("Product barcode already exists: " + barcode);
            }
        });
    }

    private Map<Long, Integer> fetchCurrentQuantities(List<Product> activeProducts) {
        try {
            return warehouseClient.getStockLevels(activeProducts.stream()
                            .map(Product::getProductId)
                            .toList())
                    .stream()
                    .collect(Collectors.toMap(
                            StockLevelQuantityResponse::getProductId,
                            stockLevel -> stockLevel.getCurrentQuantity() == null ? 0 : stockLevel.getCurrentQuantity(),
                            (firstValue, secondValue) -> secondValue));
        } catch (Exception exception) {
            throw new WarehouseServiceException("Unable to fetch live stock levels from warehouse-service.", exception);
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0 || size <= 0) {
            throw new InvalidProductRequestException("page must be zero or greater and size must be greater than zero.");
        }
        return PageRequest.of(page, size, buildSort(sortBy, sortDir));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String resolvedSortBy = StringUtils.hasText(sortBy) ? sortBy.trim() : "productId";
        if (!SORT_FIELDS.contains(resolvedSortBy)) {
            throw new InvalidProductRequestException("Unsupported sortBy value: " + resolvedSortBy);
        }

        return Sort.by(parseSortDirection(sortDir), resolvedSortBy);
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if (!StringUtils.hasText(sortDir)) {
            return Sort.Direction.ASC;
        }

        try {
            return Sort.Direction.fromString(sortDir.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidProductRequestException("sortDir must be either asc or desc.");
        }
    }

    private BigDecimal normalizeRequiredDecimal(BigDecimal value, String requiredMessage, String negativeMessage) {
        if (value == null) {
            throw new InvalidProductRequestException(requiredMessage);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductRequestException(negativeMessage);
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private Integer normalizeRequiredInteger(Integer value, String message) {
        if (value == null) {
            throw new InvalidProductRequestException(message);
        }
        if (value < 0) {
            throw new InvalidProductRequestException("Lead time days cannot be negative.");
        }
        return value;
    }

    private String normalizeSku(String sku) {
        return normalizeRequiredText(sku, "SKU is required.").toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidProductRequestException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
