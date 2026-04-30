package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierResponseDTO toResponse(Supplier supplier) {
        return SupplierResponseDTO.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .country(supplier.getCountry())
                .taxId(supplier.getTaxId())
                .paymentTerms(supplier.getPaymentTerms())
                .leadTimeDays(supplier.getLeadTimeDays())
                .rating(supplier.getRating())
                .totalOrders(supplier.getTotalOrders())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
