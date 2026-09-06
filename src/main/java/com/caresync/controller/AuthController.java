package com.caresync.controller;

import com.caresync.dto.*;
import com.caresync.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.caresync.util.JwtUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and Authorization APIs")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account and send verification OTP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or user already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegisterRequest request) {
        log.info("New user registration request for email: {}", request.getEmail());
        UserResponse userResponse = authService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully. Please verify your email.", userResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and get JWT tokens")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email not verified")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> loginUser(
            @Valid @RequestBody UserLoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthenticationResponse response = authService.loginUser(request);
        return ResponseEntity
                .ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify the OTP sent to the user's email")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request) {
        log.info("OTP verification request for email: {}", request.getEmail());
        authService.verifyOtp(request);
        return ResponseEntity
                .ok(ApiResponse.success("OTP verified successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset OTP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent to email"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset request for email: {}", request.getEmail());
        authService.forgotPassword(request);
        return ResponseEntity
                .ok(ApiResponse.success("Password reset OTP sent to your email"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using OTP")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid OTP or passwords don't match")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset for email: {}", request.getEmail());
        AuthenticationResponse response = authService.resetPassword(request);
        return ResponseEntity
                .ok(ApiResponse.success("Password reset successfully", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Get a new access token using refresh token")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New token generated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid authorization header"));
        }
        String refreshToken = authHeader.substring(7);
        AuthenticationResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity
                .ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Logout the current user and revoke refresh tokens")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Authentication authentication) {
        String email = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName();
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            email = jwtUtil.extractEmail(token);
        }

        if (email == null) {
            throw new com.caresync.exception.UnauthorizedException("User is not authenticated");
        }

        log.info("User logout request for email: {}", email);
        authService.logout(email);
        return ResponseEntity
                .ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the authentication service is running")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity
                .ok(ApiResponse.success("Authentication service is running", "OK"));
    }
}
