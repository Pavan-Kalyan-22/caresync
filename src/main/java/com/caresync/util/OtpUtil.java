package com.caresync.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@Slf4j
public class OtpUtil {

    @Value("${email.otp-expiry:300}")
    private int otpExpirySeconds;

    private static final int OTP_LENGTH = 6;
    private static final String OTP_CHARACTERS = "0123456789";

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(OTP_CHARACTERS.charAt(random.nextInt(OTP_CHARACTERS.length())));
        }
        log.debug("OTP generated: {}", otp);
        return otp.toString();
    }

    public int getOtpExpirySeconds() {
        return otpExpirySeconds;
    }

    public boolean isValidOtpFormat(String otp) {
        return otp != null && otp.matches("\\d{" + OTP_LENGTH + "}");
    }
}
