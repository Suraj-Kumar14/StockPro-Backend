package com.stockpro.product.controller;

import com.stockpro.product.dto.LowStockProductResponse;
import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.dto.ProductResponse;
import com.stockpro.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@Tag(name = "Product Management", description = "REST endpoints for the StockPro Product/Item-Service.")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Create product")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(productRequest));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get product by id")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getById(productId));
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getBySku(sku));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<ProductResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getByCategory(category));
    }

    @GetMapping("/brand/{brand}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get products by brand")
    public ResponseEntity<List<ProductResponse>> getByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getByBrand(brand));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get product by barcode")
    public ResponseEntity<ProductResponse> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getByBarcode(barcode));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Search products by product name")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get all products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get products with current quantity at or below reorder level")
    public ResponseEntity<List<LowStockProductResponse>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Update product")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long productId,
                                                         @Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(productService.updateProduct(productId, productRequest));
    }

    @PutMapping("/{productId}/deactivate")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Deactivate product")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long productId) {
        productService.deactivateProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Delete product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}