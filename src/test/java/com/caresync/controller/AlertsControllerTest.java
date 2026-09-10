package com.caresync.controller;

import com.caresync.dto.alert.AlertsResponse;
import com.caresync.dto.dashboard.DashboardAlertDto;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.service.AlertsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AlertsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AlertsService alertsService;

    @InjectMocks
    private AlertsController alertsController;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(alertsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("GET /api/v1/alerts with valid authentication returns 200 OK and alerts payload")
    void testGetAlerts_Authenticated_Returns200() throws Exception {
        DashboardAlertDto alert1 = DashboardAlertDto.builder()
                .type("HIGH_TEMPERATURE")
                .severity("WARNING")
                .title("High temperature")
                .message("Stay hydrated today.")
                .build();

        DashboardAlertDto alert2 = DashboardAlertDto.builder()
                .type("HYDRATION_REMINDER")
                .severity("INFO")
                .title("Hydration reminder")
                .message("You have not logged any water intake today.")
                .build();

        AlertsResponse sampleResponse = AlertsResponse.builder()
                .alerts(Arrays.asList(alert1, alert2))
                .count(2)
                .build();

        when(alertsService.getAlerts("user@example.com", "Bengaluru", null, null))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("city", "Bengaluru")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Alerts retrieved successfully"))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.alerts[0].type").value("HIGH_TEMPERATURE"))
                .andExpect(jsonPath("$.data.alerts[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.data.alerts[0].title").value("High temperature"))
                .andExpect(jsonPath("$.data.alerts[0].message").value("Stay hydrated today."))
                .andExpect(jsonPath("$.data.alerts[1].type").value("HYDRATION_REMINDER"))
                .andExpect(jsonPath("$.data.alerts[1].title").value("Hydration reminder"));

        verify(alertsService).getAlerts("user@example.com", "Bengaluru", null, null);
    }

    @Test
    @DisplayName("GET /api/v1/alerts with no alerts returns 200 OK, empty array, and count 0")
    void testGetAlerts_EmptyAlerts_ReturnsCountZero() throws Exception {
        AlertsResponse emptyResponse = AlertsResponse.builder()
                .alerts(Collections.emptyList())
                .count(0)
                .build();

        when(alertsService.getAlerts("user@example.com", null, null, null))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/alerts")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.alerts").isArray())
                .andExpect(jsonPath("$.data.alerts").isEmpty());

        verify(alertsService).getAlerts("user@example.com", null, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/alerts with coordinates returns 200 OK")
    void testGetAlerts_WithCoordinates_Returns200() throws Exception {
        AlertsResponse sampleResponse = AlertsResponse.builder()
                .alerts(Collections.emptyList())
                .count(0)
                .build();

        when(alertsService.getAlerts("user@example.com", null, 12.9716, 77.5946))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/alerts")
                        .param("lat", "12.9716")
                        .param("lon", "77.5946")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(alertsService).getAlerts("user@example.com", null, 12.9716, 77.5946);
    }

    @Test
    @DisplayName("GET /api/v1/alerts when service throws BadRequestException returns 400 Bad Request")
    void testGetAlerts_BadRequest_Returns400() throws Exception {
        when(alertsService.getAlerts(eq("user@example.com"), any(), any(), any()))
                .thenThrow(new BadRequestException("Both latitude and longitude must be provided for coordinate-based alerts lookup"));

        mockMvc.perform(get("/api/v1/alerts")
                        .param("lat", "12.9716")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Both latitude and longitude must be provided for coordinate-based alerts lookup"));
    }

    @Test
    @DisplayName("GET /api/v1/alerts when user not found returns 404 Not Found")
    void testGetAlerts_UserNotFound_Returns404() throws Exception {
        when(alertsService.getAlerts(eq("user@example.com"), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("User not found with email: user@example.com"));

        mockMvc.perform(get("/api/v1/alerts")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found with email: user@example.com"));
    }
}
