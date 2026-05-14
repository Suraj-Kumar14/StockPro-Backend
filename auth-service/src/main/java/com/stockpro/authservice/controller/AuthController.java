package com.stockpro.authservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.stockpro.authservice.dto.*;
import com.stockpro.authservice.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh and profile management")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    // ──────────────────────────────────────────────────────────────
    // Public endpoints
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates a new user account. No authentication required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully",
                     content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or email already exists",
                     content = @Content),
        @ApiResponse(responseCode = "409", description = "User already exists",
                     content = @Content)
    })
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        log.info("Register request: {}", dto.getEmail());
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP",
               description = "Validates the OTP for signup or password reset without redirecting the client.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OTP verified successfully",
                     content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid OTP", content = @Content)
    })
    public ResponseEntity<RegisterResponseDTO> verifyOtp(@Valid @RequestBody OtpVerificationRequestDTO dto) {
        return ResponseEntity.ok(authService.verifyOtp(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT tokens",
               description = "Authenticates the user and returns an access token and refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
                     content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                     content = @Content)
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        log.info("Login request: {}", dto.getEmail());
        return ResponseEntity.ok(authService.login(dto.getEmail(), dto.getPassword()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
               description = "Exchanges a valid refresh token for a new access token.")
    @ApiResponse(responseCode = "200", description = "New token issued",
                 content = @Content(schema = @Schema(implementation = LoginResponseDTO.class)))
    public ResponseEntity<LoginResponseDTO> refresh(
            @Parameter(description = "Refresh token string", required = true)
            @RequestBody String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken.trim()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset OTP",
               description = "Sends an OTP to the user email if the account exists.")
    @ApiResponse(responseCode = "200", description = "Password reset OTP initiated",
                 content = @Content(schema = @Schema(implementation = MessageResponseDTO.class)))
    public ResponseEntity<MessageResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        return ResponseEntity.ok(authService.forgotPassword(dto));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP",
               description = "Resets the user password after OTP verification.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password updated successfully",
                     content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid OTP", content = @Content)
    })
    public ResponseEntity<MessageResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        return ResponseEntity.ok(authService.resetPassword(dto));
    }

    // ──────────────────────────────────────────────────────────────
    // Authenticated endpoints
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout (invalidate token)",
               description = "Invalidates the current JWT access token.")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved",
                 content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    public ResponseEntity<UserResponseDTO> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.getUserProfile(auth.getName()));
    }

    @PutMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current user profile")
    @ApiResponse(responseCode = "200", description = "Profile updated",
                 content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    public ResponseEntity<UserResponseDTO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.updateProfile(auth.getName(), dto));
    }

    @PutMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        authService.changePassword(auth.getName(), dto);
        return ResponseEntity.ok("Password changed successfully");
    }

    // ──────────────────────────────────────────────────────────────
    // Admin-only endpoints
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all users (Admin only)")
    @ApiResponse(responseCode = "200", description = "List of all users")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(authService.getUsersPage(page, size, search, role, status));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a user account (Admin only)")
    @ApiResponse(responseCode = "200", description = "User created successfully")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody AdminCreateUserRequestDTO dto) {
        return ResponseEntity.ok(authService.createAdminUser(dto));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a user account (Admin only)")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequestDTO dto) {
        return ResponseEntity.ok(authService.updateAdminUser(id, dto));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change user role (Admin only)")
    @ApiResponse(responseCode = "200", description = "User role updated successfully")
    public ResponseEntity<UserResponseDTO> changeUserRole(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AdminChangeUserRoleRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(authService.changeUserRole(id, dto, authentication.getName()));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user by id (Admin only)")
    @ApiResponse(responseCode = "200", description = "User profile")
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @GetMapping("/users/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user summary (Admin only)")
    @ApiResponse(responseCode = "200", description = "User summary counts")
    public ResponseEntity<UserSummaryDTO> getUserSummary() {
        return ResponseEntity.ok(authService.getUserSummary());
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a user account (Admin only) [DEPRECATED: use PUT /users/{id}/deactivate]")
    @ApiResponse(responseCode = "200", description = "Account deactivated successfully")
    public ResponseEntity<String> deactivateUserLegacy(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        authService.deactivate(id, null);
        return ResponseEntity.ok("Account deactivated successfully");
    }

    @PutMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a user account (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<String> deactivateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            Authentication authentication) {
        log.info("[AUDIT] Admin {} deactivating userId={}", authentication.getName(), id);
        authService.deactivate(id, authentication.getName());
        return ResponseEntity.ok("User deactivated successfully");
    }

    @PatchMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a user account (Admin only)")
    public ResponseEntity<String> deactivateUserPatch(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            Authentication authentication) {
        return deactivateUser(id, authentication);
    }

    @PutMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activate a deactivated user account (Admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<String> activateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            Authentication authentication) {
        log.info("[AUDIT] Admin {} activating userId={}", authentication.getName(), id);
        authService.activate(id);
        return ResponseEntity.ok("User activated successfully");
    }

    @PatchMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activate a user account (Admin only)")
    public ResponseEntity<String> activateUserPatch(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            Authentication authentication) {
        return activateUser(id, authentication);
    }
}
