package com.stockpro.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.stockpro.dtos.LoginRequest;
import com.stockpro.dtos.RegisterRequest;
import com.stockpro.dtos.UpdateProfileRequest;
import com.stockpro.dtos.UserResponseDTO;
import com.stockpro.dtos.AuthResponse;
import com.stockpro.entity.User;
import com.stockpro.exception.UnauthorizedException;
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
        // 1. Check if user exists
        Optional<User> userOptional = userRepository.findByEmail(registerRequest.getEmail());

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();

            // If user is already active, prevent re-registration
            if (user.isActive()) {
                throw new RuntimeException("User already registered with this email!");
            }

            // If user is inactive, update their info (in case they changed name/phone/pass)
            updateUserDetails(user, registerRequest);
        } else {
            // Create a brand new user
            user = new User();
            user.setFullName(registerRequest.getFullName());
            user.setEmail(registerRequest.getEmail());
            user.setRole("USER");
            user.setActive(false);
			user.setDepartment(registerRequest.getDepartment());
            updateUserDetails(user, registerRequest);
        }

        // 2. Generate and set OTP + Expiry (Refresh on every request)
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        // 3. Save to Database
        userRepository.save(user);

        // 4. Send Email
        emailService.sendOtpEmail(user.getEmail(), otp, "Registration OTP", "REGISTER");

        return "OTP sent to your email for verification.";
    }

    private void updateUserDetails(User user, RegisterRequest request) {
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        // here I Hash the password immediately
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }

    @Override
	public AuthResponse registerUser(String email, String otp) {
		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		/*
		 * Here First we validate the OTP with our DB
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new RuntimeException("OTP Invalid! please try again.");
		}

		/*
		 * here - Validatation of OTP is expire or not
		 */
		if (userdb.getOtpCode() == null || userdb.getOtpExpiry().isBefore(LocalDateTime.now())) {
			throw new RuntimeException("OTP Expired! please try again.");
		}

		userdb.setActive(true);
		userdb.setOtpCode(null);
		userRepository.save(userdb);
		String token = jwtService.generateToken(email, userdb.getUserId());
		return new AuthResponse(token, "User Register success");
	}

	// login
	@Override
	public AuthResponse loginUser(LoginRequest loginRequest) {
		/*
		 * first we check email that exist in DB
		 */
		User userdb = userRepository.findByEmail(loginRequest.getEmail())
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
		
		/*
		 * only Active user can login
		 */
		String token = jwtService.generateToken(loginRequest.getEmail(), userdb.getUserId());
		return new AuthResponse(token, "Login Success");
	}

	/*
	 * First We validate email- that exit in our database or not If email exist in
	 * our database then I will send 6-digit verification code to email otherwise
	 * stop
	 */
	@Override
	public String initiateForgetPassword(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

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
		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));

		/*
		 * Here First we validate the OTP with our DB
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(otp)) {
			throw new RuntimeException("OTP Invalid! please try again.");
		}

		/*
		 * here - Validatation of OTP is expire or not
		 */
		if (userdb.getOtpCode() == null || !userdb.getOtpExpiry().isAfter(LocalDateTime.now())) {
			throw new RuntimeException("OTP Expired! please try again.");
		}
		return "OTP Verified. You may now reset your password.";
	}

	/*
	 * Finally we update the password
	 */
	@Override
	public String resetPassword(String email, String newPassword) {
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

	@Override
	@Transactional
	public UserResponseDTO updateProfile(String email, UpdateProfileRequest updateUser) {
	    User userdb = userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found!"));

	    boolean isEmailChanging = !userdb.getEmail().equalsIgnoreCase(updateUser.getEmail());

	    if (isEmailChanging) {
	        // Check if the NEW email is already taken
	        if (userRepository.findByEmail(updateUser.getEmail()).isPresent()) {
	            throw new RuntimeException("Email already in use by another account!");
	        }

	        // Store the new email as PENDING, do not change the main email yet
	        userdb.setPendingEmail(updateUser.getEmail());
	        
	        String otp = generateOtp();
	        userdb.setOtpCode(otp);
	        userdb.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
	        
	        emailService.sendOtpEmail(updateUser.getEmail(), otp, "Email Update Verification", "UPDATE_EMAIL");
	    }

	    // Update other non-sensitive fields immediately
	    userdb.setFullName(updateUser.getFullName());
	    userdb.setPhone(updateUser.getPhone());

	    userRepository.save(userdb);

	    return new UserResponseDTO(
				userdb.getUserId(),
	            userdb.getFullName(), 
	            userdb.getEmail(), // Still returns the old email as active
	            userdb.getPhone(), 
	            userdb.getRole(), 
	            userdb.isActive(), 
				userdb.getDepartment()

	    );
	}
	
	@Transactional
	public String verifyEmailUpdate(String currentEmail, String otp) {
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
	public UserResponseDTO getUserByEmail(String email) {
		User userdb = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));
		return new UserResponseDTO(userdb.getUserId(), userdb.getFullName(), userdb.getEmail(), userdb.getPhone(), userdb.getRole(), userdb.isActive(),userdb.getDepartment());
	}

}