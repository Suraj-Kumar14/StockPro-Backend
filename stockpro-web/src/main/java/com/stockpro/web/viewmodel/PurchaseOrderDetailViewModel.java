package com.stockpro.web.viewmodel;

import com.stockpro.web.dto.response.ProductResponse;
import com.stockpro.web.dto.response.PurchaseOrderResponse;
import com.stockpro.web.dto.response.SupplierResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import java.util.Map;

public record PurchaseOrderDetailViewModel(
        PurchaseOrderResponse purchaseOrder,
        SupplierResponse supplier,
        WarehouseResponse warehouse,
        Map<Long, ProductResponse> productsById,
        boolean editSupported) {
}
