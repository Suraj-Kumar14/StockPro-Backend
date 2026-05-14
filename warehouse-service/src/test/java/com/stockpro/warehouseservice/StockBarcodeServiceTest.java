package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.client.ProductCatalogClient;
import com.stockpro.warehouseservice.dto.BarcodeStockLookupResponseDTO;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.service.StockBarcodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBarcodeServiceTest {

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @InjectMocks
    private StockBarcodeService stockBarcodeService;

    @Test
    void lookupByBarcode_shouldReturnProductAndStockLevels_whenBarcodeExists() {
        ProductLookupResponseDTO product = ProductLookupResponseDTO.builder()
                .productId(101L)
                .sku("SKU-101")
                .name("Scanner Gun")
                .barcode("BAR-101")
                .reorderLevel(10)
                .maxStockLevel(200)
                .build();

        StockLevel stock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(5L)
                .productId(101L)
                .quantity(70)
                .reservedQuantity(20)
                .binLocation("R1-B5")
                .build();

        when(productCatalogClient.getProductByBarcode("BAR-101")).thenReturn(product);
        when(stockLevelRepository.findByProductId(101L)).thenReturn(List.of(stock));

        BarcodeStockLookupResponseDTO result = stockBarcodeService.lookupByBarcode("BAR-101");

        assertEquals("Scanner Gun", result.getProduct().getName());
        assertEquals(1, result.getStockLevels().size());
        assertEquals(50, result.getStockLevels().get(0).getAvailableQuantity());
    }

    @Test
    void lookupByBarcode_shouldPropagateLookupError_whenProductServiceFails() {
        when(productCatalogClient.getProductByBarcode("UNKNOWN"))
                .thenThrow(new ProductLookupException("Product not found with barcode: UNKNOWN"));

        ProductLookupException exception = assertThrows(
                ProductLookupException.class,
                () -> stockBarcodeService.lookupByBarcode("UNKNOWN")
        );

        assertEquals("Product not found with barcode: UNKNOWN", exception.getMessage());
    }
}
