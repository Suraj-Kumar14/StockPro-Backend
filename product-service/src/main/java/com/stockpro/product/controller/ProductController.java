package com.stockpro.product.controller;

import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.request.ProductSearchRequest;
import com.stockpro.product.dto.request.UpdateProductRequest;
import com.stockpro.product.dto.response.LowStockProductResponse;
import com.stockpro.product.dto.response.ProductResponse;
import com.stockpro.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({ "/api/v1/products", "/products" })
@Tag(name = "Product Management", description = "Product catalogue CRUD, search, barcode lookup and low-stock metadata APIs.")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private static final String CREATE_PRODUCT_EXAMPLE = """
            {
              "sku": "SKU-1001",
              "name": "Industrial Drill",
              "description": "Heavy-duty industrial drill for warehouse maintenance.",
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
            """;

    private static final String UPDATE_PRODUCT_EXAMPLE = """
            {
              "name": "Industrial Drill Pro",
              "description": "Updated drill model with extended duty cycle.",
              "category": "Tools",
              "brand": "Bosch",
              "unitOfMeasure": "Piece",
              "costPrice": 1550.0000,
              "sellingPrice": 2099.9900,
              "reorderLevel": 12.0000,
              "maxStockLevel": 120.0000,
              "leadTimeDays": 7,
              "imageUrl": "https://cdn.stockpro.com/products/drill-pro.png",
              "barcode": "8901234567891",
              "isActive": true
            }
            """;

    private static final String SEARCH_PRODUCT_EXAMPLE = """
            {
              "name": "drill",
              "category": "Tools",
              "brand": "Bosch",
              "sku": "SKU-1001",
              "barcode": "8901234567890",
              "isActive": true,
              "page": 0,
              "size": 20,
              "sortBy": "name",
              "sortDir": "asc"
            }
            """;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    @Operation(
            summary = "Create product",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "CreateProduct", value = CREATE_PRODUCT_EXAMPLE))))
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get product by id")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ProductResponse> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getBySku(sku));
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<ProductResponse>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getByCategory(category));
    }

    @GetMapping("/brand/{brand}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get products by brand")
    public ResponseEntity<List<ProductResponse>> getByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getByBrand(brand));
    }

    @GetMapping("/barcode/{barcode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get product by barcode")
    public ResponseEntity<ProductResponse> getByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getByBarcode(barcode));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(productService.getAllProducts(page, size, sortBy, sortDir));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Legacy endpoint to get all products as a list")
    public ResponseEntity<List<ProductResponse>> getAllProductsLegacy() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Legacy keyword search by product name")
    public ResponseEntity<List<ProductResponse>> searchProductsByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(
            summary = "Search products with filters",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "SearchProducts", value = SEARCH_PRODUCT_EXAMPLE))))
    public ResponseEntity<Page<ProductResponse>> searchProducts(@Valid @RequestBody ProductSearchRequest request) {
        return ResponseEntity.ok(productService.searchProducts(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    @Operation(
            summary = "Update product",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "UpdateProduct", value = UPDATE_PRODUCT_EXAMPLE))))
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Deactivate product")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deactivateProduct(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER')")
    @Operation(summary = "Safely delete product by performing a soft delete")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'INVENTORY_MANAGER', 'WAREHOUSE_STAFF', 'PURCHASE_OFFICER')")
    @Operation(summary = "Get low-stock product metadata using warehouse quantities and product reorder thresholds")
    public ResponseEntity<List<LowStockProductResponse>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }
}
