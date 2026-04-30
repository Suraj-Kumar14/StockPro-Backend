package com.stockpro.supplierservice;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateSupplierException;
import com.stockpro.supplierservice.exception.InvalidSupplierDataException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import com.stockpro.supplierservice.service.SupplierMapper;
import com.stockpro.supplierservice.service.SupplierService;
import com.stockpro.supplierservice.service.SupplierValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    private SupplierService supplierService;

    private SupplierRequestDTO validSupplierDTO;
    private Supplier validSupplier;

    @BeforeEach
    void setUp() {
        supplierService = new SupplierService(
                supplierRepository,
                new SupplierValidationService(supplierRepository),
                new SupplierMapper());

        validSupplierDTO = new SupplierRequestDTO();
        validSupplierDTO.setName("  TechWorld Suppliers  ");
        validSupplierDTO.setContactPerson("Raj Kumar");
        validSupplierDTO.setEmail("RAJ@TECHWORLD.COM");
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
                .contactPerson("Raj Kumar")
                .email("raj@techworld.com")
                .phone("+919876543210")
                .address("123 MG Road")
                .city("Mumbai")
                .country("India")
                .taxId("GSTIN123456")
                .paymentTerms("NET-30")
                .leadTimeDays(7)
                .isActive(true)
                .rating(4.0)
                .ratingCount(2)
                .totalOrders(10)
                .build();
    }

    @Test
    void createSupplier_NormalizesAndPersistsSupplier() {
        when(supplierRepository.findByEmailIgnoreCase("raj@techworld.com")).thenReturn(Optional.empty());
        when(supplierRepository.findByTaxIdIgnoreCase("GSTIN123456")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        SupplierResponseDTO result = supplierService.createSupplier(validSupplierDTO);

        assertNotNull(result);
        assertEquals("TechWorld Suppliers", result.getName());
        assertEquals("raj@techworld.com", result.getEmail());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_DuplicateEmail_ThrowsException() {
        when(supplierRepository.findByEmailIgnoreCase("raj@techworld.com")).thenReturn(Optional.of(validSupplier));

        assertThrows(DuplicateSupplierException.class,
                () -> supplierService.createSupplier(validSupplierDTO));
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void getSupplierById_NonExistingId_ThrowsException() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class,
                () -> supplierService.getSupplierById(999L));
    }

    @Test
    void updateSupplier_ChangingExistingTaxId_ThrowsException() {
        SupplierRequestDTO updateDto = new SupplierRequestDTO();
        updateDto.setName("TechWorld Suppliers");
        updateDto.setContactPerson("Raj Kumar");
        updateDto.setEmail("raj@techworld.com");
        updateDto.setPhone("+919876543210");
        updateDto.setAddress("123 MG Road");
        updateDto.setCity("Mumbai");
        updateDto.setCountry("India");
        updateDto.setTaxId("NEW-TAX-ID");
        updateDto.setPaymentTerms("NET-60");
        updateDto.setLeadTimeDays(10);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.findByEmailIgnoreCase("raj@techworld.com")).thenReturn(Optional.of(validSupplier));

        assertThrows(InvalidSupplierDataException.class,
                () -> supplierService.updateSupplier(1L, updateDto));
    }

    @Test
    void searchSuppliers_CombinedFilters_UsesPagedSearch() {
        when(supplierRepository.searchSuppliers(eq("Tech"), eq("Mumbai"), eq("India"), any()))
                .thenReturn(new PageImpl<>(List.of(validSupplier)));

        List<SupplierResponseDTO> result =
                supplierService.searchSuppliers(null, "Tech", "Mumbai", "India", 0, 20);

        assertEquals(1, result.size());
        assertEquals(validSupplier.getSupplierId(), result.get(0).getSupplierId());
    }

    @Test
    void updateRating_ValidRating_UpdatesAverageUsingRatingCount() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.updateRating(1L, 5.0);

        assertEquals(13.0 / 3.0, validSupplier.getRating());
        assertEquals(3, validSupplier.getRatingCount());
        verify(supplierRepository).save(validSupplier);
    }

    @Test
    void updateRating_InvalidRating_ThrowsException() {
        assertThrows(InvalidSupplierDataException.class,
                () -> supplierService.updateRating(1L, 6.0));
    }

    @Test
    void deactivateSupplier_ExistingId_SoftDeletesSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        supplierService.deactivateSupplier(1L);

        assertFalse(validSupplier.getIsActive());
        verify(supplierRepository).save(validSupplier);
    }

    @Test
    void deleteSupplier_UsesSoftDeleteBehavior() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        supplierService.deleteSupplier(1L);

        assertFalse(validSupplier.getIsActive());
        verify(supplierRepository).save(validSupplier);
        verify(supplierRepository, never()).deleteById(any());
    }
}
