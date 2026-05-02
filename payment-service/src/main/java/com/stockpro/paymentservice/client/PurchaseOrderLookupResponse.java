package com.stockpro.paymentservice.client;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PurchaseOrderLookupResponse {
    private Long poId;
    private Long purchaseOrderId;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private String status;
    private BigDecimal totalAmount;
    private Long createdBy;
    private Long createdById;
}
