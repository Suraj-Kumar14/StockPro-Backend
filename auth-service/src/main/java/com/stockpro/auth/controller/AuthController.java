package com.stockpro.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stockpro.auth.service.AuthService;
import com.stockpro.user.dto.AuthResponse;
import com.stockpro.user.dto.LoginRequest;
import com.stockpro.user.dto.RegisterRequest;
import com.stockpro.user.entity.User;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(req.getPasswordHash())
                .phone(req.getPhone())
                .role(req.getRole())
                .department(req.getDepartment())
                .build();

        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        String token = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(new AuthResponse(token, "Login Successful"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable int id, @RequestBody User user) {
        return ResponseEntity.ok(authService.updateProfile(id, user));
    }

    @PutMapping("/password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable int id, @RequestBody String password) {
        authService.changePassword(id, password);
        return ResponseEntity.ok("Password updated");
    }

    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<?> deactivate(@PathVariable int id) {
        authService.deactivateUser(id);
        return ResponseEntity.ok("User deactivated");
    }
}