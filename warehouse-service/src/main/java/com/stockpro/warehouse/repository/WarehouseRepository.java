package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.Warehouse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByManagerId(Long managerId);

    List<Warehouse> findByIsActive(Boolean isActive);

    List<Warehouse> findByLocation(String location);

    long countByIsActive(Boolean isActive);

    @Query("select w.warehouseId from Warehouse w where w.isActive = :isActive")
    List<Long> findWarehouseIdsByIsActive(@Param("isActive") Boolean isActive);
}
