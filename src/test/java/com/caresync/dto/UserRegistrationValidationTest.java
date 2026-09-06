package com.caresync.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserRegistrationValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UserRegisterRequest createValidRequest() {
        return UserRegisterRequest.builder()
                .fullName("John Doe")
                .email("john.doe@example.com")
                .password("SecurePass@123")
                .confirmPassword("SecurePass@123")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender("MALE")
                .height(178.5)
                .weight(75.0)
                .country("United States")
                .occupation("Software Engineer")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    @DisplayName("Valid registration request should have no validation violations")
    void testValidRegistration() {
        UserRegisterRequest request = createValidRequest();
        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid registration request should have no violations");
    }

    @Test
    @DisplayName("Registration request with invalid email should fail validation")
    void testInvalidEmail() {
        UserRegisterRequest request = createValidRequest();
        request.setEmail("not-a-valid-email");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Registration request with weak password should fail validation")
    void testWeakPassword() {
        UserRegisterRequest request = createValidRequest();
        request.setPassword("weak"); // Too short, no uppercase, no special char

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("Registration request with future date of birth should fail validation")
    void testFutureDateOfBirth() {
        UserRegisterRequest request = createValidRequest();
        request.setDateOfBirth(LocalDate.now().plusDays(1));

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth")));
    }

    @Test
    @DisplayName("Registration request with negative height or weight should fail validation")
    void testNegativeHeightAndWeight() {
        UserRegisterRequest request = createValidRequest();
        request.setHeight(-170.0);
        request.setWeight(-65.0);

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("height")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("weight")));
    }

    @Test
    @DisplayName("Registration request with invalid gender should fail validation")
    void testInvalidGender() {
        UserRegisterRequest request = createValidRequest();
        request.setGender("UNKNOWN");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("gender")));
    }

    @Test
    @DisplayName("Registration request with blank full name should fail validation")
    void testBlankFullName() {
        UserRegisterRequest request = createValidRequest();
        request.setFullName("   ");

        Set<ConstraintViolation<UserRegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fullName")));
    }
}
