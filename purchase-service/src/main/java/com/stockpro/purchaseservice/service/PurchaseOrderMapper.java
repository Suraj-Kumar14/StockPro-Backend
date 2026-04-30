package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.PurchaseOrderResponseDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderResponseDTO.POLineItemResponseDTO;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PurchaseOrderMapper {

    public PurchaseOrderResponseDTO toResponse(PurchaseOrder purchaseOrder) {
        List<POLineItemResponseDTO> lineItems = purchaseOrder.getLineItems().stream()
                .map(this::toLineItemResponse)
                .toList();

        return PurchaseOrderResponseDTO.builder()
                .poId(purchaseOrder.getPoId())
                .supplierId(purchaseOrder.getSupplierId())
                .warehouseId(purchaseOrder.getWarehouseId())
                .createdById(purchaseOrder.getCreatedById())
                .status(purchaseOrder.getStatus())
                .totalAmount(purchaseOrder.getTotalAmount())
                .orderDate(purchaseOrder.getOrderDate())
                .expectedDate(purchaseOrder.getExpectedDate())
                .receivedDate(purchaseOrder.getReceivedDate())
                .notes(purchaseOrder.getNotes())
                .referenceNumber(purchaseOrder.getReferenceNumber())
                .createdAt(purchaseOrder.getCreatedAt())
                .lineItems(lineItems)
                .build();
    }

    private POLineItemResponseDTO toLineItemResponse(POLineItem lineItem) {
        return POLineItemResponseDTO.builder()
                .lineItemId(lineItem.getLineItemId())
                .productId(lineItem.getProductId())
                .quantity(lineItem.getQuantity())
                .unitCost(lineItem.getUnitCost())
                .totalCost(lineItem.getTotalCost())
                .receivedQty(lineItem.getReceivedQty())
                .build();
    }
}
