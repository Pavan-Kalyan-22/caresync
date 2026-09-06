package com.caresync.security;

import com.caresync.controller.UserController;
import com.caresync.dto.UserResponse;
import com.caresync.entity.User;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.mapper.UserMapper;
import com.caresync.repository.UserRepository;
import com.caresync.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IdorSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private UserMapper userMapper;
    private UserServiceImpl userService;
    private UserController userController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserServiceImpl(userRepository, userMapper);
        userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("User A accessing their own profile by ID should succeed with 200 OK")
    void testUserAccessingOwnProfileSucceeds() throws Exception {
        Long userId = 100L;
        String userEmail = "userA@example.com";

        User userA = User.builder()
                .id(userId)
                .email(userEmail)
                .fullName("User A")
                .isActive(true)
                .isEmailVerified(true)
                .build();

        when(authentication.getName()).thenReturn(userEmail);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userA));

        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .principal((Principal) authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(userEmail))
                .andExpect(jsonPath("$.data.fullName").value("User A"));
    }

    @Test
    @DisplayName("User A attempting to access User B's profile by ID must be denied with 403 Forbidden")
    void testUserAccessingAnotherUsersProfileDenied() throws Exception {
        Long userBId = 200L;
        String userAEmail = "userA@example.com";
        String userBEmail = "userB@example.com";

        User userB = User.builder()
                .id(userBId)
                .email(userBEmail)
                .fullName("User B")
                .isActive(true)
                .isEmailVerified(true)
                .build();

        when(authentication.getName()).thenReturn(userAEmail);
        when(userRepository.findById(userBId)).thenReturn(Optional.of(userB));

        mockMvc.perform(get("/api/v1/users/{id}", userBId)
                        .principal((Principal) authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You are not authorized to view another user's profile"));
    }

    @Test
    @DisplayName("Accessing non-existent user ID should return 404 Not Found")
    void testAccessingNonExistentUserReturns404() throws Exception {
        Long nonExistentId = 999L;
        String userAEmail = "userA@example.com";

        when(authentication.getName()).thenReturn(userAEmail);
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/{id}", nonExistentId)
                        .principal((Principal) authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}
