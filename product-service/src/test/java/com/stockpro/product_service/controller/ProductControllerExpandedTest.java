package com.stockpro.product_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.dto.response.ProductResponse;
import com.stockpro.product_service.dto.response.ProductSummaryResponse;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.service.CurrentUserContext;
import com.stockpro.product_service.service.ProductService;

@WebMvcTest(ProductController.class)
@Import(ProductControllerExpandedTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class ProductControllerExpandedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ProductService productService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private CurrentUserContext currentUserContext;

    @Test
    void getProducts_shouldReturnBodyAndForwardPagingParameters() throws Exception {
        when(productService.getAllProducts(2, 5, "sku", "desc"))
                .thenReturn(new PageImpl<>(List.of(buildResponse(1L, "SKU-001", true))));

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "sku")
                        .param("sortDir", "desc")
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$.content[0].isActive").value(true));
    }

    @Test
    void searchProducts_shouldReturnMatchingBody() throws Exception {
        when(productService.searchProducts("lap", "Electronics", "Dell", true, 0, 10, "name", "asc"))
                .thenReturn(new PageImpl<>(List.of(buildResponse(2L, "SKU-002", true))));

        mockMvc.perform(get("/api/v1/products/search")
                        .param("keyword", "lap")
                        .param("category", "Electronics")
                        .param("brand", "Dell")
                        .param("isActive", "true")
                        .with(user("officer").roles("OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(2L))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-002"));
    }

    @Test
    void summaryAndLookupEndpoints_shouldReturnResponseBodies() throws Exception {
        when(productService.getProductSummary()).thenReturn(ProductSummaryResponse.builder()
                .totalProducts(12)
                .activeProducts(9)
                .inactiveProducts(3)
                .categoriesCount(4)
                .brandsCount(5)
                .build());
        when(productService.getCategories()).thenReturn(List.of("Electronics", "Hardware"));
        when(productService.getBrands()).thenReturn(List.of("Dell", "HP"));
        when(productService.getProductBySku("SKU-100")).thenReturn(buildResponse(100L, "SKU-100", true));
        when(productService.getProductByBarcode("BAR-100")).thenReturn(buildResponse(100L, "SKU-100", true));
        when(productService.getProductsByCategory("Electronics")).thenReturn(List.of(buildResponse(10L, "SKU-010", true)));
        when(productService.getProductsByBrand("Dell")).thenReturn(List.of(buildResponse(11L, "SKU-011", false)));

        mockMvc.perform(get("/api/v1/products/summary").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(12))
                .andExpect(jsonPath("$.brandsCount").value(5));

        mockMvc.perform(get("/api/v1/products/categories").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Electronics"));

        mockMvc.perform(get("/api/v1/products/brands").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("HP"));

        mockMvc.perform(get("/api/v1/products/sku/SKU-100").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(100L));

        mockMvc.perform(get("/api/v1/products/barcode/BAR-100").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("BAR-100"));

        mockMvc.perform(get("/api/v1/products/category/Electronics").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"));

        mockMvc.perform(get("/api/v1/products/brand/Dell").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand").value("Dell"));
    }

    @Test
    void createUpdateAndLifecycleEndpoints_shouldUseActorIdAndReturnBodies() throws Exception {
        when(currentUserContext.getActorId()).thenReturn(77L);
        when(productService.createProduct(any(CreateProductRequest.class), eq(77L)))
                .thenReturn(buildResponse(3L, "SKU-003", true));
        when(productService.updateProduct(eq(3L), any(UpdateProductRequest.class), eq(77L)))
                .thenReturn(buildResponse(3L, "SKU-003", true));
        when(productService.activateProduct(3L, 77L)).thenReturn(buildResponse(3L, "SKU-003", true));
        when(productService.deactivateProduct(3L, 77L)).thenReturn(buildResponse(3L, "SKU-003", false));

        mockMvc.perform(post("/api/v1/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(3L));

        mockMvc.perform(put("/api/v1/products/3")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-003"));

        mockMvc.perform(patch("/api/v1/products/3/activate")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(patch("/api/v1/products/3/deactivate")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        verify(currentUserContext, org.mockito.Mockito.times(4)).getActorId();
    }

    @Test
    void updateWithInvalidBody_shouldReturnBadRequestAndSkipServiceCall() throws Exception {
        UpdateProductRequest invalid = new UpdateProductRequest();
        invalid.setName("  ");
        invalid.setCategory("  ");
        invalid.setUnitOfMeasure(" ");
        invalid.setCostPrice(BigDecimal.ONE);
        invalid.setSellingPrice(BigDecimal.TEN);
        invalid.setReorderLevel(0);
        invalid.setMaxStockLevel(10);
        invalid.setLeadTimeDays(2);

        mockMvc.perform(put("/api/v1/products/5")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.unitOfMeasure").exists());

        verify(productService, never()).updateProduct(eq(5L), any(UpdateProductRequest.class), any());
    }

    @Test
    void deleteShouldBeForbiddenForManagerAndAllowedForAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/products/9")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/products/9")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(9L);
    }

    @Test
    void invalidBusinessStateFromService_shouldReturnBadRequest() throws Exception {
        when(productService.getProductById(41L))
                .thenThrow(new InvalidProductDataException("Bad product state"));

        mockMvc.perform(get("/api/v1/products/41")
                        .with(user("staff").roles("STAFF")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bad product state"));
    }

    private CreateProductRequest buildCreateRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("SKU-003");
        request.setName("Keyboard");
        request.setDescription("Mechanical keyboard");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(60));
        request.setSellingPrice(BigDecimal.valueOf(95));
        request.setReorderLevel(4);
        request.setMaxStockLevel(30);
        request.setLeadTimeDays(6);
        request.setBarcode("BAR-003");
        return request;
    }

    private UpdateProductRequest buildUpdateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Keyboard Pro");
        request.setDescription("Mechanical keyboard");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(70));
        request.setSellingPrice(BigDecimal.valueOf(110));
        request.setReorderLevel(5);
        request.setMaxStockLevel(40);
        request.setLeadTimeDays(8);
        request.setBarcode("BAR-003");
        request.setIsActive(true);
        return request;
    }

    private ProductResponse buildResponse(Long productId, String sku, boolean active) {
        return ProductResponse.builder()
                .productId(productId)
                .sku(sku)
                .name("Laptop")
                .description("Product")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("Piece")
                .costPrice(BigDecimal.valueOf(50))
                .sellingPrice(BigDecimal.valueOf(75))
                .reorderLevel(2)
                .maxStockLevel(20)
                .leadTimeDays(3)
                .barcode("BAR-" + productId)
                .isActive(active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}
