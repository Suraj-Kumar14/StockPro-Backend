package com.stockpro.product.repository;

import com.stockpro.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository layer for product master data.
 * Custom query methods support SKU lookup, barcode lookup and filtered search.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p
            from Product p
            where lower(p.sku) = lower(:sku)
            """)
    Optional<Product> findBySku(@Param("sku") String sku);

    Optional<Product> findByProductId(Long productId);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    @Query("""
            select p
            from Product p
            where lower(p.category) = lower(:category)
            order by p.name asc
            """)
    List<Product> findByCategory(@Param("category") String category);

    @Query("""
            select p
            from Product p
            where lower(p.brand) = lower(:brand)
            order by p.name asc
            """)
    List<Product> findByBrand(@Param("brand") String brand);

    List<Product> findByIsActive(Boolean isActive);

    long countByCategory(String category);

    @Query("""
            select p
            from Product p
            where lower(p.name) like lower(concat('%', :name, '%'))
            order by p.name asc
            """)
    List<Product> searchByName(@Param("name") String name);
}
