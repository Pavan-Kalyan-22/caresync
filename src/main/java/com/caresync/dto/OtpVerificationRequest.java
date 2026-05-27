package com.caresync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerificationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be a 6-digit number")
    private String otp;

    @NotBlank(message = "OTP type is required")
    @Pattern(regexp = "^(EMAIL_VERIFICATION|PASSWORD_RESET|TWO_FACTOR)$", 
            message = "OTP type must be EMAIL_VERIFICATION, PASSWORD_RESET, or TWO_FACTOR")
    private String otpType;
}
