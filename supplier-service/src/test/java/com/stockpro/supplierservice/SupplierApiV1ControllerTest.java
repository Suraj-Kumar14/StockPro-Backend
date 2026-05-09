package com.stockpro.supplierservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.supplierservice.config.SecurityConfig;
import com.stockpro.supplierservice.controller.SupplierApiV1Controller;
import com.stockpro.supplierservice.dto.request.BlacklistSupplierRequest;
import com.stockpro.supplierservice.dto.request.CreateSupplierRequest;
import com.stockpro.supplierservice.dto.request.DeactivateSupplierRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRatingRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRequest;
import com.stockpro.supplierservice.dto.response.SupplierPerformanceResponse;
import com.stockpro.supplierservice.dto.response.SupplierPurchaseValidationResponse;
import com.stockpro.supplierservice.dto.response.SupplierResponse;
import com.stockpro.supplierservice.dto.response.SupplierSummaryResponse;
import com.stockpro.supplierservice.entity.SupplierStatus;
import com.stockpro.supplierservice.exception.DuplicateSupplierException;
import com.stockpro.supplierservice.exception.GlobalExceptionHandler;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.security.JwtAuthenticationFilter;
import com.stockpro.supplierservice.service.SupplierManagementService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SupplierApiV1Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SupplierApiV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierManagementService supplierManagementService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void postSuppliersReturns201ForAdmin() throws Exception {
        when(supplierManagementService.createSupplier(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierCode").value("SUP-20260501-0001"));
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void postSuppliersReturns201ForPurchaseOfficer() throws Exception {
        when(supplierManagementService.createSupplier(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void postSuppliersReturns403ForWarehouseStaff() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void putSupplierReturns200ForAllowedRoles() throws Exception {
        when(supplierManagementService.updateSupplier(eq(1L), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Supply"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void putSupplierReturns403ForDisallowedRole() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void patchBlacklistReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/1/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BlacklistSupplierRequest("Risk"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchBlacklistReturns200ForAdmin() throws Exception {
        when(supplierManagementService.blacklistSupplier(eq(1L), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/suppliers/1/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BlacklistSupplierRequest("Risk"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getSuppliersReturns200ForAllowedRoles() throws Exception {
        when(supplierManagementService.getAllSuppliers(null, null, 0, 10, "name", "asc"))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supplierId").value(1L));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void searchSuppliersReturns200ForAllowedRoles() throws Exception {
        when(supplierManagementService.searchSuppliers("acme", SupplierStatus.ACTIVE, true, "Pune", "India",
                BigDecimal.valueOf(4.5), 7, 0, 10, "rating", "desc"))
                .thenReturn(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/suppliers/search")
                        .param("keyword", "acme")
                        .param("status", "ACTIVE")
                        .param("isActive", "true")
                        .param("city", "Pune")
                        .param("country", "India")
                        .param("minRating", "4.5")
                        .param("maxLeadTimeDays", "7")
                        .param("sortBy", "rating")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Acme Supply"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getActiveSuppliersReturns200() throws Exception {
        when(supplierManagementService.getActiveSuppliers()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/suppliers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierCode").value("SUP-20260501-0001"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getSupplierByIdReturns200() throws Exception {
        when(supplierManagementService.getSupplierById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/suppliers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(1L));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getSupplierByCodeReturns200() throws Exception {
        when(supplierManagementService.getSupplierByCode("SUP-20260501-0001")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/suppliers/code/SUP-20260501-0001"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getSupplierByEmailReturns200() throws Exception {
        when(supplierManagementService.getSupplierByEmail("acme@example.com")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/suppliers/email/acme@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void activateSupplierReturns200() throws Exception {
        when(supplierManagementService.activateSupplier(1L, null)).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/suppliers/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deactivateSupplierReturns200() throws Exception {
        when(supplierManagementService.deactivateSupplier(eq(1L), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/suppliers/1/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeactivateSupplierRequest("No longer preferred"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void updateRatingReturns200() throws Exception {
        when(supplierManagementService.updateSupplierRating(eq(1L), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/v1/suppliers/1/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateSupplierRatingRequest(
                                BigDecimal.valueOf(4.7), null, null, null, null, "Improved"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void getPerformanceReturns200() throws Exception {
        when(supplierManagementService.getSupplierPerformance(1L)).thenReturn(new SupplierPerformanceResponse(
                1L,
                "Acme Supply",
                3,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(4.5),
                BigDecimal.valueOf(4.5),
                LocalDateTime.now()));

        mockMvc.perform(get("/api/v1/suppliers/1/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(1L));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getSummaryReturns200() throws Exception {
        when(supplierManagementService.getSupplierSummary()).thenReturn(new SupplierSummaryResponse(
                2L,
                1L,
                1L,
                0L,
                0L,
                BigDecimal.valueOf(4.25),
                BigDecimal.valueOf(5.50)));

        mockMvc.perform(get("/api/v1/suppliers/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSuppliers").value(2L));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getTopRatedSuppliersReturns200() throws Exception {
        when(supplierManagementService.getTopRatedSuppliers()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/suppliers/top-rated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(4.5));
    }

    @Test
    @WithMockUser(roles = "OFFICER")
    void validateForPurchaseReturns200() throws Exception {
        when(supplierManagementService.validateSupplierForPurchase(1L)).thenReturn(new SupplierPurchaseValidationResponse(
                1L,
                "Acme Supply",
                true,
                SupplierStatus.ACTIVE,
                "NET-30",
                5,
                true,
                null));

        mockMvc.perform(get("/api/v1/suppliers/1/validate-for-purchase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canUseForPurchase").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSupplierReturns204ForAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/1"))
                .andExpect(status().isNoContent());

        verify(supplierManagementService).deleteSupplier(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteSupplierReturns403ForNonAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidRequestReturns400() throws Exception {
        CreateSupplierRequest invalid = new CreateSupplierRequest(
                null, "", null, "bad-email", null, null, null, null, null, null, null, null, null, "", -1, BigDecimal.valueOf(7), null);

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void notFoundReturns404() throws Exception {
        when(supplierManagementService.getSupplierById(99L)).thenThrow(new SupplierNotFoundException("Supplier not found"));

        mockMvc.perform(get("/api/v1/suppliers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateSupplierCodeReturns409() throws Exception {
        when(supplierManagementService.createSupplier(any(), any())).thenThrow(new DuplicateSupplierException("Supplier code already exists"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateEmailReturns409() throws Exception {
        when(supplierManagementService.createSupplier(any(), any())).thenThrow(new DuplicateSupplierException("Supplier email already exists"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isConflict());
    }

    private CreateSupplierRequest sampleCreateRequest() {
        return new CreateSupplierRequest(
                "SUP-20260501-0001",
                "Acme Supply",
                "Raj",
                "acme@example.com",
                "+919999999999",
                null,
                "Line 1",
                "Pune",
                "MH",
                "India",
                "411001",
                "TAX-1",
                "GST-1",
                "NET-30",
                5,
                BigDecimal.valueOf(4.5),
                "Preferred");
    }

    private UpdateSupplierRequest sampleUpdateRequest() {
        return new UpdateSupplierRequest(
                "Acme Supply",
                "Raj",
                "acme@example.com",
                "+919999999999",
                null,
                "Line 1",
                "Pune",
                "MH",
                "India",
                "411001",
                "TAX-1",
                "GST-1",
                "NET-45",
                5,
                BigDecimal.valueOf(4.5),
                SupplierStatus.ACTIVE,
                true,
                "Preferred");
    }

    private SupplierResponse sampleResponse() {
        return new SupplierResponse(
                1L,
                "SUP-20260501-0001",
                "Acme Supply",
                "Raj",
                "acme@example.com",
                "+919999999999",
                null,
                "Line 1",
                "Pune",
                "MH",
                "India",
                "411001",
                "TAX-1",
                "GST-1",
                "NET-30",
                5,
                BigDecimal.valueOf(4.5),
                SupplierStatus.ACTIVE,
                true,
                "Preferred",
                LocalDateTime.now(),
                LocalDateTime.now(),
                1L,
                1L);
    }
}
