package com.caresync.security;

import com.caresync.controller.WaterController;
import com.caresync.dto.water.WaterHistoryResponse;
import com.caresync.dto.water.WaterLogRequest;
import com.caresync.dto.water.WaterLogResponse;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.service.WaterService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WaterSecurityTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private WaterService waterService;

    @InjectMocks
    private WaterController waterController;

    private JwtAuthenticationEntryPoint entryPoint;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(waterController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        entryPoint = new JwtAuthenticationEntryPoint();
        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Authenticated user with valid JWT can access GET /api/v1/water/history")
    void testAuthenticatedUser_AccessesWaterHistorySuccessfully() throws Exception {
        WaterHistoryResponse mockResponse = WaterHistoryResponse.builder()
                .days(Collections.emptyList())
                .build();

        when(waterService.getWaterHistory("user@example.com", null, null, null))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/water/history")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(waterService).getWaterHistory("user@example.com", null, null, null);
    }

    @Test
    @DisplayName("Authenticated user with valid JWT can access POST /api/v1/water/log")
    void testAuthenticatedUser_LogsWaterSuccessfully() throws Exception {
        WaterLogRequest request = WaterLogRequest.builder().amountMl(250).build();
        WaterLogResponse mockResponse = WaterLogResponse.builder()
                .id(1L)
                .amountMl(250)
                .loggedAt(LocalDateTime.now())
                .build();

        when(waterService.logWaterIntake(eq("user@example.com"), any(WaterLogRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/water/log")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(waterService).logWaterIntake(eq("user@example.com"), any(WaterLogRequest.class));
    }

    @Test
    @DisplayName("Security Scenario 1: No JWT token provided returns 401 Unauthorized")
    void testNoJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/water/history");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Full authentication is required to access this resource"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Full authentication is required"));
    }

    @Test
    @DisplayName("Security Scenario 2: Invalid JWT token returns 401 Unauthorized")
    void testInvalidJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/water/history");
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Invalid or malformed JWT token"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Invalid or malformed JWT token"));
    }

    @Test
    @DisplayName("Security Scenario 3: Expired JWT token returns 401 Unauthorized")
    void testExpiredJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/water/log");
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("JWT token has expired"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("JWT token has expired"));
    }
}
