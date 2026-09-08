package com.caresync.client;

import com.caresync.client.impl.OpenWeatherApiClient;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.exception.WeatherException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OpenWeatherApiClientTest {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final String API_KEY = "test-api-key";

    private MockRestServiceServer mockServer;
    private OpenWeatherApiClient weatherClient;

    private static final String SUCCESS_RESPONSE_JSON = """
            {
              "coord": { "lon": 77.5946, "lat": 12.9716 },
              "weather": [
                { "id": 800, "main": "Clear", "description": "clear sky", "icon": "01d" }
              ],
              "main": {
                "temp": 28.5,
                "feels_like": 29.8,
                "temp_min": 26.0,
                "temp_max": 30.5,
                "humidity": 65
              },
              "wind": { "speed": 3.6 },
              "sys": { "country": "IN" },
              "name": "Bengaluru",
              "dt": 1725710000
            }
            """;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        weatherClient = new OpenWeatherApiClient(restClient);
        ReflectionTestUtils.setField(weatherClient, "apiKey", API_KEY);
        ReflectionTestUtils.setField(weatherClient, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("fetchWeatherByCity should parse OpenWeatherMap JSON and return mapped WeatherResponse in Celsius")
    void testFetchWeatherByCity_Success() {
        mockServer.expect(requestTo(containsString("/weather")))
                .andExpect(requestTo(containsString("q=Bengaluru")))
                .andExpect(requestTo(containsString("units=metric")))
                .andExpect(requestTo(containsString("appid=" + API_KEY)))
                .andRespond(withSuccess(SUCCESS_RESPONSE_JSON, MediaType.APPLICATION_JSON));

        WeatherResponse response = weatherClient.fetchWeatherByCity("Bengaluru");

        assertNotNull(response);
        assertEquals("Bengaluru", response.getLocation());
        assertEquals("IN", response.getCountry());
        assertEquals(12.9716, response.getLatitude());
        assertEquals(77.5946, response.getLongitude());
        assertEquals(28.5, response.getTemperature());
        assertEquals(29.8, response.getFeelsLikeTemperature());
        assertEquals(26.0, response.getMinimumTemperature());
        assertEquals(30.5, response.getMaximumTemperature());
        assertEquals(65, response.getHumidity());
        assertEquals("Clear", response.getWeatherCondition());
        assertEquals("clear sky", response.getWeatherDescription());
        assertEquals(3.6, response.getWindSpeed());
        assertNotNull(response.getObservedAt());

        mockServer.verify();
    }

    @Test
    @DisplayName("fetchWeatherByCoordinates should retrieve weather for valid latitude and longitude")
    void testFetchWeatherByCoordinates_Success() {
        mockServer.expect(requestTo(containsString("/weather")))
                .andExpect(requestTo(containsString("lat=12.9716")))
                .andExpect(requestTo(containsString("lon=77.5946")))
                .andExpect(requestTo(containsString("units=metric")))
                .andRespond(withSuccess(SUCCESS_RESPONSE_JSON, MediaType.APPLICATION_JSON));

        WeatherResponse response = weatherClient.fetchWeatherByCoordinates(12.9716, 77.5946);

        assertNotNull(response);
        assertEquals("Bengaluru", response.getLocation());
        assertEquals(28.5, response.getTemperature());
        mockServer.verify();
    }

    @Test
    @DisplayName("Missing API key should throw WeatherException with 503 SERVICE_UNAVAILABLE")
    void testApiKeyNotConfigured_ThrowsWeatherException() {
        ReflectionTestUtils.setField(weatherClient, "apiKey", "");

        WeatherException exception = assertThrows(WeatherException.class,
                () -> weatherClient.fetchWeatherByCity("Bengaluru"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertTrue(exception.getMessage().contains("API key is not configured"));
    }

    @Test
    @DisplayName("External API 404 response should be mapped to ResourceNotFoundException")
    void testExternal404_ThrowsResourceNotFoundException() {
        mockServer.expect(requestTo(containsString("/weather")))
                .andRespond(withResourceNotFound());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> weatherClient.fetchWeatherByCity("NonExistentCityXYZ"));

        assertTrue(exception.getMessage().contains("NonExistentCityXYZ"));
        mockServer.verify();
    }

    @Test
    @DisplayName("External API 401/403 should be mapped to safe 503 without leaking credentials")
    void testExternal401_ThrowsWeatherException503() {
        mockServer.expect(requestTo(containsString("/weather")))
                .andRespond(withUnauthorizedRequest());

        WeatherException exception = assertThrows(WeatherException.class,
                () -> weatherClient.fetchWeatherByCity("Bengaluru"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertFalse(exception.getMessage().contains(API_KEY));
        assertTrue(exception.getMessage().contains("Weather service is currently unavailable"));
        mockServer.verify();
    }

    @Test
    @DisplayName("External API 5xx server failure should be mapped to 503 SERVICE_UNAVAILABLE")
    void testExternal500_ThrowsWeatherException503() {
        mockServer.expect(requestTo(containsString("/weather")))
                .andRespond(withServerError());

        WeatherException exception = assertThrows(WeatherException.class,
                () -> weatherClient.fetchWeatherByCity("Bengaluru"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertTrue(exception.getMessage().contains("External weather service is temporarily unavailable"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Network timeout should be mapped to WeatherException 503")
    void testNetworkTimeout_ThrowsWeatherException503() {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.net.URI.class))).thenThrow(new ResourceAccessException("Read timed out"));

        OpenWeatherApiClient timeoutClient = new OpenWeatherApiClient(mockRestClient);
        ReflectionTestUtils.setField(timeoutClient, "apiKey", API_KEY);
        ReflectionTestUtils.setField(timeoutClient, "baseUrl", BASE_URL);

        WeatherException exception = assertThrows(WeatherException.class,
                () -> timeoutClient.fetchWeatherByCity("Bengaluru"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    @DisplayName("Malformed/unexpected response body should be handled safely")
    void testMalformedResponse_HandledSafely() {
        String malformedJson = """
                {
                  "coord": null,
                  "weather": [],
                  "main": null,
                  "wind": null,
                  "name": "UnknownLocation",
                  "dt": null
                }
                """;

        mockServer.expect(requestTo(containsString("/weather")))
                .andRespond(withSuccess(malformedJson, MediaType.APPLICATION_JSON));

        WeatherResponse response = weatherClient.fetchWeatherByCity("UnknownLocation");

        assertNotNull(response);
        assertEquals("UnknownLocation", response.getLocation());
        assertNull(response.getTemperature());
        assertNotNull(response.getObservedAt());
        mockServer.verify();
    }
}
