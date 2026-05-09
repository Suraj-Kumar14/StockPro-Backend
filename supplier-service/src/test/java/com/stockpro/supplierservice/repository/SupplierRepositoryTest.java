package com.stockpro.supplierservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.entity.SupplierStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SupplierRepositoryTest {

    @Autowired
    private SupplierRepository supplierRepository;

    private Supplier activeSupplier;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
        activeSupplier = supplierRepository.save(supplier("SUP-001", "Acme Supplies", "acme@example.com", "TAX-1", "Pune", "India", SupplierStatus.ACTIVE, true, 4.8));
        supplierRepository.save(supplier("SUP-002", "Beta Metals", "beta@example.com", "TAX-2", "Mumbai", "India", SupplierStatus.INACTIVE, false, 3.9));
        supplierRepository.save(supplier("SUP-003", "Gamma Global", "gamma@example.com", "TAX-3", "Delhi", "UAE", SupplierStatus.ACTIVE, true, 4.5));
    }

    @Test
    void shouldSupportCaseInsensitiveLookupsAndExistenceChecks() {
        assertTrue(supplierRepository.findBySupplierId(activeSupplier.getSupplierId()).isPresent());
        assertTrue(supplierRepository.findBySupplierCode("SUP-001").isPresent());
        assertTrue(supplierRepository.findByEmailIgnoreCase("ACME@example.com").isPresent());
        assertTrue(supplierRepository.findByTaxIdIgnoreCase("tax-1").isPresent());
        assertTrue(supplierRepository.existsByEmailIgnoreCase("ACME@example.com"));
        assertTrue(supplierRepository.existsByTaxIdIgnoreCase("tax-1"));
        assertFalse(supplierRepository.existsBySupplierCode("SUP-999"));
    }

    @Test
    void shouldReturnFilteredPagesAndSearchResults() {
        assertEquals(2, supplierRepository.findByIsActive(true, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, supplierRepository.findByStatus(SupplierStatus.INACTIVE, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, supplierRepository.findByCityContainingIgnoreCase("mum", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(3, supplierRepository.searchSuppliers("a").size());
        assertEquals(1, supplierRepository.searchByName("beta").size());
        assertEquals(1, supplierRepository.searchSuppliers("acme", "pune", "india", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void shouldReturnCountsAndTopRatedSuppliers() {
        assertEquals(2L, supplierRepository.countByStatus(SupplierStatus.ACTIVE));
        assertEquals(2L, supplierRepository.countByIsActive(true));
        List<Supplier> topRated = supplierRepository.findTopRatedSuppliers(4.0);
        assertEquals(List.of("Acme Supplies", "Gamma Global"), topRated.stream().map(Supplier::getName).toList());
    }

    private Supplier supplier(String code, String name, String email, String taxId, String city, String country, SupplierStatus status, boolean active, double rating) {
        return Supplier.builder()
                .supplierCode(code)
                .name(name)
                .email(email)
                .taxId(taxId)
                .city(city)
                .country(country)
                .paymentTerms("NET-30")
                .leadTimeDays(5)
                .status(status)
                .isActive(active)
                .rating(BigDecimal.valueOf(rating))
                .build();
    }
}
