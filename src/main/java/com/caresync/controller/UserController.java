package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.UserResponse;
import com.caresync.dto.UserUpdateRequest;
import com.caresync.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
@SecurityRequirement(name = "Bearer Token")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieve the authenticated user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(Authentication authentication) {
        log.info("Fetching profile for user: {}", authentication.getName());
        UserResponse userResponse = userService.getUserByEmail(authentication.getName());
        return ResponseEntity
                .ok(ApiResponse.success("User profile retrieved successfully", userResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve user profile by user ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Fetching user with id: {}", id);
        UserResponse userResponse = userService.getUserById(id);
        return ResponseEntity
                .ok(ApiResponse.success("User retrieved successfully", userResponse));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update the authenticated user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        log.info("Updating profile for user: {}", authentication.getName());
        UserResponse updatedUser = userService.updateUser(authentication.getName(), request);
        return ResponseEntity
                .ok(ApiResponse.success("User profile updated successfully", updatedUser));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete user account", description = "Delete the authenticated user's account (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteUserAccount(Authentication authentication) {
        log.info("Deleting account for user: {}", authentication.getName());
        userService.deleteUser(authentication.getName());
        return ResponseEntity
                .ok(ApiResponse.success("User account deleted successfully"));
    }

    @GetMapping("/exists/{email}")
    @Operation(summary = "Check if user exists", description = "Check if a user with given email exists")
    public ResponseEntity<ApiResponse<Boolean>> userExists(@PathVariable String email) {
        log.info("Checking if user exists with email: {}", email);
        boolean exists = userService.userExists(email);
        return ResponseEntity
                .ok(ApiResponse.success("User existence check completed", exists));
    }
}
