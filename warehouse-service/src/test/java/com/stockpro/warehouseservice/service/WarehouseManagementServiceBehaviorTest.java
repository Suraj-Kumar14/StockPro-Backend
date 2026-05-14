package com.stockpro.warehouseservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.warehouseservice.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouseservice.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouseservice.dto.response.WarehouseResponse;
import com.stockpro.warehouseservice.dto.response.WarehouseSummaryResponse;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.events.WarehouseEvent;
import com.stockpro.warehouseservice.exception.CapacityExceededException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WarehouseManagementServiceBehaviorTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseEventPublisher warehouseEventPublisher;

    @InjectMocks
    private WarehouseManagementService warehouseManagementService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .warehouseId(7L)
                .name("Central Hub")
                .code("WH-007")
                .location("Delhi")
                .address("Sector 12")
                .city("Delhi")
                .state("Delhi")
                .country("India")
                .managerId(5L)
                .capacity(500)
                .usedCapacity(120)
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(warehouseManagementService, "warehouseCreatedRouting", "warehouse.created");
        ReflectionTestUtils.setField(warehouseManagementService, "warehouseUpdatedRouting", "warehouse.updated");
        ReflectionTestUtils.setField(warehouseManagementService, "warehouseActivatedRouting", "warehouse.activated");
        ReflectionTestUtils.setField(warehouseManagementService, "warehouseDeactivatedRouting", "warehouse.deactivated");
    }

    @Test
    void createWarehouse_shouldNormalizePersistAndPublish() {
        CreateWarehouseRequest request = new CreateWarehouseRequest();
        request.setName("  Central Hub  ");
        request.setCode(" wh-007 ");
        request.setLocation(" Delhi ");
        request.setAddress(" Sector 12 ");
        request.setCity(" Delhi ");
        request.setState(" Delhi ");
        request.setCountry(" India ");
        request.setManagerId(5L);
        request.setCapacity(500);
        request.setPhone("9876543210");

        when(warehouseRepository.existsByCode("WH-007")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse saved = invocation.getArgument(0);
            saved.setWarehouseId(7L);
            return saved;
        });

        WarehouseResponse response = warehouseManagementService.createWarehouse(request, 99L);

        assertEquals("Central Hub", response.name());
        assertEquals("WH-007", response.code());
        assertEquals(0, response.usedCapacity());
        ArgumentCaptor<WarehouseEvent> captor = ArgumentCaptor.forClass(WarehouseEvent.class);
        verify(warehouseEventPublisher).publishWarehouseEvent(eq("warehouse.created"), captor.capture());
        assertEquals("WAREHOUSE_CREATED", captor.getValue().eventType());
    }

    @Test
    void createWarehouse_shouldRejectDuplicateCode() {
        CreateWarehouseRequest request = new CreateWarehouseRequest();
        request.setName("Central Hub");
        request.setCode("WH-007");
        request.setLocation("Delhi");
        request.setCity("Delhi");
        request.setState("Delhi");
        request.setCountry("India");
        request.setCapacity(500);

        when(warehouseRepository.existsByCode("WH-007")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> warehouseManagementService.createWarehouse(request, 1L));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getWarehouseMethods_shouldReturnMappedResponsesOrThrow() {
        when(warehouseRepository.findByWarehouseId(7L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.sumQuantityByWarehouseId(7L)).thenReturn(120);
        when(warehouseRepository.findByCode("WH-007")).thenReturn(Optional.of(warehouse));

        assertEquals(7L, warehouseManagementService.getWarehouseById(7L).warehouseId());
        assertEquals("WH-007", warehouseManagementService.getWarehouseByCode("WH-007").code());
        assertEquals(120, warehouseManagementService.getWarehouseEntity(7L).getUsedCapacity());

        when(warehouseRepository.findByWarehouseId(99L)).thenReturn(Optional.empty());
        assertThrows(WarehouseNotFoundException.class,
                () -> warehouseManagementService.getWarehouseById(99L));
    }

    @Test
    void getAllWarehouses_shouldHandleStatusSearchAndFilters() {
        when(warehouseRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warehouse)));

        assertEquals(1, warehouseManagementService
                .getAllWarehouses(null, "hub", "ACTIVE", "Delhi", "Delhi", 0, 10, "warehouseName", "asc")
                .getTotalElements());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> warehouseManagementService.getAllWarehouses(null, null, "BROKEN", null, null, 0, 10, "name", "asc"));
        assertEquals("Invalid status 'BROKEN'. Allowed values: ACTIVE, INACTIVE, ALL", ex.getMessage());
    }

    @Test
    void getActiveAndManagerAndSummary_shouldAggregateValues() {
        Warehouse inactive = Warehouse.builder()
                .warehouseId(8L)
                .name("South Hub")
                .code("WH-008")
                .location("Chennai")
                .city("Chennai")
                .state("Tamil Nadu")
                .country("India")
                .capacity(300)
                .usedCapacity(30)
                .isActive(false)
                .build();

        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of(warehouse));
        when(warehouseRepository.findByManagerId(5L)).thenReturn(List.of(warehouse));
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse, inactive));

        assertEquals(1, warehouseManagementService.getActiveWarehouses().size());
        assertEquals(1, warehouseManagementService.getWarehousesByManager(5L).size());

        WarehouseSummaryResponse summary = warehouseManagementService.getWarehouseSummary();
        assertEquals(2, summary.totalWarehouses());
        assertEquals(1, summary.activeWarehouses());
        assertEquals(800, summary.totalCapacity());
        assertEquals(150, summary.usedCapacity());
    }

    @Test
    void updateWarehouse_shouldApplyChangesAndRejectBadCapacityOrDuplicateCode() {
        UpdateWarehouseRequest request = new UpdateWarehouseRequest();
        request.setName("Updated Hub");
        request.setCode("WH-NEW");
        request.setLocation("Mumbai");
        request.setAddress("Dockyard");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setCountry("India");
        request.setManagerId(11L);
        request.setCapacity(600);
        request.setPhone("9876543210");
        request.setIsActive(false);

        when(warehouseRepository.findByWarehouseId(7L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.sumQuantityByWarehouseId(7L)).thenReturn(120);
        when(warehouseRepository.existsByCode("WH-NEW")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse response = warehouseManagementService.updateWarehouse(7L, request, 55L);

        assertEquals("Updated Hub", response.name());
        assertEquals("WH-NEW", response.code());
        assertEquals(false, response.isActive());
        verify(warehouseEventPublisher).publishWarehouseEvent(eq("warehouse.updated"), any(WarehouseEvent.class));

        request.setCapacity(100);
        assertThrows(CapacityExceededException.class,
                () -> warehouseManagementService.updateWarehouse(7L, request, 55L));

        request.setCapacity(600);
        request.setCode("WH-DUP");
        when(warehouseRepository.existsByCode("WH-DUP")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> warehouseManagementService.updateWarehouse(7L, request, 55L));
    }

    @Test
    void activateDeactivateAndAssignManager_shouldPersistAndPublish() {
        when(warehouseRepository.findByWarehouseId(7L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.sumQuantityByWarehouseId(7L)).thenReturn(120);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse deactivated = warehouseManagementService.deactivateWarehouse(7L, 10L);
        WarehouseResponse activated = warehouseManagementService.activateWarehouse(7L, 11L);
        WarehouseResponse reassigned = warehouseManagementService.assignManager(7L, 77L, 12L);

        assertEquals(false, deactivated.isActive());
        assertEquals(true, activated.isActive());
        assertEquals(77L, reassigned.managerId());
        verify(warehouseEventPublisher).publishWarehouseEvent(eq("warehouse.deactivated"), any(WarehouseEvent.class));
        verify(warehouseEventPublisher).publishWarehouseEvent(eq("warehouse.activated"), any(WarehouseEvent.class));
        verify(warehouseEventPublisher).publishWarehouseEvent(eq("warehouse.updated"), any(WarehouseEvent.class));
    }
}
