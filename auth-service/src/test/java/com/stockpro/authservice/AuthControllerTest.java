package com.stockpro.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.authservice.controller.AuthController;
import com.stockpro.authservice.dto.ForgotPasswordRequestDTO;
import com.stockpro.authservice.dto.LoginRequestDTO;
import com.stockpro.authservice.dto.LoginResponseDTO;
import com.stockpro.authservice.dto.MessageResponseDTO;
import com.stockpro.authservice.dto.OtpVerificationRequestDTO;
import com.stockpro.authservice.dto.RegisterResponseDTO;
import com.stockpro.authservice.dto.ResetPasswordRequestDTO;
import com.stockpro.authservice.dto.UserRequestDTO;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
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
}
