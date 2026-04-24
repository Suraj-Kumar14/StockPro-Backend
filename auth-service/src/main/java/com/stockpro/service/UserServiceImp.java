package com.stockpro.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
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

@RequiredArgsConstructor
@Service
public class UserServiceImp implements UserService{
	private static final SecureRandom random = new SecureRandom();
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final EmailService emailService;

	// register request
	@Transactional
	@Override
	public String registerRequest(RegisterRequest registerRequest) {
		String normalizedEmail = normalizeEmail(registerRequest.getEmail());

		if (normalizedEmail == null || normalizedEmail.isBlank()) {
			throw new BadRequestException("Email is required.");
		}

		Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

		User user;
		if (userOptional.isPresent()) {
			user = userOptional.get();

			if (user.isActive()) {
				throw new BadRequestException("User already registered with this email!");
			}

			updateUserDetails(user, registerRequest);
			user.setDepartment(registerRequest.getDepartment());
		} else {
			user = new User();
			user.setFullName(registerRequest.getFullName());
			user.setEmail(normalizedEmail);
			user.setRole(Roles.WAREHOUSE_STAFF);
			user.setActive(false);
			user.setDepartment(registerRequest.getDepartment());
			updateUserDetails(user, registerRequest);
		}

		String otp = generateOtp();
		user.setOtpCode(otp);
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

		userRepository.save(user);

		emailService.sendOtpEmail(user.getEmail(), otp, "Registration OTP", "REGISTER");

		return "OTP sent to your email for verification.";
	}

	private void updateUserDetails(User user, RegisterRequest request) {
		if (request.getFullName() == null || request.getFullName().isBlank()) {
			throw new BadRequestException("Full name is required.");
		}
		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new BadRequestException("Password is required.");
		}
		if (request.getPhone() == null || request.getPhone().isBlank()) {
			throw new BadRequestException("Phone is required.");
		}

		user.setFullName(request.getFullName().trim());
		user.setPhone(request.getPhone().trim());
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
	}

    @Override
	public AuthResponse registerUser(String email, String otp) {
		email = normalizeEmail(email);
		otp = normalizeOtp(otp);

		if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
			throw new BadRequestException("Email and OTP are required.");
		}

		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));

		/*
		 * Here First we validate the OTP with our DB
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new BadRequestException("OTP Invalid! please try again.");
		}

		/*
		 * here - Validatation of OTP is expire or not
		 */
		if (userdb.getOtpExpiry() == null || userdb.getOtpExpiry().isBefore(LocalDateTime.now())) {
			throw new BadRequestException("OTP Expired! please try again.");
		}

		userdb.setActive(true);
		userdb.setOtpCode(null);
		userRepository.save(userdb);
		String token = jwtService.generateToken(email, userdb.getUserId(), userdb.getRole());
		return new AuthResponse(token, "User Register success");
	}

	// login
	@Override
	public AuthResponse loginUser(LoginRequest loginRequest) {
		/*
		 * first we check email that exist in DB - normalize email to lowercase
		 */
		String normalizedEmail = normalizeEmail(loginRequest.getEmail());
		User userdb = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new RuntimeException("User not found!"));

		if (!passwordEncoder.matches(loginRequest.getPassword(), userdb.getPasswordHash())) {
			throw new RuntimeException("Invalid Password!");
		}
		
		/*
		 * here - CHECK ACTIVE STATUS
		 */
		if(!userdb.isActive()) {
			throw new RuntimeException("Account is not verified. Please verify your email using the OTP sent during registration.");
		}
		
		userdb.setLastLoginAt(LocalDateTime.now());
        userRepository.save(userdb);


		/*
		 * only Active user can login
		 */
		String token = jwtService.generateToken(normalizedEmail, userdb.getUserId(), userdb.getRole());
		return new AuthResponse(token, "Login Success");
	}

	/*
	 * First We validate email- that exit in our database or not If email exist in
	 * our database then I will send 6-digit verification code to email otherwise
	 * stop
	 */
	@Override
	public String initiateForgetPassword(String email) {
		email = normalizeEmail(email);
		User user = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));

		// 1. Generate 6-digit code
		String otp = generateOtp();

		// 2. Save OTP and Expiry (e.g., 5 minutes from now) to DB
		user.setOtpCode(otp);
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
		userRepository.save(user);

		// 3. Here I will- Call to EmailService to send 'otp' to 'email' with subject,
		// and purpose
		emailService.sendOtpEmail(email, otp, "Forgot Password OTP", "FORGOT_PASSWORD");

		return "Verification code sent to your email.";
	}

	/*
	 * Once user get OTP then we verify that in our DB -
	 */
	@Override
	public String verifyOtp(String email, String otp) {
		email = normalizeEmail(email);
		otp = normalizeOtp(otp);

		if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
			throw new BadRequestException("Email and OTP are required.");
		}

		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));

		/*
		 * Here First we validate the OTP with our DB
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new BadRequestException("OTP Invalid! please try again.");
		}

		/*
		 * here - Validatation of OTP is expire or not
		 */
		if (!userdb.getOtpExpiry().isAfter(LocalDateTime.now())) {
			throw new BadRequestException("OTP Expired! please try again.");
		}
		return "OTP Verified. You may now reset your password.";
	}

	/*
	 * Finally we update the password
	 */
	@Override
	public String resetPassword(String email, String newPassword) {
		email = normalizeEmail(email);
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		// Ensure you encode the password before saving!
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setOtpCode(null); // Clear OTP after use
		userRepository.save(user);

		return "Password updated successfully.";
	}
	
	/*
	 * This function will generate OTP
	 */
	private String generateOtp() {
	    int otp = 100000 + random.nextInt(900000);
	    return String.valueOf(otp);
	}

	private String normalizeEmail(String email) {
	    return email == null ? null : email.trim().toLowerCase();
	}

	private String normalizeOtp(String otp) {
	    return otp == null ? null : otp.trim();
	}

	@Override
	@Transactional
	public UserResponseDTO updateProfile(String email, UpdateProfileRequest updateUser) {
	    email = normalizeEmail(email);
	    User userdb = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found!"));

	    String normalizedNewEmail = normalizeEmail(updateUser.getEmail());
	    boolean isEmailChanging = !userdb.getEmail().equalsIgnoreCase(normalizedNewEmail);

	    if (isEmailChanging) {
	        // Check if the NEW email is already taken
	        if (userRepository.findByEmail(normalizedNewEmail).isPresent()) {
	            throw new RuntimeException("Email already in use by another account!");
	        }

	        // Store the new email as PENDING, do not change the main email yet
	        userdb.setPendingEmail(normalizedNewEmail);
	        
	        String otp = generateOtp();
	        userdb.setOtpCode(otp);
	        userdb.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
	        
	        emailService.sendOtpEmail(normalizedNewEmail, otp, "Email Update Verification", "UPDATE_EMAIL");
	    }

	    // Update other non-sensitive fields immediately
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
	
	@Transactional
	public String verifyEmailUpdate(String currentEmail, String otp) {
	    currentEmail = normalizeEmail(currentEmail);
	    User user = userRepository.findByEmail(currentEmail)
	            .orElseThrow(() -> new RuntimeException("User not found!"));

	    // 1. Validate OTP
	    if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
	        throw new RuntimeException("Invalid OTP!");
	    }
	    if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
	        throw new RuntimeException("OTP Expired!");
	    }

	    // 2. Perform the swap
	    if (user.getPendingEmail() != null) {
	        user.setEmail(user.getPendingEmail());
	        user.setPendingEmail(null); // Clear the pending status
	        user.setOtpCode(null);
	        userRepository.save(user);
	        return "Email updated successfully to " + user.getEmail();
	    }

	    throw new RuntimeException("No pending email update found.");
	}

	@Override
	public UserResponseDTO getUserByEmail(String email) {		email = normalizeEmail(email);		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new BadRequestException("User not found!"));
		return new UserResponseDTO(userdb.getUserId(), userdb.getFullName(), userdb.getEmail(), userdb.getPhone(), userdb.getRole(), userdb.isActive(),userdb.getDepartment());
	}

	@Override
	public List<UserResponseDTO> getAllUsers() {
		return userRepository.findAll().stream()
			.map(user -> new UserResponseDTO(user.getUserId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(), user.getDepartment()))
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void deactivateUser(Long id) {
	    User user = userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("User not found"));

	    user.setActive(false); // or setEnabled(false)
	    userRepository.save(user);
	}

	@Override
	public void logout(String token) {
	    // Do nothing - client should remove token
	}

}
