package com.caresync.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_requests", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_otp_type", columnList = "otp_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType otpType;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum OtpType {
        EMAIL_VERIFICATION, PASSWORD_RESET, TWO_FACTOR
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "OtpRequest{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", otpType=" + otpType +
                ", isVerified=" + isVerified +
                ", createdAt=" + createdAt +
                '}';
    }
}
