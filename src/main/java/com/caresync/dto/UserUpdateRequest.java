package com.caresync.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;

    @Positive(message = "Height must be a positive number")
    private Double height;

    @Positive(message = "Weight must be a positive number")
    private Double weight;

    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String country;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @Pattern(regexp = "^$|^[\\d\\-\\+\\s()]{10,20}$", message = "Phone number is invalid")
    private String phoneNumber;

    @Size(max = 255, message = "Profile image URL must not exceed 255 characters")
    private String profileImageUrl;
}
