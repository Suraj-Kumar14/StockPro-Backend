package com.stockpro.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stockpro.authservice.entity.OtpPurpose;
import com.stockpro.authservice.entity.OtpToken;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    List<OtpToken> findByEmailAndPurposeAndConsumedAtIsNull(String email, OtpPurpose purpose);
}
