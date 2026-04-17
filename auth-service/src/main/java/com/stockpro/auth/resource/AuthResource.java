package com.stockpro.auth.resource;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stockpro.auth.service.AuthService;
import com.stockpro.user.entity.User;

@RestController
@RequestMapping("/auth")
public class AuthResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        return ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        String token = authService.login(email, password);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok("Logout successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader("Authorization") String token) {
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(newToken);
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable int userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<User> updateProfile(@PathVariable int userId, @RequestBody User user) {
        return ResponseEntity.ok(authService.updateProfile(userId, user));
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<String> changePassword(@PathVariable int userId,
                                                 @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");
        authService.changePassword(userId, newPassword);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<String> deactivateUser(@PathVariable int userId) {
        authService.deactivateUser(userId);
        return ResponseEntity.ok("User deactivated successfully");
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }
}