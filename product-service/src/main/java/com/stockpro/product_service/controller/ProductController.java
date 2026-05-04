package com.stockpro.product_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.dto.response.ProductResponse;
import com.stockpro.product_service.dto.response.ProductSummaryResponse;
import com.stockpro.product_service.service.CurrentUserContext;
import com.stockpro.product_service.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/products")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product management APIs for the StockPro inventory catalog")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private static final String AUTHENTICATED_ROLES = "hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')";
    private static final String MANAGE_ROLES = "hasAnyRole('ADMIN','MANAGER')";

    private final ProductService productService;
    private final CurrentUserContext currentUserContext;

    @PostMapping
    @PreAuthorize(MANAGE_ROLES)
    @Operation(summary = "Create product", description = "Allowed roles: ADMIN, MANAGER")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU or barcode", content = @Content)
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Long actorId = currentUserContext.getActorId();
        ProductResponse response = productService.createProduct(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get paginated products", description = "Allowed roles: ADMIN, MANAGER, OFFICER, STAFF")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Search products", description = "Search by keyword, category, brand, and active status")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.searchProducts(
                keyword, category, brand, isActive, page, size, sortBy, sortDir));
    }

    @GetMapping("/summary")
    @PreAuthorize(MANAGE_ROLES)
    @Operation(summary = "Get product summary", description = "Allowed roles: ADMIN, MANAGER")
    public ResponseEntity<ProductSummaryResponse> getSummary() {
        return ResponseEntity.ok(productService.getProductSummary());
    }

    @GetMapping("/categories")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get categories", description = "Returns distinct product categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(productService.getCategories());
    }

    @GetMapping("/brands")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get brands", description = "Returns distinct product brands")
    public ResponseEntity<List<String>> getBrands() {
        return ResponseEntity.ok(productService.getBrands());
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ProductResponse> getProductBySku(
            @Parameter(description = "Unique product SKU", required = true)
            @PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get product by barcode", description = "Barcode lookup endpoint for scanner workflows")
    public ResponseEntity<ProductResponse> getProductByBarcode(
            @Parameter(description = "Product barcode", required = true)
            @PathVariable String barcode) {
        return ResponseEntity.ok(productService.getProductByBarcode(barcode));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    @GetMapping("/brand/{brand}")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get products by brand")
    public ResponseEntity<List<ProductResponse>> getProductsByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getProductsByBrand(brand));
    }

    @GetMapping("/{productId:\\d+}")
    @PreAuthorize(AUTHENTICATED_ROLES)
    @Operation(summary = "Get product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PutMapping("/{productId:\\d+}")
    @PreAuthorize(MANAGE_ROLES)
    @Operation(summary = "Update product", description = "Allowed roles: ADMIN, MANAGER")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("[PRODUCT UPDATE CONTROLLER] id={}, request={}", productId, request);
        Long actorId = currentUserContext.getActorId();
        return ResponseEntity.ok(productService.updateProduct(productId, request, actorId));
    }

    @PatchMapping("/{productId:\\d+}/deactivate")
    @PreAuthorize(MANAGE_ROLES)
    @Operation(summary = "Deactivate product", description = "Soft-deactivates the product")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long productId) {
        Long actorId = currentUserContext.getActorId();
        return ResponseEntity.ok(productService.deactivateProduct(productId, actorId));
    }

    @PatchMapping("/{productId:\\d+}/activate")
    @PreAuthorize(MANAGE_ROLES)
    @Operation(summary = "Activate product", description = "Reactivates the product")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long productId) {
        Long actorId = currentUserContext.getActorId();
        return ResponseEntity.ok(productService.activateProduct(productId, actorId));
    }

    @DeleteMapping("/{productId:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Hard-deletes only if the product is already inactive and safe to remove")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
