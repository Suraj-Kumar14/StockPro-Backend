package com.stockpro.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.product.config.JwtAuthenticationFilter;
import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.response.ProductResponse;
import com.stockpro.product.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createProductShouldReturnCreatedResponse() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-1001")
                .name("Industrial Drill")
                .description("Heavy-duty industrial drill")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("1499.5000"))
                .sellingPrice(new BigDecimal("1999.9000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .barcode("8901234567890")
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(productResponse(1L, "SKU-1001"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.sku").value("SKU-1001"));
    }

    @Test
    void getAllProductsShouldReturnPagedContent() throws Exception {
        when(productService.getAllProducts(0, 20, "productId", "asc")).thenReturn(new PageImpl<>(List.of(
                productResponse(1L, "SKU-1001"))));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-1001"));
    }

    private ProductResponse productResponse(Long productId, String sku) {
        return ProductResponse.builder()
                .productId(productId)
                .sku(sku)
                .name("Industrial Drill")
                .description("Heavy-duty industrial drill")
                .category("Tools")
                .brand("Bosch")
                .unitOfMeasure("Piece")
                .costPrice(new BigDecimal("1499.5000"))
                .sellingPrice(new BigDecimal("1999.9000"))
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .leadTimeDays(5)
                .imageUrl("https://cdn.stockpro.com/products/drill.png")
                .isActive(Boolean.TRUE)
                .barcode("8901234567890")
                .createdAt(LocalDateTime.of(2026, 4, 24, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 24, 10, 0))
                .build();
    }
}
