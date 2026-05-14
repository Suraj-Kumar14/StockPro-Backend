package com.stockpro.authservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.authservice.entity.OtpPurpose;
import com.stockpro.authservice.entity.OtpToken;
import com.stockpro.authservice.entity.User;
import com.stockpro.authservice.entity.UserRole;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AuthRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        otpTokenRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(user("Admin User", "admin@example.com", UserRole.ADMIN, true, "Operations"));
        userRepository.save(user("Warehouse Staff", "staff@example.com", UserRole.STAFF, true, "Warehouse"));
        userRepository.save(user("Inactive User", "inactive@example.com", UserRole.MANAGER, false, "Finance"));

        persistToken("admin@example.com", "111111", OtpPurpose.SIGNUP_VERIFICATION, LocalDateTime.now().minusMinutes(2), null);
        persistToken("admin@example.com", "222222", OtpPurpose.SIGNUP_VERIFICATION, LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
        persistToken("admin@example.com", "333333", OtpPurpose.SIGNUP_VERIFICATION, LocalDateTime.now(), null);
    }

    @Test
    void userRepository_shouldSupportCaseInsensitiveLookupAndSearchFilters() {
        assertTrue(userRepository.findByEmail("admin@example.com").isPresent());
        assertTrue(userRepository.findByEmailIgnoreCase("ADMIN@example.com").isPresent());
        assertTrue(userRepository.existsByEmail("staff@example.com"));
        assertTrue(userRepository.existsByEmailIgnoreCase("STAFF@example.com"));

        var activeStaff = userRepository.searchUsers("warehouse", UserRole.STAFF, true, PageRequest.of(0, 10));
        assertEquals(1, activeStaff.getTotalElements());
        assertEquals("staff@example.com", activeStaff.getContent().get(0).getEmail());

        var inactiveManagers = userRepository.searchUsers(null, UserRole.MANAGER, false, PageRequest.of(0, 10));
        assertEquals(1, inactiveManagers.getTotalElements());
    }

    @Test
    void otpTokenRepository_shouldReturnLatestAndUnconsumedTokens() {
        var latest = otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("admin@example.com", OtpPurpose.SIGNUP_VERIFICATION).orElseThrow();
        var unconsumed = otpTokenRepository.findByEmailAndPurposeAndConsumedAtIsNull("admin@example.com", OtpPurpose.SIGNUP_VERIFICATION);

        assertEquals("333333", latest.getOtpCode());
        assertEquals(2, unconsumed.size());
        assertFalse(unconsumed.stream().anyMatch(token -> token.getConsumedAt() != null));
    }

    private User user(String name, String email, UserRole role, boolean active, String department) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hash");
        user.setPhone("9999999999");
        user.setRole(role);
        user.setDepartment(department);
        user.setIsActive(active);
        return user;
    }

    private OtpToken token(String email, String otp, OtpPurpose purpose, LocalDateTime createdAt, LocalDateTime consumedAt) {
        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setOtpCode(otp);
        token.setPurpose(purpose);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setConsumedAt(consumedAt);
        token.setCreatedAt(createdAt);
        return token;
    }

    private void persistToken(String email, String otp, OtpPurpose purpose, LocalDateTime createdAt, LocalDateTime consumedAt) {
        otpTokenRepository.saveAndFlush(token(email, otp, purpose, createdAt, consumedAt));
        jdbcTemplate.update(
                "update \"otp_token\" set \"created_at\" = ? where \"otp_code\" = ?",
                Timestamp.valueOf(createdAt),
                otp);
    }
}
