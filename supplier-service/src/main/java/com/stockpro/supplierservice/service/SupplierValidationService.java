package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateSupplierException;
import com.stockpro.supplierservice.exception.InvalidSupplierDataException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupplierValidationService {

    private static final double MIN_RATING = 1.0;
    private static final double MAX_RATING = 5.0;

    private final SupplierRepository supplierRepository;

    public void validateForCreate(SupplierRequestDTO dto) {
        validateRequest(dto);
        ensureUniqueEmail(dto.getEmail(), null);
        ensureUniqueTaxId(dto.getTaxId(), null);
    }

    public void validateForUpdate(Supplier existingSupplier, SupplierRequestDTO dto) {
        validateRequest(dto);
        ensureUniqueEmail(dto.getEmail(), existingSupplier.getSupplierId());
        validateTaxIdUpdate(existingSupplier, dto.getTaxId());
    }

    public void validateRating(Double rating) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new InvalidSupplierDataException("Rating must be between 1 and 5");
        }
    }

    private void validateRequest(SupplierRequestDTO dto) {
        if (dto.getLeadTimeDays() != null && dto.getLeadTimeDays() < 0) {
            throw new InvalidSupplierDataException("Lead time cannot be negative");
        }
        if (dto.getPaymentTerms() == null || dto.getPaymentTerms().trim().isEmpty()) {
            throw new InvalidSupplierDataException("Payment terms are required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new InvalidSupplierDataException("Supplier name is required");
        }
    }

    private void ensureUniqueEmail(String email, Long currentSupplierId) {
        if (email == null) {
            return;
        }

        supplierRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (currentSupplierId == null || !existing.getSupplierId().equals(currentSupplierId)) {
                throw new DuplicateSupplierException(
                        "Supplier with email " + email + " already exists");
            }
        });
    }

    private void ensureUniqueTaxId(String taxId, Long currentSupplierId) {
        if (taxId == null || taxId.trim().isEmpty()) {
            return;
        }

        supplierRepository.findByTaxIdIgnoreCase(taxId).ifPresent(existing -> {
            if (currentSupplierId == null || !existing.getSupplierId().equals(currentSupplierId)) {
                throw new DuplicateSupplierException(
                        "Supplier with Tax ID " + taxId + " already exists");
            }
        });
    }

    private void validateTaxIdUpdate(Supplier existingSupplier, String requestedTaxId) {
        String currentTaxId = normalize(existingSupplier.getTaxId());
        String newTaxId = normalize(requestedTaxId);
        if (currentTaxId != null && newTaxId != null && !currentTaxId.equalsIgnoreCase(newTaxId)) {
            throw new InvalidSupplierDataException("Tax ID cannot be changed once assigned");
        }
        if (currentTaxId == null) {
            ensureUniqueTaxId(newTaxId, existingSupplier.getSupplierId());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
