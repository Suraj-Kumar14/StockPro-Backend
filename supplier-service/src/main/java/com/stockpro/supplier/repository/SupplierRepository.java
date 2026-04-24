package com.stockpro.supplier.repository;

import com.stockpro.supplier.entity.Supplier;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierId(Long supplierId);

    List<Supplier> findByCityIgnoreCase(String city);

    List<Supplier> findByCountryIgnoreCase(String country);

    @Query("select s from Supplier s where lower(s.name) like lower(concat('%', :name, '%'))")
    List<Supplier> searchByName(@Param("name") String name);

    List<Supplier> findByIsActive(Boolean isActive);

    Optional<Supplier> findByTaxIdIgnoreCase(String taxId);

    long countByIsActive(Boolean isActive);
}
