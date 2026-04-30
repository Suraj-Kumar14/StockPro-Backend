package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.InactiveSupplierException;
import com.stockpro.supplierservice.exception.InvalidSupplierDataException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupplierService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final SupplierRepository supplierRepository;
    private final SupplierValidationService validationService;
    private final SupplierMapper supplierMapper;

    @Transactional
    public SupplierResponseDTO createSupplier(SupplierRequestDTO dto) {
        SupplierRequestDTO normalizedRequest = normalizeRequest(dto);
        validationService.validateForCreate(normalizedRequest);

        Supplier supplier = new Supplier();
        applyMutableFields(supplier, normalizedRequest, false);
        supplier.setIsActive(true);
        supplier.setRating(defaultRating(normalizedRequest));
        supplier.setTotalOrders(0);
        supplier.setRatingCount(0);

        Supplier savedSupplier = saveSupplier(supplier);
        log.info("Created supplier {} with email {}", savedSupplier.getSupplierId(), savedSupplier.getEmail());
        return supplierMapper.toResponse(savedSupplier);
    }

    public SupplierResponseDTO getSupplierById(Long id) {
        return supplierMapper.toResponse(getSupplier(id));
    }

    public List<SupplierResponseDTO> getAllSuppliers() {
        return supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    public List<SupplierResponseDTO> getActiveSuppliers() {
        return supplierRepository.findByIsActive(true).stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .map(supplierMapper::toResponse)
                .toList();
    }

    public List<SupplierResponseDTO> getSuppliersByCity(String city) {
        return supplierRepository.findByCityContainingIgnoreCase(normalizeFilter(city)).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    public List<SupplierResponseDTO> getSuppliersByCountry(String country) {
        return supplierRepository.findByCountryContainingIgnoreCase(normalizeFilter(country)).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    public List<SupplierResponseDTO> searchSuppliers(String keyword) {
        String normalizedKeyword = normalizeFilter(keyword);
        if (normalizedKeyword == null) {
            return getAllSuppliers();
        }
        return supplierRepository.searchSuppliers(normalizedKeyword).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    public List<SupplierResponseDTO> searchSuppliers(
            String keyword, String name, String city, String country, Integer page, Integer size) {
        Pageable pageable = buildPageable(page, size);
        String normalizedName = firstNonNull(normalizeFilter(name), normalizeFilter(keyword));
        String normalizedCity = normalizeFilter(city);
        String normalizedCountry = normalizeFilter(country);

        return supplierRepository.searchSuppliers(normalizedName, normalizedCity, normalizedCountry, pageable)
                .getContent()
                .stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    public List<SupplierResponseDTO> getTopRatedSuppliers(Double minRating) {
        double normalizedMinRating = minRating == null ? 4.0 : minRating;
        if (normalizedMinRating < 0.0 || normalizedMinRating > 5.0) {
            throw new InvalidSupplierDataException("Minimum rating must be between 0 and 5");
        }
        return supplierRepository.findTopRatedSuppliers(normalizedMinRating).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO dto) {
        Supplier supplier = getSupplier(id);
        SupplierRequestDTO normalizedRequest = normalizeRequest(dto);
        validationService.validateForUpdate(supplier, normalizedRequest);

        applyMutableFields(supplier, normalizedRequest, true);
        Supplier updatedSupplier = saveSupplier(supplier);
        log.info("Updated supplier {}", updatedSupplier.getSupplierId());
        return supplierMapper.toResponse(updatedSupplier);
    }

    @Transactional
    public void updateRating(Long id, Double newRating) {
        validationService.validateRating(newRating);

        Supplier supplier = getSupplier(id);
        double currentRating = supplier.getRating() == null ? 0.0 : supplier.getRating();
        int ratingCount = supplier.getRatingCount() == null ? 0 : supplier.getRatingCount();
        double averageRating = ((currentRating * ratingCount) + newRating) / (ratingCount + 1);

        supplier.setRating(averageRating);
        supplier.setRatingCount(ratingCount + 1);
        saveSupplier(supplier);

        log.info("Updated rating for supplier {}. New average={}, ratingCount={}",
                supplier.getSupplierId(), supplier.getRating(), supplier.getRatingCount());
    }

    @Transactional
    public void deactivateSupplier(Long id) {
        Supplier supplier = getSupplier(id);
        if (Boolean.FALSE.equals(supplier.getIsActive())) {
            log.info("Supplier {} is already inactive", id);
            return;
        }

        supplier.setIsActive(false);
        saveSupplier(supplier);
        log.info("Deactivated supplier {}", id);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        deactivateSupplier(id);
    }

    private Supplier getSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));
    }

    private SupplierRequestDTO normalizeRequest(SupplierRequestDTO dto) {
        SupplierRequestDTO normalized = new SupplierRequestDTO();
        normalized.setName(trimToNull(dto.getName()));
        normalized.setContactPerson(trimToNull(dto.getContactPerson()));
        normalized.setEmail(lowercase(trimToNull(dto.getEmail())));
        normalized.setPhone(trimToNull(dto.getPhone()));
        normalized.setAddress(trimToNull(dto.getAddress()));
        normalized.setCity(trimToNull(dto.getCity()));
        normalized.setCountry(trimToNull(dto.getCountry()));
        normalized.setTaxId(trimToNull(dto.getTaxId()));
        normalized.setPaymentTerms(trimToNull(dto.getPaymentTerms()));
        normalized.setLeadTimeDays(dto.getLeadTimeDays());
        return normalized;
    }

    private void applyMutableFields(Supplier supplier, SupplierRequestDTO dto, boolean keepImmutableTaxId) {
        supplier.setName(dto.getName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setCity(dto.getCity());
        supplier.setCountry(dto.getCountry());
        if (!keepImmutableTaxId || supplier.getTaxId() == null) {
            supplier.setTaxId(dto.getTaxId());
        }
        supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setLeadTimeDays(dto.getLeadTimeDays());
    }

    private Supplier saveSupplier(Supplier supplier) {
        try {
            return supplierRepository.save(supplier);
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidSupplierDataException("Supplier data violates a persistence constraint");
        }
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int requestedSize = size == null || size < 1 ? DEFAULT_SIZE : size;
        int resolvedSize = Math.min(requestedSize, MAX_PAGE_SIZE);
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.ASC, "name"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String lowercase(String value) {
        return value == null ? null : value.toLowerCase();
    }

    private String normalizeFilter(String value) {
        return trimToNull(value);
    }

    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private Double defaultRating(SupplierRequestDTO dto) {
        return 0.0;
    }
}
