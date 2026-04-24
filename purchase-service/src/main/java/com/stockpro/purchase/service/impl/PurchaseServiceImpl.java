package com.stockpro.purchase.service.impl;

import com.stockpro.purchase.client.ProductClient;
import com.stockpro.purchase.client.SupplierClient;
import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.dto.ProductSummaryResponse;
import com.stockpro.purchase.dto.SupplierSummaryResponse;
import com.stockpro.purchase.dto.WarehouseStockUpdateRequest;
import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.PurchaseOrderStatus;
import com.stockpro.purchase.exception.BadRequestException;
import com.stockpro.purchase.exception.ConflictException;
import com.stockpro.purchase.exception.ExternalServiceException;
import com.stockpro.purchase.exception.ResourceNotFoundException;
import com.stockpro.purchase.repository.PurchaseRepository;
import com.stockpro.purchase.security.JwtService;
import com.stockpro.purchase.service.PurchaseService;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductClient productClient;
    private final SupplierClient supplierClient;
    private final WarehouseClient warehouseClient;
    private final JwtService jwtService;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
            ProductClient productClient,
            SupplierClient supplierClient,
            WarehouseClient warehouseClient,
            JwtService jwtService) {
        this.purchaseRepository = purchaseRepository;
        this.productClient = productClient;
        this.supplierClient = supplierClient;
        this.warehouseClient = warehouseClient;
        this.jwtService = jwtService;
    }

    @Override
    public PurchaseOrder createPO(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            throw new BadRequestException("Purchase order payload is required.");
        }

        if (purchaseOrder.getSupplierId() == null) {
            throw new BadRequestException("supplierId is required.");
        }

        if (purchaseOrder.getWarehouseId() == null) {
            throw new BadRequestException("warehouseId is required.");
        }

        validateSupplierExists(purchaseOrder.getSupplierId());

        PurchaseOrder persistentPurchaseOrder = new PurchaseOrder();
        persistentPurchaseOrder.setPoId(null);
        persistentPurchaseOrder.setSupplierId(purchaseOrder.getSupplierId());
        persistentPurchaseOrder.setWarehouseId(purchaseOrder.getWarehouseId());
        persistentPurchaseOrder.setCreatedById(resolveCreatedById(purchaseOrder.getCreatedById()));
        persistentPurchaseOrder.setStatus(resolveInitialStatus(purchaseOrder.getStatus()));
        persistentPurchaseOrder.setOrderDate(purchaseOrder.getOrderDate() != null ? purchaseOrder.getOrderDate() : LocalDate.now());
        persistentPurchaseOrder.setExpectedDate(purchaseOrder.getExpectedDate());
        persistentPurchaseOrder.setNotes(normalizeOptionalText(purchaseOrder.getNotes()));
        persistentPurchaseOrder.setReferenceNumber(normalizeOptionalText(purchaseOrder.getReferenceNumber()));
        persistentPurchaseOrder.setReceivedDate(null);
        persistentPurchaseOrder.setLineItems(new ArrayList<>());

        if (persistentPurchaseOrder.getExpectedDate() != null
                && persistentPurchaseOrder.getExpectedDate().isBefore(persistentPurchaseOrder.getOrderDate())) {
            throw new BadRequestException("expectedDate cannot be before orderDate.");
        }

        List<POLineItem> inputLineItems = purchaseOrder.getLineItems();
        if (inputLineItems == null || inputLineItems.isEmpty()) {
            throw new BadRequestException("At least one PO line item is required.");
        }

        Set<Long> seenProductIds = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (POLineItem inputLineItem : inputLineItems) {
            if (inputLineItem == null) {
                throw new BadRequestException("PO line items cannot contain null values.");
            }

            Long productId = inputLineItem.getProductId();
            if (productId == null) {
                throw new BadRequestException("Each PO line item must include productId.");
            }

            if (!seenProductIds.add(productId)) {
                throw new BadRequestException("Duplicate productId found in purchase order: " + productId);
            }

            Integer orderedQuantity = inputLineItem.getQuantity();
            if (orderedQuantity == null || orderedQuantity <= 0) {
                throw new BadRequestException("Ordered quantity must be greater than zero for productId " + productId);
            }

            ProductSummaryResponse product = validateProductExists(productId);
            BigDecimal unitCost = inputLineItem.getUnitCost() != null
                    ? inputLineItem.getUnitCost()
                    : product.getCostPrice();

            if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Unit cost must be greater than zero for productId " + productId);
            }

            POLineItem persistentLineItem = POLineItem.builder()
                    .lineItemId(null)
                    .purchaseOrder(persistentPurchaseOrder)
                    .productId(productId)
                    .quantity(orderedQuantity)
                    .unitCost(unitCost)
                    .totalCost(unitCost.multiply(BigDecimal.valueOf(orderedQuantity)))
                    .receivedQty(0)
                    .build();

            persistentPurchaseOrder.getLineItems().add(persistentLineItem);
            totalAmount = totalAmount.add(persistentLineItem.getTotalCost());
        }

        persistentPurchaseOrder.setTotalAmount(totalAmount);
        return purchaseRepository.save(persistentPurchaseOrder);
    }

    @Override
    public PurchaseOrder approvePO(Long poId) {
        PurchaseOrder purchaseOrder = getPurchaseOrder(poId);

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new ConflictException("Cancelled purchase orders cannot be approved.");
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.FULLY_RECEIVED
                || purchaseOrder.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ConflictException("Purchase orders with received goods cannot be re-approved.");
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.APPROVED) {
            return purchaseOrder;
        }

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT
                && purchaseOrder.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new ConflictException("Only DRAFT or PENDING_APPROVAL purchase orders can be approved.");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        return purchaseRepository.save(purchaseOrder);
    }

    @Override
    public PurchaseOrder receiveGoods(Long poId, List<POLineItem> receivedItems) {
        PurchaseOrder purchaseOrder = getPurchaseOrder(poId);

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new ConflictException("Cancelled purchase orders cannot receive goods.");
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.DRAFT
                || purchaseOrder.getStatus() == PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new ConflictException("Purchase order must be approved before goods can be received.");
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.FULLY_RECEIVED) {
            throw new ConflictException("All goods for this purchase order have already been received.");
        }

        if (receivedItems == null || receivedItems.isEmpty()) {
            throw new BadRequestException("At least one received item is required.");
        }

        Map<Long, POLineItem> lineItemsById = new HashMap<>();
        Map<Long, POLineItem> lineItemsByProductId = new HashMap<>();

        for (POLineItem existingLineItem : purchaseOrder.getLineItems()) {
            lineItemsById.put(existingLineItem.getLineItemId(), existingLineItem);
            lineItemsByProductId.put(existingLineItem.getProductId(), existingLineItem);
        }

        Map<Long, Integer> receivedQtyByLineItemId = new LinkedHashMap<>();

        for (POLineItem receivedItem : receivedItems) {
            if (receivedItem == null) {
                throw new BadRequestException("Received items cannot contain null values.");
            }

            POLineItem targetLineItem = resolveTargetLineItem(receivedItem, lineItemsById, lineItemsByProductId);
            Integer receivedQuantity = resolveReceivedQuantity(receivedItem);
            receivedQtyByLineItemId.merge(targetLineItem.getLineItemId(), receivedQuantity, Integer::sum);
        }

        List<StockReceiptInstruction> stockReceipts = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : receivedQtyByLineItemId.entrySet()) {
            POLineItem targetLineItem = lineItemsById.get(entry.getKey());
            int newReceivedQty = defaultZero(targetLineItem.getReceivedQty()) + entry.getValue();

            if (newReceivedQty > defaultZero(targetLineItem.getQuantity())) {
                throw new BadRequestException(
                        "Received quantity exceeds remaining quantity for productId " + targetLineItem.getProductId());
            }

            targetLineItem.setReceivedQty(newReceivedQty);
            stockReceipts.add(new StockReceiptInstruction(targetLineItem.getProductId(), entry.getValue()));
        }

        for (StockReceiptInstruction stockReceipt : stockReceipts) {
            updateWarehouseStock(
                    purchaseOrder.getWarehouseId(),
                    stockReceipt.productId(),
                    stockReceipt.quantity(),
                    purchaseOrder.getPoId());
        }

        if (purchaseOrder.getLineItems().stream()
                .allMatch(lineItem -> defaultZero(lineItem.getReceivedQty()) >= defaultZero(lineItem.getQuantity()))) {
            purchaseOrder.setStatus(PurchaseOrderStatus.FULLY_RECEIVED);
        } else {
            purchaseOrder.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        purchaseOrder.setReceivedDate(LocalDate.now());
        return purchaseRepository.save(purchaseOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPOsByStatus(String status) {
        return purchaseRepository.findByStatus(parseStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPOsByDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BadRequestException("Both start and end dates are required.");
        }

        if (end.isBefore(start)) {
            throw new BadRequestException("end date cannot be before start date.");
        }

        return purchaseRepository.findByOrderDateBetween(start, end);
    }

    @Override
    public PurchaseOrder cancelPO(Long poId) {
        PurchaseOrder purchaseOrder = getPurchaseOrder(poId);

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELLED) {
            return purchaseOrder;
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.FULLY_RECEIVED) {
            throw new ConflictException("Fully received purchase orders cannot be cancelled.");
        }

        boolean hasAnyReceivedQuantity = purchaseOrder.getLineItems().stream()
                .anyMatch(lineItem -> defaultZero(lineItem.getReceivedQty()) > 0);

        if (hasAnyReceivedQuantity) {
            throw new ConflictException("Purchase orders with received goods cannot be cancelled.");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
        return purchaseRepository.save(purchaseOrder);
    }

    private PurchaseOrder getPurchaseOrder(Long poId) {
        if (poId == null) {
            throw new BadRequestException("poId is required.");
        }

        return purchaseRepository.findByPoId(poId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + poId));
    }

    private SupplierSummaryResponse validateSupplierExists(Long supplierId) {
        try {
            SupplierSummaryResponse supplier = supplierClient.getSupplierById(supplierId);
            if (supplier.getIsActive() != null && !supplier.getIsActive()) {
                throw new BadRequestException("Supplier is inactive: " + supplierId);
            }
            return supplier;
        } catch (FeignException.NotFound exception) {
            throw new ResourceNotFoundException("Supplier not found with id: " + supplierId);
        } catch (FeignException exception) {
            throw new ExternalServiceException("Unable to validate supplier using supplier-service.", exception);
        }
    }

    private ProductSummaryResponse validateProductExists(Long productId) {
        try {
            ProductSummaryResponse product = productClient.getProductById(productId);
            if (product.getIsActive() != null && !product.getIsActive()) {
                throw new BadRequestException("Product is inactive: " + productId);
            }
            return product;
        } catch (FeignException.NotFound exception) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        } catch (FeignException exception) {
            throw new ExternalServiceException("Unable to validate product using product-service.", exception);
        }
    }

    private void updateWarehouseStock(Long warehouseId, Long productId, Integer quantityChange, Long purchaseOrderId) {
        try {
            warehouseClient.updateStock(warehouseId, productId, new WarehouseStockUpdateRequest(
                    quantityChange,
                    "STOCK_IN",
                    purchaseOrderId,
                    "PURCHASE_ORDER",
                    "Goods received against approved purchase order " + purchaseOrderId + "."));
        } catch (FeignException.NotFound exception) {
            throw new ResourceNotFoundException(
                    "Warehouse stock update failed because warehouse or product was not found.");
        } catch (FeignException exception) {
            throw new ExternalServiceException("Unable to record stock-in movement using warehouse-service.", exception);
        }
    }

    private PurchaseOrderStatus resolveInitialStatus(PurchaseOrderStatus requestedStatus) {
        if (requestedStatus == null) {
            return PurchaseOrderStatus.DRAFT;
        }

        if (requestedStatus != PurchaseOrderStatus.DRAFT && requestedStatus != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new BadRequestException("New purchase orders can only start in DRAFT or PENDING_APPROVAL.");
        }

        return requestedStatus;
    }

    private PurchaseOrderStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BadRequestException("status is required.");
        }

        try {
            return PurchaseOrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported purchase order status: " + status);
        }
    }

    private POLineItem resolveTargetLineItem(POLineItem receivedItem,
            Map<Long, POLineItem> lineItemsById,
            Map<Long, POLineItem> lineItemsByProductId) {
        if (receivedItem.getLineItemId() != null) {
            POLineItem target = lineItemsById.get(receivedItem.getLineItemId());
            if (target == null) {
                throw new ResourceNotFoundException(
                        "PO line item not found with id: " + receivedItem.getLineItemId());
            }
            return target;
        }

        if (receivedItem.getProductId() != null) {
            POLineItem target = lineItemsByProductId.get(receivedItem.getProductId());
            if (target == null) {
                throw new ResourceNotFoundException(
                        "PO line item not found for productId: " + receivedItem.getProductId());
            }
            return target;
        }

        throw new BadRequestException("Each received item must contain either lineItemId or productId.");
    }

    private Integer resolveReceivedQuantity(POLineItem receivedItem) {
        Integer quantity = receivedItem.getReceivedQty();

        if (quantity == null || quantity <= 0) {
            quantity = receivedItem.getQuantity();
        }

        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Received quantity must be greater than zero.");
        }

        return quantity;
    }

    private Long resolveCreatedById(Long requestedCreatedById) {
        if (requestedCreatedById != null) {
            return requestedCreatedById;
        }

        String token = extractCurrentToken();
        if (token != null) {
            try {
                Long userId = jwtService.extractUserId(token);
                if (userId != null) {
                    return userId;
                }
            } catch (Exception exception) {
                // Fall through to explicit validation error below.
            }
        }

        throw new BadRequestException("createdById is required.");
    }

    private String extractCurrentToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        String authorizationHeader = servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record StockReceiptInstruction(Long productId, Integer quantity) {
    }
}
