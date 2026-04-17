package com.stockpro.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stockpro.user.entity.User;
import com.stockpro.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

	private final UserRepository userRepository;
	
	@Value("${app.jwt.secret:mySecretKey}")
	private String jwtSecret;
	
	@Value("${app.jwt.expiry:3600000}")
	private long tokenExpiry;
	
	@Override
	public User register(User user) {
		if(userRepository.existsByEmail(user.getEmail())) {
			throw new RuntimeException("Email already exists");
		}
		
		user.setActive(true);
		user.setCreatedAt(LocalDateTime.now());
		
        // Here passwordHash is stored directly because your diagram uses passwordHash field.
        // Later we should encode it using PasswordEncoder.
        return userRepository.save(user);

	}

	@Override
	public String login(String email, String password) {
		User user = userRepository.findByEmail(email).orElseThrow(()-> new RuntimeException("Invalid email"));
		
		if(!user.isActive()) {
			throw new RuntimeException("User account is inactive");
		}
		
		if(!user.getPasswordHash().equals(password)) {
			throw new RuntimeException("Invalid password");
		}
		
		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);
		
        // Dummy JWT token for now
        return "JWT_TOKEN_FOR_" + user.getEmail();

	}

    @Override
    public void logout(String token) {
        System.out.println("Logout successful for token: " + token);
    }

    @Override
    public boolean validateToken(String token) {
        return token != null && token.startsWith("JWT_TOKEN_FOR_");
    }

    @Override
    public String refreshToken(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }

        String email = token.replace("JWT_TOKEN_FOR_", "");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return "JWT_TOKEN_FOR_" + user.getEmail();
    }

    @Override
    public User getUserById(int userId) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public User updateProfile(int userId, User user) {
        User existingUser = userRepository.findByUserId(userId);

        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());
        existingUser.setDepartment(user.getDepartment());

        return userRepository.save(existingUser);
    }

    @Override
    public void changePassword(int userId, String newPassword) {
        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        user.setPasswordHash(newPassword);
        userRepository.save(user);
    }

    @Override
    public void deactivateUser(int userId) {
        User user = userRepository.findByUserId(userId);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        user.setActive(false);
        userRepository.save(user);
    }

	@Override
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

}
