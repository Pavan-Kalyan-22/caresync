package com.caresync.controller;

import com.caresync.dto.UserResponse;
import com.caresync.dto.UserUpdateRequest;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.service.UserService;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Valid profile update request should return 200 OK")
    void testValidProfileUpdate() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .gender("MALE")
                .height(180.0)
                .weight(78.0)
                .country("USA")
                .occupation("Developer")
                .build();

        UserResponse mockResponse = UserResponse.builder()
                .id(1L)
                .fullName("Updated Name")
                .email("user@example.com")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .height(180.0)
                .weight(78.0)
                .build();

        when(authentication.getName()).thenReturn("user@example.com");
        when(userService.updateUser(eq("user@example.com"), any(UserUpdateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/users/profile")
                        .principal((Principal) authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("Invalid profile update with negative height and invalid gender should return 400 Bad Request")
    void testInvalidProfileUpdateValidation() throws Exception {
        UserUpdateRequest invalidRequest = UserUpdateRequest.builder()
                .height(-50.0) // Negative height
                .gender("NOT_A_GENDER") // Invalid gender enum
                .dateOfBirth(LocalDate.now().plusYears(1)) // Future date
                .build();

        mockMvc.perform(put("/api/v1/users/profile")
                        .principal((Principal) authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isMap());
    }
}
