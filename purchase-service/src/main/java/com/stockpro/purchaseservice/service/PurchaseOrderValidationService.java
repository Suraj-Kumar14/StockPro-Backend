package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.GoodsReceiptDTO;
import com.stockpro.purchaseservice.dto.POLineItemDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderRequestDTO;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.exception.InvalidLineItemException;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.exception.OverReceiptException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PurchaseOrderValidationService {

    public void validatePurchaseOrderRequest(PurchaseOrderRequestDTO dto) {
        if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
            throw new InvalidPOStateException("At least one line item is required");
        }

        Set<Long> productIds = new HashSet<>();
        for (POLineItemDTO lineItem : dto.getLineItems()) {
            validateLineItem(lineItem);
            if (!productIds.add(lineItem.getProductId())) {
                throw new InvalidLineItemException(
                        "Duplicate product in purchase order: " + lineItem.getProductId());
            }
        }
    }

    public void validateGoodsReceipts(List<GoodsReceiptDTO> receipts) {
        if (receipts == null || receipts.isEmpty()) {
            throw new InvalidPOStateException("At least one goods receipt line is required");
        }

        Set<Long> lineItemIds = new HashSet<>();
        for (GoodsReceiptDTO receipt : receipts) {
            if (!lineItemIds.add(receipt.getLineItemId())) {
                throw new InvalidLineItemException(
                        "Duplicate receipt line item: " + receipt.getLineItemId());
            }
        }
    }

    public void validateReceiptQuantity(POLineItem lineItem, Integer incomingQuantity) {
        if (incomingQuantity == null || incomingQuantity < 1) {
            throw new InvalidLineItemException("Received quantity must be greater than zero");
        }

        int updatedReceivedQty = defaultIfNull(lineItem.getReceivedQty()) + incomingQuantity;
        if (updatedReceivedQty > defaultIfNull(lineItem.getQuantity())) {
            throw new OverReceiptException(
                    "Received quantity exceeds ordered quantity for product "
                            + lineItem.getProductId());
        }
    }

    private void validateLineItem(POLineItemDTO lineItem) {
        if (lineItem.getProductId() == null) {
            throw new InvalidLineItemException("Product ID is required");
        }
        if (lineItem.getQuantity() == null || lineItem.getQuantity() < 1) {
            throw new InvalidLineItemException("Line item quantity must be greater than zero");
        }
        if (lineItem.getUnitCost() == null || lineItem.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidLineItemException("Line item unit cost cannot be negative");
        }
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
