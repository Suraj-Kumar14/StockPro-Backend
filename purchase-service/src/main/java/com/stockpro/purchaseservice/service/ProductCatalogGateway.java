package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;

public interface ProductCatalogGateway {

    StockProductThresholdDTO getProductThresholds(Long productId);
}
