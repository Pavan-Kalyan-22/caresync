package com.caresync.security;

import com.caresync.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtSecurityTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtFilter;
    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        entryPoint = new JwtAuthenticationEntryPoint();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valid JWT access token should populate SecurityContextHolder with authenticated principal")
    void testValidJwtSetsAuthentication() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "alice@example.com";
        UserDetails userDetails = new User(email, "password", true, true, true, true, Collections.emptyList());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.isTokenValid(token, email)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be set in SecurityContext for valid JWT");
        assertEquals(email, auth.getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Invalid JWT token should NOT set authentication in SecurityContext")
    void testInvalidJwtDoesNotSetAuthentication() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        String email = "hacker@example.com";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(jwtUtil.isTokenValid(token, email)).thenReturn(false); // Invalid!

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication must NOT be set for invalid JWT");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Expired JWT token should throw exception in JwtUtil and not authenticate")
    void testExpiredJwtDoesNotAuthenticate() throws ServletException, IOException {
        String token = "expired.jwt.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.extractEmail(token)).thenThrow(new IllegalArgumentException("JWT expired"));

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication must NOT be set for expired JWT");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Missing JWT token should not set authentication in SecurityContext")
    void testMissingJwtDoesNotAuthenticate() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Authentication must be null when no Authorization header is provided");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JwtAuthenticationEntryPoint should return 401 Unauthorized with structured JSON error on missing or bad credentials")
    void testAuthenticationEntryPointReturns401() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException authException = new AuthenticationException("Full authentication is required to access this resource") {};

        entryPoint.commence(request, response, authException);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("\"success\":false"));
        assertTrue(body.contains("401"));
    }
}
