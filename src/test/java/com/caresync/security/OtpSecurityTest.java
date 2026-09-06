package com.caresync.security;

import com.caresync.dto.OtpVerificationRequest;
import com.caresync.dto.UserResponse;
import com.caresync.entity.OtpRequest;
import com.caresync.entity.User;
import com.caresync.exception.OtpException;
import com.caresync.mapper.UserMapper;
import com.caresync.repository.OtpRepository;
import com.caresync.repository.RefreshTokenRepository;
import com.caresync.repository.UserRepository;
import com.caresync.service.EmailService;
import com.caresync.service.impl.AuthServiceImpl;
import com.caresync.util.JwtUtil;
import com.caresync.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, otpRepository, emailService, passwordEncoder,
                jwtUtil, otpUtil, userMapper, refreshTokenRepository
        );
        // Set max OTP attempts to 5
        ReflectionTestUtils.setField(authService, "maxOtpAttempts", 5);
    }

    private OtpRequest createActiveOtpRequest(String email, String otp) {
        return OtpRequest.builder()
                .id(1L)
                .email(email)
                .otp(otp)
                .otpType(OtpRequest.OtpType.EMAIL_VERIFICATION)
                .isVerified(false)
                .attemptCount(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Valid OTP should succeed and mark OTP as verified")
    void testValidOtpSucceeds() {
        String email = "test@example.com";
        String otp = "123456";
        OtpRequest otpRequest = createActiveOtpRequest(email, otp);

        User user = User.builder().id(1L).email(email).isEmailVerified(false).build();

        when(otpRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, OtpRequest.OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRequest));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        OtpVerificationRequest request = OtpVerificationRequest.builder()
                .email(email)
                .otp(otp)
                .otpType("EMAIL_VERIFICATION")
                .build();

        authService.verifyOtp(request);

        assertTrue(otpRequest.getIsVerified(), "OTP should be marked as verified");
        assertTrue(user.getIsEmailVerified(), "User email should be marked as verified");
        verify(otpRepository).save(otpRequest);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Invalid OTP should increment attemptCount and persist to database")
    void testInvalidOtpIncrementsAttempts() {
        String email = "test@example.com";
        OtpRequest otpRequest = createActiveOtpRequest(email, "123456");

        when(otpRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, OtpRequest.OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRequest));

        OtpVerificationRequest request = OtpVerificationRequest.builder()
                .email(email)
                .otp("999999") // Wrong OTP
                .otpType("EMAIL_VERIFICATION")
                .build();

        OtpException exception = assertThrows(OtpException.class, () -> authService.verifyOtp(request));
        assertEquals("Invalid or expired OTP", exception.getMessage());
        assertEquals(1, otpRequest.getAttemptCount(), "Attempt count must be incremented to 1");

        verify(otpRepository).save(otpRequest);
    }

    @Test
    @DisplayName("Reaching maximum OTP attempts should lock out the OTP and reject")
    void testOtpLockoutOnMaxAttempts() {
        String email = "test@example.com";
        OtpRequest otpRequest = createActiveOtpRequest(email, "123456");
        otpRequest.setAttemptCount(4); // 4 previous failed attempts

        when(otpRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, OtpRequest.OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRequest));

        OtpVerificationRequest request = OtpVerificationRequest.builder()
                .email(email)
                .otp("000000") // 5th failed attempt
                .otpType("EMAIL_VERIFICATION")
                .build();

        OtpException exception = assertThrows(OtpException.class, () -> authService.verifyOtp(request));
        assertEquals("Maximum OTP attempts exceeded. Please request a new OTP.", exception.getMessage());
        assertEquals(5, otpRequest.getAttemptCount());
        verify(otpRepository).save(otpRequest);
    }

    @Test
    @DisplayName("Submitting the correct OTP after lockout has been reached must be rejected")
    void testCorrectOtpAfterLockoutIsRejected() {
        String email = "test@example.com";
        OtpRequest otpRequest = createActiveOtpRequest(email, "123456");
        otpRequest.setAttemptCount(5); // Already locked out

        when(otpRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, OtpRequest.OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRequest));

        OtpVerificationRequest request = OtpVerificationRequest.builder()
                .email(email)
                .otp("123456") // Correct OTP, but already locked out
                .otpType("EMAIL_VERIFICATION")
                .build();

        OtpException exception = assertThrows(OtpException.class, () -> authService.verifyOtp(request));
        assertEquals("Maximum OTP attempts exceeded. Please request a new OTP.", exception.getMessage());
        assertFalse(otpRequest.getIsVerified(), "Locked out OTP must not be verified even with correct code");
    }

    @Test
    @DisplayName("Expired OTP must be rejected")
    void testExpiredOtpIsRejected() {
        String email = "test@example.com";
        OtpRequest otpRequest = createActiveOtpRequest(email, "123456");
        otpRequest.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired 1 min ago

        when(otpRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, OtpRequest.OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRequest));

        OtpVerificationRequest request = OtpVerificationRequest.builder()
                .email(email)
                .otp("123456")
                .otpType("EMAIL_VERIFICATION")
                .build();

        OtpException exception = assertThrows(OtpException.class, () -> authService.verifyOtp(request));
        assertEquals("Invalid or expired OTP", exception.getMessage());
    }

    @Test
    @DisplayName("Already verified OTP must not be reusable")
    void testAlreadyVerifiedOtpCannotBeReused() {
        String email = "test@example.com";
        OtpRequest otpRequest = createActiveOtpRequest(email, "123456");
        otpRequest.setIsVerified(true); // Already verified

        when(otpRepository.findTopByEmailAndOtpTypeOrderByCreatedAtDesc(email, OtpRequest.OtpType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(otpRequest));

        OtpVerificationRequest request = OtpVerificationRequest.builder()
                .email(email)
                .otp("123456")
                .otpType("EMAIL_VERIFICATION")
                .build();

        OtpException exception = assertThrows(OtpException.class, () -> authService.verifyOtp(request));
        assertEquals("Invalid or expired OTP", exception.getMessage());
    }
}
