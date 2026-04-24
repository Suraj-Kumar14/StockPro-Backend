package com.stockpro.purchase.service;

import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseService {

    PurchaseOrder createPO(PurchaseOrder purchaseOrder);

    PurchaseOrder approvePO(Long poId);

    PurchaseOrder receiveGoods(Long poId, List<POLineItem> receivedItems);

    List<PurchaseOrder> getPOsByStatus(String status);

    List<PurchaseOrder> getPOsByDateRange(LocalDate start, LocalDate end);

    PurchaseOrder cancelPO(Long poId);
}
