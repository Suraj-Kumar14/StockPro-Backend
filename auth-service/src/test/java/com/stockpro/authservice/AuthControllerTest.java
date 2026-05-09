package com.stockpro.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.authservice.controller.AuthController;
import com.stockpro.authservice.dto.ForgotPasswordRequestDTO;
import com.stockpro.authservice.dto.LoginRequestDTO;
import com.stockpro.authservice.dto.LoginResponseDTO;
import com.stockpro.authservice.dto.MessageResponseDTO;
import com.stockpro.authservice.dto.OtpVerificationRequestDTO;
import com.stockpro.authservice.dto.ChangePasswordDTO;
import com.stockpro.authservice.dto.UpdateProfileDTO;
import com.stockpro.authservice.dto.RegisterResponseDTO;
import com.stockpro.authservice.dto.ResetPasswordRequestDTO;
import com.stockpro.authservice.dto.AdminChangeUserRoleRequestDTO;
import com.stockpro.authservice.dto.AdminCreateUserRequestDTO;
import com.stockpro.authservice.dto.AdminUpdateUserRequestDTO;
import com.stockpro.authservice.dto.UserResponseDTO;
import com.stockpro.authservice.dto.UserSummaryDTO;
import com.stockpro.authservice.dto.UserRequestDTO;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.GlobalExceptionHandler;
import com.stockpro.authservice.exception.InactiveAccountException;
import com.stockpro.authservice.exception.InvalidCredentialsException;
import com.stockpro.authservice.security.JwtFilter;
import com.stockpro.authservice.security.JwtUtil;
import com.stockpro.authservice.security.OAuth2AuthenticationFailureHandler;
import com.stockpro.authservice.security.OAuth2AuthenticationSuccessHandler;
import com.stockpro.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;

    @Test
    void login_shouldReturnTokens_whenCredentialsValid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("Password@123");

        when(authService.login("user@example.com", "Password@123"))
                .thenReturn(new LoginResponseDTO("access-token", "refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_shouldReturnBadRequest_whenCredentialsInvalid() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("wrong-password");

        when(authService.login(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_shouldReturnForbidden_whenAccountInactive() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("Password@123");

        when(authService.login(anyString(), anyString()))
                .thenThrow(new InactiveAccountException("Your account is inactive. Please contact administrator."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account is inactive. Please contact administrator."));
    }

    @Test
    void register_shouldReturnOk_whenRequestValid() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("User Example");
        request.setEmail("user@example.com");
        request.setPassword("Password@123");
        request.setRole(UserRole.STAFF);

        when(authService.register(any(UserRequestDTO.class)))
                .thenReturn(new RegisterResponseDTO(null, "OTP sent to your email. Please verify to complete registration."));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to your email. Please verify to complete registration."));
    }

    @Test
    void forgotPassword_shouldReturnOk_whenEmailValid() throws Exception {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("user@example.com");

        when(authService.forgotPassword(any(ForgotPasswordRequestDTO.class)))
                .thenReturn(new MessageResponseDTO("OTP sent successfully"));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));

        verify(authService).forgotPassword(any(ForgotPasswordRequestDTO.class));
    }

    @Test
    void forgotPassword_shouldReturnBadRequest_whenEmailInvalid() throws Exception {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    @Test
    void resetPassword_shouldReturnOk_whenOtpAndPasswordValid() throws Exception {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("user@example.com");
        request.setOtp("123456");
        request.setNewPassword("Password@123");

        when(authService.resetPassword(any(ResetPasswordRequestDTO.class)))
                .thenReturn(new MessageResponseDTO("Password updated successfully"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    void verifyOtp_shouldReturnSuccess_whenOtpValid() throws Exception {
        OtpVerificationRequestDTO request = new OtpVerificationRequestDTO();
        request.setEmail("user@example.com");
        request.setOtp("123456");

        when(authService.verifyOtp(any(OtpVerificationRequestDTO.class)))
                .thenReturn(new RegisterResponseDTO(null, "OTP verified successfully"));

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    void getUserById_shouldReturnUser_whenAdminRequests() throws Exception {
        UserResponseDTO user = new UserResponseDTO();
        user.setUserId(7L);
        user.setName("User Example");
        user.setEmail("user@example.com");
        user.setRole(UserRole.STAFF);
        user.setIsActive(true);

        when(authService.getUserById(7L)).thenReturn(user);

        mockMvc.perform(get("/auth/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void getUserSummary_shouldReturnSummary_whenAdminRequests() throws Exception {
        when(authService.getUserSummary()).thenReturn(new UserSummaryDTO(10, 8, 2, 1, 3, 2, 4, 6));

        mockMvc.perform(get("/auth/users/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(10))
                .andExpect(jsonPath("$.activeUsers").value(8))
                .andExpect(jsonPath("$.warehouseStaffCount").value(4));
    }

    @Test
    void unknownActuatorEndpoint_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void refreshAndLogout_shouldDelegateToService() throws Exception {
        when(authService.refresh("refresh-token")).thenReturn(new LoginResponseDTO("new-access", "new-refresh"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("refresh-token"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(authService).refresh("refresh-token");
        verify(authService).logout("access-token");
    }

    @Test
    void profileEndpoints_shouldUseAuthenticatedUser() throws Exception {
        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setUserId(9L);
        userResponse.setName("Profile User");
        userResponse.setEmail("profile@example.com");
        userResponse.setRole(UserRole.MANAGER);
        userResponse.setIsActive(true);

        UpdateProfileDTO updateProfileDTO = new UpdateProfileDTO();
        updateProfileDTO.setName("Updated User");
        updateProfileDTO.setPhone("9999999999");
        updateProfileDTO.setDepartment("Ops");

        when(authService.getUserProfile("profile@example.com")).thenReturn(userResponse);
        when(authService.updateProfile(anyString(), any(UpdateProfileDTO.class))).thenReturn(userResponse);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("profile@example.com", null));
        try {
            mockMvc.perform(get("/auth/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("profile@example.com"));

            mockMvc.perform(put("/auth/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(updateProfileDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(9));

            ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
            changePasswordDTO.setOldPassword("OldPassword@123");
            changePasswordDTO.setNewPassword("NewPassword@123");

            mockMvc.perform(put("/auth/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(changePasswordDTO)))
                    .andExpect(status().isOk());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void adminUserEndpoints_shouldReturnManagedPayloads() throws Exception {
        UserResponseDTO managedUser = new UserResponseDTO();
        managedUser.setUserId(11L);
        managedUser.setName("Managed User");
        managedUser.setEmail("managed@example.com");
        managedUser.setRole(UserRole.STAFF);
        managedUser.setIsActive(true);

        AdminCreateUserRequestDTO createRequest = new AdminCreateUserRequestDTO();
        createRequest.setName("Managed User");
        createRequest.setEmail("managed@example.com");
        createRequest.setPassword("Password@123");
        createRequest.setRole(UserRole.STAFF);

        AdminUpdateUserRequestDTO updateRequest = new AdminUpdateUserRequestDTO();
        updateRequest.setName("Managed User Updated");
        updateRequest.setEmail("managed@example.com");
        updateRequest.setPhone("8888888888");
        updateRequest.setDepartment("Warehouse");
        updateRequest.setIsActive(true);

        AdminChangeUserRoleRequestDTO changeRoleRequest = new AdminChangeUserRoleRequestDTO();
        changeRoleRequest.setRole(UserRole.MANAGER);

        when(authService.getUsersPage(0, 20, null, null, null)).thenReturn(new PageImpl<>(List.of(managedUser)));
        when(authService.createAdminUser(any(AdminCreateUserRequestDTO.class))).thenReturn(managedUser);
        when(authService.updateAdminUser(any(Long.class), any(AdminUpdateUserRequestDTO.class))).thenReturn(managedUser);
        when(authService.changeUserRole(any(Long.class), any(AdminChangeUserRoleRequestDTO.class), anyString())).thenReturn(managedUser);

        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("managed@example.com"));

        mockMvc.perform(post("/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(11));

        mockMvc.perform(put("/auth/users/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Managed User"));

        mockMvc.perform(patch("/auth/users/11/role")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(changeRoleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("managed@example.com"));
    }

    @Test
    void accountActivationEndpoints_shouldReturnSuccessMessages() throws Exception {
        mockMvc.perform(delete("/auth/user/15"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/auth/users/15/deactivate")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/auth/users/15/deactivate")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/auth/users/15/activate")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/auth/users/15/activate")
                        .principal(new UsernamePasswordAuthenticationToken("admin@example.com", null)))
                .andExpect(status().isOk());
    }
}
