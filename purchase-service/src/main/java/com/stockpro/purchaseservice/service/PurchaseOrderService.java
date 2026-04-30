package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.GoodsReceiptDTO;
import com.stockpro.purchaseservice.dto.POLineItemDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderRequestDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderResponseDTO;
import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import com.stockpro.purchaseservice.exception.InvalidLineItemException;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.exception.InvalidPOStatusException;
import com.stockpro.purchaseservice.exception.PurchaseOrderNotFoundException;
import com.stockpro.purchaseservice.rabbitmq.POEventPublisher;
import com.stockpro.purchaseservice.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final EnumSet<POStatus> OVERDUE_STATUSES = EnumSet.of(
            POStatus.APPROVED, POStatus.PARTIALLY_RECEIVED);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderValidationService validationService;
    private final PurchaseOrderWorkflow workflow;
    private final SupplierGateway supplierGateway;
    private final WarehouseGateway warehouseGateway;
    private final ProductCatalogGateway productCatalogGateway;
    private final POEventPublisher poEventPublisher;

    @Transactional
    public PurchaseOrderResponseDTO createPO(PurchaseOrderRequestDTO dto) {
        validationService.validatePurchaseOrderRequest(dto);
        validateBusinessReferences(dto);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        applyEditableFields(purchaseOrder, dto);
        purchaseOrder.setStatus(POStatus.DRAFT);
        purchaseOrder.setReceivedDate(null);

        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("Created PO {} for supplier {} and warehouse {} with status {}",
                saved.getPoId(), saved.getSupplierId(), saved.getWarehouseId(), saved.getStatus());
        return purchaseOrderMapper.toResponse(saved);
    }

    public PurchaseOrderResponseDTO getPOById(Long id) {
        return purchaseOrderMapper.toResponse(getPOEntity(id));
    }

    public List<PurchaseOrderResponseDTO> getAllPOs() {
        return purchaseOrderRepository.findAll().stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId).stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByWarehouse(Long warehouseId) {
        return purchaseOrderRepository.findByWarehouseId(warehouseId).stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByStatus(String status) {
        POStatus requestedStatus = parseStatus(status);
        List<POStatus> statuses = requestedStatus == POStatus.RECEIVED
                ? List.of(POStatus.RECEIVED, POStatus.FULLY_RECEIVED)
                : List.of(requestedStatus);

        return purchaseOrderRepository.findAllByStatusIn(statuses).stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByCreatedBy(Long userId) {
        return purchaseOrderRepository.findByCreatedById(userId).stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        return purchaseOrderRepository.findByOrderDateBetween(startDate, endDate).stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    public List<PurchaseOrderResponseDTO> getOverduePOs() {
        return purchaseOrderRepository.findOverduePurchaseOrders(OVERDUE_STATUSES, LocalDate.now()).stream()
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }

    @Transactional
    public PurchaseOrderResponseDTO submitForApproval(Long id) {
        PurchaseOrder purchaseOrder = getPOEntity(id);
        workflow.assertCanSubmit(snapshotFor(purchaseOrder));

        purchaseOrder.setStatus(POStatus.PENDING);
        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("PO {} moved to {}", saved.getPoId(), saved.getStatus());

        try {
            poEventPublisher.publishPOPending(saved.getPoId(), saved.getSupplierId(),
                    saved.getWarehouseId(), saved.getCreatedById(), saved.getExpectedDate());
        } catch (Exception ex) {
            log.warn("Pending alert hook failed for PO {}: {}", saved.getPoId(), ex.getMessage());
        }

        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO approvePO(Long id) {
        PurchaseOrder purchaseOrder = getPOEntity(id);
        workflow.assertCanApprove(snapshotFor(purchaseOrder));

        purchaseOrder.setStatus(POStatus.APPROVED);
        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("PO {} moved to {}", saved.getPoId(), saved.getStatus());

        try {
            poEventPublisher.publishPOApproved(saved.getPoId(), saved.getSupplierId(),
                    saved.getWarehouseId(), saved.getCreatedById(),
                    saved.getTotalAmount(), saved.getExpectedDate());
        } catch (Exception ex) {
            log.warn("Approved alert hook failed for PO {}: {}", saved.getPoId(), ex.getMessage());
        }

        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO rejectPO(Long id, String reason) {
        PurchaseOrder purchaseOrder = getPOEntity(id);
        workflow.assertCanReject(snapshotFor(purchaseOrder));

        purchaseOrder.setStatus(POStatus.CANCELLED);
        purchaseOrder.setNotes(prependAuditNote("REJECTED", requireReason(reason), purchaseOrder.getNotes()));

        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("PO {} rejected and moved to {}", saved.getPoId(), saved.getStatus());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO cancelPO(Long id, String reason) {
        PurchaseOrder purchaseOrder = getPOEntity(id);
        workflow.assertCanCancel(snapshotFor(purchaseOrder));

        purchaseOrder.setStatus(POStatus.CANCELLED);
        purchaseOrder.setNotes(prependAuditNote("CANCELLED", requireReason(reason), purchaseOrder.getNotes()));

        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("PO {} cancelled from workflow state {}", saved.getPoId(), saved.getStatus());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO receiveGoods(Long poId, List<GoodsReceiptDTO> receipts) {
        PurchaseOrder purchaseOrder = getPOEntity(poId);
        workflow.assertCanReceive(snapshotFor(purchaseOrder));
        validationService.validateGoodsReceipts(receipts);

        Map<Long, POLineItem> lineItemsById = indexLineItems(purchaseOrder);
        for (GoodsReceiptDTO receipt : receipts) {
            POLineItem lineItem = lineItemsById.get(receipt.getLineItemId());
            if (lineItem == null) {
                throw new InvalidLineItemException(
                        "Line item not found for receipt: " + receipt.getLineItemId());
            }
            validationService.validateReceiptQuantity(lineItem, receipt.getReceivedQty());
        }

        for (GoodsReceiptDTO receipt : receipts) {
            POLineItem lineItem = lineItemsById.get(receipt.getLineItemId());
            lineItem.setReceivedQty(lineItem.getReceivedQty() + receipt.getReceivedQty());

            StockProductThresholdDTO thresholds =
                    productCatalogGateway.getProductThresholds(lineItem.getProductId());

            log.info("Recording GRN for PO {}, product {}, warehouse {}, quantity {}",
                    purchaseOrder.getPoId(), lineItem.getProductId(),
                    purchaseOrder.getWarehouseId(), receipt.getReceivedQty());

            warehouseGateway.increaseStock(purchaseOrder.getWarehouseId(),
                    lineItem.getProductId(), receipt.getReceivedQty(), thresholds);
        }

        if (purchaseOrder.getLineItems().stream()
                .allMatch(this::isFullyReceived)) {
            purchaseOrder.setStatus(POStatus.RECEIVED);
            purchaseOrder.setReceivedDate(LocalDate.now());
        } else {
            purchaseOrder.setStatus(POStatus.PARTIALLY_RECEIVED);
            purchaseOrder.setReceivedDate(null);
        }

        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("Completed goods receipt for PO {}. New status: {}",
                saved.getPoId(), saved.getStatus());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO updatePO(Long id, PurchaseOrderRequestDTO dto) {
        PurchaseOrder purchaseOrder = getPOEntity(id);
        workflow.assertDraft(snapshotFor(purchaseOrder));
        validationService.validatePurchaseOrderRequest(dto);
        validateBusinessReferences(dto);

        applyEditableFields(purchaseOrder, dto);
        PurchaseOrder saved = savePurchaseOrder(purchaseOrder);
        log.info("Updated DRAFT PO {}", saved.getPoId());
        return purchaseOrderMapper.toResponse(saved);
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void checkOverduePOs() {
        log.info("Running scheduled overdue PO check");
        List<PurchaseOrder> overduePOs = purchaseOrderRepository
                .findOverduePurchaseOrders(OVERDUE_STATUSES, LocalDate.now());

        for (PurchaseOrder purchaseOrder : overduePOs) {
            try {
                poEventPublisher.publishPOOverdue(purchaseOrder.getPoId(),
                        purchaseOrder.getSupplierId(), purchaseOrder.getWarehouseId(),
                        purchaseOrder.getExpectedDate());
            } catch (Exception ex) {
                log.warn("Overdue alert hook failed for PO {}: {}",
                        purchaseOrder.getPoId(), ex.getMessage());
            }
        }

        log.info("Overdue PO check complete. Found {} overdue POs", overduePOs.size());
    }

    private void validateBusinessReferences(PurchaseOrderRequestDTO dto) {
        supplierGateway.ensureSupplierExists(dto.getSupplierId());
        warehouseGateway.ensureWarehouseExists(dto.getWarehouseId());
    }

    private void applyEditableFields(PurchaseOrder purchaseOrder, PurchaseOrderRequestDTO dto) {
        purchaseOrder.setSupplierId(dto.getSupplierId());
        purchaseOrder.setWarehouseId(dto.getWarehouseId());
        purchaseOrder.setCreatedById(dto.getCreatedById());
        purchaseOrder.setExpectedDate(dto.getExpectedDate());
        purchaseOrder.setNotes(trimToNull(dto.getNotes()));
        purchaseOrder.setReferenceNumber(trimToNull(dto.getReferenceNumber()));
        replaceLineItems(purchaseOrder, dto.getLineItems());
        purchaseOrder.setTotalAmount(calculateTotalAmount(purchaseOrder.getLineItems()));
    }

    private void replaceLineItems(PurchaseOrder purchaseOrder, List<POLineItemDTO> lineItemDTOs) {
        purchaseOrder.getLineItems().clear();
        for (POLineItemDTO lineItemDTO : lineItemDTOs) {
            POLineItem lineItem = POLineItem.builder()
                    .productId(lineItemDTO.getProductId())
                    .quantity(lineItemDTO.getQuantity())
                    .unitCost(lineItemDTO.getUnitCost())
                    .totalCost(calculateLineTotal(lineItemDTO))
                    .receivedQty(0)
                    .purchaseOrder(purchaseOrder)
                    .build();
            purchaseOrder.getLineItems().add(lineItem);
        }
    }

    private BigDecimal calculateTotalAmount(List<POLineItem> lineItems) {
        return lineItems.stream()
                .map(POLineItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLineTotal(POLineItemDTO lineItemDTO) {
        return lineItemDTO.getUnitCost()
                .multiply(BigDecimal.valueOf(lineItemDTO.getQuantity()));
    }

    private PurchaseOrderWorkflow.PurchaseOrderStateSnapshot snapshotFor(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderWorkflow.PurchaseOrderStateSnapshot(normalizeStatus(purchaseOrder.getStatus()));
    }

    private POStatus normalizeStatus(POStatus status) {
        return status == POStatus.FULLY_RECEIVED ? POStatus.RECEIVED : status;
    }

    private POStatus parseStatus(String status) {
        try {
            POStatus parsedStatus = POStatus.valueOf(status.trim().toUpperCase());
            return normalizeStatus(parsedStatus);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    private Map<Long, POLineItem> indexLineItems(PurchaseOrder purchaseOrder) {
        Map<Long, POLineItem> lineItemsById = new HashMap<>();
        for (POLineItem lineItem : purchaseOrder.getLineItems()) {
            lineItemsById.put(lineItem.getLineItemId(), lineItem);
        }
        return lineItemsById;
    }

    private boolean isFullyReceived(POLineItem lineItem) {
        return lineItem.getReceivedQty() != null
                && lineItem.getQuantity() != null
                && lineItem.getReceivedQty().intValue() == lineItem.getQuantity().intValue();
    }

    private String requireReason(String reason) {
        String trimmedReason = trimToNull(reason);
        if (trimmedReason == null) {
            throw new InvalidPOStateException("A reason is required for this operation");
        }
        return trimmedReason;
    }

    private String prependAuditNote(String action, String reason, String existingNotes) {
        String auditNote = action + ": " + reason;
        return existingNotes == null ? auditNote : auditNote + " | " + existingNotes;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private PurchaseOrder getPOEntity(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(
                        "Purchase order not found with ID: " + id));
    }

    private PurchaseOrder savePurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            purchaseOrder.setTotalAmount(calculateTotalAmount(purchaseOrder.getLineItems()));
            return purchaseOrderRepository.save(purchaseOrder);
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidPOStatusException("Purchase order data violates a persistence constraint");
        }
    }
}
