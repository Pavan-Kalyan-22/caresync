package com.caresync.repository;

import com.caresync.entity.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRequest, Long> {

    Optional<OtpRequest> findTopByEmailAndOtpTypeOrderByCreatedAtDesc(
            String email, OtpRequest.OtpType otpType);

    Optional<OtpRequest> findByEmailAndOtpAndOtpType(
            String email, String otp, OtpRequest.OtpType otpType);

    void deleteByEmailAndOtpType(String email, OtpRequest.OtpType otpType);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
