package com.caresync.service;

public interface EmailService {

    void sendOtpEmail(String email, String otp);

    void sendVerificationEmail(String email, String userName);

    void sendPasswordResetEmail(String email, String otp);

    void sendPasswordResetSuccessEmail(String email);
}
