package com.stockpro.warehouseservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouseservice.dto.request.AdjustStockRequest;
import com.stockpro.warehouseservice.dto.request.CreateStockLevelRequest;
import com.stockpro.warehouseservice.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouseservice.dto.request.ReserveStockRequest;
import com.stockpro.warehouseservice.dto.request.StockIssueRequest;
import com.stockpro.warehouseservice.dto.request.StockReceiptRequest;
import com.stockpro.warehouseservice.dto.request.TransferStockRequest;
import com.stockpro.warehouseservice.dto.request.UpdateStockRequest;
import com.stockpro.warehouseservice.dto.response.StockLevelResponse;
import com.stockpro.warehouseservice.dto.response.StockSummaryResponse;
import com.stockpro.warehouseservice.dto.response.TransferStockResponse;
import com.stockpro.warehouseservice.enums.TransferStatus;
import com.stockpro.warehouseservice.exception.GlobalExceptionHandler;
import com.stockpro.warehouseservice.security.AuthenticatedUser;
import com.stockpro.warehouseservice.service.StockManagementService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

@WebMvcTest(controllers = StockApiV1Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StockApiV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StockManagementService stockManagementService;

    @Test
    void createStockLevel_shouldReturnCreatedAndPassActorId() throws Exception {
        CreateStockLevelRequest request = new CreateStockLevelRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(25);
        request.setReservedQuantity(5);
        request.setLocationCode("BIN-A1");

        StockLevelResponse response = stockResponse(11L, 1L, 100L, 25, 5, "BIN-A1");
        when(stockManagementService.createStockLevel(any(CreateStockLevelRequest.class), isNull()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/stocks")
                        .with(authentication(new TestingAuthenticationToken(
                                new AuthenticatedUser(77L, "manager@stockpro.com", "MANAGER", "token"),
                                null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateStockLevelRequest> captor = ArgumentCaptor.forClass(CreateStockLevelRequest.class);
        verify(stockManagementService).createStockLevel(captor.capture(), isNull());
        Assertions.assertEquals(100L, captor.getValue().getProductId());
    }

    @Test
    void createStockLevel_shouldReturnBadRequestWhenBodyIsInvalid() throws Exception {
        CreateStockLevelRequest request = new CreateStockLevelRequest();
        request.setWarehouseId(1L);
        request.setQuantity(-1);

        mockMvc.perform(post("/api/v1/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void searchStock_shouldForwardAllFiltersAndReturnPage() throws Exception {
        when(stockManagementService.searchStock(1L, 100L, "bin", true, false, 2, 5))
                .thenReturn(new PageImpl<>(List.of(stockResponse(21L, 1L, 100L, 40, 2, "BIN-1"))));

        mockMvc.perform(get("/api/v1/stocks")
                        .param("warehouseId", "1")
                        .param("productId", "100")
                        .param("locationCode", "bin")
                        .param("lowStockOnly", "true")
                        .param("overstockOnly", "false")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].stockId").value(21))
                .andExpect(jsonPath("$.content[0].locationCode").value("BIN-1"));
    }

    @Test
    void receiveStock_shouldPassNullActorWhenAuthenticationMissing() throws Exception {
        StockReceiptRequest request = new StockReceiptRequest();
        request.setWarehouseId(1L);
        request.setProductId(200L);
        request.setQuantity(10);
        request.setReason("PO receipt");

        when(stockManagementService.receiveStock(any(StockReceiptRequest.class), isNull()))
                .thenReturn(stockResponse(30L, 1L, 200L, 10, 0, "RECV-1"));

        mockMvc.perform(post("/api/v1/stocks/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(200))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void transferStock_shouldReturnTransferResponse() throws Exception {
        TransferStockRequest request = new TransferStockRequest();
        request.setSourceWarehouseId(1L);
        request.setDestinationWarehouseId(2L);
        request.setProductId(300L);
        request.setQuantity(15);
        request.setReasonCode("REBALANCE");
        request.setNotes("Move stock");

        when(stockManagementService.transferStock(any(TransferStockRequest.class), isNull()))
                .thenReturn(TransferStockResponse.builder()
                        .productId(300L)
                        .sourceWarehouseId(1L)
                        .destinationWarehouseId(2L)
                        .quantity(15)
                        .sourceBalanceAfter(25)
                        .destinationBalanceAfter(35)
                        .status(TransferStatus.COMPLETED)
                        .message("Stock transferred successfully")
                        .transferredAt(LocalDateTime.now())
                        .build());

        mockMvc.perform(post("/api/v1/stocks/transfer")
                        .with(authentication(new TestingAuthenticationToken(
                                new AuthenticatedUser(88L, "staff@stockpro.com", "STAFF", "token"),
                                null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(stockManagementService).transferStock(any(TransferStockRequest.class), isNull());
    }

    @Test
    void issueStock_shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
        StockIssueRequest request = new StockIssueRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(0);

        mockMvc.perform(post("/api/v1/stocks/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void reserveReleaseAdjustUpdateAndSummaryEndpoints_shouldDelegate() throws Exception {
        ReserveStockRequest reserveRequest = new ReserveStockRequest();
        reserveRequest.setWarehouseId(1L);
        reserveRequest.setProductId(100L);
        reserveRequest.setQuantity(3);

        ReleaseReservationRequest releaseRequest = new ReleaseReservationRequest();
        releaseRequest.setWarehouseId(1L);
        releaseRequest.setProductId(100L);
        releaseRequest.setQuantity(2);

        AdjustStockRequest adjustRequest = new AdjustStockRequest();
        adjustRequest.setWarehouseId(1L);
        adjustRequest.setProductId(100L);
        adjustRequest.setNewQuantity(50);

        UpdateStockRequest updateRequest = new UpdateStockRequest();
        updateRequest.setWarehouseId(1L);
        updateRequest.setProductId(100L);
        updateRequest.setQuantity(60);

        StockLevelResponse response = stockResponse(41L, 1L, 100L, 60, 4, "BIN-Z9");
        when(stockManagementService.reserveStock(any(ReserveStockRequest.class), isNull())).thenReturn(response);
        when(stockManagementService.releaseReservation(any(ReleaseReservationRequest.class), isNull())).thenReturn(response);
        when(stockManagementService.adjustStock(any(AdjustStockRequest.class), isNull())).thenReturn(response);
        when(stockManagementService.updateStock(any(UpdateStockRequest.class), isNull())).thenReturn(response);
        when(stockManagementService.getLowStockItems()).thenReturn(List.of(response));
        when(stockManagementService.getOverstockItems()).thenReturn(List.of(response));
        when(stockManagementService.getStockSummary()).thenReturn(StockSummaryResponse.builder()
                .totalStockItems(3)
                .totalQuantity(200)
                .totalReservedQuantity(10)
                .totalAvailableQuantity(190)
                .lowStockItemsCount(1)
                .overstockItemsCount(1)
                .build());
        when(stockManagementService.getStockByWarehouse(1L, 0, 10))
                .thenReturn(new PageImpl<>(List.of(response)));
        when(stockManagementService.getStockByProduct(100L, 0, 10))
                .thenReturn(new PageImpl<>(List.of(response)));
        when(stockManagementService.getStockLevel(1L, 100L)).thenReturn(response);

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                new AuthenticatedUser(55L, "admin@stockpro.com", "ADMIN", "token"),
                null);

        mockMvc.perform(post("/api/v1/stocks/reserve").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stocks/release").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(releaseRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stocks/adjust").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stocks/update").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stocks/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockId").value(41));

        mockMvc.perform(get("/api/v1/stocks/overstock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockId").value(41));

        mockMvc.perform(get("/api/v1/stocks/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(200));

        mockMvc.perform(get("/api/v1/stocks/warehouse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].warehouseId").value(1));

        mockMvc.perform(get("/api/v1/stocks/product/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(100));

        mockMvc.perform(get("/api/v1/stocks/warehouse/1/product/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationCode").value("BIN-Z9"));

        verify(stockManagementService).getStockByWarehouse(1L, 0, 10);
        verify(stockManagementService).getStockByProduct(100L, 0, 10);
        verify(stockManagementService).getStockLevel(1L, 100L);
        verify(stockManagementService).reserveStock(any(ReserveStockRequest.class), isNull());
        verify(stockManagementService).releaseReservation(any(ReleaseReservationRequest.class), isNull());
        verify(stockManagementService).adjustStock(any(AdjustStockRequest.class), isNull());
        verify(stockManagementService).updateStock(any(UpdateStockRequest.class), isNull());
    }

    private StockLevelResponse stockResponse(
            Long stockId,
            Long warehouseId,
            Long productId,
            Integer quantity,
            Integer reservedQuantity,
            String locationCode) {
        return StockLevelResponse.builder()
                .stockId(stockId)
                .warehouseId(warehouseId)
                .warehouseName("Main Warehouse")
                .productId(productId)
                .productName("Sample Product")
                .sku("SKU-" + productId)
                .quantity(quantity)
                .reservedQuantity(reservedQuantity)
                .availableQuantity(quantity - reservedQuantity)
                .locationCode(locationCode)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
