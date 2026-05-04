package com.stockpro.authservice.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stockpro.authservice.dto.AdminUpdateUserRequestDTO;
import com.stockpro.authservice.dto.ChangePasswordDTO;
import com.stockpro.authservice.dto.CreateUserRequestDTO;
import com.stockpro.authservice.dto.ForgotPasswordRequestDTO;
import com.stockpro.authservice.dto.LoginResponseDTO;
import com.stockpro.authservice.dto.MessageResponseDTO;
import com.stockpro.authservice.dto.OtpVerificationRequestDTO;
import com.stockpro.authservice.dto.RegisterResponseDTO;
import com.stockpro.authservice.dto.ResetPasswordRequestDTO;
import com.stockpro.authservice.dto.UpdateProfileDTO;
import com.stockpro.authservice.dto.UserRequestDTO;
import com.stockpro.authservice.dto.UserResponseDTO;
import com.stockpro.authservice.dto.UserSummaryDTO;
import com.stockpro.authservice.entity.OtpPurpose;
import com.stockpro.authservice.entity.OtpToken;
import com.stockpro.authservice.entity.User;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.InactiveAccountException;
import com.stockpro.authservice.exception.InvalidOtpException;
import com.stockpro.authservice.exception.InvalidCredentialsException;
import com.stockpro.authservice.exception.ResourceNotFoundException;
import com.stockpro.authservice.exception.SelfDeactivationNotAllowedException;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.repository.OtpTokenRepository;
import com.stockpro.authservice.repository.UserRepository;
import com.stockpro.authservice.security.JwtUtil;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpMailService otpMailService;

    @Value("${app.otp.expiry-minutes}")
    private long otpExpiryMinutes;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public AuthService(
            UserRepository userRepository,
            OtpTokenRepository otpTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            OtpMailService otpMailService) {
        this.userRepository = userRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpMailService = otpMailService;
    }

    public RegisterResponseDTO register(UserRequestDTO dto) {
        log.info("Registering user with OTP verification: {}", dto.getEmail());
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        clearActiveOtps(dto.getEmail(), OtpPurpose.SIGNUP_VERIFICATION);

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(dto.getEmail());
        otpToken.setOtpCode(generateOtp());
        otpToken.setPurpose(OtpPurpose.SIGNUP_VERIFICATION);
        otpToken.setName(dto.getName());
        otpToken.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        otpToken.setPhone(dto.getPhone());
        otpToken.setRole(dto.getRole());
        otpToken.setDepartment(dto.getDepartment());
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        otpTokenRepository.save(otpToken);
        log.info("OTP generated for signup: {}", dto.getEmail());

        otpMailService.sendSignupOtp(dto.getEmail(), otpToken.getOtpCode());
        log.info("OTP sent for signup: {}", dto.getEmail());

        return new RegisterResponseDTO(null, "OTP sent to your email. Please verify to complete registration.");
    }

    public RegisterResponseDTO verifyOtp(OtpVerificationRequestDTO dto) {
        log.info("Verifying OTP for: {}", dto.getEmail());

        OtpToken otpToken = findLatestValidOtp(dto.getEmail(), dto.getOtp());

        if (otpToken.getPurpose() == OtpPurpose.SIGNUP_VERIFICATION) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }

            User user = new User();
            user.setName(otpToken.getName());
            user.setEmail(otpToken.getEmail());
            user.setPassword(otpToken.getPasswordHash());
            user.setPhone(otpToken.getPhone());
            user.setRole(otpToken.getRole());
            user.setDepartment(otpToken.getDepartment());
            User savedUser = userRepository.save(user);

            consumeOtp(otpToken);
            clearActiveOtps(dto.getEmail(), OtpPurpose.SIGNUP_VERIFICATION);

            log.info("User registered successfully after OTP verification: {}", dto.getEmail());
            return new RegisterResponseDTO(savedUser.getId(), "User registered successfully");
        }

        log.info("Password reset OTP verified successfully: {}", dto.getEmail());
        return new RegisterResponseDTO(null, "OTP verified successfully");
    }

    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO dto) {
        log.info("Forgot password requested for: {}", dto.getEmail());
        clearActiveOtps(dto.getEmail(), OtpPurpose.PASSWORD_RESET);

        userRepository.findByEmail(dto.getEmail()).ifPresent(user -> {
            OtpToken otpToken = new OtpToken();
            otpToken.setEmail(user.getEmail());
            otpToken.setOtpCode(generateOtp());
            otpToken.setPurpose(OtpPurpose.PASSWORD_RESET);
            otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            otpTokenRepository.save(otpToken);
            log.info("OTP generated for password reset: {}", user.getEmail());
            otpMailService.sendPasswordResetOtp(user.getEmail(), otpToken.getOtpCode());
            log.info("OTP sent for password reset: {}", user.getEmail());
        });

        return new MessageResponseDTO("OTP sent successfully");
    }

    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO dto) {
        log.info("Resetting password with OTP for: {}", dto.getEmail());
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new InvalidOtpException("Invalid OTP"));

        OtpToken otpToken = getValidOtp(dto.getEmail(), dto.getOtp(), OtpPurpose.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        consumeOtp(otpToken);
        clearActiveOtps(dto.getEmail(), OtpPurpose.PASSWORD_RESET);

        return new MessageResponseDTO("Password updated successfully");
    }

    public LoginResponseDTO login(String email, String password) {
        log.info("Login attempt: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InactiveAccountException("Your account is inactive. Please contact administrator.");
        }

        if (!isPasswordValid(password, user)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login successful: {}", email);
        return issueTokens(user);
    }

    public LoginResponseDTO handleGoogleLogin(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, name));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InactiveAccountException("Your account is inactive. Please contact administrator.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return issueTokens(user);
    }

    public LoginResponseDTO refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        String type = jwtUtil.extractType(refreshToken);
        if (!"REFRESH".equals(type)) {
            throw new RuntimeException("Not a refresh token");
        }
        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InactiveAccountException("Your account is inactive. Please contact administrator.");
        }

        jwtUtil.blacklistToken(refreshToken);

        log.info("Token refreshed for: {}", email);
        return issueTokens(user);
    }

    public void logout(String token) {
        jwtUtil.blacklistToken(token);
        log.info("Token blacklisted - user logged out");
    }

    public UserResponseDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    public UserResponseDTO updateProfile(String email, UpdateProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setDepartment(dto.getDepartment());
        userRepository.save(user);
        log.info("Profile updated for: {}", email);
        return mapToDTO(user);
    }

    public void changePassword(String email, ChangePasswordDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (dto.getConfirmPassword() != null && !dto.getConfirmPassword().equals(dto.getNewPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }
        if (!passwordEncoder.matches(dto.getEffectiveCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for: {}", email);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream().map(this::mapToDTO).toList();
    }

    public List<UserResponseDTO> searchUsers(String keyword, UserRole role, Boolean isActive) {
        List<User> users = (keyword == null || keyword.isBlank())
                ? userRepository.findAll()
                : userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword.trim(), keyword.trim());

        return users.stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> isActive == null || Boolean.TRUE.equals(user.getIsActive()) == isActive)
                .map(this::mapToDTO)
                .toList();
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDTO(user);
    }

    public UserSummaryDTO getUserSummary() {
        List<User> users = userRepository.findAll();
        long activeUsers = users.stream().filter(user -> Boolean.TRUE.equals(user.getIsActive())).count();
        long inactiveUsers = users.size() - activeUsers;
        long adminCount = users.stream().filter(user -> user.getRole() == UserRole.ADMIN).count();
        long inventoryManagerCount = users.stream().filter(user -> user.getRole() == UserRole.MANAGER).count();
        long purchaseOfficerCount = users.stream().filter(user -> user.getRole() == UserRole.OFFICER).count();
        long warehouseStaffCount = users.stream().filter(user -> user.getRole() == UserRole.STAFF).count();
        LocalDateTime recentLoginCutoff = LocalDateTime.now().minusDays(7);
        long recentLoginCount = users.stream()
                .filter(user -> user.getLastLoginAt() != null && !user.getLastLoginAt().isBefore(recentLoginCutoff))
                .count();

        return new UserSummaryDTO(
                users.size(),
                activeUsers,
                inactiveUsers,
                adminCount,
                inventoryManagerCount,
                purchaseOfficerCount,
                warehouseStaffCount,
                recentLoginCount);
    }

    public UserResponseDTO createUser(CreateUserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (dto.getConfirmPassword() != null && !dto.getConfirmPassword().equals(dto.getPassword())) {
            throw new RuntimeException("Password and confirm password do not match");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setDepartment(dto.getDepartment());
        user.setIsActive(dto.getIsActive() == null ? true : dto.getIsActive());

        User savedUser = userRepository.save(user);
        log.info("[AUDIT] action=CREATE_USER userId={} role={}", savedUser.getId(), savedUser.getRole());
        return mapToDTO(savedUser);
    }

    public UserResponseDTO updateUser(Long id, AdminUpdateUserRequestDTO dto, String actorEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (actorEmail != null
                && actorEmail.equalsIgnoreCase(user.getEmail())
                && Boolean.FALSE.equals(dto.getIsActive())) {
            throw new SelfDeactivationNotAllowedException("You cannot deactivate your own account.");
        }

        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setDepartment(dto.getDepartment());
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }

        User savedUser = userRepository.save(user);
        log.info("[AUDIT] action=UPDATE_USER userId={} actor={}", id, actorEmail);
        return mapToDTO(savedUser);
    }

    public void deactivate(Long id, String actorEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (actorEmail != null && actorEmail.equalsIgnoreCase(user.getEmail())) {
            throw new SelfDeactivationNotAllowedException("You cannot deactivate your own account.");
        }
        user.setIsActive(false);
        userRepository.save(user);
        log.info("[AUDIT] action=DEACTIVATE userId={} timestamp={}", id, LocalDateTime.now());
        otpMailService.sendAccountDeactivatedEmail(user.getEmail(), user.getName());
    }

    public void activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("User account is already active.");
        }
        user.setIsActive(true);
        userRepository.save(user);
        log.info("[AUDIT] action=ACTIVATE userId={} timestamp={}", id, LocalDateTime.now());
        otpMailService.sendAccountReactivatedEmail(user.getEmail(), user.getName());
    }

    private LoginResponseDTO issueTokens(User user) {
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        return new LoginResponseDTO(accessToken, refreshToken, jwtExpirationMs / 1000, mapToDTO(user));
    }

    private boolean isPasswordValid(String rawPassword, User user) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcrypt(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        if (rawPassword.equals(storedPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            log.info("Upgraded legacy plain-text password to BCrypt for: {}", user.getEmail());
            return true;
        }

        return false;
    }

    private boolean looksLikeBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private User createGoogleUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName((name == null || name.isBlank()) ? email : name);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(UserRole.STAFF);
        user.setIsActive(true);
        user.setProvider("GOOGLE");
        User savedUser = userRepository.save(user);
        log.info("Created new Google-authenticated user: {}", email);
        return savedUser;
    }

    private OtpToken getValidOtp(String email, String otp, OtpPurpose purpose) {
        OtpToken otpToken = otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new InvalidOtpException("Invalid OTP"));

        if (otpToken.isConsumed() || otpToken.isExpired() || !otpToken.getOtpCode().equals(otp)) {
            throw new InvalidOtpException("Invalid OTP");
        }

        return otpToken;
    }

    private OtpToken findLatestValidOtp(String email, String otp) {
        OtpToken passwordResetOtp = otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.PASSWORD_RESET)
                .orElse(null);

        if (isMatchingOtp(passwordResetOtp, otp)) {
            return passwordResetOtp;
        }

        OtpToken signupOtp = otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.SIGNUP_VERIFICATION)
                .orElse(null);

        if (isMatchingOtp(signupOtp, otp)) {
            return signupOtp;
        }

        throw new InvalidOtpException("Invalid OTP");
    }

    private boolean isMatchingOtp(OtpToken otpToken, String otp) {
        return otpToken != null
                && !otpToken.isConsumed()
                && !otpToken.isExpired()
                && otpToken.getOtpCode().equals(otp);
    }

    private void consumeOtp(OtpToken otpToken) {
        otpToken.setConsumedAt(LocalDateTime.now());
        otpTokenRepository.save(otpToken);
    }

    private void clearActiveOtps(String email, OtpPurpose purpose) {
        List<OtpToken> activeTokens = otpTokenRepository.findByEmailAndPurposeAndConsumedAtIsNull(email, purpose);
        if (!activeTokens.isEmpty()) {
            otpTokenRepository.deleteAll(activeTokens);
        }
    }

    private String generateOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    private UserResponseDTO mapToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setDepartment(user.getDepartment());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setRoleLabel(toRoleLabel(user.getRole()));
        dto.setProvider(user.getProvider());
        return dto;
    }

    private String toRoleLabel(UserRole role) {
        return switch (role) {
            case ADMIN -> "Administrator";
            case MANAGER -> "Inventory Manager";
            case OFFICER -> "Purchase Officer";
            case STAFF -> "Warehouse Staff";
        };
    }
}
