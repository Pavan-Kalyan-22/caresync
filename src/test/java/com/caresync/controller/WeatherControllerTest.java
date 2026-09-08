package com.caresync.controller;

import com.caresync.dto.weather.WeatherResponse;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.exception.WeatherException;
import com.caresync.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    private WeatherResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = WeatherResponse.builder()
                .location("Bengaluru")
                .country("IN")
                .latitude(12.9716)
                .longitude(77.5946)
                .temperature(28.5)
                .feelsLikeTemperature(29.8)
                .minimumTemperature(26.0)
                .maximumTemperature(30.5)
                .humidity(65)
                .weatherCondition("Clear")
                .weatherDescription("clear sky")
                .windSpeed(3.6)
                .observedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/weather/current with valid city returns 200 OK")
    void testGetCurrentWeather_ValidCity_Returns200() throws Exception {
        when(weatherService.getCurrentWeather("Bengaluru", null, null)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/weather/current")
                        .param("city", "Bengaluru")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current weather retrieved successfully"))
                .andExpect(jsonPath("$.data.location").value("Bengaluru"))
                .andExpect(jsonPath("$.data.temperature").value(28.5))
                .andExpect(jsonPath("$.data.humidity").value(65))
                .andExpect(jsonPath("$.data.weatherCondition").value("Clear"));
    }

    @Test
    @DisplayName("GET /api/v1/weather/current with valid coordinates returns 200 OK")
    void testGetCurrentWeather_ValidCoordinates_Returns200() throws Exception {
        when(weatherService.getCurrentWeather(null, 12.9716, 77.5946)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/weather/current")
                        .param("lat", "12.9716")
                        .param("lon", "77.5946")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.latitude").value(12.9716))
                .andExpect(jsonPath("$.data.longitude").value(77.5946));
    }

    @Test
    @DisplayName("GET /api/v1/weather/current missing location parameters returns 400 Bad Request")
    void testGetCurrentWeather_MissingParameters_Returns400() throws Exception {
        when(weatherService.getCurrentWeather(null, null, null))
                .thenThrow(new BadRequestException("Either city or coordinates (latitude and longitude) must be provided"));

        mockMvc.perform(get("/api/v1/weather/current"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Either city or coordinates (latitude and longitude) must be provided"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/weather/current with invalid coordinates returns 400 Bad Request")
    void testGetCurrentWeather_InvalidCoordinates_Returns400() throws Exception {
        when(weatherService.getCurrentWeather(null, 95.0, 77.0))
                .thenThrow(new BadRequestException("Latitude must be between -90 and 90 degrees"));

        mockMvc.perform(get("/api/v1/weather/current")
                        .param("lat", "95.0")
                        .param("lon", "77.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Latitude must be between -90 and 90 degrees"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/weather/current for unknown location returns 404 Not Found")
    void testGetCurrentWeather_LocationNotFound_Returns404() throws Exception {
        when(weatherService.getCurrentWeather("UnknownCityXYZ", null, null))
                .thenThrow(new ResourceNotFoundException("Weather data not found for location: UnknownCityXYZ"));

        mockMvc.perform(get("/api/v1/weather/current")
                        .param("city", "UnknownCityXYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Weather data not found for location: UnknownCityXYZ"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/weather/current when external provider is down returns 503 Service Unavailable")
    void testGetCurrentWeather_ServiceUnavailable_Returns503() throws Exception {
        when(weatherService.getCurrentWeather("Bengaluru", null, null))
                .thenThrow(new WeatherException(HttpStatus.SERVICE_UNAVAILABLE, "External weather service is temporarily unavailable. Please try again later."));

        mockMvc.perform(get("/api/v1/weather/current")
                        .param("city", "Bengaluru"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("External weather service is temporarily unavailable. Please try again later."))
                .andExpect(jsonPath("$.statusCode").value(503));
    }
}
