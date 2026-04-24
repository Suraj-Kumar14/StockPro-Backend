package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;
import java.util.List;

public interface SupplierService {

    Supplier createSupplier(Supplier supplier);

    Supplier getById(Long supplierId);

    List<Supplier> getAllSuppliers();

    List<Supplier> searchSuppliers(String name);

    Supplier updateSupplier(Long supplierId, Supplier supplier);

    Supplier deactivateSupplier(Long supplierId);

    List<Supplier> getByCity(String city);

    List<Supplier> getByCountry(String country);

    Supplier updateRating(Long supplierId, Double newRating);
}
