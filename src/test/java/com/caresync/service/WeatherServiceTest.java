package com.caresync.service;

import com.caresync.client.WeatherApiClient;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.exception.BadRequestException;
import com.caresync.service.impl.WeatherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherApiClient weatherApiClient;

    @InjectMocks
    private WeatherServiceImpl weatherService;

    private WeatherResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = WeatherResponse.builder()
                .location("Bengaluru")
                .country("IN")
                .latitude(12.9716)
                .longitude(77.5946)
                .temperature(28.5)
                .humidity(65)
                .weatherCondition("Clear")
                .build();
    }

    @Test
    @DisplayName("Valid city should invoke weatherApiClient.fetchWeatherByCity")
    void testGetCurrentWeather_WithCity_CallsClientByCity() {
        when(weatherApiClient.fetchWeatherByCity("Bengaluru")).thenReturn(sampleResponse);

        WeatherResponse result = weatherService.getCurrentWeather("Bengaluru", null, null);

        assertNotNull(result);
        assertEquals("Bengaluru", result.getLocation());
        assertEquals(28.5, result.getTemperature());
        verify(weatherApiClient).fetchWeatherByCity("Bengaluru");
        verify(weatherApiClient, never()).fetchWeatherByCoordinates(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Valid coordinates should invoke weatherApiClient.fetchWeatherByCoordinates")
    void testGetCurrentWeather_WithCoordinates_CallsClientByCoords() {
        when(weatherApiClient.fetchWeatherByCoordinates(12.9716, 77.5946)).thenReturn(sampleResponse);

        WeatherResponse result = weatherService.getCurrentWeather(null, 12.9716, 77.5946);

        assertNotNull(result);
        assertEquals(12.9716, result.getLatitude());
        verify(weatherApiClient).fetchWeatherByCoordinates(12.9716, 77.5946);
        verify(weatherApiClient, never()).fetchWeatherByCity(anyString());
    }

    @Test
    @DisplayName("When both city and coordinates are supplied, coordinates take precedence")
    void testGetCurrentWeather_BothCityAndCoords_PrefersCoordinates() {
        when(weatherApiClient.fetchWeatherByCoordinates(12.9716, 77.5946)).thenReturn(sampleResponse);

        WeatherResponse result = weatherService.getCurrentWeather("Bengaluru", 12.9716, 77.5946);

        assertNotNull(result);
        verify(weatherApiClient).fetchWeatherByCoordinates(12.9716, 77.5946);
        verify(weatherApiClient, never()).fetchWeatherByCity(anyString());
    }

    @Test
    @DisplayName("Missing both city and coordinates should throw BadRequestException")
    void testGetCurrentWeather_MissingBoth_ThrowsBadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, null, null));
        assertTrue(ex.getMessage().contains("Either city or coordinates"));

        BadRequestException ex2 = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather("   ", null, null));
        assertTrue(ex2.getMessage().contains("Either city or coordinates"));
    }

    @Test
    @DisplayName("Providing latitude without longitude should throw BadRequestException")
    void testGetCurrentWeather_LatitudeWithoutLongitude_ThrowsBadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, 12.9716, null));
        assertTrue(ex.getMessage().contains("Both latitude and longitude must be provided"));
    }

    @Test
    @DisplayName("Providing longitude without latitude should throw BadRequestException")
    void testGetCurrentWeather_LongitudeWithoutLatitude_ThrowsBadRequest() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, null, 77.5946));
        assertTrue(ex.getMessage().contains("Both latitude and longitude must be provided"));
    }

    @Test
    @DisplayName("Invalid latitude (> 90 or < -90) should throw BadRequestException")
    void testGetCurrentWeather_InvalidLatitude_ThrowsBadRequest() {
        BadRequestException exHigh = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, 90.1, 77.5946));
        assertTrue(exHigh.getMessage().contains("Latitude must be between -90 and 90"));

        BadRequestException exLow = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, -90.1, 77.5946));
        assertTrue(exLow.getMessage().contains("Latitude must be between -90 and 90"));
    }

    @Test
    @DisplayName("Invalid longitude (> 180 or < -180) should throw BadRequestException")
    void testGetCurrentWeather_InvalidLongitude_ThrowsBadRequest() {
        BadRequestException exHigh = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, 12.9716, 180.1));
        assertTrue(exHigh.getMessage().contains("Longitude must be between -180 and 180"));

        BadRequestException exLow = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(null, 12.9716, -180.1));
        assertTrue(exLow.getMessage().contains("Longitude must be between -180 and 180"));
    }

    @Test
    @DisplayName("City name exceeding maximum length should throw BadRequestException")
    void testGetCurrentWeather_CityTooLong_ThrowsBadRequest() {
        String longCity = "A".repeat(101);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> weatherService.getCurrentWeather(longCity, null, null));
        assertTrue(ex.getMessage().contains("exceeds maximum allowed length"));
    }
}
