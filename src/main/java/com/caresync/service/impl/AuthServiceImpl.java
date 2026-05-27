package com.caresync.service.impl;

import com.caresync.dto.*;
import com.caresync.entity.OtpRequest;
import com.caresync.entity.User;
import com.caresync.exception.*;
import com.caresync.mapper.UserMapper;
import com.caresync.repository.OtpRepository;
import com.caresync.repository.UserRepository;
import com.caresync.service.AuthService;
import com.caresync.service.EmailService;
import com.caresync.util.JwtUtil;
import com.caresync.util.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, OtpRepository otpRepository,
                         EmailService emailService, PasswordEncoder passwordEncoder,
                         JwtUtil jwtUtil, OtpUtil otpUtil, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpUtil = otpUtil;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse registerUser(UserRegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User with email " + request.getEmail() + " already exists");
        }

        // Calculate age from date of birth
        Integer age = calculateAge(request.getDateOfBirth());

        // Create new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .dateOfBirth(request.getDateOfBirth())
                .age(age)
                .gender(request.getGender() != null ? User.Gender.valueOf(request.getGender().toUpperCase()) : null)
                .height(request.getHeight())
                .weight(request.getWeight())
                .country(request.getCountry())
                .occupation(request.getOccupation())
                .phoneNumber(request.getPhoneNumber())
                .isEmailVerified(false)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with email: {}", request.getEmail());

        // Generate and send OTP
        String otp = otpUtil.generateOtp();
        createAndSendOtp(request.getEmail(), otp, OtpRequest.OtpType.EMAIL_VERIFICATION);

        return userMapper.toUserResponse(user);
    }

    @Override
    public AuthenticationResponse loginUser(UserLoginRequest request) {
        log.info("Attempting login for user: {}", request.getEmail());

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsEmailVerified()) {
            throw new BadRequestException("Please verify your email before login");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("User login successful for: {}", request.getEmail());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    public void verifyOtp(OtpVerificationRequest request) {
        log.info("Verifying OTP for email: {}", request.getEmail());

        OtpRequest.OtpType otpType = OtpRequest.OtpType.valueOf(request.getOtpType().toUpperCase());

        Optional<OtpRequest> otpRequestOpt = otpRepository.findByEmailAndOtpAndOtpType(
                request.getEmail(), request.getOtp(), otpType);

        if (otpRequestOpt.isEmpty()) {
            throw new OtpException("Invalid OTP");
        }

        OtpRequest otpRequest = otpRequestOpt.get();

        if (otpRequest.isExpired()) {
            throw new OtpException("OTP has expired. Please request a new OTP.");
        }

        if (otpRequest.getAttemptCount() >= 5) {
            throw new OtpException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Mark OTP as verified
        otpRequest.setIsVerified(true);
        otpRepository.save(otpRequest);

        // If email verification, mark user as verified
        if (otpType == OtpRequest.OtpType.EMAIL_VERIFICATION) {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setIsEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified successfully for: {}", request.getEmail());
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + request.getEmail() + " not found"));

        // Generate and send OTP
        String otp = otpUtil.generateOtp();
        createAndSendOtp(request.getEmail(), otp, OtpRequest.OtpType.PASSWORD_RESET);

        log.info("Password reset OTP sent to: {}", request.getEmail());
    }

    @Override
    public AuthenticationResponse resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password for email: {}", request.getEmail());

        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Verify OTP
        OtpRequest otpRequest = otpRepository.findByEmailAndOtpAndOtpType(
                request.getEmail(), request.getOtp(), OtpRequest.OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new OtpException("Invalid OTP"));

        if (otpRequest.isExpired()) {
            throw new OtpException("OTP has expired");
        }

        if (!otpRequest.getIsVerified()) {
            throw new OtpException("OTP not verified");
        }

        // Get user and update password
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete used OTP
        otpRepository.deleteByEmailAndOtpType(request.getEmail(), OtpRequest.OtpType.PASSWORD_RESET);

        // Send success email
        emailService.sendPasswordResetSuccessEmail(request.getEmail());

        // Generate JWT tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("Password reset successfully for: {}", request.getEmail());

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    public void logout(String email) {
        log.info("User logged out: {}", email);
        // In a real application, you might want to invalidate tokens or update user status
    }

    @Override
    public AuthenticationResponse refreshToken(String refreshToken) {
        log.info("Refreshing token");

        try {
            String email = jwtUtil.extractEmail(refreshToken);

            if (!jwtUtil.isTokenValid(refreshToken, email)) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            String accessToken = jwtUtil.generateAccessToken(email);

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getExpirationTime())
                    .user(userMapper.toUserResponse(user))
                    .build();
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    private void createAndSendOtp(String email, String otp, OtpRequest.OtpType otpType) {
        // Delete any existing OTP for this email and type
        otpRepository.deleteByEmailAndOtpType(email, otpType);

        // Create new OTP request
        OtpRequest otpRequest = OtpRequest.builder()
                .email(email)
                .otp(otp)
                .otpType(otpType)
                .isVerified(false)
                .attemptCount(0)
                .expiresAt(LocalDateTime.now().plusSeconds(otpUtil.getOtpExpirySeconds()))
                .build();

        otpRepository.save(otpRequest);

        // Send OTP email
        if (otpType == OtpRequest.OtpType.EMAIL_VERIFICATION) {
            emailService.sendOtpEmail(email, otp);
        } else if (otpType == OtpRequest.OtpType.PASSWORD_RESET) {
            emailService.sendPasswordResetEmail(email, otp);
        }
    }

    private Integer calculateAge(java.time.LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return java.time.LocalDate.now().getYear() - dateOfBirth.getYear();
    }
}
