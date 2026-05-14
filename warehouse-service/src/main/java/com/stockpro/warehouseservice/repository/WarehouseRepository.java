package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long>, JpaSpecificationExecutor<Warehouse> {

    Optional<Warehouse> findByWarehouseId(Long warehouseId);

    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);

    List<Warehouse> findByIsActive(Boolean isActive);

    Page<Warehouse> findByIsActive(Boolean isActive, Pageable pageable);

    List<Warehouse> findByManagerId(Long managerId);

    Optional<Warehouse> findByName(String name);

    boolean existsByName(String name);

    long countByIsActive(Boolean isActive);
}
