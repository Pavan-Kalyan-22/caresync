package com.caresync.service;

import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.entity.User;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.exception.WeatherException;
import com.caresync.repository.UserRepository;
import com.caresync.service.hydration.HydrationCalculationEngine;
import com.caresync.service.impl.HydrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HydrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WeatherService weatherService;

    @Spy
    private HydrationCalculationEngine calculationEngine = new HydrationCalculationEngine();

    @InjectMocks
    private HydrationServiceImpl hydrationService;

    private User sampleUser;
    private WeatherResponse sampleWeather;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .weight(70.0)
                .occupation("Software Engineer")
                .age(30)
                .isActive(true)
                .build();

        sampleWeather = WeatherResponse.builder()
                .location("Bengaluru")
                .country("IN")
                .temperature(30.0)
                .humidity(60)
                .weatherCondition("Clear")
                .build();
    }

    @Test
    @DisplayName("Valid city request successfully coordinates user lookup, weather lookup, and engine calculation")
    void testGetRecommendation_WithCity_Success() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com")).thenReturn(Optional.of(sampleUser));
        when(weatherService.getCurrentWeather("Bengaluru", null, null)).thenReturn(sampleWeather);

        HydrationResponse response = hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null);

        assertNotNull(response);
        assertEquals(2950, response.getDailyWaterTargetMl()); // 2450 (base) + 500 (temp 30°C)
        assertEquals(2.95, response.getDailyWaterTargetLitres());
        verify(userRepository).findByEmailAndIsActiveTrue("user@example.com");
        verify(weatherService).getCurrentWeather("Bengaluru", null, null);
    }

    @Test
    @DisplayName("Valid coordinates request successfully coordinates with weather service by lat/lon")
    void testGetRecommendation_WithCoordinates_Success() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com")).thenReturn(Optional.of(sampleUser));
        when(weatherService.getCurrentWeather(null, 12.9716, 77.5946)).thenReturn(sampleWeather);

        HydrationResponse response = hydrationService.getRecommendation("user@example.com", null, 12.9716, 77.5946);

        assertNotNull(response);
        assertEquals(2950, response.getDailyWaterTargetMl());
        verify(weatherService).getCurrentWeather(null, 12.9716, 77.5946);
    }

    @Test
    @DisplayName("Missing location parameters (neither city nor coords) throws BadRequestException without country fallback")
    void testGetRecommendation_MissingLocation_ThrowsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> hydrationService.getRecommendation("user@example.com", null, null, null));

        assertTrue(ex.getMessage().contains("Either city or coordinates"));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(weatherService);
    }

    @Test
    @DisplayName("Incomplete coordinates (only latitude or only longitude) throws BadRequestException")
    void testGetRecommendation_IncompleteCoordinates_ThrowsBadRequestException() {
        BadRequestException exLatOnly = assertThrows(BadRequestException.class,
                () -> hydrationService.getRecommendation("user@example.com", null, 12.9716, null));
        assertTrue(exLatOnly.getMessage().contains("Both latitude and longitude must be provided"));

        BadRequestException exLonOnly = assertThrows(BadRequestException.class,
                () -> hydrationService.getRecommendation("user@example.com", null, null, 77.5946));
        assertTrue(exLonOnly.getMessage().contains("Both latitude and longitude must be provided"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Non-existent active user throws ResourceNotFoundException")
    void testGetRecommendation_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmailAndIsActiveTrue("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> hydrationService.getRecommendation("unknown@example.com", "Bengaluru", null, null));

        verify(userRepository).findByEmailAndIsActiveTrue("unknown@example.com");
        verifyNoInteractions(weatherService);
    }

    @Test
    @DisplayName("User with missing weight in profile throws BadRequestException")
    void testGetRecommendation_MissingWeight_ThrowsBadRequestException() {
        sampleUser.setWeight(null);
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null));

        assertTrue(ex.getMessage().contains("weight is required for hydration calculation"));
        verifyNoInteractions(weatherService);
    }

    @Test
    @DisplayName("Underage user (< 18 years) throws BadRequestException")
    void testGetRecommendation_UnderageUser_ThrowsBadRequestException() {
        sampleUser.setAge(16);
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null));

        assertTrue(ex.getMessage().contains("tailored for adult users (18+)"));
        verifyNoInteractions(weatherService);
    }

    @Test
    @DisplayName("Invalid user age (> 120) throws BadRequestException")
    void testGetRecommendation_InvalidAge_ThrowsBadRequestException() {
        sampleUser.setAge(125);
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com")).thenReturn(Optional.of(sampleUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null));

        assertTrue(ex.getMessage().contains("Invalid age in user profile"));
        verifyNoInteractions(weatherService);
    }

    @Test
    @DisplayName("Weather service failure propagates WeatherException cleanly")
    void testGetRecommendation_WeatherServiceFails_PropagatesException() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com")).thenReturn(Optional.of(sampleUser));
        when(weatherService.getCurrentWeather("Bengaluru", null, null))
                .thenThrow(new WeatherException(HttpStatus.SERVICE_UNAVAILABLE, "Weather service timed out"));

        assertThrows(WeatherException.class,
                () -> hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null));
    }
}
