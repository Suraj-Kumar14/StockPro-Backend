package com.stockpro.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stockpro.dtos.LoginRequest;
import com.stockpro.dtos.RegisterRequest;
import com.stockpro.dtos.UpdateProfileRequest;
import com.stockpro.dtos.UserResponseDTO;
import com.stockpro.config.Roles;
import com.stockpro.dtos.AuthResponse;
import com.stockpro.entity.User;
import com.stockpro.exception.BadRequestException;
import com.stockpro.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User Service Implementation
 *
 * ROLE ASSIGNMENT STRATEGY:
 * ========================
 * 1. Email/Password Registration: Users get WAREHOUSE_STAFF role by default
 *    - Reason: WAREHOUSE_STAFF is the lowest privilege role for regular employees
 *    - Admin can promote users to INVENTORY_MANAGER, PURCHASE_OFFICER, or ADMIN
 *
 * 2. Google OAuth2 Login: Users also get WAREHOUSE_STAFF role by default
 *    - Ensures consistency with email registration
 *    - Admin can assign different roles based on department
 *
 * 3. Role Promotion: Only ADMIN can change user roles (via Admin Service)
 *    - This prevents privilege escalation
 *    - Each role has specific permissions in SecurityConfig
 *
 * ROLES DEFINED:
 * ==============
 * - ADMIN: Full system access, can deactivate/manage users
 * - INVENTORY_MANAGER: Access to inventory module
 * - WAREHOUSE_STAFF: Access to warehouse operations (default)
 * - PURCHASE_OFFICER: Access to purchase module
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImp implements UserService{
	private static final SecureRandom random = new SecureRandom();
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailService emailService;

	/**
	 * Step 1 of Registration: User submits their details and receives OTP
	 * - Generates 6-digit OTP
	 * - Sends OTP to email
	 * - Creates or reactivates user account (but keeps it inactive until OTP is verified)
	 */
	@Transactional
	@Override
    public String registerRequest(RegisterRequest registerRequest) {
        // Normalize email to lowercase for consistency
        String normalizedEmail = normalizeEmail(registerRequest.getEmail());
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();

            // If user is already active, prevent re-registration
            if (user.isActive()) {
                throw new RuntimeException("User already registered with this email!");
            }

            // If user is inactive, update their info (in case they changed details)
            updateUserDetails(user, registerRequest);
        } else {
            // Create a brand new user with WAREHOUSE_STAFF role (default)
            user = new User();
            user.setFullName(registerRequest.getFullName());
            user.setEmail(normalizedEmail);
            user.setRole(Roles.WAREHOUSE_STAFF); // Default role for new users
            user.setActive(false); // Not active until OTP is verified
			user.setDepartment(registerRequest.getDepartment());
            updateUserDetails(user, registerRequest);
        }

        // Generate 6-digit OTP and set expiry to 5 minutes
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        // Save user with pending status
        userRepository.save(user);

        // Send OTP email
        emailService.sendOtpEmail(user.getEmail(), otp, "Registration OTP", "REGISTER");

        log.info("Registration request received for email: {}", normalizedEmail);
        return "OTP sent to your email for verification.";
    }

    private void updateUserDetails(User user, RegisterRequest request) {
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        // Hash password immediately using PasswordEncoder
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }

    /**
     * Step 2 of Registration: User verifies OTP and account is activated
     * - Validates OTP matches and hasn't expired
     * - Activates user account
     * - Generates JWT token
     */
    @Override
	public AuthResponse registerUser(String email, String otp) {
		email = normalizeEmail(email);
		otp = normalizeOtp(otp);

		if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
			throw new BadRequestException("Email and OTP are required.");
		}

		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));

		// Validate OTP matches
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new BadRequestException("OTP Invalid! please try again.");
		}

		// Validate OTP hasn't expired
		if (userdb.getOtpExpiry() == null || userdb.getOtpExpiry().isBefore(LocalDateTime.now())) {
			throw new BadRequestException("OTP Expired! please try again.");
		}

		// Activate account
		userdb.setActive(true);
		userdb.setOtpCode(null);
		userRepository.save(userdb);

		String token = jwtService.generateToken(email, userdb.getUserId());
		log.info("User registered successfully: {}", email);
		return new AuthResponse(token, "User Register success");
	}

	/**
	 * Login with email and password
	 * - Validates email exists
	 * - Validates password
	 * - Validates account is active
	 * - Generates JWT token
	 */
	@Override
	public AuthResponse loginUser(LoginRequest loginRequest) {
		// Normalize email to lowercase
		String normalizedEmail = normalizeEmail(loginRequest.getEmail());
		User userdb = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new RuntimeException("User not found!"));

		// Validate password using encoder
		if (!passwordEncoder.matches(loginRequest.getPassword(), userdb.getPasswordHash())) {
			throw new RuntimeException("Invalid Password!");
		}
		
		// Check if account is active (email verified via OTP)
		if(!userdb.isActive()) {
			throw new RuntimeException("Account is not verified. Please verify your email using the OTP sent during registration.");
		}
		
		// Update last login time
		userdb.setLastLoginAt(LocalDateTime.now());
        userRepository.save(userdb);

		// Generate JWT token
		String token = jwtService.generateToken(normalizedEmail, userdb.getUserId());
		log.info("User login successful: {}", normalizedEmail);
		return new AuthResponse(token, "Login Success");
	}

	/**
	 * Step 1 of Forgot Password: Send OTP to user's email
	 * - Validates email exists in database
	 * - Generates OTP
	 * - Sends OTP to email
	 */
	@Override
	public String initiateForgetPassword(String email) {
		email = normalizeEmail(email);
		User user = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));

		// Generate 6-digit OTP
		String otp = generateOtp();

		// Save OTP and expiry (valid for 5 minutes)
		user.setOtpCode(otp);
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
		userRepository.save(user);

		// Send OTP email
		emailService.sendOtpEmail(email, otp, "Forgot Password OTP", "FORGOT_PASSWORD");

		log.info("Forgot password request for email: {}", email);
		return "Verification code sent to your email.";
	}

	/**
	 * Step 2 of Forgot Password: Verify OTP is correct
	 * - Validates OTP matches
	 * - Validates OTP hasn't expired
	 */
	@Override
	public String verifyOtp(String email, String otp) {
		email = normalizeEmail(email);
		otp = normalizeOtp(otp);

		if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
			throw new BadRequestException("Email and OTP are required.");
		}

		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));

		// Validate OTP matches
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new BadRequestException("OTP Invalid! please try again.");
		}

		// Validate OTP hasn't expired
		if (!userdb.getOtpExpiry().isAfter(LocalDateTime.now())) {
			throw new BadRequestException("OTP Expired! please try again.");
		}
		log.info("OTP verified for email: {}", email);
		return "OTP Verified. You may now reset your password.";
	}

	/**
	 * Step 3 of Forgot Password: Reset password to new value
	 * - Encodes new password
	 * - Clears OTP after use
	 */
	@Override
	public String resetPassword(String email, String newPassword) {
		email = normalizeEmail(email);
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		// Encode new password before saving
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setOtpCode(null); // Clear OTP after use
		userRepository.save(user);

		log.info("Password reset for email: {}", email);
		return "Password updated successfully.";
	}
	
	/**
	 * Generate random 6-digit OTP
	 */
	private String generateOtp() {
	    int otp = 100000 + random.nextInt(900000);
	    return String.valueOf(otp);
	}

	/**
	 * Normalize email: trim whitespace and convert to lowercase
	 */
	private String normalizeEmail(String email) {
	    return email == null ? null : email.trim().toLowerCase();
	}

	/**
	 * Normalize OTP: trim whitespace
	 */
	private String normalizeOtp(String otp) {
	    return otp == null ? null : otp.trim();
	}

	/**
	 * Update user profile (fullName, phone, email)
	 * - If email changes, sends OTP to new email for verification
	 * - Sets account to inactive until new email is verified
	 */
	@Override
	@Transactional
	public UserResponseDTO updateProfile(String email, UpdateProfileRequest updateUser) {
	    email = normalizeEmail(email);
	    User userdb = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found!"));

	    String normalizedNewEmail = normalizeEmail(updateUser.getEmail());
	    boolean isEmailChanging = !userdb.getEmail().equalsIgnoreCase(normalizedNewEmail);

	    if (isEmailChanging) {
	        // Check if new email is already in use
	        if (userRepository.findByEmail(normalizedNewEmail).isPresent()) {
	            throw new RuntimeException("Email already in use by another account!");
	        }

	        // Store new email as PENDING until verified
	        userdb.setPendingEmail(normalizedNewEmail);
	        
	        // Generate OTP for email verification
	        String otp = generateOtp();
	        userdb.setOtpCode(otp);
	        userdb.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
	        
	        emailService.sendOtpEmail(normalizedNewEmail, otp, "Email Update Verification", "UPDATE_EMAIL");
	    }

	    // Update non-sensitive fields immediately
	    userdb.setFullName(updateUser.getFullName());
	    userdb.setPhone(updateUser.getPhone());

	    userRepository.save(userdb);

	    return new UserResponseDTO(
				userdb.getUserId(),
	            userdb.getFullName(), 
	            email,
	            userdb.getPhone(), 
	            userdb.getRole(), 
	            userdb.isActive(), 
				userdb.getDepartment()

	    );
	}
	
	/**
	 * Verify email change OTP
	 * - Validates OTP
	 * - Confirms new email (moves from pending to active)
	 */
	@Transactional
	public String verifyEmailUpdate(String currentEmail, String otp) {
	    currentEmail = normalizeEmail(currentEmail);
	    User user = userRepository.findByEmail(currentEmail)
	            .orElseThrow(() -> new RuntimeException("User not found!"));

	    // Validate OTP
	    if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
	        throw new RuntimeException("Invalid OTP!");
	    }
	    if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
	        throw new RuntimeException("OTP Expired!");
	    }

	    // Perform email swap from pending to active
	    if (user.getPendingEmail() != null) {
	        user.setEmail(user.getPendingEmail());
	        user.setPendingEmail(null); // Clear pending status
	        user.setOtpCode(null);
	        userRepository.save(user);
	        log.info("Email updated successfully for user: {}", user.getEmail());
	        return "Email updated successfully to " + user.getEmail();
	    }

	    throw new RuntimeException("No pending email update found.");
	}

	/**
	 * Get user details by email
	 */
	@Override
	public UserResponseDTO getUserByEmail(String email) {
		email = normalizeEmail(email);
		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));
		return new UserResponseDTO(userdb.getUserId(), userdb.getFullName(), userdb.getEmail(), userdb.getPhone(), userdb.getRole(), userdb.isActive(),userdb.getDepartment());
	}

	/**
	 * Get all users (debug endpoint)
	 */
	@Override
	public List<UserResponseDTO> getAllUsers() {
		return userRepository.findAll().stream()
			.map(user -> new UserResponseDTO(user.getUserId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(), user.getDepartment()))
			.collect(Collectors.toList());
	}

	/**
	 * Admin deactivates user account
	 */
	@Override
	@Transactional
	public void deactivateUser(Long id) {
	    User user = userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    user.setActive(false);
	    userRepository.save(user);
	    log.info("User deactivated by admin: {}", id);
	}

	/**
	 * Logout endpoint (token invalidation handled by frontend)
	 */
	@Override
	public void logout(String token) {
	    // Token invalidation is handled on frontend side
	    // Backend does not maintain token blacklist in this simple implementation
	}

}