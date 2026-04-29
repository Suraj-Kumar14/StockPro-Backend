package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.client.ProductCatalogClient;
import com.stockpro.warehouseservice.dto.BarcodeStockLookupResponseDTO;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockBarcodeService {

    @Autowired
    private ProductCatalogClient productCatalogClient;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    public BarcodeStockLookupResponseDTO lookupByBarcode(String barcode) {
        ProductLookupResponseDTO product = productCatalogClient.getProductByBarcode(barcode);
        List<StockLevelResponseDTO> stockLevels = stockLevelRepository
                .findByProductId(product.getProductId())
                .stream()
                .map(this::mapToDto)
                .toList();

        return BarcodeStockLookupResponseDTO.builder()
                .product(product)
                .stockLevels(stockLevels)
                .build();
    }

    private StockLevelResponseDTO mapToDto(StockLevel stock) {
        return StockLevelResponseDTO.builder()
                .stockId(stock.getStockId())
                .warehouseId(stock.getWarehouseId())
                .productId(stock.getProductId())
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(stock.getAvailableQuantity())
                .binLocation(stock.getBinLocation())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }
}
