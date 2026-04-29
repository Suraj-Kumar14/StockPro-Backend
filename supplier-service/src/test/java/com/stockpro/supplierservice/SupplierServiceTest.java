package com.stockpro.supplierservice;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateTaxIdException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import com.stockpro.supplierservice.service.SupplierService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private SupplierRequestDTO validSupplierDTO;
    private Supplier validSupplier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        validSupplierDTO = new SupplierRequestDTO();
        validSupplierDTO.setName("TechWorld Suppliers");
        validSupplierDTO.setContactPerson("Raj Kumar");
        validSupplierDTO.setEmail("raj@techworld.com");
        validSupplierDTO.setPhone("+919876543210");
        validSupplierDTO.setAddress("123 MG Road");
        validSupplierDTO.setCity("Mumbai");
        validSupplierDTO.setCountry("India");
        validSupplierDTO.setTaxId("GSTIN123456");
        validSupplierDTO.setPaymentTerms("NET-30");
        validSupplierDTO.setLeadTimeDays(7);

        validSupplier = Supplier.builder()
                .supplierId(1L)
                .name("TechWorld Suppliers")
                .email("raj@techworld.com")
                .city("Mumbai")
                .isActive(true)
                .rating(4.5)
                .totalOrders(10)
                .build();
    }

    @Test
    void createSupplier_ValidSupplier_ReturnsSupplierResponse() {
        when(supplierRepository.existsByEmail(validSupplierDTO.getEmail())).thenReturn(false);
        when(supplierRepository.existsByTaxId(validSupplierDTO.getTaxId())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        SupplierResponseDTO result = supplierService.createSupplier(validSupplierDTO);

        assertNotNull(result);
        assertEquals(validSupplierDTO.getName(), result.getName());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_DuplicateEmail_ThrowsException() {
        when(supplierRepository.existsByEmail(validSupplierDTO.getEmail())).thenReturn(true);

        assertThrows(DuplicateTaxIdException.class, () -> 
            supplierService.createSupplier(validSupplierDTO)
        );
    }

    @Test
    void getSupplierById_ExistingId_ReturnsSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));

        SupplierResponseDTO result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals(validSupplier.getName(), result.getName());
    }

    @Test
    void getSupplierById_NonExistingId_ThrowsException() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class, () -> 
            supplierService.getSupplierById(999L)
        );
    }

    @Test
    void updateRating_ValidRating_UpdatesAverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        supplierService.updateRating(1L, 5.0);

        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void deactivateSupplier_ExistingId_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        supplierService.deactivateSupplier(1L);

        assertFalse(validSupplier.getIsActive());
        verify(supplierRepository).save(validSupplier);
    }
}