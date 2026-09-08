package com.caresync.security;

import com.caresync.controller.HydrationController;
import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.service.HydrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HydrationSecurityTest {

    private MockMvc mockMvc;

    @Mock
    private HydrationService hydrationService;

    @InjectMocks
    private HydrationController hydrationController;

    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hydrationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        entryPoint = new JwtAuthenticationEntryPoint();
    }

    @Test
    @DisplayName("Authenticated user with valid credentials can access hydration recommendation endpoint")
    void testAuthenticatedUser_AccessesHydrationSuccessfully() throws Exception {
        HydrationResponse mockResponse = HydrationResponse.builder()
                .dailyWaterTargetMl(2950)
                .dailyWaterTargetLitres(2.95)
                .baseRequirementMl(2450)
                .location("Bengaluru")
                .calculatedAt(LocalDateTime.now())
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(mockResponse);

        Authentication auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .param("city", "Bengaluru")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dailyWaterTargetMl").value(2950));

        verify(hydrationService).getRecommendation("user@example.com", "Bengaluru", null, null);
    }

    @Test
    @DisplayName("Security Scenario 1: No JWT token provided to hydration endpoint returns 401 Unauthorized")
    void testNoJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/hydration/recommendation");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Full authentication is required to access this resource"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Full authentication is required"));
    }

    @Test
    @DisplayName("Security Scenario 2: Invalid JWT token provided to hydration endpoint returns 401 Unauthorized")
    void testInvalidJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/hydration/recommendation");
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Invalid or malformed JWT token"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
    }

    @Test
    @DisplayName("Security Scenario 3: Expired JWT token provided to hydration endpoint returns 401 Unauthorized")
    void testExpiredJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/hydration/recommendation");
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("JWT token has expired"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
    }
}
