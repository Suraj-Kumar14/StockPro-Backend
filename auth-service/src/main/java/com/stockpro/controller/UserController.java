package com.stockpro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockpro.dtos.LoginRequest;
import com.stockpro.dtos.RegisterRequest;
import com.stockpro.dtos.UpdateProfileRequest;
import com.stockpro.dtos.UserResponseDTO;
import com.stockpro.dtos.AuthResponse;
import com.stockpro.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth/user")
@Tag(name = "User Authentication", description = "User must have to register or login for using our service")
public class UserController {


	private UserService userService;
	public UserController(UserService userService) {
		this.userService = userService;
	}

	/*
	 * Example Data for swagger-ui
	 */

    private static final String REGISTER_EXAMPLE = """
            {
                "fullName":"ravi",
                "email":"ravi@gmail.com",
                "password":"ravi1234",
                "phone":"8970676956"
            }
            """;

        private static final String LOGIN_EXAMPLE = """
            {
                "email":"ravi@gmail.com",
                "password":"ravi1234"
            }
            """;


    /*
     * This is for testing welcome API
     */
	@GetMapping("/welcome")
	@Operation(summary = "0. Welcome API", description = "Check if service is running")
	public String welcome() {
		return "welcome! It is working.";
	}


	/*
	 * This is register request API- this will send OTP for new user
	 */
	@PostMapping("/register-request")
	 @Operation(
		summary = "1. Register Request (Send OTP)",
	    description = "User submits registration details and receives OTP on email",
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	        required = true,
    		content = @Content(examples = {
    			@ExampleObject(name = "User Register Testing", value = REGISTER_EXAMPLE)
    		})
	    )
	)
	public ResponseEntity<String> registerationRequest(@RequestBody RegisterRequest registerRequest) {
		return ResponseEntity.ok(userService.registerRequest(registerRequest));
	}


	/*
	 * This method validate the email and correct OTP for new user
	 */
	@PostMapping("/register-user")
	@Operation(
	    summary = "2. Verify OTP & Create Account",
	    description = "User verifies OTP and account is created"
	)
	public ResponseEntity<AuthResponse> registerUsers(@Parameter(description = "User Email", example = "ravi@gmail.com") @RequestParam String email, @Parameter(description = "OTP received on email", example = "123456") @RequestParam String otp) {
		return ResponseEntity.ok(userService.registerUser(email,otp));
	}


	/*
	 * This is login API - it will check user exist in DB for login
	 */
	@PostMapping("/login")
	@Operation(
	    summary = "3. Login User",
	    description = "User login with email and password",
	    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
	        required = true,
    		content = @Content(examples = {
    			@ExampleObject(name = "User Login Testing", value = LOGIN_EXAMPLE)
    		})
	    )
	)
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
		return ResponseEntity.ok(userService.loginUser(loginRequest));
	}


	/*
	 * STEP 1: Request the OTP
	 */
	@PostMapping("/forgot-password/request")
	@Operation(
	    summary = "4. Request OTP",
	    description = "User enters email to receive OTP"
	)
    public ResponseEntity<String> requestOtp(@Parameter(description = "User Email", example = "ravi@gmail.com") @RequestParam String email) {
        String response = userService.initiateForgetPassword(email);
        return ResponseEntity.ok(response);
    }


	/*
	 * STEP 2: Verify the OTP
	 */
    @PostMapping("/forgot-password/verify")
	@Operation(
	    summary = "5. Verify OTP",
		description = "Verify OTP sent to email"
	)
    public ResponseEntity<String> verifyOtp(@Parameter(description = "User Email", example = "ravi@gmail.com") @RequestParam String email, @Parameter(description = "OTP", example = "123456") @RequestParam String otp) {
        String response = userService.verifyOtp(email, otp);
        return ResponseEntity.ok(response);
    }


    /*
     *  STEP 3: Submit New Password
     */
    @PostMapping("/forgot-password/reset")
	@Operation(
	    summary = "6. Reset Password",
	    description = "Set new password after OTP verification"
	)
    public ResponseEntity<String> resetPassword(@Parameter(description = "User Email", example = "ravi@gmail.com") @RequestParam String email, @Parameter(description = "New Password", example = "newpass123") @RequestParam String newPassword) {
        String response = userService.resetPassword(email, newPassword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{email}")
    @Operation(summary = "7. Get User by email")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PostMapping("/update-profile/{email}")
    @Operation(
        summary = "8a. Update Profile",
        description = "Updates user info. NOTE: If email is changed, account becomes INACTIVE until new email is verified via OTP.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(value = "{\"fullName\":\"Ravi K\",\"email\":\"newravi@gmail.com\",\"phone\":\"1234567890\"}"))
        )
    )
    public ResponseEntity<UserResponseDTO> updateProfile(@PathVariable String email, @RequestBody UpdateProfileRequest updateRequest) {
        return ResponseEntity.ok(userService.updateProfile(email, updateRequest));
    }

    @PostMapping("/verify-email-update")
    @Operation(summary = "8b. Verify Email Update OTP", description = "Finalizes the email change after OTP verification")
    public ResponseEntity<String> verifyEmailUpdate(@RequestParam String currentEmail, @RequestParam String otp) {
        return ResponseEntity.ok(userService.verifyEmailUpdate(currentEmail, otp));
    }

	@Operation(summary = "Deactivate user account")
    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}