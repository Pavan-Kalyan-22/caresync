package com.caresync.security;

import com.caresync.controller.AlertsController;
import com.caresync.dto.alert.AlertsResponse;
import com.caresync.dto.dashboard.DashboardAlertDto;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.service.AlertsService;
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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertsSecurityTest {

    private MockMvc mockMvc;

    @Mock
    private AlertsService alertsService;

    @InjectMocks
    private AlertsController alertsController;

    private JwtAuthenticationEntryPoint entryPoint;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(alertsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        entryPoint = new JwtAuthenticationEntryPoint();
        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Authenticated user with valid JWT can access GET /api/v1/alerts")
    void testAuthenticatedUser_AccessesAlertsSuccessfully() throws Exception {
        DashboardAlertDto alert = DashboardAlertDto.builder()
                .type("HYDRATION_REMINDER")
                .severity("INFO")
                .title("Hydration reminder")
                .message("You have not logged any water intake today.")
                .build();

        AlertsResponse mockResponse = AlertsResponse.builder()
                .alerts(Collections.singletonList(alert))
                .count(1)
                .build();

        when(alertsService.getAlerts("user@example.com", "Bengaluru", null, null))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("city", "Bengaluru")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.alerts[0].type").value("HYDRATION_REMINDER"));

        verify(alertsService).getAlerts("user@example.com", "Bengaluru", null, null);
    }

    @Test
    @DisplayName("Security Scenario 1: No JWT token provided to /api/v1/alerts returns 401 Unauthorized")
    void testNoJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Full authentication is required to access this resource"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Full authentication is required"));
    }

    @Test
    @DisplayName("Security Scenario 2: Invalid JWT token to /api/v1/alerts returns 401 Unauthorized")
    void testInvalidJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts");
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
    @DisplayName("Security Scenario 3: Expired JWT token to /api/v1/alerts returns 401 Unauthorized")
    void testExpiredJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/alerts");
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
