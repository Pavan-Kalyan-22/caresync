package com.caresync.service;

import com.caresync.dto.*;

public interface AuthService {

    UserResponse registerUser(UserRegisterRequest request);

    AuthenticationResponse loginUser(UserLoginRequest request);

    void verifyOtp(OtpVerificationRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    AuthenticationResponse resetPassword(ResetPasswordRequest request);

    void logout(String email);

    AuthenticationResponse refreshToken(String refreshToken);
}
