package com.stockpro.warehouseservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.dto.StockUpdateDTO;
import com.stockpro.warehouseservice.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.dto.response.WarehouseSummaryResponse;
import com.stockpro.warehouseservice.exception.GlobalExceptionHandler;
import com.stockpro.warehouseservice.service.StockLevelService;
import com.stockpro.warehouseservice.service.WarehouseManagementService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {WarehouseController.class, StockLevelController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WarehouseControllerExpandedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseManagementService warehouseManagementService;

    @MockBean
    private StockLevelService stockLevelService;

    @Test
    void warehouseEndpoints_shouldCoverListLookupMutationsAndSummary() throws Exception {
        WarehouseResponse response = warehouseResponse(3L, "West Hub", "WH-WEST");
        when(warehouseManagementService.getAllWarehouses(true, "hub", "ACTIVE", "Pune", "Maharashtra", 1, 5, "name", "desc"))
                .thenReturn(new PageImpl<>(List.of(response)));
        when(warehouseManagementService.getActiveWarehouses()).thenReturn(List.of(response));
        when(warehouseManagementService.getWarehouseByCode("WH-WEST")).thenReturn(response);
        when(warehouseManagementService.updateWarehouse(eq(3L), any(UpdateWarehouseRequest.class), isNull())).thenReturn(response);
        when(warehouseManagementService.deactivateWarehouse(3L, null)).thenReturn(response);
        when(warehouseManagementService.activateWarehouse(3L, null)).thenReturn(response);
        when(warehouseManagementService.getWarehousesByManager(9L)).thenReturn(List.of(response));
        when(warehouseManagementService.assignManager(3L, 9L, null)).thenReturn(response);
        when(warehouseManagementService.getWarehouseSummary()).thenReturn(WarehouseSummaryResponse.builder()
                .totalWarehouses(4)
                .activeWarehouses(3)
                .inactiveWarehouses(1)
                .totalCapacity(1000)
                .usedCapacity(300)
                .availableCapacity(700)
                .averageUtilizationPercentage(30.0)
                .build());

        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest();
        updateRequest.setName("West Hub");
        updateRequest.setCode("WH-WEST");
        updateRequest.setLocation("Pune");
        updateRequest.setCity("Pune");
        updateRequest.setState("Maharashtra");
        updateRequest.setCountry("India");
        updateRequest.setCapacity(400);
        updateRequest.setPhone("9876543210");

        mockMvc.perform(get("/api/v1/warehouses")
                        .param("isActive", "true")
                        .param("search", "hub")
                        .param("status", "ACTIVE")
                        .param("city", "Pune")
                        .param("state", "Maharashtra")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].warehouseCode").value("WH-WEST"));

        mockMvc.perform(get("/api/v1/warehouses/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseId").value(3));

        mockMvc.perform(get("/api/v1/warehouses/code/WH-WEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Pune"));

        mockMvc.perform(put("/api/v1/warehouses/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId").value(3));

        mockMvc.perform(patch("/api/v1/warehouses/3/deactivate"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/warehouses/3/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/warehouses/manager/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].managerId").value(9));

        mockMvc.perform(put("/api/v1/warehouses/3/manager/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId").value(3));

        mockMvc.perform(get("/api/v1/warehouses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageUtilizationPercentage").value(30.0));

        verify(warehouseManagementService).updateWarehouse(eq(3L), any(UpdateWarehouseRequest.class), isNull());
        verify(warehouseManagementService).assignManager(3L, 9L, null);
    }

    @Test
    void stockLevelControllerEndpoints_shouldCoverRemainingPaths() throws Exception {
        StockLevelResponseDTO dto = StockLevelResponseDTO.builder()
                .stockId(44L)
                .warehouseId(2L)
                .productId(88L)
                .quantity(50)
                .reservedQuantity(5)
                .availableQuantity(45)
                .binLocation("BIN-8")
                .build();
        when(stockLevelService.getStockLevel(2L, 88L)).thenReturn(dto);
        when(stockLevelService.getStockByProduct(88L)).thenReturn(List.of(dto));
        when(stockLevelService.updateStock(eq(2L), any(StockUpdateDTO.class))).thenReturn(dto);
        when(stockLevelService.getLowStockItems(10)).thenReturn(List.of(dto));

        StockUpdateDTO update = new StockUpdateDTO();
        update.setProductId(88L);
        update.setQuantity(50);
        update.setBinLocation("BIN-8");

        mockMvc.perform(get("/stock/warehouse/2/product/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(45));

        mockMvc.perform(get("/stock/product/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].binLocation").value("BIN-8"));

        mockMvc.perform(put("/stock/warehouse/2/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(88));

        mockMvc.perform(post("/stock/reserve")
                        .param("warehouseId", "2")
                        .param("productId", "88")
                        .param("quantity", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string("Stock reserved successfully"));

        mockMvc.perform(post("/stock/release")
                        .param("warehouseId", "2")
                        .param("productId", "88")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Reservation released successfully"));

        mockMvc.perform(get("/stock/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockId").value(44));
    }

    @Test
    void warehouseAndStockValidation_shouldReturnBadRequest() throws Exception {
        UpdateWarehouseRequest badWarehouse = new UpdateWarehouseRequest();
        badWarehouse.setName("");
        badWarehouse.setCode("");
        badWarehouse.setLocation("");
        badWarehouse.setCity("");
        badWarehouse.setState("");
        badWarehouse.setCountry("");
        badWarehouse.setCapacity(0);
        badWarehouse.setPhone("123");

        StockUpdateDTO badStock = new StockUpdateDTO();
        badStock.setProductId(88L);
        badStock.setQuantity(-1);

        mockMvc.perform(put("/api/v1/warehouses/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badWarehouse)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.capacity").exists())
                .andExpect(jsonPath("$.phone").exists());

        mockMvc.perform(put("/stock/warehouse/2/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badStock)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").exists());
    }

    private WarehouseResponse warehouseResponse(Long id, String name, String code) {
        return WarehouseResponse.builder()
                .warehouseId(id)
                .name(name)
                .code(code)
                .location("Pune")
                .address("Industrial Area")
                .city("Pune")
                .state("Maharashtra")
                .country("India")
                .managerId(9L)
                .capacity(400)
                .usedCapacity(120)
                .availableCapacity(280)
                .utilizationPercentage(30.0)
                .isActive(true)
                .phone("9876543210")
                .build();
    }
}
