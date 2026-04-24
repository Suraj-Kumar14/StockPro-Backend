package com.stockpro.supplier.service.impl;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.exception.BadRequestException;
import com.stockpro.supplier.exception.ConflictException;
import com.stockpro.supplier.exception.ResourceNotFoundException;
import com.stockpro.supplier.repository.SupplierRepository;
import com.stockpro.supplier.service.SupplierService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public Supplier createSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new BadRequestException("Supplier payload is required.");
        }

        Supplier persistentSupplier = new Supplier();
        applySupplierValues(persistentSupplier, supplier, true);
        persistentSupplier.setSupplierId(null);
        persistentSupplier.setIsActive(supplier.getIsActive() == null ? Boolean.TRUE : supplier.getIsActive());
        persistentSupplier.setRating(supplier.getRating() == null ? 0.0 : validateRating(supplier.getRating()));

        return supplierRepository.save(persistentSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier getById(Long supplierId) {
        return getSupplierEntity(supplierId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> searchSuppliers(String name) {
        if (!StringUtils.hasText(name)) {
            return getAllSuppliers();
        }

        return supplierRepository.searchByName(name.trim()).stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Supplier updateSupplier(Long supplierId, Supplier supplier) {
        Supplier existingSupplier = getSupplierEntity(supplierId);

        if (supplier == null) {
            throw new BadRequestException("Supplier payload is required.");
        }

        applySupplierValues(existingSupplier, supplier, false);

        if (supplier.getIsActive() != null) {
            existingSupplier.setIsActive(supplier.getIsActive());
        }

        if (supplier.getRating() != null) {
            existingSupplier.setRating(validateRating(supplier.getRating()));
        }

        return supplierRepository.save(existingSupplier);
    }

    @Override
    public Supplier deactivateSupplier(Long supplierId) {
        Supplier supplier = getSupplierEntity(supplierId);
        supplier.setIsActive(Boolean.FALSE);
        return supplierRepository.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getByCity(String city) {
        if (!StringUtils.hasText(city)) {
            throw new BadRequestException("city is required.");
        }

        return supplierRepository.findByCityIgnoreCase(city.trim()).stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getByCountry(String country) {
        if (!StringUtils.hasText(country)) {
            throw new BadRequestException("country is required.");
        }

        return supplierRepository.findByCountryIgnoreCase(country.trim()).stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Supplier updateRating(Long supplierId, Double newRating) {
        Supplier supplier = getSupplierEntity(supplierId);
        supplier.setRating(validateRating(newRating));
        return supplierRepository.save(supplier);
    }

    private Supplier getSupplierEntity(Long supplierId) {
        if (supplierId == null) {
            throw new BadRequestException("supplierId is required.");
        }

        return supplierRepository.findBySupplierId(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private void applySupplierValues(Supplier target, Supplier source, boolean isCreate) {
        String normalizedTaxId = normalizeRequiredText(source.getTaxId(), "Supplier taxId is required.")
                .toUpperCase(Locale.ROOT);

        supplierRepository.findByTaxIdIgnoreCase(normalizedTaxId)
                .filter(existingSupplier -> isCreate || !existingSupplier.getSupplierId().equals(target.getSupplierId()))
                .ifPresent(existingSupplier -> {
                    throw new ConflictException("Supplier taxId already exists: " + normalizedTaxId);
                });

        target.setName(normalizeRequiredText(source.getName(), "Supplier name is required."));
        target.setContactPerson(normalizeRequiredText(source.getContactPerson(), "Contact person is required."));
        target.setEmail(normalizeRequiredText(source.getEmail(), "Email is required.").toLowerCase(Locale.ROOT));
        target.setPhone(normalizeOptionalText(source.getPhone()));
        target.setAddress(normalizeOptionalText(source.getAddress()));
        target.setCity(normalizeRequiredText(source.getCity(), "City is required."));
        target.setCountry(normalizeRequiredText(source.getCountry(), "Country is required."));
        target.setTaxId(normalizedTaxId);
        target.setPaymentTerms(normalizeRequiredText(source.getPaymentTerms(), "Payment terms are required.")
                .toUpperCase(Locale.ROOT));
        target.setLeadTimeDays(validateLeadTime(source.getLeadTimeDays()));
    }

    private Integer validateLeadTime(Integer leadTimeDays) {
        if (leadTimeDays == null || leadTimeDays < 0) {
            throw new BadRequestException("leadTimeDays must be zero or greater.");
        }
        return leadTimeDays;
    }

    private Double validateRating(Double rating) {
        if (rating == null || rating < 0.0 || rating > 5.0) {
            throw new BadRequestException("Supplier rating must be between 0.0 and 5.0.");
        }
        return rating;
    }

    private String normalizeRequiredText(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(errorMessage);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
