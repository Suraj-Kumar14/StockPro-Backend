package com.stockpro.supplierservice;

import com.stockpro.supplierservice.dto.request.BlacklistSupplierRequest;
import com.stockpro.supplierservice.dto.request.CreateSupplierRequest;
import com.stockpro.supplierservice.dto.request.DeactivateSupplierRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRatingRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRequest;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.entity.SupplierStatus;
import com.stockpro.supplierservice.exception.DuplicateSupplierException;
import com.stockpro.supplierservice.exception.InvalidSupplierDataException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import com.stockpro.supplierservice.service.SupplierEventPublisher;
import com.stockpro.supplierservice.service.SupplierManagementService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierManagementServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierEventPublisher supplierEventPublisher;

    private SupplierManagementService service;
    private Supplier supplier;

    @BeforeEach
    void setUp() throws Exception {
        service = new SupplierManagementService(supplierRepository, supplierEventPublisher);
        setField("createdRouting", "supplier.created");
        setField("updatedRouting", "supplier.updated");
        setField("activatedRouting", "supplier.activated");
        setField("deactivatedRouting", "supplier.deactivated");
        setField("blacklistedRouting", "supplier.blacklisted");
        setField("ratingUpdatedRouting", "supplier.rating-updated");
        setField("performanceUpdatedRouting", "supplier.performance-updated");

        supplier = Supplier.builder()
                .supplierId(1L)
                .supplierCode("SUP-20260501-0001")
                .name("Acme Supply")
                .email("acme@example.com")
                .city("Pune")
                .country("India")
                .paymentTerms("NET-30")
                .leadTimeDays(7)
                .rating(BigDecimal.valueOf(4.5))
                .status(SupplierStatus.ACTIVE)
                .isActive(true)
                .totalOrders(3)
                .build();
    }

    @Test
    void createSupplier_shouldCreateSupplier_whenValidRequest() {
        when(supplierRepository.findBySupplierCode(anyString())).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("acme@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.findByTaxIdIgnoreCase("TAX-1")).thenReturn(Optional.empty());
        when(supplierRepository.existsBySupplierCode(any())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        var result = service.createSupplier(new CreateSupplierRequest(
                null, "Acme Supply", "Raj", "acme@example.com", "+919999999999", null,
                "Line 1", "Pune", "MH", "India", "411001", "TAX-1", "GST-1", "NET-30", 7,
                BigDecimal.valueOf(4.5), "Preferred"), 99L);

        assertEquals("SUP-20260501-0001", result.supplierCode());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_shouldNormalizeValuesAndDefaultFields() {
        when(supplierRepository.findBySupplierCode("SUP-CUSTOM")).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("mixed@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.findByTaxIdIgnoreCase("TAX-9")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(23L);
            return saved;
        });

        var result = service.createSupplier(new CreateSupplierRequest(
                " sup-custom ", "  Acme Supply  ", " Raj ", "Mixed@Example.com", "+919999999999", " ",
                " Addr ", " Pune ", " MH ", " India ", " 411001 ", " TAX-9 ", " GST-9 ", " NET-30 ", null,
                null, " "), 11L);

        assertEquals("SUP-CUSTOM", result.supplierCode());
        assertEquals("mixed@example.com", result.email());
        assertEquals(BigDecimal.ZERO, result.rating());
        assertNull(result.alternatePhone());
        assertNull(result.notes());
    }

    @Test
    void createSupplier_shouldThrowConflict_whenSupplierCodeAlreadyExists() {
        when(supplierRepository.findBySupplierCode("SUP-1")).thenReturn(Optional.of(supplier));

        assertThrows(DuplicateSupplierException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "acme@example.com", null, null, null, "Pune", null, "India", null,
                null, null, "NET-30", 5, BigDecimal.ONE, null), 1L));
    }

    @Test
    void createSupplier_shouldThrowConflict_whenEmailAlreadyExists() {
        when(supplierRepository.findBySupplierCode("SUP-1")).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("acme@example.com")).thenReturn(Optional.of(supplier));

        assertThrows(DuplicateSupplierException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "acme@example.com", null, null, null, "Pune", null, "India", null,
                null, null, "NET-30", 5, BigDecimal.ONE, null), 1L));
    }

    @Test
    void createSupplier_shouldThrowConflict_whenTaxNumberAlreadyExists() {
        when(supplierRepository.findBySupplierCode("SUP-1")).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("acme@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.findByTaxIdIgnoreCase("TAX-1")).thenReturn(Optional.of(supplier));

        assertThrows(DuplicateSupplierException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "acme@example.com", null, null, null, "Pune", null, "India", null,
                "TAX-1", null, "NET-30", 5, BigDecimal.ONE, null), 1L));
    }

    @Test
    void createSupplier_shouldGenerateSupplierCode_whenMissing() {
        when(supplierRepository.existsBySupplierCode(any())).thenReturn(false);
        when(supplierRepository.findByEmailIgnoreCase("auto@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(21L);
            return saved;
        });

        var result = service.createSupplier(new CreateSupplierRequest(
                null, "Auto Code Supply", null, "auto@example.com", null, null, null, "Delhi", null, "India", null,
                null, null, "NET-30", 3, BigDecimal.valueOf(4.2), null), 5L);

        assertTrue(result.supplierCode().startsWith("SUP-"));
    }

    @Test
    void createSupplier_shouldRetryGeneratedCode_whenFirstExists() {
        when(supplierRepository.existsBySupplierCode(any())).thenReturn(true, false);
        when(supplierRepository.findByEmailIgnoreCase("auto@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier saved = invocation.getArgument(0);
            saved.setSupplierId(22L);
            return saved;
        });

        var result = service.createSupplier(new CreateSupplierRequest(
                null, "Auto Code Supply", null, "auto@example.com", null, null, null, "Delhi", null, "India", null,
                null, null, "NET-30", 3, BigDecimal.valueOf(4.2), null), 5L);

        assertTrue(result.supplierCode().matches("SUP-\\d{8}-\\d{4}"));
    }

    @Test
    void createSupplier_shouldThrowBadRequest_whenRatingInvalid() {
        assertThrows(InvalidSupplierDataException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "acme@example.com", null, null, null, "Pune", null, "India", null,
                null, null, "NET-30", 5, BigDecimal.valueOf(6), null), 1L));
    }

    @Test
    void createSupplier_shouldThrowBadRequest_whenLeadTimeNegative() {
        assertThrows(InvalidSupplierDataException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "acme@example.com", null, null, null, "Pune", null, "India", null,
                null, null, "NET-30", -1, BigDecimal.ONE, null), 1L));
    }

    @Test
    void createSupplier_shouldThrowBadRequest_whenPhoneInvalid() {
        assertThrows(InvalidSupplierDataException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "acme@example.com", "bad", null, null, "Pune", null, "India", null,
                null, null, "NET-30", 5, BigDecimal.ONE, null), 1L));
    }

    @Test
    void createSupplier_shouldThrowBadRequest_whenEmailInvalid() {
        assertThrows(InvalidSupplierDataException.class, () -> service.createSupplier(new CreateSupplierRequest(
                "SUP-1", "Acme Supply", null, "bad-email", null, null, null, "Pune", null, "India", null,
                null, null, "NET-30", 5, BigDecimal.ONE, null), 1L));
    }

    @Test
    void updateSupplier_shouldUpdate_whenValidRequest() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findBySupplierCode("SUP-20260501-0001")).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.findByTaxIdIgnoreCase("TAX-1")).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSupplier(1L, new UpdateSupplierRequest(
                "Updated Supply", "Raj", "updated@example.com", "+919999999999", null, "Addr",
                "Pune", "MH", "India", "411001", "TAX-1", null, "NET-45", 10, BigDecimal.valueOf(4.8),
                SupplierStatus.ACTIVE, true, "Updated"), 88L);

        assertEquals("Updated Supply", result.name());
        assertEquals("updated@example.com", result.email());
    }

    @Test
    void updateSupplier_shouldSetInactiveStatus_whenFlagIsFalse() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findBySupplierCode("SUP-20260501-0001")).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSupplier(1L, new UpdateSupplierRequest(
                "Updated Supply", "Raj", "updated@example.com", "+919999999999", null, "Addr",
                "Pune", "MH", "India", "411001", null, null, "NET-45", 10, BigDecimal.valueOf(4.8),
                SupplierStatus.ACTIVE, false, "Updated"), 88L);

        assertFalse(result.isActive());
        assertEquals(SupplierStatus.INACTIVE, result.status());
    }

    @Test
    void updateSupplier_shouldRestoreActiveStatus_whenActiveAndNotBlacklisted() {
        supplier.setStatus(SupplierStatus.PENDING_REVIEW);
        supplier.setIsActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findBySupplierCode("SUP-20260501-0001")).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSupplier(1L, new UpdateSupplierRequest(
                "Updated Supply", "Raj", "updated@example.com", "+919999999999", null, "Addr",
                "Pune", "MH", "India", "411001", null, null, "NET-45", 10, BigDecimal.valueOf(4.8),
                SupplierStatus.PENDING_REVIEW, true, "Updated"), 88L);

        assertTrue(result.isActive());
        assertEquals(SupplierStatus.ACTIVE, result.status());
    }

    @Test
    void updateSupplier_shouldKeepBlacklistedStatus_whenActiveFlagTrue() {
        supplier.setStatus(SupplierStatus.BLACKLISTED);
        supplier.setIsActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findBySupplierCode("SUP-20260501-0001")).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSupplier(1L, new UpdateSupplierRequest(
                "Updated Supply", "Raj", "updated@example.com", "+919999999999", null, "Addr",
                "Pune", "MH", "India", "411001", null, null, "NET-45", 10, BigDecimal.valueOf(4.8),
                SupplierStatus.BLACKLISTED, true, "Updated"), 88L);

        assertTrue(result.isActive());
        assertEquals(SupplierStatus.BLACKLISTED, result.status());
    }

    @Test
    void updateSupplier_shouldRejectDuplicateEmailFromAnotherSupplier() {
        Supplier other = Supplier.builder().supplierId(2L).supplierCode("SUP-2").email("dup@example.com").name("Other").paymentTerms("NET-30").leadTimeDays(1).status(SupplierStatus.ACTIVE).isActive(true).build();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.findBySupplierCode("SUP-20260501-0001")).thenReturn(Optional.of(supplier));
        when(supplierRepository.findByEmailIgnoreCase("dup@example.com")).thenReturn(Optional.of(other));

        assertThrows(DuplicateSupplierException.class, () -> service.updateSupplier(1L, new UpdateSupplierRequest(
                "Updated Supply", "Raj", "dup@example.com", null, null, null,
                "Pune", null, "India", null, null, null, "NET-45", 10, BigDecimal.valueOf(4.8),
                SupplierStatus.ACTIVE, true, null), 88L));
    }

    @Test
    void getSupplierById_shouldReturnSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        assertEquals(1L, service.getSupplierById(1L).supplierId());
    }

    @Test
    void getSupplierById_shouldThrowNotFound() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(SupplierNotFoundException.class, () -> service.getSupplierById(999L));
    }

    @Test
    void getSupplierByCode_shouldReturnSupplier() {
        when(supplierRepository.findBySupplierCode("SUP-20260501-0001")).thenReturn(Optional.of(supplier));
        assertEquals("SUP-20260501-0001", service.getSupplierByCode("SUP-20260501-0001").supplierCode());
    }

    @Test
    void getSupplierByEmail_shouldNormalizeLookup() {
        when(supplierRepository.findByEmailIgnoreCase("acme@example.com")).thenReturn(Optional.of(supplier));
        assertEquals(1L, service.getSupplierByEmail(" Acme@Example.com ").supplierId());
    }

    @Test
    void getAllSuppliers_shouldUseDefaultPagingAndSort() {
        when(supplierRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(supplier)));

        var result = service.getAllSuppliers(true, SupplierStatus.ACTIVE, -1, 0, " ", "desc");

        assertEquals(1, result.getContent().size());
    }

    @Test
    void activateSupplier_shouldSetActiveStatus() {
        supplier.setIsActive(false);
        supplier.setStatus(SupplierStatus.INACTIVE);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.activateSupplier(1L, 7L);

        assertTrue(result.isActive());
        assertEquals(SupplierStatus.ACTIVE, result.status());
    }

    @Test
    void activateSupplier_shouldReturnExisting_whenAlreadyActive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        var result = service.activateSupplier(1L, 7L);

        assertTrue(result.isActive());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void deactivateSupplier_shouldSetInactiveStatus() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.deactivateSupplier(1L, new DeactivateSupplierRequest("No longer preferred"), 7L);

        assertFalse(result.isActive());
        assertEquals(SupplierStatus.INACTIVE, result.status());
    }

    @Test
    void deactivateSupplier_shouldReturnExisting_whenAlreadyInactive() {
        supplier.setIsActive(false);
        supplier.setStatus(SupplierStatus.INACTIVE);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        var result = service.deactivateSupplier(1L, new DeactivateSupplierRequest("Still inactive"), 7L);

        assertFalse(result.isActive());
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void deactivateSupplier_shouldRequireReason() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(InvalidSupplierDataException.class, () ->
                service.deactivateSupplier(1L, new DeactivateSupplierRequest(" "), 7L));
    }

    @Test
    void blacklistSupplier_shouldSetBlacklistedStatus() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.blacklistSupplier(1L, new BlacklistSupplierRequest("Fraud risk"), 7L);

        assertEquals(SupplierStatus.BLACKLISTED, result.status());
        assertFalse(result.isActive());
    }

    @Test
    void blacklistSupplier_shouldRequireReason() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(InvalidSupplierDataException.class, () ->
                service.blacklistSupplier(1L, new BlacklistSupplierRequest(" "), 7L));
    }

    @Test
    void updateSupplierRating_shouldUpdateRating() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSupplierRating(1L, new UpdateSupplierRatingRequest(BigDecimal.valueOf(4.9), null, null, null, null, "Improved"), 8L);

        assertEquals(0, BigDecimal.valueOf(4.9).compareTo(result.rating()));
    }

    @Test
    void updateSupplierRating_shouldDefaultToZeroAndAppendRemarks() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSupplierRating(1L, new UpdateSupplierRatingRequest(null, null, null, null, null, "Improved"), 8L);

        assertEquals(BigDecimal.ZERO, result.rating());
        assertTrue(result.notes().contains("Rating: Improved"));
    }

    @Test
    void validateSupplierForPurchase_shouldReturnTrueForActiveSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        assertTrue(service.validateSupplierForPurchase(1L).canUseForPurchase());
    }

    @Test
    void validateSupplierForPurchase_shouldReturnFalseForInactiveSupplier() {
        supplier.setIsActive(false);
        supplier.setStatus(SupplierStatus.INACTIVE);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        var result = service.validateSupplierForPurchase(1L);
        assertFalse(result.canUseForPurchase());
        assertEquals("Supplier is inactive", result.reason());
    }

    @Test
    void validateSupplierForPurchase_shouldReturnFalseForBlacklistedSupplier() {
        supplier.setStatus(SupplierStatus.BLACKLISTED);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        var result = service.validateSupplierForPurchase(1L);
        assertFalse(result.canUseForPurchase());
        assertEquals("Supplier is blacklisted", result.reason());
    }

    @Test
    void getSupplierSummary_shouldReturnCorrectCounts() {
        Supplier inactive = Supplier.builder().supplierId(2L).supplierCode("SUP-2").name("B").paymentTerms("NET-30").leadTimeDays(4).status(SupplierStatus.INACTIVE).isActive(false).rating(BigDecimal.valueOf(3.5)).build();
        when(supplierRepository.findAll()).thenReturn(List.of(supplier, inactive));
        when(supplierRepository.countByIsActive(true)).thenReturn(1L);
        when(supplierRepository.countByStatus(SupplierStatus.INACTIVE)).thenReturn(1L);
        when(supplierRepository.countByStatus(SupplierStatus.BLACKLISTED)).thenReturn(0L);
        when(supplierRepository.countByStatus(SupplierStatus.PENDING_REVIEW)).thenReturn(0L);

        var result = service.getSupplierSummary();

        assertEquals(2L, result.totalSuppliers());
        assertEquals(1L, result.activeSuppliers());
        assertEquals(0, BigDecimal.valueOf(5.50).compareTo(result.averageLeadTimeDays()));
    }

    @Test
    void getSupplierSummary_shouldHandleEmptyCollection() {
        when(supplierRepository.findAll()).thenReturn(List.of());
        when(supplierRepository.countByIsActive(true)).thenReturn(0L);
        when(supplierRepository.countByStatus(SupplierStatus.INACTIVE)).thenReturn(0L);
        when(supplierRepository.countByStatus(SupplierStatus.BLACKLISTED)).thenReturn(0L);
        when(supplierRepository.countByStatus(SupplierStatus.PENDING_REVIEW)).thenReturn(0L);

        var result = service.getSupplierSummary();

        assertEquals(BigDecimal.ZERO, result.averageRating());
        assertEquals(BigDecimal.ZERO, result.averageLeadTimeDays());
    }

    @Test
    void getSupplierPerformance_shouldDefaultMissingMetricsToZero() {
        supplier.setRating(null);
        supplier.setTotalOrders(null);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        var result = service.getSupplierPerformance(1L);

        assertEquals(0, result.totalOrders());
        assertEquals(BigDecimal.ZERO, result.overallRating());
    }

    @Test
    void getActiveSuppliers_shouldFilterBlacklistedAndSortByName() {
        Supplier zeta = Supplier.builder().supplierId(2L).supplierCode("SUP-2").name("Zeta").paymentTerms("NET-30").leadTimeDays(2).status(SupplierStatus.ACTIVE).isActive(true).build();
        Supplier alpha = Supplier.builder().supplierId(3L).supplierCode("SUP-3").name("Alpha").paymentTerms("NET-30").leadTimeDays(2).status(SupplierStatus.ACTIVE).isActive(true).build();
        Supplier blacklisted = Supplier.builder().supplierId(4L).supplierCode("SUP-4").name("Blocked").paymentTerms("NET-30").leadTimeDays(2).status(SupplierStatus.BLACKLISTED).isActive(true).build();
        when(supplierRepository.findByIsActive(true)).thenReturn(List.of(zeta, blacklisted, alpha));

        var result = service.getActiveSuppliers();

        assertEquals(2, result.size());
        assertEquals("Alpha", result.get(0).name());
    }

    @Test
    void getTopRatedSuppliers_shouldReturnOnlyActiveTopTen() {
        List<Supplier> suppliers = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> Supplier.builder()
                        .supplierId((long) index)
                        .supplierCode("SUP-" + index)
                        .name("Supplier " + index)
                        .paymentTerms("NET-30")
                        .leadTimeDays(2)
                        .rating(BigDecimal.valueOf(5.0 - (index * 0.1)))
                        .status(SupplierStatus.ACTIVE)
                        .isActive(index != 11)
                        .build())
                .toList();
        when(supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "rating"))).thenReturn(suppliers);

        var result = service.getTopRatedSuppliers();

        assertEquals(10, result.size());
        assertTrue(result.stream().allMatch(SupplierResponse -> Boolean.TRUE.equals(SupplierResponse.isActive())));
    }

    @Test
    void searchSuppliers_shouldReturnFilteredPagedResult() {
        when(supplierRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(supplier)));

        var result = service.searchSuppliers("acme", SupplierStatus.ACTIVE, true, "Pune", "India",
                BigDecimal.valueOf(4), 10, 0, 10, "name", "asc");

        assertEquals(1, result.getContent().size());
    }

    @Test
    void deleteSupplier_shouldDeleteOrRejectSafely() {
        supplier.setTotalOrders(2);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        assertThrows(DuplicateSupplierException.class, () -> service.deleteSupplier(1L));
        verify(supplierRepository, never()).delete(any(Supplier.class));
    }

    @Test
    void deleteSupplier_shouldDelete_whenNoOrdersExist() {
        supplier.setTotalOrders(0);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        service.deleteSupplier(1L);

        verify(supplierRepository).delete(supplier);
    }

    private void setField(String fieldName, String value) throws Exception {
        Field field = SupplierManagementService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }
}
