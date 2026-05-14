package com.stockpro.product_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.dto.response.ProductSummaryResponse;
import com.stockpro.product_service.service.CurrentUserContext;
import com.stockpro.product_service.service.ProductService;

@WebMvcTest(ProductController.class)
@Import(ProductControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ProductService productService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private CurrentUserContext currentUserContext;

    @Test
    void postProducts_returns201_forAdminAndManager() throws Exception {
        ProductResponse response = buildResponse(true);
        when(currentUserContext.getActorId()).thenReturn(10L);
        when(productService.createProduct(any(CreateProductRequest.class), anyLong())).thenReturn(response);

        for (String role : List.of("ADMIN", "MANAGER")) {
            mockMvc.perform(post("/api/v1/products")
                            .with(user(role.toLowerCase()).roles(role))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void postProducts_returns403_forOfficerAndStaff() throws Exception {
        for (String role : List.of("OFFICER", "STAFF")) {
            mockMvc.perform(post("/api/v1/products")
                            .with(user(role.toLowerCase()).roles(role))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCreateRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void getProducts_returns200_forAllAuthenticatedRoles() throws Exception {
        when(productService.getAllProducts(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(buildResponse(true))));

        for (String role : List.of("ADMIN", "MANAGER", "OFFICER", "STAFF")) {
            mockMvc.perform(get("/api/v1/products").with(user(role.toLowerCase()).roles(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void getProductById_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(buildResponse(true));

        mockMvc.perform(get("/api/v1/products/1").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void getProductBySkuAndBarcode_return200() throws Exception {
        when(productService.getProductBySku("SKU-001")).thenReturn(buildResponse(true));
        when(productService.getProductByBarcode("BAR-001")).thenReturn(buildResponse(true));

        mockMvc.perform(get("/api/v1/products/sku/SKU-001").with(user("officer").roles("OFFICER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/barcode/BAR-001").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void searchProductsAndLookupEndpoints_return200() throws Exception {
        when(productService.searchProducts(any(), any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(buildResponse(true))));
        when(productService.getProductsByCategory("Electronics")).thenReturn(List.of(buildResponse(true)));
        when(productService.getProductsByBrand("Dell")).thenReturn(List.of(buildResponse(true)));
        when(productService.getCategories()).thenReturn(List.of("Electronics"));
        when(productService.getBrands()).thenReturn(List.of("Dell"));

        mockMvc.perform(get("/api/v1/products/search").with(user("officer").roles("OFFICER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/category/Electronics").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/brand/Dell").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/categories").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/brands").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void summaryActivateAndDeleteEndpoints_returnExpectedStatuses() throws Exception {
        when(productService.getProductSummary()).thenReturn(ProductSummaryResponse.builder()
                .totalProducts(10)
                .activeProducts(8)
                .inactiveProducts(2)
                .categoriesCount(3)
                .brandsCount(4)
                .build());
        when(currentUserContext.getActorId()).thenReturn(10L);
        when(productService.activateProduct(1L, 10L)).thenReturn(buildResponse(true));

        mockMvc.perform(get("/api/v1/products/summary").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/products/1/activate").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/products/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void putProductsById_returns200_forAdminAndManager() throws Exception {
        when(currentUserContext.getActorId()).thenReturn(10L);
        when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class), anyLong()))
                .thenReturn(buildResponse(true));

        for (String role : List.of("ADMIN", "MANAGER")) {
            mockMvc.perform(put("/api/v1/products/1")
                            .with(user(role.toLowerCase()).roles(role))
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void patchDeactivate_returns200() throws Exception {
        when(currentUserContext.getActorId()).thenReturn(10L);
        when(productService.deactivateProduct(1L, 10L)).thenReturn(buildResponse(false));

        mockMvc.perform(patch("/api/v1/products/1/deactivate").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void invalidRequest_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSkuFormat_returns400WithUpdatedMessage() throws Exception {
        CreateProductRequest request = buildCreateRequest();
        request.setSku("sku-001");

        mockMvc.perform(post("/api/v1/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.sku")
                        .value("SKU must be valid format like SKU-001 or CAN-INK-001"));
    }

    @Test
    void duplicateSku_returns409() throws Exception {
        when(currentUserContext.getActorId()).thenReturn(10L);
        when(productService.createProduct(any(CreateProductRequest.class), anyLong()))
                .thenThrow(new DuplicateSkuException("Duplicate SKU number"));

        mockMvc.perform(post("/api/v1/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void productNotFound_returns404() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/v1/products/99").with(user("staff").roles("STAFF")))
                .andExpect(status().isNotFound());
    }

    private CreateProductRequest buildCreateRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("CAN-INK-001");
        request.setName("Laptop");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(750.00));
        request.setSellingPrice(BigDecimal.valueOf(999.00));
        request.setReorderLevel(5);
        request.setMaxStockLevel(25);
        request.setLeadTimeDays(7);
        request.setBarcode("BAR-001");
        return request;
    }

    private UpdateProductRequest buildUpdateRequest() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Laptop Pro");
        request.setCategory("Electronics");
        request.setBrand("Dell");
        request.setUnitOfMeasure("Piece");
        request.setCostPrice(BigDecimal.valueOf(800.00));
        request.setSellingPrice(BigDecimal.valueOf(1099.00));
        request.setReorderLevel(8);
        request.setMaxStockLevel(30);
        request.setLeadTimeDays(10);
        request.setBarcode("BAR-002");
        request.setIsActive(true);
        return request;
    }

    private ProductResponse buildResponse(boolean active) {
        return ProductResponse.builder()
                .productId(1L)
                .sku("CAN-INK-001")
                .name("Laptop")
                .description("Warehouse laptop")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("Piece")
                .costPrice(BigDecimal.valueOf(750.00))
                .sellingPrice(BigDecimal.valueOf(999.00))
                .reorderLevel(5)
                .maxStockLevel(25)
                .leadTimeDays(7)
                .barcode("BAR-001")
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
