package com.caresync.controller;

import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.exception.WeatherException;
import com.caresync.service.HydrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HydrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HydrationService hydrationService;

    @InjectMocks
    private HydrationController hydrationController;

    private HydrationResponse sampleResponse;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(hydrationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());

        sampleResponse = HydrationResponse.builder()
                .dailyWaterTargetMl(2950)
                .dailyWaterTargetLitres(2.95)
                .baseRequirementMl(2450)
                .activityAdjustmentMl(0)
                .temperatureAdjustmentMl(500)
                .humidityAdjustmentMl(0)
                .weightKg(70.0)
                .occupation("Software Engineer")
                .activityLevel("SEDENTARY")
                .temperatureCelsius(31.5)
                .humidityPercent(60)
                .weatherCondition("Clouds")
                .location("Bengaluru")
                .explanation("Base requirement of 2450 ml adjusted for elevated ambient temperature of 31.5°C (+500 ml).")
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/hydration/recommendation with valid city returns 200 OK and breakdown")
    void testRecommendation_ValidCity_Returns200() throws Exception {
        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .param("city", "Bengaluru")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hydration recommendation calculated successfully"))
                .andExpect(jsonPath("$.data.dailyWaterTargetMl").value(2950))
                .andExpect(jsonPath("$.data.dailyWaterTargetLitres").value(2.95))
                .andExpect(jsonPath("$.data.baseRequirementMl").value(2450))
                .andExpect(jsonPath("$.data.temperatureAdjustmentMl").value(500))
                .andExpect(jsonPath("$.data.activityLevel").value("SEDENTARY"))
                .andExpect(jsonPath("$.data.location").value("Bengaluru"));
    }

    @Test
    @DisplayName("GET /api/v1/hydration/recommendation with coordinates returns 200 OK")
    void testRecommendation_ValidCoordinates_Returns200() throws Exception {
        when(hydrationService.getRecommendation("user@example.com", null, 12.9716, 77.5946))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .param("lat", "12.9716")
                        .param("lon", "77.5946")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dailyWaterTargetMl").value(2950));
    }

    @Test
    @DisplayName("GET /api/v1/hydration/recommendation missing location returns 400 Bad Request")
    void testRecommendation_MissingLocation_Returns400() throws Exception {
        when(hydrationService.getRecommendation("user@example.com", null, null, null))
                .thenThrow(new BadRequestException("Either city or coordinates (latitude and longitude) must be provided for hydration calculation"));

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Either city or coordinates (latitude and longitude) must be provided for hydration calculation"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/hydration/recommendation with incomplete profile returns 400 Bad Request")
    void testRecommendation_IncompleteProfile_Returns400() throws Exception {
        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new BadRequestException("User profile is incomplete: weight is required for hydration calculation. Please update your profile."));

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .param("city", "Bengaluru")
                        .principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User profile is incomplete: weight is required for hydration calculation. Please update your profile."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/hydration/recommendation when user not found returns 404 Not Found")
    void testRecommendation_UserNotFound_Returns404() throws Exception {
        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new ResourceNotFoundException("User not found with email: user@example.com"));

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .param("city", "Bengaluru")
                        .principal(auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/hydration/recommendation when weather provider down returns 503 Service Unavailable")
    void testRecommendation_WeatherServiceUnavailable_Returns503() throws Exception {
        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new WeatherException(HttpStatus.SERVICE_UNAVAILABLE, "External weather service is temporarily unavailable. Please try again later."));

        mockMvc.perform(get("/api/v1/hydration/recommendation")
                        .param("city", "Bengaluru")
                        .principal(auth))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(503));
    }
}
