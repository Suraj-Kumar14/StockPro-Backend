package com.stockpro.product.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stockpro.product.client.WarehouseClient;
import com.stockpro.product.dto.response.StockLevelQuantityResponse;
import com.stockpro.product.entity.Product;
import com.stockpro.product.repository.ProductRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductServiceIntegrationTest {

    private static final String JWT_SECRET =
            "ThisIsASecretKeyForJwtTokenGenerationThatMustBeAtLeast32BytesLong12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private WarehouseClient warehouseClient;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        when(warehouseClient.getStockLevels(anyList())).thenReturn(List.of(
                StockLevelQuantityResponse.builder().productId(1L).currentQuantity(5).build()));
    }

    @Test
    void createProductShouldPersistProduct() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L, "admin@stockpro.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1001",
                                  "name": "Industrial Drill",
                                  "description": "Heavy-duty industrial drill",
                                  "category": "Tools",
                                  "brand": "Bosch",
                                  "unitOfMeasure": "Piece",
                                  "costPrice": 1499.5000,
                                  "sellingPrice": 1999.9000,
                                  "reorderLevel": 10.0000,
                                  "maxStockLevel": 100.0000,
                                  "leadTimeDays": 5,
                                  "imageUrl": "https://cdn.stockpro.com/products/drill.png",
                                  "barcode": "8901234567890"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").isNumber())
                .andExpect(jsonPath("$.sku").value("SKU-1001"))
                .andExpect(jsonPath("$.isActive").value(true));

        assertThat(productRepository.findAll()).hasSize(1);
        assertThat(productRepository.findAll().get(0).getName()).isEqualTo("Industrial Drill");
    }

    @Test
    void createProductShouldRejectDuplicateSku() throws Exception {
        productRepository.save(product("SKU-1001", "Industrial Drill", "8901234567890", Boolean.TRUE));

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L, "admin@stockpro.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1001",
                                  "name": "Industrial Drill Duplicate",
                                  "description": "Duplicate",
                                  "category": "Tools",
                                  "brand": "Bosch",
                                  "unitOfMeasure": "Piece",
                                  "costPrice": 1499.5000,
                                  "sellingPrice": 1999.9000,
                                  "reorderLevel": 10.0000,
                                  "maxStockLevel": 100.0000,
                                  "leadTimeDays": 5,
                                  "barcode": "8901234567899"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Product SKU already exists: SKU-1001"));
    }

    @Test
    void getByBarcodeShouldReturnProduct() throws Exception {
        Product product = productRepository.save(product("SKU-1001", "Industrial Drill", "8901234567890", Boolean.TRUE));

        mockMvc.perform(get("/api/v1/products/barcode/{barcode}", product.getBarcode())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(2L, "staff@stockpro.com", "WAREHOUSE_STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(product.getProductId()))
                .andExpect(jsonPath("$.sku").value("SKU-1001"));
    }

    @Test
    void searchProductsShouldReturnFilteredResults() throws Exception {
        productRepository.save(product("SKU-1001", "Industrial Drill", "8901234567890", Boolean.TRUE));
        productRepository.save(product("SKU-1002", "Office Chair", "8901234567891", Boolean.TRUE));

        mockMvc.perform(post("/api/v1/products/search")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(3L, "purchase@stockpro.com", "PURCHASE_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "drill",
                                  "page": 0,
                                  "size": 20,
                                  "sortBy": "name",
                                  "sortDir": "asc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1001"));
    }

    @Test
    void deactivateProductShouldMarkProductInactive() throws Exception {
        Product product = productRepository.save(product("SKU-1001", "Industrial Drill", "8901234567890", Boolean.TRUE));

        mockMvc.perform(put("/api/v1/products/{id}/deactivate", product.getProductId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(4L, "inventory@stockpro.com", "INVENTORY_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        assertThat(productRepository.findById(product.getProductId()).orElseThrow().getIsActive()).isFalse();
    }

    @Test
    void createProductShouldRejectWhenMaxStockLevelIsLessThanReorderLevel() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L, "admin@stockpro.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1001",
                                  "name": "Industrial Drill",
                                  "description": "Heavy-duty industrial drill",
                                  "category": "Tools",
                                  "brand": "Bosch",
                                  "unitOfMeasure": "Piece",
                                  "costPrice": 1499.5000,
                                  "sellingPrice": 1999.9000,
                                  "reorderLevel": 100.0000,
                                  "maxStockLevel": 10.0000,
                                  "leadTimeDays": 5,
                                  "barcode": "8901234567890"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("maxStockLevel must be greater than or equal to reorderLevel."));
    }

    private Product product(String sku, String name, String barcode, Boolean isActive) {
        return Product.builder()
                .sku(sku)
                .name(name)
                .description(name + " description")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("1499.5000"))
                .sellingPrice(new BigDecimal("1999.9000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .imageUrl("https://cdn.stockpro.com/products/drill.png")
                .barcode(barcode)
                .isActive(isActive)
                .build();
    }

    private String bearerToken(Long userId, String email, String role) {
        return "Bearer " + Jwts.builder()
                .setClaims(Map.of("userId", userId, "role", role))
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
