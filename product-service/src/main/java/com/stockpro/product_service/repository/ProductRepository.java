package com.stockpro.product_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.stockpro.product_service.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductId(Long productId);

    Optional<Product> findBySkuIgnoreCase(String sku);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndProductIdNot(String sku, Long productId);

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndProductIdNot(String barcode, Long productId);

    Page<Product> findByIsActive(Boolean isActive, Pageable pageable);

    List<Product> findByCategoryIgnoreCaseOrderByNameAsc(String category);

    List<Product> findByBrandIgnoreCaseOrderByNameAsc(String brand);

    List<Product> findByIsActiveTrueOrderByNameAsc();

    long countByIsActive(Boolean isActive);

    @Query("""
            select count(distinct p.category)
            from Product p
            where p.category is not null and trim(p.category) <> ''
            """)
    long countDistinctCategories();

    @Query("""
            select count(distinct lower(p.brand))
            from Product p
            where p.brand is not null and trim(p.brand) <> ''
            """)
    long countDistinctBrands();

    @Query("""
            select distinct p.category
            from Product p
            where p.category is not null and trim(p.category) <> ''
            order by p.category asc
            """)
    List<String> findDistinctCategories();

    @Query("""
            select distinct p.brand
            from Product p
            where p.brand is not null and trim(p.brand) <> ''
            order by p.brand asc
            """)
    List<String> findDistinctBrands();
}
