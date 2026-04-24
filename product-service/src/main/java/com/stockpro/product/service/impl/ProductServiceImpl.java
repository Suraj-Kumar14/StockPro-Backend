package com.stockpro.product.service.impl;

import com.stockpro.product.client.WarehouseClient;
import com.stockpro.product.dto.LowStockProductResponse;
import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.dto.ProductResponse;
import com.stockpro.product.dto.StockLevelResponse;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.BadRequestException;
import com.stockpro.product.exception.DuplicateResourceException;
import com.stockpro.product.exception.ResourceNotFoundException;
import com.stockpro.product.exception.WarehouseServiceException;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.service.ProductService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service implementation for Product Service.
 * The logic is intentionally straightforward and easy to follow.
 */
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final WarehouseClient warehouseClient;

    public ProductServiceImpl(ProductRepository productRepository, WarehouseClient warehouseClient) {
        this.productRepository = productRepository;
        this.warehouseClient = warehouseClient;
    }

    @Override
    public ProductResponse createProduct(ProductRequest productRequest) {
        validateBusinessRules(productRequest);

        String normalizedSku = normalizeSku(productRequest.getSku());
        String normalizedBarcode = normalizeText(productRequest.getBarcode());

        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new DuplicateResourceException("Product SKU already exists: " + normalizedSku);
        }

        if (productRepository.existsByBarcode(normalizedBarcode)) {
            throw new DuplicateResourceException("Product barcode already exists: " + normalizedBarcode);
        }

        Product product = new Product();
        copyRequestToEntity(productRequest, product);
        product.setSku(normalizedSku);
        product.setBarcode(normalizedBarcode);
        product.setIsActive(productRequest.getIsActive() != null ? productRequest.getIsActive() : Boolean.TRUE);

        return mapToResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(Long productId, ProductRequest productRequest) {
        validateBusinessRules(productRequest);

        Product existingProduct = getProductEntity(productId);
        String normalizedSku = normalizeSku(productRequest.getSku());
        String normalizedBarcode = normalizeText(productRequest.getBarcode());

        if (!existingProduct.getSku().equalsIgnoreCase(normalizedSku)
                && productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new DuplicateResourceException("Product SKU already exists: " + normalizedSku);
        }

        if (!existingProduct.getBarcode().equals(normalizedBarcode)
                && productRepository.existsByBarcode(normalizedBarcode)) {
            throw new DuplicateResourceException("Product barcode already exists: " + normalizedBarcode);
        }

        copyRequestToEntity(productRequest, existingProduct);
        existingProduct.setSku(normalizedSku);
        existingProduct.setBarcode(normalizedBarcode);
        existingProduct.setIsActive(
                productRequest.getIsActive() != null ? productRequest.getIsActive() : existingProduct.getIsActive());

        return mapToResponse(productRepository.save(existingProduct));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public ProductResponse getById(Long productId) {
        return mapToResponse(getProductEntity(productId));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public ProductResponse getBySku(String sku) {
        Product product = productRepository.findBySku(normalizeSku(sku))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for SKU: " + sku));
        return mapToResponse(product);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProductResponse> getByCategory(String category) {
        List<ProductResponse> products = productRepository.findByCategory(normalizeText(category)).stream()
                .map(this::mapToResponse)
                .toList();

        if (products.isEmpty()) {
            throw new ResourceNotFoundException("No products found for category: " + category);
        }

        return products;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProductResponse> getByBrand(String brand) {
        List<ProductResponse> products = productRepository.findByBrand(normalizeText(brand)).stream()
                .map(this::mapToResponse)
                .toList();

        if (products.isEmpty()) {
            throw new ResourceNotFoundException("No products found for brand: " + brand);
        }

        return products;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchByName(normalizeText(keyword)).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public ProductResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(normalizeText(barcode))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for barcode: " + barcode));
        return mapToResponse(product);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<LowStockProductResponse> getLowStockProducts() {
        List<Product> activeProducts = productRepository.findByIsActive(Boolean.TRUE).stream()
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (activeProducts.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> quantityByProductId = fetchCurrentQuantities(activeProducts);

        return activeProducts.stream()
                .map(product -> {
                    Integer currentQuantity = quantityByProductId.getOrDefault(product.getProductId(), 0);
                    return LowStockProductResponse.builder()
                            .productId(product.getProductId())
                            .sku(product.getSku())
                            .name(product.getName())
                            .category(product.getCategory())
                            .brand(product.getBrand())
                            .unitOfMeasure(product.getUnitOfMeasure())
                            .reorderLevel(product.getReorderLevel())
                            .currentQuantity(currentQuantity)
                            .isActive(product.getIsActive())
                            .barcode(product.getBarcode())
                            .build();
                })
                .filter(product -> product.getCurrentQuantity() <= product.getReorderLevel())
                .toList();
    }

    @Override
    public void deactivateProduct(Long productId) {
        Product product = getProductEntity(productId);
        product.setIsActive(Boolean.FALSE);
        productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long productId) {
        Product product = getProductEntity(productId);
        productRepository.delete(product);
    }

    private Product getProductEntity(Long productId) {
        return productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private void validateBusinessRules(ProductRequest productRequest) {
        if (productRequest.getReorderLevel() > productRequest.getMaxStockLevel()) {
            throw new BadRequestException("Reorder level cannot be greater than max stock level.");
        }
    }

    private void copyRequestToEntity(ProductRequest productRequest, Product product) {
        product.setName(normalizeText(productRequest.getName()));
        product.setDescription(normalizeOptionalText(productRequest.getDescription()));
        product.setCategory(normalizeText(productRequest.getCategory()));
        product.setBrand(normalizeText(productRequest.getBrand()));
        product.setUnitOfMeasure(normalizeText(productRequest.getUnitOfMeasure()));
        product.setCostPrice(productRequest.getCostPrice());
        product.setSellingPrice(productRequest.getSellingPrice());
        product.setReorderLevel(productRequest.getReorderLevel());
        product.setMaxStockLevel(productRequest.getMaxStockLevel());
        product.setLeadTimeDays(productRequest.getLeadTimeDays());
        product.setImageUrl(normalizeOptionalText(productRequest.getImageUrl()));
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .unitOfMeasure(product.getUnitOfMeasure())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .reorderLevel(product.getReorderLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .leadTimeDays(product.getLeadTimeDays())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .barcode(product.getBarcode())
                .build();
    }

    private Map<Long, Integer> fetchCurrentQuantities(List<Product> activeProducts) {
        try {
            List<Long> productIds = activeProducts.stream()
                    .map(Product::getProductId)
                    .toList();

            return warehouseClient.getStockLevels(productIds).stream()
                    .collect(Collectors.toMap(StockLevelResponse::getProductId,
                            stockLevel -> stockLevel.getCurrentQuantity() == null ? 0 : stockLevel.getCurrentQuantity(),
                            (firstValue, secondValue) -> secondValue));
        } catch (Exception exception) {
            throw new WarehouseServiceException("Unable to fetch live stock levels from warehouse-service.", exception);
        }
    }

    private String normalizeSku(String sku) {
        return normalizeText(sku).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Required text value cannot be blank.");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
