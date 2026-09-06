package com.caresync.security;

import com.caresync.controller.AuthController;
import com.caresync.dto.AuthenticationResponse;
import com.caresync.dto.UserResponse;
import com.caresync.entity.RefreshToken;
import com.caresync.entity.User;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.UnauthorizedException;
import com.caresync.mapper.UserMapper;
import com.caresync.repository.OtpRepository;
import com.caresync.repository.RefreshTokenRepository;
import com.caresync.repository.UserRepository;
import com.caresync.service.AuthService;
import com.caresync.service.EmailService;
import com.caresync.service.impl.AuthServiceImpl;
import com.caresync.util.JwtUtil;
import com.caresync.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LogoutAndRefreshTokenSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private Authentication authentication;

    private AuthServiceImpl authService;
    private AuthController authController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, otpRepository, emailService, passwordEncoder,
                jwtUtil, otpUtil, userMapper, refreshTokenRepository
        );
        authController = new AuthController(authService, jwtUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Authenticated user logging out should revoke refresh tokens and return 200 OK")
    void testAuthenticatedUserLogoutRevokesTokens() throws Exception {
        String userEmail = "logged-in-user@example.com";
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userEmail);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal((Principal) authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        // Verify that server-side refresh tokens were revoked for the dynamic authenticated user
        verify(refreshTokenRepository).revokeAllByUserEmail(userEmail);
    }

    @Test
    @DisplayName("Unauthenticated user attempting to logout should be rejected with 401 Unauthorized")
    void testUnauthenticatedLogoutRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(401));

        verify(refreshTokenRepository, never()).revokeAllByUserEmail(any());
    }

    @Test
    @DisplayName("Valid non-revoked refresh token should successfully generate new access token")
    void testValidRefreshTokenSucceeds() {
        String email = "alice@example.com";
        String rawRefreshToken = "valid-refresh-token-123";
        String newAccessToken = "new-access-token-456";

        RefreshToken storedToken = RefreshToken.builder()
                .id(1L)
                .userEmail(email)
                .token(rawRefreshToken)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        User user = User.builder().id(10L).email(email).build();
        UserResponse userResponse = UserResponse.builder().id(10L).email(email).build();

        when(jwtUtil.extractEmail(rawRefreshToken)).thenReturn(email);
        when(jwtUtil.isTokenValid(rawRefreshToken, email)).thenReturn(true);
        when(refreshTokenRepository.findByToken(rawRefreshToken)).thenReturn(Optional.of(storedToken));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(email)).thenReturn(newAccessToken);
        when(jwtUtil.getExpirationTime()).thenReturn(86400000L);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        AuthenticationResponse response = authService.refreshToken(rawRefreshToken);

        assertNotNull(response);
        assertEquals(newAccessToken, response.getAccessToken());
        assertEquals(rawRefreshToken, response.getRefreshToken());
    }

    @Test
    @DisplayName("Revoked refresh token must be rejected with 401 Unauthorized")
    void testRevokedRefreshTokenRejected() {
        String email = "alice@example.com";
        String revokedTokenStr = "revoked-refresh-token-123";

        RefreshToken revokedStoredToken = RefreshToken.builder()
                .id(1L)
                .userEmail(email)
                .token(revokedTokenStr)
                .isRevoked(true) // Revoked!
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(jwtUtil.extractEmail(revokedTokenStr)).thenReturn(email);
        when(jwtUtil.isTokenValid(revokedTokenStr, email)).thenReturn(true);
        when(refreshTokenRepository.findByToken(revokedTokenStr)).thenReturn(Optional.of(revokedStoredToken));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.refreshToken(revokedTokenStr)
        );

        assertEquals("Refresh token has been revoked", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Expired refresh token must be rejected with 401 Unauthorized")
    void testExpiredRefreshTokenRejected() {
        String email = "alice@example.com";
        String expiredTokenStr = "expired-refresh-token-123";

        RefreshToken expiredStoredToken = RefreshToken.builder()
                .id(1L)
                .userEmail(email)
                .token(expiredTokenStr)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().minusHours(1)) // Expired
                .build();

        when(jwtUtil.extractEmail(expiredTokenStr)).thenReturn(email);
        when(jwtUtil.isTokenValid(expiredTokenStr, email)).thenReturn(true);
        when(refreshTokenRepository.findByToken(expiredTokenStr)).thenReturn(Optional.of(expiredStoredToken));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.refreshToken(expiredTokenStr)
        );

        assertEquals("Refresh token has expired", exception.getMessage());
    }
}
