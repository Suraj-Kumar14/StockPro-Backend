package com.stockpro.warehouseservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouseservice.controller.StockLevelController;
import com.stockpro.warehouseservice.controller.WarehouseController;
import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.dto.StockTransferDTO;
import com.stockpro.warehouseservice.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.exception.GlobalExceptionHandler;
import com.stockpro.warehouseservice.exception.InsufficientStockException;
import com.stockpro.warehouseservice.service.StockLevelService;
import com.stockpro.warehouseservice.service.WarehouseManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {WarehouseController.class, StockLevelController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseManagementService warehouseManagementService;

    @MockBean
    private StockLevelService stockLevelService;

    @Test
    void createWarehouse_shouldReturnCreated_whenRequestIsValid() throws Exception {
        CreateWarehouseRequest request = new CreateWarehouseRequest();
        request.setName("Central Warehouse");
        request.setCode("WH-CENTRAL");
        request.setLocation("Delhi");
        request.setAddress("Sector 12");
        request.setCity("Delhi");
        request.setState("Delhi");
        request.setCountry("India");
        request.setManagerId(7L);
        request.setCapacity(500);
        request.setPhone("9876543210");

        WarehouseResponse response = WarehouseResponse.builder()
                .warehouseId(1L)
                .name("Central Warehouse")
                .code("WH-CENTRAL")
                .location("Delhi")
                .city("Delhi")
                .state("Delhi")
                .country("India")
                .capacity(500)
                .isActive(true)
                .build();

        when(warehouseManagementService.createWarehouse(any(CreateWarehouseRequest.class), isNull())).thenReturn(response);

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").value(1L))
                .andExpect(jsonPath("$.name").value("Central Warehouse"))
                .andExpect(jsonPath("$.location").value("Delhi"));
    }

    @Test
    void getStockLevels_shouldReturnStockList_whenWarehouseHasInventory() throws Exception {
        List<StockLevelResponseDTO> stock = List.of(
                StockLevelResponseDTO.builder()
                        .stockId(11L)
                        .warehouseId(1L)
                        .productId(101L)
                        .quantity(150)
                        .reservedQuantity(25)
                        .availableQuantity(125)
                        .binLocation("A-01")
                        .build()
        );

        when(stockLevelService.getStockByWarehouse(1L)).thenReturn(stock);

        mockMvc.perform(get("/stock/warehouse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(101L))
                .andExpect(jsonPath("$[0].availableQuantity").value(125));
    }

    @Test
    void getStockLevels_shouldReturnEmptyList_whenWarehouseHasNoInventory() throws Exception {
        when(stockLevelService.getStockByWarehouse(99L)).thenReturn(List.of());

        mockMvc.perform(get("/stock/warehouse/99"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void transferStock_shouldSucceed_whenRequestIsValid() throws Exception {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(101L);
        request.setQuantity(20);
        request.setReason("Rebalancing inventory");

        mockMvc.perform(post("/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Stock transferred successfully"));

        verify(stockLevelService).transferStock(any(StockTransferDTO.class));
    }

    @Test
    void transferStock_shouldFail_whenInsufficientStock() throws Exception {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(101L);
        request.setQuantity(2000);
        request.setReason("Emergency transfer");

        doThrow(new InsufficientStockException("Insufficient stock in source warehouse. Available: 50, Requested: 2000"))
                .when(stockLevelService)
                .transferStock(any(StockTransferDTO.class));

        mockMvc.perform(post("/stock/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock in source warehouse. Available: 50, Requested: 2000"));
    }

    @Test
    void getWarehouseById_shouldReturnWarehouse_whenWarehouseExists() throws Exception {
        WarehouseResponse response = WarehouseResponse.builder()
                .warehouseId(5L)
                .name("South Hub")
                .code("WH-SOUTH")
                .location("Chennai")
                .city("Chennai")
                .state("Tamil Nadu")
                .country("India")
                .capacity(300)
                .isActive(true)
                .build();

        when(warehouseManagementService.getWarehouseById(eq(5L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/warehouses/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warehouseId").value(5L))
                .andExpect(jsonPath("$.name").value("South Hub"));
    }
}
