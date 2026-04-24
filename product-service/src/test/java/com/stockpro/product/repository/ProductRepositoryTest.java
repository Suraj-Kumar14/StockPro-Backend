package com.stockpro.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpro.product.dto.request.ProductSearchRequest;
import com.stockpro.product.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findBySkuIgnoreCaseAndCountByCategoryShouldReturnExpectedResults() {
        productRepository.save(product("SKU-1001", "Industrial Drill", "Tools", "Bosch", "8901234567890", Boolean.TRUE));
        productRepository.save(product("SKU-1002", "Angle Grinder", "Tools", "Makita", "8901234567891", Boolean.FALSE));

        assertThat(productRepository.findBySkuIgnoreCase("sku-1001")).isPresent();
        assertThat(productRepository.findByBarcode("8901234567890")).isPresent();
        assertThat(productRepository.findByCategoryIgnoreCaseOrderByNameAsc("tools")).hasSize(2);
        assertThat(productRepository.countByCategory("Tools")).isEqualTo(2);
    }

    @Test
    void specificationsShouldFilterByNameBrandAndIsActive() {
        productRepository.save(product("SKU-1001", "Industrial Drill", "Tools", "Bosch", "8901234567890", Boolean.TRUE));
        productRepository.save(product("SKU-1002", "Industrial Saw", "Tools", "Bosch", "8901234567891", Boolean.FALSE));
        productRepository.save(product("SKU-1003", "Office Chair", "Furniture", "Ikea", "8901234567892", Boolean.TRUE));

        ProductSearchRequest request = ProductSearchRequest.builder()
                .name("drill")
                .brand("bosch")
                .isActive(Boolean.TRUE)
                .build();

        assertThat(productRepository.findAll(ProductSpecifications.withFilters(request)))
                .extracting(Product::getSku)
                .containsExactly("SKU-1001");
    }

    private Product product(String sku, String name, String category, String brand, String barcode, Boolean isActive) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .description(name + " description")
                .category(category)
                .brand(brand)
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("100.0000"))
                .sellingPrice(new BigDecimal("150.0000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .barcode(barcode)
                .isActive(isActive)
                .build();
    }
}
