package com.stockpro.product_service.repository;

import com.stockpro.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByCategoryIgnoreCaseOrderByNameAsc(String category);

    List<Product> findByBrandIgnoreCaseOrderByNameAsc(String brand);

    List<Product> findByIsActive(Boolean isActive);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndProductIdNot(String barcode, Long productId);

    Long countByCategory(String category);

    @Query("""
            SELECT p
            FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.name ASC
            """)
    Page<Product> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT p
            FROM Product p
            WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT(:name, '%')))
              AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
              AND (:brand IS NULL OR LOWER(p.brand) = LOWER(:brand))
            ORDER BY p.name ASC
            """)
    Page<Product> searchByFilters(
            @Param("name") String name,
            @Param("category") String category,
            @Param("brand") String brand,
            Pageable pageable);

    @Query("""
            SELECT p
            FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.name ASC
            """)
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}
