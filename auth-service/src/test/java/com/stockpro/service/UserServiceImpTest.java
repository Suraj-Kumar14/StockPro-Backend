package com.stockpro.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.stockpro.dtos.*;
import com.stockpro.entity.User;
import com.stockpro.exception.BadRequestException;
import com.stockpro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImp userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Suraj Kumar");
        registerRequest.setEmail("suraj@test.com");
        registerRequest.setPassword("Suraj@123");
        registerRequest.setPhone("9876543210");
        registerRequest.setDepartment("IT");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("suraj@test.com");
        loginRequest.setPassword("Suraj@123");

        user = new User();
        user.setUserId(1L);
        user.setFullName("Suraj Kumar");
        user.setEmail("suraj@test.com");
        user.setPasswordHash("encodedPassword");
        user.setPhone("9876543210");
        user.setRole("WAREHOUSE_STAFF");
        user.setDepartment("IT");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void registerRequest_ShouldSendOtp_ForNewUser() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Suraj@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String response = userService.registerRequest(registerRequest);

        assertEquals("OTP sent to your email for verification.", response);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendOtpEmail(eq("suraj@test.com"), anyString(), eq("Registration OTP"), eq("REGISTER"));
    }
    
    @Test
    void registerRequest_ShouldThrowException_WhenUserAlreadyActive() {
        user.setActive(true);
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.registerRequest(registerRequest));

        assertEquals("User already registered with this email!", ex.getMessage());
        verify(emailService, never()).sendOtpEmail(any(), any(), any(), any());
    }
    
    @Test
    void registerRequest_ShouldUpdateInactiveUser_AndResendOtp() {
        user.setActive(false);
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Suraj@123")).thenReturn("encodedPassword");

        String response = userService.registerRequest(registerRequest);

        assertEquals("OTP sent to your email for verification.", response);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendOtpEmail(eq("suraj@test.com"), anyString(), eq("Registration OTP"), eq("REGISTER"));
    }
    
    @Test
    void registerUser_ShouldActivateUser_WhenOtpIsValid() {
        user.setActive(false);
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("suraj@test.com", 1L)).thenReturn("jwt-token");

        AuthResponse response = userService.registerUser("suraj@test.com", "123456");

        assertEquals("jwt-token", response.getToken());
        assertEquals("User Register success", response.getMessage());
        assertTrue(user.isActive());
        assertNull(user.getOtpCode());
    }
    
    @Test
    void registerUser_ShouldThrowException_WhenOtpIsInvalid() {
        user.setOtpCode("999999");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.registerUser("suraj@test.com", "123456"));

        assertEquals("OTP Invalid! please try again.", ex.getMessage());
    }
    
    @Test
    void registerUser_ShouldThrowException_WhenOtpExpired() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.registerUser("suraj@test.com", "123456"));

        assertEquals("OTP Expired! please try again.", ex.getMessage());
    }
    
    @Test
    void loginUser_ShouldReturnToken_WhenCredentialsValid() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Suraj@123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken("suraj@test.com", 1L)).thenReturn("jwt-token");

        AuthResponse response = userService.loginUser(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals("Login Success", response.getMessage());
    }
    
    @Test
    void loginUser_ShouldThrowException_WhenPasswordInvalid() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Suraj@123", "encodedPassword")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.loginUser(loginRequest));

        assertEquals("Invalid Password!", ex.getMessage());
    }
    
    @Test
    void loginUser_ShouldThrowException_WhenUserInactive() {
        user.setActive(false);
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Suraj@123", "encodedPassword")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.loginUser(loginRequest));

        assertEquals("Account is not verified. Please verify your email using the OTP sent during registration.", ex.getMessage());
    }
    
    @Test
    void initiateForgetPassword_ShouldSendOtp_WhenUserExists() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        String response = userService.initiateForgetPassword("suraj@test.com");

        assertEquals("Verification code sent to your email.", response);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendOtpEmail(eq("suraj@test.com"), anyString(), eq("Forgot Password OTP"), eq("FORGOT_PASSWORD"));
    }
    
    @Test
    void initiateForgetPassword_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.initiateForgetPassword("suraj@test.com"));

        assertEquals("User not found!", ex.getMessage());
    }
    
    @Test
    void verifyOtp_ShouldReturnSuccess_WhenOtpValid() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        String response = userService.verifyOtp("suraj@test.com", "123456");

        assertEquals("OTP Verified. You may now reset your password.", response);
    }
    
    @Test
    void verifyOtp_ShouldThrowException_WhenOtpInvalid() {
        user.setOtpCode("999999");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.verifyOtp("suraj@test.com", "123456"));

        assertEquals("OTP Invalid! please try again.", ex.getMessage());
    }
    
    @Test
    void verifyOtp_ShouldThrowException_WhenOtpExpired() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> userService.verifyOtp("suraj@test.com", "123456"));

        assertEquals("OTP Expired! please try again.", ex.getMessage());
    }
    
    @Test
    void resetPassword_ShouldUpdatePassword_WhenUserExists() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("newEncodedPassword");

        String response = userService.resetPassword("suraj@test.com", "NewPass@123");

        assertEquals("Password updated successfully.", response);
        assertEquals("newEncodedPassword", user.getPasswordHash());
        assertNull(user.getOtpCode());
    }
    
    @Test
    void updateProfile_ShouldUpdateBasicDetails_WhenEmailNotChanged() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setEmail("suraj@test.com");
        request.setPhone("9999999999");

        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.updateProfile("suraj@test.com", request);

        assertEquals("Updated Name", response.getFullName());
        assertEquals("9999999999", response.getPhone());
    }
    
    @Test
    void updateProfile_ShouldSendOtp_WhenEmailChanges() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setEmail("newmail@test.com");
        request.setPhone("9999999999");

        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("newmail@test.com")).thenReturn(Optional.empty());

        UserResponseDTO response = userService.updateProfile("suraj@test.com", request);

        assertEquals("Updated Name", response.getFullName());
        verify(emailService).sendOtpEmail(eq("newmail@test.com"), anyString(), eq("Email Update Verification"), eq("UPDATE_EMAIL"));
    }
    
    @Test
    void verifyEmailUpdate_ShouldUpdateEmail_WhenOtpValid() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setPendingEmail("newmail@test.com");

        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        String response = userService.verifyEmailUpdate("suraj@test.com", "123456");

        assertEquals("Email updated successfully to newmail@test.com", response);
        assertEquals("newmail@test.com", user.getEmail());
        assertNull(user.getPendingEmail());
    }
    
    @Test
    void getUserByEmail_ShouldReturnUserResponse() {
        when(userRepository.findByEmail("suraj@test.com")).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getUserByEmail("suraj@test.com");

        assertEquals("Suraj Kumar", response.getFullName());
        assertEquals("suraj@test.com", response.getEmail());
    }
    
    @Test
    void getAllUsers_ShouldReturnUserList() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDTO> users = userService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("Suraj Kumar", users.get(0).getFullName());
    }
    
    @Test
    void deactivateUser_ShouldSetUserInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }
    
    @Test
    void deactivateUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.deactivateUser(1L));

        assertEquals("User not found", ex.getMessage());
    }
    
    @Test
    void logout_ShouldDoNothing() {
        assertDoesNotThrow(() -> userService.logout("dummy-token"));
    }
}