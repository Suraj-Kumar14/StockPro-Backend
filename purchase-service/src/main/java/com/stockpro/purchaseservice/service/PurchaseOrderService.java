package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.*;
import com.stockpro.purchaseservice.dto.PurchaseOrderResponseDTO.POLineItemResponseDTO;
import com.stockpro.purchaseservice.entity.*;
import com.stockpro.purchaseservice.exception.*;
import com.stockpro.purchaseservice.rabbitmq.POEventPublisher;
import com.stockpro.purchaseservice.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private POLineItemRepository lineItemRepository;

    @Autowired
    private POEventPublisher poEventPublisher;

    @Transactional
    public PurchaseOrderResponseDTO createPO(PurchaseOrderRequestDTO dto) {
        log.info("Creating PO for supplier: {}", dto.getSupplierId());

        PurchaseOrder po = PurchaseOrder.builder()
                .supplierId(dto.getSupplierId())
                .warehouseId(dto.getWarehouseId())
                .createdById(dto.getCreatedById())
                .expectedDate(dto.getExpectedDate())
                .notes(dto.getNotes())
                .referenceNumber(dto.getReferenceNumber())
                .status(POStatus.DRAFT)
                .build();

        List<POLineItem> items = dto.getLineItems().stream()
                .map(itemDto -> {
                    BigDecimal total = itemDto.getUnitCost()
                            .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                    return POLineItem.builder()
                            .productId(itemDto.getProductId())
                            .quantity(itemDto.getQuantity())
                            .unitCost(itemDto.getUnitCost())
                            .totalCost(total)
                            .receivedQty(0)
                            .purchaseOrder(po)
                            .build();
                }).toList();

        po.setLineItems(items);

        BigDecimal totalAmount = items.stream()
                .map(POLineItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setTotalAmount(totalAmount);

        PurchaseOrder saved = poRepository.save(po);
        log.info("PO created with ID: {}", saved.getPoId());
        return mapToDTO(saved);
    }

    public PurchaseOrderResponseDTO getPOById(Long id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(
                        "Purchase order not found with ID: " + id));
        return mapToDTO(po);
    }

    public List<PurchaseOrderResponseDTO> getAllPOs() {
        return poRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsBySupplier(Long supplierId) {
        return poRepository.findBySupplierId(supplierId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByWarehouse(Long warehouseId) {
        return poRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByStatus(String status) {
        try {
            POStatus poStatus = POStatus.valueOf(status.toUpperCase());
            return poRepository.findByStatus(poStatus)
                    .stream().map(this::mapToDTO).toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    public List<PurchaseOrderResponseDTO> getPOsByCreatedBy(Long userId) {
        return poRepository.findByCreatedById(userId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }
        return poRepository.findByOrderDateBetween(startDate, endDate)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getOverduePOs() {
        return poRepository.findByStatusAndExpectedDateBefore(
                        POStatus.APPROVED, LocalDate.now())
                .stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public PurchaseOrderResponseDTO submitForApproval(Long id) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new InvalidPOStatusException(
                    "Only DRAFT POs can be submitted. Current: " + po.getStatus());
        }
        po.setStatus(POStatus.PENDING);
        return mapToDTO(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponseDTO approvePO(Long id) {
        log.info("Approving PO: {}", id);
        PurchaseOrder po = getPOEntity(id);

        if (po.getStatus() != POStatus.PENDING) {
            throw new InvalidPOStatusException(
                    "Only PENDING POs can be approved. Current: " + po.getStatus());
        }

        po.setStatus(POStatus.APPROVED);
        PurchaseOrder saved = poRepository.save(po);

        // ✅ PUBLISH PO_APPROVED EVENT
        try {
            poEventPublisher.publishPOApproved(
                    saved.getPoId(),
                    saved.getSupplierId(),
                    saved.getWarehouseId(),
                    saved.getCreatedById(),
                    saved.getTotalAmount(),
                    saved.getExpectedDate());
        } catch (Exception e) {
            log.error("Failed to publish PO_APPROVED event: {}", e.getMessage());
        }

        return mapToDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO rejectPO(Long id, String reason) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.PENDING) {
            throw new InvalidPOStatusException(
                    "Only PENDING POs can be rejected. Current: " + po.getStatus());
        }
        po.setStatus(POStatus.DRAFT);
        po.setNotes("REJECTED: " + reason
                + (po.getNotes() != null ? " | " + po.getNotes() : ""));
        return mapToDTO(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponseDTO cancelPO(Long id, String reason) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() == POStatus.FULLY_RECEIVED
                || po.getStatus() == POStatus.CANCELLED) {
            throw new InvalidPOStatusException(
                    "Cannot cancel a " + po.getStatus() + " PO");
        }
        po.setStatus(POStatus.CANCELLED);
        po.setNotes("CANCELLED: " + reason
                + (po.getNotes() != null ? " | " + po.getNotes() : ""));
        return mapToDTO(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponseDTO receiveGoods(Long poId,
            List<GoodsReceiptDTO> receipts) {
        PurchaseOrder po = getPOEntity(poId);

        if (po.getStatus() != POStatus.APPROVED
                && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new InvalidPOStatusException(
                    "Goods can only be received for APPROVED or PARTIALLY_RECEIVED POs.");
        }

        for (GoodsReceiptDTO receipt : receipts) {
            POLineItem lineItem = po.getLineItems().stream()
                    .filter(i -> i.getLineItemId().equals(receipt.getLineItemId()))
                    .findFirst()
                    .orElseThrow(() -> new PurchaseOrderNotFoundException(
                            "Line item not found: " + receipt.getLineItemId()));

            int newQty = lineItem.getReceivedQty() + receipt.getReceivedQty();
            if (newQty > lineItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Received qty exceeds ordered qty for product "
                        + lineItem.getProductId());
            }
            lineItem.setReceivedQty(newQty);
        }

        boolean allReceived = po.getLineItems().stream()
                .allMatch(i -> i.getReceivedQty().equals(i.getQuantity()));

        po.setStatus(allReceived
                ? POStatus.FULLY_RECEIVED : POStatus.PARTIALLY_RECEIVED);
        if (allReceived) po.setReceivedDate(LocalDate.now());

        return mapToDTO(poRepository.save(po));
    }

    @Transactional
    public PurchaseOrderResponseDTO updatePO(Long id, PurchaseOrderRequestDTO dto) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new InvalidPOStatusException(
                    "Only DRAFT POs can be updated. Current: " + po.getStatus());
        }
        po.setSupplierId(dto.getSupplierId());
        po.setWarehouseId(dto.getWarehouseId());
        po.setExpectedDate(dto.getExpectedDate());
        po.setNotes(dto.getNotes());
        po.setReferenceNumber(dto.getReferenceNumber());
        po.getLineItems().clear();
        List<POLineItem> newItems = dto.getLineItems().stream()
                .map(itemDto -> {
                    BigDecimal total = itemDto.getUnitCost()
                            .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                    return POLineItem.builder()
                            .productId(itemDto.getProductId())
                            .quantity(itemDto.getQuantity())
                            .unitCost(itemDto.getUnitCost())
                            .totalCost(total)
                            .receivedQty(0)
                            .purchaseOrder(po)
                            .build();
                }).toList();
        po.getLineItems().addAll(newItems);
        BigDecimal totalAmount = newItems.stream()
                .map(POLineItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setTotalAmount(totalAmount);
        return mapToDTO(poRepository.save(po));
    }

    // ✅ SCHEDULED: Check for overdue POs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void checkOverduePOs() {
        log.info("Running scheduled overdue PO check");
        List<PurchaseOrder> overduePOs = poRepository
                .findByStatusAndExpectedDateBefore(
                        POStatus.APPROVED, LocalDate.now());

        for (PurchaseOrder po : overduePOs) {
            try {
                poEventPublisher.publishPOOverdue(
                        po.getPoId(),
                        po.getSupplierId(),
                        po.getWarehouseId(),
                        po.getExpectedDate());
            } catch (Exception e) {
                log.error("Failed to publish PO_OVERDUE for PO {}: {}",
                        po.getPoId(), e.getMessage());
            }
        }

        log.info("Overdue PO check complete. Found {} overdue POs",
                overduePOs.size());
    }

    private PurchaseOrder getPOEntity(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(
                        "Purchase order not found with ID: " + id));
    }

    private PurchaseOrderResponseDTO mapToDTO(PurchaseOrder po) {
        List<POLineItemResponseDTO> lineItems = po.getLineItems().stream()
                .map(item -> POLineItemResponseDTO.builder()
                        .lineItemId(item.getLineItemId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .totalCost(item.getTotalCost())
                        .receivedQty(item.getReceivedQty())
                        .build())
                .toList();

        return PurchaseOrderResponseDTO.builder()
                .poId(po.getPoId())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .createdById(po.getCreatedById())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .orderDate(po.getOrderDate())
                .expectedDate(po.getExpectedDate())
                .receivedDate(po.getReceivedDate())
                .notes(po.getNotes())
                .referenceNumber(po.getReferenceNumber())
                .createdAt(po.getCreatedAt())
                .lineItems(lineItems)
                .build();
    }
}