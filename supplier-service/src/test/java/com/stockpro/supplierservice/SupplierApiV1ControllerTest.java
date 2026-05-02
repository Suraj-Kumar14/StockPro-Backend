package com.stockpro.supplierservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.supplierservice.config.SecurityConfig;
import com.stockpro.supplierservice.controller.SupplierApiV1Controller;
import com.stockpro.supplierservice.dto.request.BlacklistSupplierRequest;
import com.stockpro.supplierservice.dto.request.CreateSupplierRequest;
import com.stockpro.supplierservice.dto.request.UpdateSupplierRequest;
import com.stockpro.supplierservice.dto.response.SupplierResponse;
import com.stockpro.supplierservice.entity.SupplierStatus;
import com.stockpro.supplierservice.exception.DuplicateSupplierException;
import com.stockpro.supplierservice.exception.GlobalExceptionHandler;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.security.JwtAuthenticationFilter;
import com.stockpro.supplierservice.service.SupplierManagementService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(status().isCreated());
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
                .andExpect(status().isOk());
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
    @WithMockUser(roles = "STAFF")
    void getSuppliersReturns200ForAllowedRoles() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk());
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
