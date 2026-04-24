package com.stockpro.product.repository;

import com.stockpro.product.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    Optional<Product> findByProductId(Long productId);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByCategoryIgnoreCaseOrderByNameAsc(String category);

    List<Product> findByBrandIgnoreCaseOrderByNameAsc(String brand);

    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Product> findByIsActive(Boolean isActive);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcode(String barcode);

    long countByCategory(String category);
}
