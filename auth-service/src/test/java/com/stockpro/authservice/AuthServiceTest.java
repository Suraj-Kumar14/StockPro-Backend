package com.stockpro.authservice;

import com.stockpro.authservice.dto.ForgotPasswordRequestDTO;
import com.stockpro.authservice.dto.AdminCreateUserRequestDTO;
import com.stockpro.authservice.dto.AdminChangeUserRoleRequestDTO;
import com.stockpro.authservice.dto.AdminUpdateUserRequestDTO;
import com.stockpro.authservice.dto.ChangePasswordDTO;
import com.stockpro.authservice.dto.LoginResponseDTO;
import com.stockpro.authservice.dto.OtpVerificationRequestDTO;
import com.stockpro.authservice.dto.RegisterResponseDTO;
import com.stockpro.authservice.dto.ResetPasswordRequestDTO;
import com.stockpro.authservice.dto.UpdateProfileDTO;
import com.stockpro.authservice.dto.UserResponseDTO;
import com.stockpro.authservice.dto.UserRequestDTO;
import com.stockpro.authservice.entity.OtpPurpose;
import com.stockpro.authservice.entity.OtpToken;
import com.stockpro.authservice.entity.User;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.EmailDeliveryException;
import com.stockpro.authservice.exception.InactiveAccountException;
import com.stockpro.authservice.exception.InvalidOtpException;
import com.stockpro.authservice.exception.InvalidCredentialsException;
import com.stockpro.authservice.exception.SelfDeactivationNotAllowedException;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.exception.ResourceNotFoundException;
import com.stockpro.authservice.repository.OtpTokenRepository;
import com.stockpro.authservice.repository.UserRepository;
import com.stockpro.authservice.security.JwtUtil;
import com.stockpro.authservice.service.AuthService;
import com.stockpro.authservice.service.OtpEventPublisher;
import com.stockpro.authservice.service.OtpMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpMailService otpMailService;

    @Mock
    private OtpEventPublisher otpEventPublisher;

    @InjectMocks
    private AuthService authService;

    private User user;
    private UserRequestDTO registerRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpiryMinutes", 10L);

        user = new User();
        user.setId(1L);
        user.setName("User Example");
        user.setEmail("user@example.com");
        user.setPassword("$2a$encoded");
        user.setRole(UserRole.STAFF);
        user.setIsActive(true);

        registerRequest = new UserRequestDTO();
        registerRequest.setName("User Example");
        registerRequest.setEmail("user@example.com");
        registerRequest.setPassword("Password@123");
        registerRequest.setRole(UserRole.STAFF);
        registerRequest.setDepartment("Ops");
        registerRequest.setPhone("9999999999");
    }

    @Test
    void login_shouldReturnTokens_whenCredentialsValid() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com", "STAFF", 1L)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user@example.com")).thenReturn("refresh-token");

        LoginResponseDTO response = authService.login("user@example.com", "Password@123");

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(jwtUtil).generateToken("user@example.com", "STAFF", 1L);
        verify(jwtUtil).generateRefreshToken("user@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void login_shouldThrowException_whenPasswordInvalid() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-password", user.getPassword())).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> authService.login("user@example.com", "bad-password"));

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> authService.login("missing@example.com", "Password@123"));

        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void login_shouldRejectInactiveUser() {
        user.setIsActive(false);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        InactiveAccountException exception = assertThrows(InactiveAccountException.class,
                () -> authService.login("user@example.com", "Password@123"));

        assertEquals("Your account is inactive. Please contact administrator.", exception.getMessage());
    }

    @Test
    void register_shouldGenerateSignupOtp_whenValidData() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$encoded");
        when(otpEventPublisher.publish(any())).thenReturn(true);

        RegisterResponseDTO response = authService.register(registerRequest);

        assertEquals("OTP sent successfully. Please check your email.", response.getMessage());
        ArgumentCaptor<OtpToken> otpCaptor = ArgumentCaptor.forClass(OtpToken.class);
        verify(otpTokenRepository).save(otpCaptor.capture());
        verify(otpEventPublisher).publish(any());
        assertEquals(OtpPurpose.SIGNUP_VERIFICATION, otpCaptor.getValue().getPurpose());
        assertEquals("$2a$encoded", otpCaptor.getValue().getPasswordHash());
    }

    @Test
    void register_shouldNormalizeEmailBeforeGeneratingSignupOtp() {
        registerRequest.setEmail("  USER@Example.COM ");
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$encoded");
        when(otpEventPublisher.publish(any())).thenReturn(true);

        authService.register(registerRequest);

        ArgumentCaptor<OtpToken> otpCaptor = ArgumentCaptor.forClass(OtpToken.class);
        verify(otpTokenRepository).save(otpCaptor.capture());
        assertEquals("user@example.com", otpCaptor.getValue().getEmail());
        verify(otpEventPublisher).publish(any());
    }

    @Test
    void register_shouldFail_whenEmailExists() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email already registered", exception.getMessage());
        verify(otpTokenRepository, never()).save(any());
        verify(otpEventPublisher, never()).publish(any());
    }

    @Test
    void register_shouldThrowEmailDeliveryException_whenOtpPublicationFails() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$encoded");
        when(otpEventPublisher.publish(any())).thenReturn(false);

        EmailDeliveryException exception = assertThrows(EmailDeliveryException.class,
                () -> authService.register(registerRequest));

        assertEquals("OTP delivery is temporarily unavailable. Please try again.", exception.getMessage());
        verify(otpTokenRepository).save(any(OtpToken.class));
    }

    @Test
    void forgotPassword_shouldGenerateOtpAndSendEmail_whenUserExists() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("user@example.com");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(otpEventPublisher.publish(any())).thenReturn(true);

        authService.forgotPassword(request);

        ArgumentCaptor<OtpToken> otpCaptor = ArgumentCaptor.forClass(OtpToken.class);
        verify(otpTokenRepository).save(otpCaptor.capture());
        verify(otpEventPublisher).publish(any());
        assertEquals(OtpPurpose.PASSWORD_RESET, otpCaptor.getValue().getPurpose());
        assertEquals("user@example.com", otpCaptor.getValue().getEmail());
    }

    @Test
    void forgotPassword_shouldNotSendEmail_whenUserNotFound() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setEmail("missing@example.com");

        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(otpTokenRepository, never()).save(any());
        verify(otpEventPublisher, never()).publish(any());
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenOtpValid() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("user@example.com");
        request.setOtp("123456");
        request.setNewPassword("NewPassword@123");

        OtpToken otpToken = passwordResetOtp("123456");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("user@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(otpToken));
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("$2a$new-password");

        authService.resetPassword(request);

        assertEquals("$2a$new-password", user.getPassword());
        assertNotNull(otpToken.getConsumedAt());
        verify(userRepository).save(user);
        verify(otpTokenRepository, times(1)).save(otpToken);
    }

    @Test
    void resetPassword_shouldFail_whenOtpInvalid() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setEmail("user@example.com");
        request.setOtp("000000");
        request.setNewPassword("NewPassword@123");

        OtpToken otpToken = passwordResetOtp("123456");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("user@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(otpToken));

        assertThrows(InvalidOtpException.class, () -> authService.resetPassword(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyOtp_shouldSaveUser_whenSignupOtpValid() {
        OtpVerificationRequestDTO request = new OtpVerificationRequestDTO();
        request.setEmail("user@example.com");
        request.setOtp("123456");

        OtpToken signupOtp = signupOtp("123456");
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("user@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("user@example.com", OtpPurpose.SIGNUP_VERIFICATION))
                .thenReturn(Optional.of(signupOtp));
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        RegisterResponseDTO response = authService.verifyOtp(request);

        assertEquals(99L, response.getUserId());
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyOtp_shouldFail_whenEmailWasCreatedAfterOtpRequest() {
        OtpVerificationRequestDTO request = new OtpVerificationRequestDTO();
        request.setEmail("user@example.com");
        request.setOtp("123456");

        OtpToken signupOtp = signupOtp("123456");
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("user@example.com", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("user@example.com", OtpPurpose.SIGNUP_VERIFICATION))
                .thenReturn(Optional.of(signupOtp));
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                () -> authService.verifyOtp(request));

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createAdminUser_shouldCreateUserWithoutOtp_whenEmailAvailable() {
        AdminCreateUserRequestDTO request = adminCreateRequest("  NEW@Example.COM ");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$admin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var response = authService.createAdminUser(request);

        assertEquals(10L, response.getUserId());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new@example.com", userCaptor.getValue().getEmail());
        verify(otpEventPublisher, never()).publish(any());
    }

    @Test
    void createAdminUser_shouldFail_whenEmailExistsIgnoringCase() {
        AdminCreateUserRequestDTO request = adminCreateRequest("TEST@Mail.com");
        when(userRepository.existsByEmailIgnoreCase("test@mail.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
                () -> authService.createAdminUser(request));

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void handleGoogleLogin_shouldReturnTokens_whenExistingUserFound() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user@example.com", "STAFF", 1L)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user@example.com")).thenReturn("refresh-token");

        LoginResponseDTO response = authService.handleGoogleLogin("user@example.com", "User Example");

        assertEquals("access-token", response.getAccessToken());
        verify(userRepository).save(user);
    }

    @Test
    void handleGoogleLogin_shouldCreateUserAndReturnTokens_whenUserMissing() {
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$google");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("new@example.com", "STAFF", null)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("new@example.com")).thenReturn("refresh-token");

        LoginResponseDTO response = authService.handleGoogleLogin("new@example.com", "Google User");

        assertEquals("access-token", response.getAccessToken());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        assertTrue(userCaptor.getAllValues().get(0).getPassword().startsWith("$2a$"));
        assertEquals(UserRole.STAFF, userCaptor.getAllValues().get(0).getRole());
    }

    @Test
    void deactivate_shouldRejectSelfDeactivation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        SelfDeactivationNotAllowedException exception = assertThrows(SelfDeactivationNotAllowedException.class,
                () -> authService.deactivate(1L, "user@example.com"));

        assertEquals("You cannot deactivate your own account.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserSummary_shouldReturnCounts() {
        User admin = new User();
        admin.setId(2L);
        admin.setName("Admin");
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);
        admin.setIsActive(true);
        admin.setLastLoginAt(LocalDateTime.now());

        user.setLastLoginAt(LocalDateTime.now());
        when(userRepository.findAll()).thenReturn(List.of(user, admin));

        var summary = authService.getUserSummary();

        assertEquals(2, summary.getTotalUsers());
        assertEquals(2, summary.getActiveUsers());
        assertEquals(0, summary.getInactiveUsers());
        assertEquals(1, summary.getAdminCount());
        assertEquals(1, summary.getWarehouseStaffCount());
        assertEquals(2, summary.getRecentLoginCount());
    }

    @Test
    void getUsersPage_shouldSearchByTextAndIgnoreAllFilters() {
        user.setName("Suraj Admin");
        user.setEmail("suraj.admin@stockpro.com");
        user.setPhone("9876543201");
        user.setDepartment("Administration");
        user.setRole(UserRole.ADMIN);

        when(userRepository.searchUsers(eq("Suraj"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        var result = authService.getUsersPage(0, 50, "  Suraj  ", "ALL", "ALL");

        assertEquals(1, result.getTotalElements());
        assertEquals("Suraj Admin", result.getContent().get(0).getName());
        verify(userRepository).searchUsers(eq("Suraj"), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getUsersPage_shouldMapRoleAndActiveStatusFilters() {
        when(userRepository.searchUsers(isNull(), eq(UserRole.MANAGER), eq(Boolean.TRUE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        authService.getUsersPage(0, 50, "", "manager", "active");

        verify(userRepository).searchUsers(isNull(), eq(UserRole.MANAGER), eq(Boolean.TRUE), any(Pageable.class));
    }

    @Test
    void getUsersPage_shouldMapInactiveStatusFilter() {
        when(userRepository.searchUsers(isNull(), isNull(), eq(Boolean.FALSE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        authService.getUsersPage(0, 50, null, null, "INACTIVE");

        verify(userRepository).searchUsers(isNull(), isNull(), eq(Boolean.FALSE), any(Pageable.class));
    }

    @Test
    void refresh_shouldIssueNewTokensAndBlacklistRefreshToken() {
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.extractType("refresh-token")).thenReturn("REFRESH");
        when(jwtUtil.extractUsername("refresh-token")).thenReturn("user@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user@example.com", "STAFF", 1L)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken("user@example.com")).thenReturn("new-refresh");

        LoginResponseDTO response = authService.refresh("refresh-token");

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(jwtUtil).blacklistToken("refresh-token");
    }

    @Test
    void refresh_shouldRejectInvalidTokenType() {
        when(jwtUtil.validateToken("access-token")).thenReturn(true);
        when(jwtUtil.extractType("access-token")).thenReturn("ACCESS");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.refresh("access-token"));

        assertEquals("Not a refresh token", exception.getMessage());
        verify(jwtUtil, never()).blacklistToken(anyString());
    }

    @Test
    void profileMethods_shouldReadAndUpdateAuthenticatedUserData() {
        UpdateProfileDTO updateProfileDTO = new UpdateProfileDTO();
        updateProfileDTO.setName("Updated User");
        updateProfileDTO.setPhone("8888888888");
        updateProfileDTO.setDepartment("Warehouse");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserResponseDTO profile = authService.getUserProfile(" user@example.com ");
        UserResponseDTO updated = authService.updateProfile("user@example.com", updateProfileDTO);

        assertEquals(1L, profile.getUserId());
        assertEquals("Updated User", updated.getName());
        assertEquals("8888888888", updated.getPhone());
        verify(userRepository, times(2)).findByEmailIgnoreCase("user@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_shouldEncodeNewPassword_whenOldPasswordMatches() {
        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setOldPassword("OldPassword@123");
        changePasswordDTO.setNewPassword("NewPassword@123");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("$2a$new-password");

        authService.changePassword("user@example.com", changePasswordDTO);

        assertEquals("$2a$new-password", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void adminManagementMethods_shouldUpdateUserRoleStatusAndActivation() {
        AdminUpdateUserRequestDTO updateRequest = new AdminUpdateUserRequestDTO();
        updateRequest.setName(" Updated Name ");
        updateRequest.setPhone(" 8888888888 ");
        updateRequest.setDepartment(" Ops ");
        updateRequest.setIsActive(false);

        AdminChangeUserRoleRequestDTO changeRoleRequest = new AdminChangeUserRoleRequestDTO();
        changeRoleRequest.setRole(UserRole.MANAGER);

        user.setEmail("staff@example.com");
        user.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO updated = authService.updateAdminUser(1L, updateRequest);
        UserResponseDTO roleChanged = authService.changeUserRole(1L, changeRoleRequest, "admin@example.com");
        authService.activate(1L);
        authService.deactivate(1L, "admin@example.com");

        assertEquals("Updated Name", updated.getName());
        assertEquals(UserRole.MANAGER, roleChanged.getRole());
        assertEquals(Boolean.FALSE, user.getIsActive());
        verify(otpMailService).sendAccountReactivatedEmail("staff@example.com", "Updated Name");
        verify(otpMailService).sendAccountDeactivatedEmail("staff@example.com", "Updated Name");
    }

    @Test
    void adminManagementMethods_shouldRejectInvalidSelfRoleChangeAndMissingProfile() {
        AdminChangeUserRoleRequestDTO changeRoleRequest = new AdminChangeUserRoleRequestDTO();
        changeRoleRequest.setRole(UserRole.STAFF);

        user.setEmail("admin@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException selfRoleException = assertThrows(RuntimeException.class,
                () -> authService.changeUserRole(1L, changeRoleRequest, "admin@example.com"));
        ResourceNotFoundException missingUserException = assertThrows(ResourceNotFoundException.class,
                () -> authService.getUserProfile("missing@example.com"));

        assertEquals("You cannot change your own admin role.", selfRoleException.getMessage());
        assertEquals("User not found", missingUserException.getMessage());
    }

    private OtpToken passwordResetOtp(String otp) {
        OtpToken token = new OtpToken();
        token.setEmail("user@example.com");
        token.setOtpCode(otp);
        token.setPurpose(OtpPurpose.PASSWORD_RESET);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        return token;
    }

    private OtpToken signupOtp(String otp) {
        OtpToken token = new OtpToken();
        token.setEmail("user@example.com");
        token.setOtpCode(otp);
        token.setPurpose(OtpPurpose.SIGNUP_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        token.setName("User Example");
        token.setPasswordHash("$2a$encoded");
        token.setPhone("9999999999");
        token.setRole(UserRole.STAFF);
        token.setDepartment("Ops");
        return token;
    }

    private AdminCreateUserRequestDTO adminCreateRequest(String email) {
        AdminCreateUserRequestDTO request = new AdminCreateUserRequestDTO();
        request.setName("Admin Created");
        request.setEmail(email);
        request.setPassword("Password@123");
        request.setRole(UserRole.MANAGER);
        request.setDepartment("Ops");
        request.setPhone("9999999999");
        request.setIsActive(true);
        return request;
    }
}
