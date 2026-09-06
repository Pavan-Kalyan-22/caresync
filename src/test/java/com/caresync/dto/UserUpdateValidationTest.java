package com.caresync.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserUpdateValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UserUpdateRequest createValidUpdateRequest() {
        return UserUpdateRequest.builder()
                .fullName("Jane Doe")
                .dateOfBirth(LocalDate.of(1992, 8, 20))
                .gender("FEMALE")
                .height(165.0)
                .weight(60.0)
                .country("Canada")
                .occupation("Doctor")
                .phoneNumber("+19876543210")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
    }

    @Test
    @DisplayName("Valid profile update request should have no validation violations")
    void testValidUpdateRequest() {
        UserUpdateRequest request = createValidUpdateRequest();
        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid update request should have no violations");
    }

    @Test
    @DisplayName("Profile update with future date of birth should fail validation")
    void testFutureDateOfBirthInUpdate() {
        UserUpdateRequest request = createValidUpdateRequest();
        request.setDateOfBirth(LocalDate.now().plusDays(5));

        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth")));
    }

    @Test
    @DisplayName("Profile update with negative height or weight should fail validation")
    void testNegativeHeightOrWeightInUpdate() {
        UserUpdateRequest request = createValidUpdateRequest();
        request.setHeight(-10.0);
        request.setWeight(-5.0);

        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("height")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("weight")));
    }

    @Test
    @DisplayName("Profile update with invalid gender should fail validation")
    void testInvalidGenderInUpdate() {
        UserUpdateRequest request = createValidUpdateRequest();
        request.setGender("INVALID_GENDER");

        Set<ConstraintViolation<UserUpdateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("gender")));
    }

    @Test
    @DisplayName("Sensitive fields (id, password, email, isEmailVerified, isActive, createdAt, updatedAt, lastLogin, version, age) must NOT exist on UserUpdateRequest")
    void testSensitiveFieldsDoNotExistOnDto() {
        List<String> prohibitedFields = Arrays.asList(
                "id", "password", "email", "isEmailVerified", "isActive", 
                "createdAt", "updatedAt", "lastLogin", "version", "age"
        );

        Field[] declaredFields = UserUpdateRequest.class.getDeclaredFields();
        List<String> fieldNames = Arrays.stream(declaredFields).map(Field::getName).toList();

        for (String prohibited : prohibitedFields) {
            assertFalse(fieldNames.contains(prohibited),
                    "Prohibited sensitive field '" + prohibited + "' must not be present in UserUpdateRequest DTO");
        }
    }
}
