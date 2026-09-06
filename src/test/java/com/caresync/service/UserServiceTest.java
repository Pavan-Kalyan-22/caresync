package com.caresync.service;

import com.caresync.dto.UserResponse;
import com.caresync.dto.UserUpdateRequest;
import com.caresync.entity.User;
import com.caresync.mapper.UserMapper;
import com.caresync.repository.UserRepository;
import com.caresync.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserServiceImpl(userRepository, userMapper);
    }

    @Test
    @DisplayName("Should update allowed profile fields and recalculate age from DOB while preserving sensitive fields")
    void testUpdateUserRecalculatesAgeAndPreservesSensitiveFields() {
        String email = "john@example.com";
        LocalDateTime createdTimestamp = LocalDateTime.now().minusDays(10);
        
        User existingUser = User.builder()
                .id(42L)
                .fullName("John Old")
                .email(email)
                .password("$2a$12$hashedPasswordPlaceholder")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .age(36)
                .gender(User.Gender.MALE)
                .height(175.0)
                .weight(70.0)
                .country("USA")
                .occupation("Engineer")
                .isEmailVerified(true)
                .isActive(true)
                .createdAt(createdTimestamp)
                .version(1L)
                .build();

        LocalDate newDob = LocalDate.of(2000, 6, 15);
        int expectedAge = Period.between(newDob, LocalDate.now()).getYears();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .fullName("John New")
                .dateOfBirth(newDob)
                .gender("MALE")
                .height(182.0)
                .weight(76.0)
                .country("Canada")
                .occupation("Senior Architect")
                .phoneNumber("+15551234567")
                .profileImageUrl("https://example.com/avatar.png")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(email, updateRequest);

        // Verify updated fields
        assertEquals("John New", response.getFullName());
        assertEquals(newDob, response.getDateOfBirth());
        assertEquals(expectedAge, response.getAge(), "Age must be accurately recalculated from new dateOfBirth");
        assertEquals(182.0, response.getHeight());
        assertEquals(76.0, response.getWeight());
        assertEquals("Canada", response.getCountry());
        assertEquals("Senior Architect", response.getOccupation());

        // Verify sensitive fields on entity were preserved untouched
        assertEquals(42L, existingUser.getId());
        assertEquals(email, existingUser.getEmail());
        assertEquals("$2a$12$hashedPasswordPlaceholder", existingUser.getPassword());
        assertTrue(existingUser.getIsEmailVerified());
        assertTrue(existingUser.getIsActive());
        assertEquals(createdTimestamp, existingUser.getCreatedAt());
        assertEquals(1L, existingUser.getVersion());

        verify(userRepository).save(existingUser);
    }
}
