package com.stockpro.product_service.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.stockpro.product_service.entity.Product;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Product alphaLaptop;
    private Product betaMonitor;
    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        alphaLaptop = productRepository.save(buildProduct("SKU-001", "Alpha Laptop", "Electronics", "Dell", "BAR-001", true));
        betaMonitor = productRepository.save(buildProduct("SKU-002", "Beta Monitor", "Electronics", "HP", "BAR-002", false));
        productRepository.save(buildProduct("SKU-003", "Chair", "Furniture", "Ikea", null, true));

        Product blankCategory = buildProduct("SKU-004", "Cable", "   ", "Belkin", "BAR-004", true);
        Product blankBrand = buildProduct("SKU-005", "Mouse", "Electronics", "   ", "BAR-005", true);
        productRepository.save(blankCategory);
        productRepository.save(blankBrand);
    }

    @Test
    void initializeNullVersion_shouldSetVersionForLegacyRow() {
        entityManager.createNativeQuery("update products set version = null where product_id = :productId")
                .setParameter("productId", alphaLaptop.getProductId())
                .executeUpdate();
        entityManager.clear();

        int updatedRows = productRepository.initializeNullVersion(alphaLaptop.getProductId());

        assertEquals(1, updatedRows);
        assertEquals(0L, productRepository.findById(alphaLaptop.getProductId()).orElseThrow().getVersion());
    }

    @Test
    void lookupMethods_shouldHandleCaseInsensitiveSkuBarcodeAndExclusionChecks() {
        assertTrue(productRepository.findByProductId(alphaLaptop.getProductId()).isPresent());
        assertTrue(productRepository.findBySkuIgnoreCase("sku-001").isPresent());
        assertTrue(productRepository.findByBarcode("BAR-002").isPresent());

        assertTrue(productRepository.existsBySkuIgnoreCase("SKU-001"));
        assertTrue(productRepository.existsBySkuIgnoreCaseAndProductIdNot("sku-001", betaMonitor.getProductId()));
        assertFalse(productRepository.existsBySkuIgnoreCaseAndProductIdNot("sku-001", alphaLaptop.getProductId()));

        assertTrue(productRepository.existsByBarcode("BAR-001"));
        assertTrue(productRepository.existsByBarcodeAndProductIdNot("BAR-001", betaMonitor.getProductId()));
        assertFalse(productRepository.existsByBarcodeAndProductIdNot("BAR-001", alphaLaptop.getProductId()));
    }

    @Test
    void derivedQueries_shouldReturnSortedCategoryBrandAndActiveResults() {
        List<Product> electronics = productRepository.findByCategoryIgnoreCaseOrderByNameAsc("electronics");
        List<Product> dell = productRepository.findByBrandIgnoreCaseOrderByNameAsc("dell");
        List<Product> activeProducts = productRepository.findByIsActiveTrueOrderByNameAsc();

        assertEquals(List.of("Alpha Laptop", "Beta Monitor", "Mouse"), electronics.stream().map(Product::getName).toList());
        assertEquals(List.of("Alpha Laptop"), dell.stream().map(Product::getName).toList());
        assertEquals(List.of("Alpha Laptop", "Cable", "Chair", "Mouse"), activeProducts.stream().map(Product::getName).toList());
    }

    @Test
    void countAndDistinctQueries_shouldIgnoreNullAndBlankValues() {
        assertEquals(4L, productRepository.countByIsActive(true));
        assertEquals(1L, productRepository.countByIsActive(false));
        assertEquals(2L, productRepository.countDistinctCategories());
        assertEquals(4L, productRepository.countDistinctBrands());
        assertEquals(List.of("Electronics", "Furniture"), productRepository.findDistinctCategories());
        assertEquals(List.of("Belkin", "Dell", "HP", "Ikea"), productRepository.findDistinctBrands());
    }

    @Test
    void specificationSearch_shouldFilterByKeywordBrandCategoryAndStatus() {
        var page = productRepository.findAll(
                ProductSpecifications.search("laptop", "Electronics", "Dell", true),
                PageRequest.of(0, 10, Sort.by("name")));

        assertEquals(1, page.getTotalElements());
        assertEquals("Alpha Laptop", page.getContent().get(0).getName());

        var inactiveResults = productRepository.findAll(
                ProductSpecifications.search("monitor", null, null, false),
                PageRequest.of(0, 10));

        assertEquals(1, inactiveResults.getTotalElements());
        assertEquals("Beta Monitor", inactiveResults.getContent().get(0).getName());
    }

    private Product buildProduct(
            String sku,
            String name,
            String category,
            String brand,
            String barcode,
            boolean active) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .description(name + " description")
                .category(category)
                .brand(brand)
                .unitOfMeasure("Piece")
                .costPrice(BigDecimal.valueOf(50))
                .sellingPrice(BigDecimal.valueOf(75))
                .reorderLevel(2)
                .maxStockLevel(20)
                .leadTimeDays(5)
                .imageUrl("https://example.com/" + sku + ".png")
                .barcode(barcode)
                .isActive(active)
                .createdBy(1L)
                .updatedBy(1L)
                .build();
    }
}
