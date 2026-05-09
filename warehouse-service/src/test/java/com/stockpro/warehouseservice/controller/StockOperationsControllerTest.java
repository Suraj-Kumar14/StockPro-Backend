package com.stockpro.warehouseservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouseservice.dto.AcknowledgeAlertRequestDTO;
import com.stockpro.warehouseservice.dto.BarcodeStockLookupResponseDTO;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.dto.StockAlertResponseDTO;
import com.stockpro.warehouseservice.dto.StockAuditRequestDTO;
import com.stockpro.warehouseservice.dto.StockAuditResponseDTO;
import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.dto.StockMovementResponseDTO;
import com.stockpro.warehouseservice.exception.GlobalExceptionHandler;
import com.stockpro.warehouseservice.service.StockAlertService;
import com.stockpro.warehouseservice.service.StockAuditService;
import com.stockpro.warehouseservice.service.StockBarcodeService;
import com.stockpro.warehouseservice.service.StockMovementService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {StockOperationsController.class, StockLevelController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StockOperationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockMovementService stockMovementService;

    @MockBean
    private StockAuditService stockAuditService;

    @MockBean
    private StockBarcodeService stockBarcodeService;

    @MockBean
    private StockAlertService stockAlertService;

    @MockBean
    private com.stockpro.warehouseservice.service.StockLevelService stockLevelService;

    @Test
    void getMovementHistory_shouldFilterByWarehouseAndProduct() throws Exception {
        when(stockMovementService.getMovementHistory(1L, 101L)).thenReturn(List.of(
                StockMovementResponseDTO.builder()
                        .movementId(9L)
                        .warehouseId(1L)
                        .productId(101L)
                        .movementType("RECEIPT")
                        .quantityChanged(20)
                        .previousQuantity(10)
                        .newQuantity(30)
                        .createdAt(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/stock/movements")
                        .param("warehouseId", "1")
                        .param("productId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementId").value(9))
                .andExpect(jsonPath("$[0].movementType").value("RECEIPT"));
    }

    @Test
    void performAudit_shouldReturnAuditResult() throws Exception {
        StockAuditRequestDTO request = new StockAuditRequestDTO();
        request.setWarehouseId(1L);
        request.setProductId(101L);
        request.setCountedQuantity(18);
        request.setReason("Cycle count");

        when(stockAuditService.performAudit(any(StockAuditRequestDTO.class)))
                .thenReturn(StockAuditResponseDTO.builder()
                        .warehouseId(1L)
                        .productId(101L)
                        .systemQuantity(25)
                        .countedQuantity(18)
                        .discrepancy(-7)
                        .reason("Cycle count")
                        .build());

        mockMvc.perform(post("/stock/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discrepancy").value(-7));
    }

    @Test
    void performAudit_shouldReturnBadRequestWhenReasonMissing() throws Exception {
        StockAuditRequestDTO request = new StockAuditRequestDTO();
        request.setWarehouseId(1L);
        request.setProductId(101L);
        request.setCountedQuantity(18);

        mockMvc.perform(post("/stock/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("Reason is required"));
    }

    @Test
    void lookupByBarcodeAndAlerts_shouldReturnPayloads() throws Exception {
        when(stockBarcodeService.lookupByBarcode("BAR-10")).thenReturn(
                BarcodeStockLookupResponseDTO.builder()
                        .product(ProductLookupResponseDTO.builder()
                                .productId(10L)
                                .name("Scanner")
                                .barcode("BAR-10")
                                .build())
                        .stockLevels(List.of(StockLevelResponseDTO.builder()
                                .warehouseId(1L)
                                .productId(10L)
                                .quantity(20)
                                .reservedQuantity(2)
                                .availableQuantity(18)
                                .build()))
                        .build());
        when(stockAlertService.getActiveAlerts()).thenReturn(List.of(
                StockAlertResponseDTO.builder()
                        .alertId(7L)
                        .alertType("LOW_STOCK")
                        .message("Low stock threshold reached")
                        .active(true)
                        .build()));

        mockMvc.perform(get("/stock/barcode/BAR-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.name").value("Scanner"))
                .andExpect(jsonPath("$.stockLevels[0].availableQuantity").value(18));

        mockMvc.perform(get("/stock/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertId").value(7));
    }

    @Test
    void acknowledgeAlert_shouldValidateRequestBody() throws Exception {
        AcknowledgeAlertRequestDTO request = new AcknowledgeAlertRequestDTO();

        mockMvc.perform(post("/stock/alerts/9/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.acknowledgedBy").value("Acknowledged by is required"));
    }

    @Test
    void acknowledgeAlertAndStockLevelEndpoints_shouldDelegate() throws Exception {
        AcknowledgeAlertRequestDTO acknowledgeRequest = new AcknowledgeAlertRequestDTO();
        acknowledgeRequest.setAcknowledgedBy("warehouse.user");

        when(stockAlertService.acknowledgeAlert(9L, "warehouse.user")).thenReturn(
                StockAlertResponseDTO.builder()
                        .alertId(9L)
                        .acknowledged(true)
                        .acknowledgedBy("warehouse.user")
                        .build());
        when(stockLevelService.getStockLevel(1L, 10L)).thenReturn(StockLevelResponseDTO.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(10L)
                .quantity(12)
                .reservedQuantity(2)
                .availableQuantity(10)
                .binLocation("A-1")
                .build());
        when(stockLevelService.getStockByProduct(10L)).thenReturn(List.of(StockLevelResponseDTO.builder()
                .warehouseId(1L)
                .productId(10L)
                .quantity(12)
                .reservedQuantity(2)
                .availableQuantity(10)
                .build()));
        when(stockLevelService.getLowStockItems(8)).thenReturn(List.of(StockLevelResponseDTO.builder()
                .warehouseId(2L)
                .productId(11L)
                .quantity(5)
                .reservedQuantity(1)
                .availableQuantity(4)
                .build()));

        mockMvc.perform(post("/stock/alerts/9/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acknowledgeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true));

        mockMvc.perform(get("/stock/warehouse/1/product/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(10));

        mockMvc.perform(get("/stock/product/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(10));

        mockMvc.perform(get("/stock/low-stock").param("threshold", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableQuantity").value(4));

        verify(stockAlertService).acknowledgeAlert(9L, "warehouse.user");
        verify(stockLevelService).getStockLevel(1L, 10L);
        verify(stockLevelService).getStockByProduct(10L);
        verify(stockLevelService).getLowStockItems(eq(8));
    }

    @Test
    void reserveAndTransferEndpoints_shouldRejectInvalidParameters() throws Exception {
        mockMvc.perform(post("/stock/reserve")
                        .param("warehouseId", "1")
                        .param("productId", "10")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quantity must be at least 1")));

        mockMvc.perform(post("/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromWarehouseId\":1,\"toWarehouseId\":2,\"productId\":10,\"quantity\":0,\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").value("Transfer quantity must be at least 1"));
    }
}
