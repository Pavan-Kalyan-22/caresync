package com.caresync.controller;

import com.caresync.dto.dashboard.*;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.service.DashboardService;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private Authentication auth;
    private DashboardResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());

        sampleResponse = DashboardResponse.builder()
                .user(UserSummaryDto.builder().name("Pavan Kalyan").build())
                .weather(WeatherSummaryDto.builder()
                        .location("Bengaluru")
                        .temperature(32.0)
                        .humidity(65)
                        .weatherCondition("Clear")
                        .build())
                .hydration(HydrationSummaryDto.builder()
                        .dailyTargetMl(2500)
                        .dailyTargetLitres(2.50)
                        .consumedMl(1500)
                        .remainingMl(1000)
                        .progressPercentage(60.0)
                        .isWeatherAdjusted(true)
                        .build())
                .alerts(Collections.singletonList(DashboardAlertDto.builder()
                        .type("WARM_TEMPERATURE")
                        .severity("INFO")
                        .message("Warm ambient temperature of 32.0°C observed.")
                        .build()))
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/dashboard with city returns 200 OK and full structure")
    void testGetDashboard_CityRequest_Returns200() throws Exception {
        when(dashboardService.getDashboard("user@example.com", "Bengaluru", null, null))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("city", "Bengaluru")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Dashboard retrieved successfully"))
                .andExpect(jsonPath("$.data.user.name").value("Pavan Kalyan"))
                .andExpect(jsonPath("$.data.weather.location").value("Bengaluru"))
                .andExpect(jsonPath("$.data.weather.temperature").value(32.0))
                .andExpect(jsonPath("$.data.hydration.dailyTargetMl").value(2500))
                .andExpect(jsonPath("$.data.hydration.consumedMl").value(1500))
                .andExpect(jsonPath("$.data.hydration.remainingMl").value(1000))
                .andExpect(jsonPath("$.data.hydration.progressPercentage").value(60.0))
                .andExpect(jsonPath("$.data.hydration.isWeatherAdjusted").value(true))
                .andExpect(jsonPath("$.data.alerts[0].type").value("WARM_TEMPERATURE"));

        verify(dashboardService).getDashboard("user@example.com", "Bengaluru", null, null);
    }

    @Test
    @DisplayName("GET /api/v1/dashboard with coordinates returns 200 OK")
    void testGetDashboard_CoordinatesRequest_Returns200() throws Exception {
        when(dashboardService.getDashboard("user@example.com", null, 12.9716, 77.5946))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("lat", "12.9716")
                        .param("lon", "77.5946")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.name").value("Pavan Kalyan"));

        verify(dashboardService).getDashboard("user@example.com", null, 12.9716, 77.5946);
    }

    @Test
    @DisplayName("GET /api/v1/dashboard when location is missing returns 400 Bad Request")
    void testGetDashboard_MissingLocation_Returns400() throws Exception {
        when(dashboardService.getDashboard("user@example.com", null, null, null))
                .thenThrow(new BadRequestException("Either city or coordinates (latitude and longitude) must be provided for dashboard"));

        mockMvc.perform(get("/api/v1/dashboard")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Either city or coordinates (latitude and longitude) must be provided for dashboard"));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard when only one coordinate is provided returns 400 Bad Request")
    void testGetDashboard_IncompleteCoordinates_Returns400() throws Exception {
        when(dashboardService.getDashboard("user@example.com", null, 12.9716, null))
                .thenThrow(new BadRequestException("Both latitude and longitude must be provided for coordinate-based dashboard lookup"));

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("lat", "12.9716")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Both latitude and longitude must be provided for coordinate-based dashboard lookup"));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard when user does not exist returns 404 Not Found")
    void testGetDashboard_UserNotFound_Returns404() throws Exception {
        when(dashboardService.getDashboard("user@example.com", "Bengaluru", null, null))
                .thenThrow(new ResourceNotFoundException("User not found with email: user@example.com"));

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("city", "Bengaluru")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found with email: user@example.com"));
    }
}
