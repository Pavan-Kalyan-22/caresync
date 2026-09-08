package com.caresync.security;

import com.caresync.controller.WeatherController;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.service.WeatherService;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WeatherSecurityTest {

    private MockMvc mockMvc;

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        entryPoint = new JwtAuthenticationEntryPoint();
    }

    @Test
    @DisplayName("Authenticated user with valid credentials can access weather endpoint")
    void testAuthenticatedUser_AccessesWeatherSuccessfully() throws Exception {
        WeatherResponse mockResponse = WeatherResponse.builder()
                .location("Bengaluru")
                .country("IN")
                .temperature(28.5)
                .observedAt(LocalDateTime.now())
                .build();

        when(weatherService.getCurrentWeather("Bengaluru", null, null)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/weather/current")
                        .param("city", "Bengaluru")
                        .principal(() -> "user@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.location").value("Bengaluru"))
                .andExpect(jsonPath("$.data.temperature").value(28.5));

        verify(weatherService).getCurrentWeather("Bengaluru", null, null);
    }

    @Test
    @DisplayName("Scenario 1: No JWT token provided to weather endpoint should return 401 Unauthorized")
    void testNoJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/weather/current");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Full authentication is required to access this resource"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Full authentication is required"));
    }

    @Test
    @DisplayName("Scenario 2: Invalid JWT token provided to weather endpoint should return 401 Unauthorized")
    void testInvalidJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/weather/current");
        request.addHeader("Authorization", "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("Invalid or malformed JWT token"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
    }

    @Test
    @DisplayName("Scenario 3: Expired JWT token provided to weather endpoint should return 401 Unauthorized")
    void testExpiredJwt_Returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/weather/current");
        request.addHeader("Authorization", "Bearer expired.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response,
                new InsufficientAuthenticationException("JWT token has expired"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"success\":false"));
    }
}
