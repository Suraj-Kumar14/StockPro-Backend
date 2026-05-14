package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.dto.request.BlacklistSupplierRequest;
import com.stockpro.supplierservice.dto.request.CreateSupplierRequest;
import com.stockpro.supplierservice.dto.request.DeactivateSupplierRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRatingRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRequest;
import com.stockpro.supplierservice.dto.response.SupplierPerformanceResponse;
import com.stockpro.supplierservice.dto.response.SupplierPurchaseValidationResponse;
import com.stockpro.supplierservice.dto.response.SupplierResponse;
import com.stockpro.supplierservice.dto.response.SupplierSummaryResponse;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.entity.SupplierStatus;
import com.stockpro.supplierservice.enums.SupplierEventType;
import com.stockpro.supplierservice.events.SupplierEvent;
import com.stockpro.supplierservice.exception.DuplicateSupplierException;
import com.stockpro.supplierservice.exception.InvalidSupplierDataException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import com.stockpro.supplierservice.repository.SupplierSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierManagementService {

    private final SupplierRepository supplierRepository;
    private final SupplierEventPublisher supplierEventPublisher;

    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.created}") private String createdRouting;
    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.updated}") private String updatedRouting;
    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.activated}") private String activatedRouting;
    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.deactivated}") private String deactivatedRouting;
    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.blacklisted}") private String blacklistedRouting;
    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.ratingUpdated}") private String ratingUpdatedRouting;
    @org.springframework.beans.factory.annotation.Value("${stockpro.rabbitmq.supplier.routing.performanceUpdated}") private String performanceUpdatedRouting;

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request, Long actorId) {
        validateRequest(request.name(), request.email(), request.phone(), request.alternatePhone(), request.paymentTerms(), request.leadTimeDays(), request.rating());
        String supplierCode = generateSupplierCode();
        ensureUnique(supplierCode, request.name(), request.email(), request.phone(), request.gstNumber(), request.taxNumber(), null);

        Supplier supplier = Supplier.builder()
                .supplierCode(supplierCode)
                .name(request.name().trim())
                .contactPerson(trimToNull(request.contactPerson()))
                .email(normalizeEmail(request.email()))
                .phone(trimToNull(request.phone()))
                .alternatePhone(trimToNull(request.alternatePhone()))
                .address(trimToNull(request.address()))
                .city(trimToNull(request.city()))
                .state(trimToNull(request.state()))
                .country(trimToNull(request.country()))
                .postalCode(trimToNull(request.postalCode()))
                .taxId(trimToNull(request.taxNumber()))
                .gstNumber(trimToNull(request.gstNumber()))
                .paymentTerms(request.paymentTerms().trim())
                .leadTimeDays(request.leadTimeDays() == null ? 0 : request.leadTimeDays())
                .rating(defaultRating(request.rating()))
                .status(SupplierStatus.ACTIVE)
                .isActive(true)
                .notes(trimToNull(request.notes()))
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier create success for supplierCode={} supplierId={}", saved.getSupplierCode(), saved.getSupplierId());
        publish(saved, SupplierEventType.SUPPLIER_CREATED, createdRouting, actorId, null);
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long supplierId, UpdateSupplierRequest request, Long actorId) {
        Supplier supplier = getEntity(supplierId);
        validateRequest(request.name(), request.email(), request.phone(), request.alternatePhone(), request.paymentTerms(), request.leadTimeDays(), request.rating());
        ensureUnique(supplier.getSupplierCode(), request.name(), request.email(), request.phone(), request.gstNumber(), request.taxNumber(), supplierId);

        applySupplierDetails(supplier, request);
        applySupplierStatusUpdate(supplier, request);
        supplier.setNotes(trimToNull(request.notes()));
        supplier.setUpdatedBy(actorId);

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier update success for supplierId={}", saved.getSupplierId());
        publish(saved, SupplierEventType.SUPPLIER_UPDATED, updatedRouting, actorId, null);
        return toResponse(saved);
    }

    private void applySupplierDetails(Supplier supplier, UpdateSupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setContactPerson(trimToNull(request.contactPerson()));
        supplier.setEmail(normalizeEmail(request.email()));
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setAlternatePhone(trimToNull(request.alternatePhone()));
        supplier.setAddress(trimToNull(request.address()));
        supplier.setCity(trimToNull(request.city()));
        supplier.setState(trimToNull(request.state()));
        supplier.setCountry(trimToNull(request.country()));
        supplier.setPostalCode(trimToNull(request.postalCode()));
        supplier.setTaxId(trimToNull(request.taxNumber()));
        supplier.setGstNumber(trimToNull(request.gstNumber()));
        supplier.setPaymentTerms(request.paymentTerms().trim());
        supplier.setLeadTimeDays(request.leadTimeDays());
        supplier.setRating(defaultRating(request.rating()));
    }

    private void applySupplierStatusUpdate(Supplier supplier, UpdateSupplierRequest request) {
        applyRequestedStatus(supplier, request.status());
        applyRequestedActiveState(supplier, request.isActive());
    }

    private void applyRequestedStatus(Supplier supplier, SupplierStatus requestedStatus) {
        if (requestedStatus == null) {
            return;
        }
        supplier.setStatus(requestedStatus);
        supplier.setIsActive(requestedStatus == SupplierStatus.ACTIVE);
    }

    private void applyRequestedActiveState(Supplier supplier, Boolean requestedActive) {
        if (requestedActive == null) {
            return;
        }
        supplier.setIsActive(requestedActive);
        if (!requestedActive && supplier.getStatus() == SupplierStatus.ACTIVE) {
            supplier.setStatus(SupplierStatus.INACTIVE);
            return;
        }
        if (requestedActive && supplier.getStatus() != SupplierStatus.BLACKLISTED) {
            supplier.setStatus(SupplierStatus.ACTIVE);
        }
    }

    public SupplierResponse getSupplierById(Long supplierId) {
        return toResponse(getEntity(supplierId));
    }

    public SupplierResponse getSupplierByCode(String supplierCode) {
        return toResponse(supplierRepository.findBySupplierCode(supplierCode)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with code: " + supplierCode)));
    }

    public SupplierResponse getSupplierByEmail(String email) {
        return toResponse(supplierRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with email: " + email)));
    }

    public Page<SupplierResponse> getAllSuppliers(Boolean isActive, SupplierStatus status, int page, int size, String sortBy, String sortDir) {
        Specification<Supplier> spec = Specification.where(SupplierSpecifications.isActive(isActive))
                .and(SupplierSpecifications.status(status));
        return supplierRepository.findAll(spec, pageable(page, size, sortBy, sortDir)).map(this::toResponse);
    }

    public Page<SupplierResponse> searchSuppliers(String keyword, SupplierStatus status, Boolean isActive, String city, String country,
            BigDecimal minRating, Integer maxLeadTimeDays, int page, int size, String sortBy, String sortDir) {
        Specification<Supplier> spec = Specification.where(SupplierSpecifications.keyword(keyword))
                .and(SupplierSpecifications.status(status))
                .and(SupplierSpecifications.isActive(isActive))
                .and(SupplierSpecifications.city(city))
                .and(SupplierSpecifications.country(country))
                .and(SupplierSpecifications.minRating(minRating))
                .and(SupplierSpecifications.maxLeadTime(maxLeadTimeDays));
        return supplierRepository.findAll(spec, pageable(page, size, sortBy, sortDir)).map(this::toResponse);
    }

    @Transactional
    public SupplierResponse activateSupplier(Long supplierId, Long actorId) {
        Supplier supplier = getEntity(supplierId);
        if (Boolean.TRUE.equals(supplier.getIsActive()) && supplier.getStatus() == SupplierStatus.ACTIVE) {
            log.info("Supplier {} is already active", supplierId);
            return toResponse(supplier);
        }
        supplier.setIsActive(true);
        supplier.setStatus(SupplierStatus.ACTIVE);
        supplier.setUpdatedBy(actorId);
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier activate success for supplierId={}", supplierId);
        publish(saved, SupplierEventType.SUPPLIER_ACTIVATED, activatedRouting, actorId, null);
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse deactivateSupplier(Long supplierId, DeactivateSupplierRequest request, Long actorId) {
        Supplier supplier = getEntity(supplierId);
        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidSupplierDataException("Deactivation reason is required");
        }
        if (Boolean.FALSE.equals(supplier.getIsActive()) && supplier.getStatus() == SupplierStatus.INACTIVE) {
            log.info("Supplier {} is already inactive", supplierId);
            return toResponse(supplier);
        }
        supplier.setIsActive(false);
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplier.setUpdatedBy(actorId);
        appendReason(supplier, "Deactivated: " + request.reason());
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier deactivate success for supplierId={} reason={}", supplierId, request.reason());
        publish(saved, SupplierEventType.SUPPLIER_DEACTIVATED, deactivatedRouting, actorId, request.reason());
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse blacklistSupplier(Long supplierId, BlacklistSupplierRequest request, Long actorId) {
        Supplier supplier = getEntity(supplierId);
        if (request.reason() == null || request.reason().isBlank()) {
            throw new InvalidSupplierDataException("Blacklist reason is required");
        }
        supplier.setIsActive(false);
        supplier.setStatus(SupplierStatus.BLACKLISTED);
        supplier.setUpdatedBy(actorId);
        appendReason(supplier, "Blacklisted: " + request.reason());
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier blacklist success for supplierId={} reason={}", supplierId, request.reason());
        publish(saved, SupplierEventType.SUPPLIER_BLACKLISTED, blacklistedRouting, actorId, request.reason());
        return toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplierRating(Long supplierId, UpdateSupplierRatingRequest request, Long actorId) {
        Supplier supplier = getEntity(supplierId);
        supplier.setRating(defaultRating(request.rating()));
        supplier.setUpdatedBy(actorId);
        if (request.remarks() != null && !request.remarks().isBlank()) {
            appendReason(supplier, "Rating: " + request.remarks());
        }
        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier rating update success for supplierId={} rating={}", supplierId, saved.getRating());
        publish(saved, SupplierEventType.SUPPLIER_RATING_UPDATED, ratingUpdatedRouting, actorId, request.remarks());
        return toResponse(saved);
    }

    public SupplierSummaryResponse getSupplierSummary() {
        List<Supplier> suppliers = supplierRepository.findAll();
        BigDecimal averageRating = suppliers.stream()
                .map(Supplier::getRating)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long ratedCount = suppliers.stream().filter(s -> s.getRating() != null).count();
        BigDecimal averageLeadTime = suppliers.stream()
                .filter(s -> s.getLeadTimeDays() != null)
                .map(s -> BigDecimal.valueOf(s.getLeadTimeDays()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long leadTimeCount = suppliers.stream().filter(s -> s.getLeadTimeDays() != null).count();
        return new SupplierSummaryResponse(
                suppliers.size(),
                supplierRepository.countByIsActive(true),
                supplierRepository.countByStatus(SupplierStatus.INACTIVE),
                supplierRepository.countByStatus(SupplierStatus.BLACKLISTED),
                supplierRepository.countByStatus(SupplierStatus.PENDING_REVIEW),
                ratedCount == 0 ? BigDecimal.ZERO : averageRating.divide(BigDecimal.valueOf(ratedCount), 2, RoundingMode.HALF_UP),
                leadTimeCount == 0 ? BigDecimal.ZERO : averageLeadTime.divide(BigDecimal.valueOf(leadTimeCount), 2, RoundingMode.HALF_UP)
        );
    }

    public SupplierPerformanceResponse getSupplierPerformance(Long supplierId) {
        Supplier supplier = getEntity(supplierId);
        BigDecimal rating = supplier.getRating() == null ? BigDecimal.ZERO : supplier.getRating();
        return new SupplierPerformanceResponse(
                supplier.getSupplierId(),
                supplier.getName(),
                supplier.getTotalOrders() == null ? 0 : supplier.getTotalOrders(),
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                rating,
                rating,
                rating,
                supplier.getUpdatedAt()
        );
    }

    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository.findByIsActive(true).stream()
                .filter(supplier -> supplier.getStatus() != SupplierStatus.BLACKLISTED)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(this::toResponse)
                .toList();
    }

    public List<SupplierResponse> getTopRatedSuppliers() {
        return supplierRepository.findAll(Sort.by(Sort.Direction.DESC, "rating")).stream()
                .filter(supplier -> Boolean.TRUE.equals(supplier.getIsActive()))
                .limit(10)
                .map(this::toResponse)
                .toList();
    }

    public SupplierPurchaseValidationResponse validateSupplierForPurchase(Long supplierId) {
        Supplier supplier = getEntity(supplierId);
        boolean canUse = Boolean.TRUE.equals(supplier.getIsActive()) && supplier.getStatus() == SupplierStatus.ACTIVE;
        String reason = null;
        if (!Boolean.TRUE.equals(supplier.getIsActive())) {
            reason = "Supplier is inactive";
        } else if (supplier.getStatus() == SupplierStatus.BLACKLISTED) {
            reason = "Supplier is blacklisted";
        } else if (supplier.getStatus() == SupplierStatus.INACTIVE) {
            reason = "Supplier status is inactive";
        }
        log.info("Supplier purchase validation for supplierId={} result={}", supplierId, canUse);
        return new SupplierPurchaseValidationResponse(
                supplier.getSupplierId(),
                supplier.getName(),
                supplier.getIsActive(),
                supplier.getStatus(),
                supplier.getPaymentTerms(),
                supplier.getLeadTimeDays(),
                canUse,
                reason
        );
    }

    @Transactional
    public void deleteSupplier(Long supplierId) {
        Supplier supplier = getEntity(supplierId);
        if ((supplier.getTotalOrders() != null && supplier.getTotalOrders() > 0)) {
            throw new DuplicateSupplierException("Supplier has purchase orders and cannot be deleted");
        }
        supplierRepository.delete(supplier);
        log.info("Deleted supplier {}", supplierId);
    }

    private Supplier getEntity(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + supplierId));
    }

    private void validateRequest(String name, String email, String phone, String alternatePhone, String paymentTerms,
            Integer leadTimeDays, BigDecimal rating) {
        if (name == null || name.isBlank()) {
            throw new InvalidSupplierDataException("Supplier name is required");
        }
        if (paymentTerms == null || paymentTerms.isBlank()) {
            throw new InvalidSupplierDataException("Payment terms are required");
        }
        if (leadTimeDays != null && leadTimeDays < 0) {
            throw new InvalidSupplierDataException("Lead time cannot be negative");
        }
        if (rating != null && (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new InvalidSupplierDataException("Rating must be between 0 and 5");
        }
        validatePhone(phone, "Invalid phone number");
        validatePhone(alternatePhone, "Invalid alternate phone number");
        if (email != null && !email.isBlank() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new InvalidSupplierDataException("Invalid email address");
        }
    }

    private void validatePhone(String phone, String message) {
        if (phone != null && !phone.isBlank() && !phone.matches("^[+]?[0-9]{10,15}$")) {
            throw new InvalidSupplierDataException(message);
        }
    }

    private void ensureUnique(String supplierCode, String name, String email, String phone, String gstNumber, String taxNumber, Long currentSupplierId) {
        ensureUniqueSupplierCode(supplierCode, currentSupplierId);
        ensureUniqueName(name, currentSupplierId);
        ensureUniqueEmail(email, currentSupplierId);
        ensureUniquePhone(phone, currentSupplierId);
        ensureUniqueGstNumber(gstNumber, currentSupplierId);
        ensureUniqueTaxNumber(taxNumber, currentSupplierId);
    }

    private void ensureUniqueSupplierCode(String supplierCode, Long currentSupplierId) {
        assertUniqueSupplier(
                supplierRepository.findBySupplierCode(supplierCode),
                currentSupplierId,
                "Supplier code already exists",
                "Duplicate supplier code {}",
                supplierCode
        );
    }

    private void ensureUniqueName(String name, Long currentSupplierId) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            return;
        }
        assertUniqueSupplier(
                supplierRepository.findByNameIgnoreCase(normalizedName),
                currentSupplierId,
                "Supplier name already exists",
                null,
                null
        );
    }

    private void ensureUniqueEmail(String email, Long currentSupplierId) {
        if (email == null || email.isBlank()) {
            return;
        }
        assertUniqueSupplier(
                supplierRepository.findByEmailIgnoreCase(normalizeEmail(email)),
                currentSupplierId,
                "Supplier email already exists",
                "Duplicate supplier email {}",
                email
        );
    }

    private void ensureUniquePhone(String phone, Long currentSupplierId) {
        String normalizedPhone = trimToNull(phone);
        if (normalizedPhone == null) {
            return;
        }
        assertUniqueSupplier(
                supplierRepository.findByPhone(normalizedPhone),
                currentSupplierId,
                "Supplier phone already exists",
                null,
                null
        );
    }

    private void ensureUniqueGstNumber(String gstNumber, Long currentSupplierId) {
        String normalizedGstNumber = trimToNull(gstNumber);
        if (normalizedGstNumber == null) {
            return;
        }
        assertUniqueSupplier(
                supplierRepository.findByGstNumberIgnoreCase(normalizedGstNumber),
                currentSupplierId,
                "Supplier GSTIN already exists",
                null,
                null
        );
    }

    private void ensureUniqueTaxNumber(String taxNumber, Long currentSupplierId) {
        String normalizedTaxNumber = trimToNull(taxNumber);
        if (normalizedTaxNumber == null) {
            return;
        }
        assertUniqueSupplier(
                supplierRepository.findByTaxIdIgnoreCase(normalizedTaxNumber),
                currentSupplierId,
                "Supplier GSTIN already exists",
                "Duplicate supplier tax number {}",
                normalizedTaxNumber
        );
    }

    private void assertUniqueSupplier(Optional<Supplier> existingSupplier, Long currentSupplierId, String message, String logMessage, String logValue) {
        existingSupplier.ifPresent(existing -> {
            if (isDifferentSupplier(existing, currentSupplierId)) {
                if (logMessage != null) {
                    log.warn(logMessage, logValue);
                }
                throw new DuplicateSupplierException(message);
            }
        });
    }

    private boolean isDifferentSupplier(Supplier existing, Long currentSupplierId) {
        return currentSupplierId == null || !existing.getSupplierId().equals(currentSupplierId);
    }

    private String generateSupplierCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int suffix = 1;
        String code;
        do {
            code = "SUP-" + datePart + "-" + String.format("%04d", suffix++);
        } while (supplierRepository.existsBySupplierCode(code));
        return code;
    }

    private Pageable pageable(int page, int size, String sortBy, String sortDir) {
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                        (sortBy == null || sortBy.isBlank()) ? "name" : sortBy));
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.getContactPerson(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAlternatePhone(),
                supplier.getAddress(),
                supplier.getCity(),
                supplier.getState(),
                supplier.getCountry(),
                supplier.getPostalCode(),
                supplier.getTaxId(),
                supplier.getGstNumber(),
                supplier.getPaymentTerms(),
                supplier.getLeadTimeDays(),
                supplier.getRating() == null ? BigDecimal.ZERO : supplier.getRating(),
                supplier.getStatus(),
                supplier.getIsActive(),
                supplier.getNotes(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt(),
                supplier.getCreatedBy(),
                supplier.getUpdatedBy()
        );
    }

    private BigDecimal defaultRating(BigDecimal rating) {
        return rating == null ? BigDecimal.ZERO : rating.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void appendReason(Supplier supplier, String reason) {
        supplier.setNotes((supplier.getNotes() == null || supplier.getNotes().isBlank())
                ? reason
                : supplier.getNotes() + System.lineSeparator() + reason);
    }

    private void publish(Supplier supplier, SupplierEventType eventType, String routingKey, Long actorId, String reason) {
        supplierEventPublisher.publish(routingKey, new SupplierEvent(
                UUID.randomUUID().toString(),
                eventType.name(),
                supplier.getSupplierId(),
                supplier.getSupplierCode(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getPaymentTerms(),
                supplier.getLeadTimeDays(),
                supplier.getRating(),
                supplier.getStatus().name(),
                supplier.getIsActive(),
                actorId,
                LocalDateTime.now(),
                reason,
                null,
                Map.of("supplierId", supplier.getSupplierId(), "status", supplier.getStatus().name())
        ));
    }
}
