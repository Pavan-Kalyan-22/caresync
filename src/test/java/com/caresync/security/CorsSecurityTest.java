package com.caresync.security;

import com.caresync.config.SecurityConfig;
import com.caresync.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CorsSecurityTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    private SecurityConfig securityConfig;
    private CorsConfigurationSource corsConfigurationSource;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtUtil, userDetailsService);
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", "http://localhost:3000,http://localhost:5173");
        corsConfigurationSource = securityConfig.corsConfigurationSource();
    }

    @Test
    @DisplayName("Configured development origin http://localhost:3000 should be permitted")
    void testConfiguredOriginAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.addHeader("Origin", "http://localhost:3000");

        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowCredentials(), "Allow credentials should be enabled with explicit origins");
    }

    @Test
    @DisplayName("Configured development origin http://localhost:5173 should be permitted")
    void testConfiguredViteOriginAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/profile");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
    }

    @Test
    @DisplayName("Unauthorized origin must NOT be in allowed origins list")
    void testUnauthorizedOriginNotAllowed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.addHeader("Origin", "http://unauthorized-attacker.com");

        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(config);
        assertFalse(config.getAllowedOrigins().contains("http://unauthorized-attacker.com"),
                "Unauthorized origins must not be in the allowed origins list");
    }

    @Test
    @DisplayName("Wildcard origin '*' must NOT be used when allowCredentials is true")
    void testWildcardOriginNotUsedWithCredentials() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");

        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);
        assertNotNull(config);
        assertFalse(config.getAllowedOrigins().contains("*"),
                "Wildcard origin '*' must NOT be allowed when credentials are enabled");
        assertNull(config.getAllowedOriginPatterns(),
                "Wildcard origin patterns must not be used");
    }
}
