package com.stockpro.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stockpro.auth.entity.EmailOtp;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndPurposeAndUsedFalseOrderByIdDesc(String email, String purpose);

    List<EmailOtp> findByEmailAndPurposeAndUsedFalse(String email, String purpose);
}