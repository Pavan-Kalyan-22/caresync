package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.UserRegisterRequest;
import com.caresync.dto.UserResponse;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Health endpoint should be accessible at /api/v1/auth/health and return OK")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Authentication service is running"))
                .andExpect(jsonPath("$.data").value("OK"));
    }

    @Test
    @DisplayName("Valid user registration should return 201 Created")
    void testValidUserRegistration() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .fullName("Alice Wonderland")
                .email("alice@example.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .dateOfBirth(LocalDate.of(1998, 3, 10))
                .gender("FEMALE")
                .height(168.0)
                .weight(58.0)
                .country("United Kingdom")
                .occupation("Researcher")
                .phoneNumber("+441234567890")
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id(1L)
                .fullName("Alice Wonderland")
                .email("alice@example.com")
                .dateOfBirth(LocalDate.of(1998, 3, 10))
                .age(28)
                .gender("FEMALE")
                .isEmailVerified(false)
                .isActive(true)
                .build();

        when(authService.registerUser(any(UserRegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Alice Wonderland"));
    }

    @Test
    @DisplayName("Invalid user registration with blank email and weak password should return 400 Bad Request")
    void testInvalidUserRegistrationValidation() throws Exception {
        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .fullName("A") // Too short
                .email("invalid-email") // Bad email
                .password("short") // Weak password
                .confirmPassword("short")
                .dateOfBirth(LocalDate.now().plusDays(1)) // Future date
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isMap());
    }
}
