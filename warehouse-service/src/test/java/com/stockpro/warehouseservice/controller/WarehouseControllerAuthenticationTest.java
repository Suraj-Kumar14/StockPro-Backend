package com.stockpro.warehouseservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.warehouseservice.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouseservice.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.security.AuthenticatedUser;
import com.stockpro.warehouseservice.service.WarehouseManagementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerAuthenticationTest {

    @Mock
    private WarehouseManagementService warehouseManagementService;

    private WarehouseController warehouseController;

    @BeforeEach
    void setUp() {
        warehouseController = new WarehouseController(warehouseManagementService);
    }

    @Test
    void warehouseMutations_shouldPassAuthenticatedUserIdToService() {
        WarehouseResponse response = warehouseResponse();
        when(warehouseManagementService.createWarehouse(any(CreateWarehouseRequest.class), eq(77L))).thenReturn(response);
        when(warehouseManagementService.updateWarehouse(eq(3L), any(UpdateWarehouseRequest.class), eq(77L))).thenReturn(response);
        when(warehouseManagementService.deactivateWarehouse(3L, 77L)).thenReturn(response);
        when(warehouseManagementService.activateWarehouse(3L, 77L)).thenReturn(response);
        when(warehouseManagementService.assignManager(3L, 9L, 77L)).thenReturn(response);

        CreateWarehouseRequest createRequest = new CreateWarehouseRequest();
        createRequest.setName("West Hub");
        createRequest.setCode("WH-WEST");
        createRequest.setLocation("Pune");
        createRequest.setAddress("Industrial Area");
        createRequest.setCity("Pune");
        createRequest.setState("Maharashtra");
        createRequest.setCountry("India");
        createRequest.setManagerId(9L);
        createRequest.setCapacity(400);
        createRequest.setPhone("9876543210");

        UpdateWarehouseRequest updateRequest = new UpdateWarehouseRequest();
        updateRequest.setName("West Hub");
        updateRequest.setCode("WH-WEST");
        updateRequest.setLocation("Pune");
        updateRequest.setAddress("Industrial Area");
        updateRequest.setCity("Pune");
        updateRequest.setState("Maharashtra");
        updateRequest.setCountry("India");
        updateRequest.setCapacity(450);
        updateRequest.setPhone("9876543210");

        var auth = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(77L, "manager@stockpro.com", "MANAGER", "masked"),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

        var createResponse = warehouseController.createWarehouse(createRequest, auth);
        var updateResponse = warehouseController.updateWarehouse(3L, updateRequest, auth);
        var deactivateResponse = warehouseController.deactivateWarehouse(3L, auth);
        var activateResponse = warehouseController.activateWarehouse(3L, auth);
        var assignManagerResponse = warehouseController.assignManager(3L, 9L, auth);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertEquals(3L, updateResponse.warehouseId());
        assertEquals(3L, deactivateResponse.warehouseId());
        assertEquals(3L, activateResponse.warehouseId());
        assertEquals(3L, assignManagerResponse.warehouseId());

        verify(warehouseManagementService).createWarehouse(any(CreateWarehouseRequest.class), eq(77L));
        verify(warehouseManagementService).updateWarehouse(eq(3L), any(UpdateWarehouseRequest.class), eq(77L));
        verify(warehouseManagementService).deactivateWarehouse(3L, 77L);
        verify(warehouseManagementService).activateWarehouse(3L, 77L);
        verify(warehouseManagementService).assignManager(3L, 9L, 77L);
    }

    private WarehouseResponse warehouseResponse() {
        return WarehouseResponse.builder()
                .warehouseId(3L)
                .name("West Hub")
                .code("WH-WEST")
                .location("Pune")
                .address("Industrial Area")
                .city("Pune")
                .state("Maharashtra")
                .country("India")
                .managerId(9L)
                .capacity(450)
                .usedCapacity(120)
                .availableCapacity(330)
                .utilizationPercentage(26.67)
                .isActive(true)
                .phone("9876543210")
                .build();
    }
}
