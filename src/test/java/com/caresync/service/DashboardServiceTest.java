package com.caresync.service;

import com.caresync.dto.dashboard.DashboardAlertDto;
import com.caresync.dto.dashboard.DashboardResponse;
import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.entity.User;
import com.caresync.entity.WaterIntake;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.repository.UserRepository;
import com.caresync.repository.WaterIntakeRepository;
import com.caresync.service.hydration.HydrationCalculationEngine;
import com.caresync.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HydrationService hydrationService;

    @Spy
    private HydrationCalculationEngine calculationEngine = new HydrationCalculationEngine();

    @Mock
    private WaterIntakeRepository waterIntakeRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User sampleUser;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-09-10T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        dashboardService.setClock(fixedClock);
        today = LocalDate.now(fixedClock);

        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Pavan Kalyan")
                .weight(70.0)
                .occupation("Software Engineer")
                .age(28)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Successful dashboard generation with city request produces complete composite response")
    void testGetDashboard_Success_CityRequest() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2950)
                .dailyWaterTargetLitres(2.95)
                .location("Bengaluru")
                .temperatureCelsius(32.0)
                .humidityPercent(55)
                .weatherCondition("Clouds")
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder()
                .id(101L)
                .user(sampleUser)
                .amountMl(1500)
                .loggedAt(today.atTime(10, 0))
                .build();

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", null, null);

        assertNotNull(response);

        // User summary
        assertEquals("Pavan Kalyan", response.getUser().getName());

        // Weather summary
        assertNotNull(response.getWeather());
        assertEquals("Bengaluru", response.getWeather().getLocation());
        assertEquals(32.0, response.getWeather().getTemperature());
        assertEquals(55, response.getWeather().getHumidity());
        assertEquals("Clouds", response.getWeather().getWeatherCondition());

        // Hydration summary
        assertEquals(2950, response.getHydration().getDailyTargetMl());
        assertEquals(2.95, response.getHydration().getDailyTargetLitres());
        assertEquals(1500, response.getHydration().getConsumedMl());
        assertEquals(1450, response.getHydration().getRemainingMl(), "Remaining = 2950 - 1500 = 1450");
        assertEquals(50.85, response.getHydration().getProgressPercentage(), "Progress = (1500 / 2950) * 100 = 50.85%");
        assertTrue(response.getHydration().getIsWeatherAdjusted());

        // Alerts: 32°C should trigger WARM_TEMPERATURE
        assertTrue(response.getAlerts().stream().anyMatch(a -> "WARM_TEMPERATURE".equals(a.getType())));

        // Timestamp
        assertEquals(LocalDateTime.now(fixedClock), response.getCalculatedAt());
    }

    @Test
    @DisplayName("Dashboard coordinate request passes coordinates and delegates precedence correctly")
    void testGetDashboard_CoordinatesPrecedence() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2450)
                .dailyWaterTargetLitres(2.45)
                .location("Bengaluru")
                .temperatureCelsius(22.0)
                .humidityPercent(50)
                .weatherCondition("Clear")
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", 12.9716, 77.5946))
                .thenReturn(hydrationMock);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", 12.9716, 77.5946);

        assertNotNull(response);
        verify(hydrationService).getRecommendation("user@example.com", "Bengaluru", 12.9716, 77.5946);
    }

    @Test
    @DisplayName("Today's water consumption aggregates multiple intakes and calculates remaining accurately")
    void testGetDashboard_AggregatesTodayConsumption() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .dailyWaterTargetLitres(2.50)
                .temperatureCelsius(22.0)
                .humidityPercent(50)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake1 = WaterIntake.builder().amountMl(600).loggedAt(today.atTime(8, 0)).build();
        WaterIntake intake2 = WaterIntake.builder().amountMl(900).loggedAt(today.atTime(13, 0)).build();

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Arrays.asList(intake2, intake1));

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", null, null);

        assertEquals(1500, response.getHydration().getConsumedMl());
        assertEquals(1000, response.getHydration().getRemainingMl(), "2500 - 1500 = 1000");
        assertEquals(60.0, response.getHydration().getProgressPercentage(), "(1500 / 2500) * 100 = 60.0%");
    }

    @Test
    @DisplayName("Zero water consumption sets consumedMl to 0 and triggers HYDRATION_REMINDER alert")
    void testGetDashboard_ZeroConsumption_TriggersReminder() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .dailyWaterTargetLitres(2.50)
                .temperatureCelsius(22.0)
                .humidityPercent(50)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", null, null);

        assertEquals(0, response.getHydration().getConsumedMl());
        assertEquals(2500, response.getHydration().getRemainingMl(), "Remaining equals target when consumed is 0");
        assertEquals(0.0, response.getHydration().getProgressPercentage());

        assertTrue(response.getAlerts().stream().anyMatch(a -> "HYDRATION_REMINDER".equals(a.getType())),
                "Must trigger HYDRATION_REMINDER alert when consumed is 0");
    }

    @Test
    @DisplayName("Remaining water is never negative and progress exceeds 100% when consumed exceeds target")
    void testGetDashboard_RemainingNeverNegative_ProgressUncapped() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .dailyWaterTargetLitres(2.50)
                .temperatureCelsius(22.0)
                .humidityPercent(50)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder().amountMl(3000).loggedAt(today.atTime(15, 0)).build();

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", null, null);

        assertEquals(3000, response.getHydration().getConsumedMl());
        assertEquals(0, response.getHydration().getRemainingMl(), "Remaining water must clamp to 0 and never be negative");
        assertEquals(120.0, response.getHydration().getProgressPercentage(), "Progress must be uncapped (120.0%)");

        assertTrue(response.getAlerts().stream().anyMatch(a -> "HYDRATION_GOAL_REACHED".equals(a.getType())),
                "Must trigger HYDRATION_GOAL_REACHED alert when consumed >= target");
    }

    @Test
    @DisplayName("Temperature >= 35°C triggers HIGH_TEMPERATURE warning alert")
    void testGetDashboard_Alert_HighTemperature() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(3200)
                .dailyWaterTargetLitres(3.20)
                .temperatureCelsius(36.5)
                .humidityPercent(45)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", null, null);

        DashboardAlertDto alert = response.getAlerts().stream()
                .filter(a -> "HIGH_TEMPERATURE".equals(a.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(alert);
        assertEquals("WARNING", alert.getSeverity());
        assertTrue(alert.getMessage().contains("36.5°C"));
    }

    @Test
    @DisplayName("Temperature >= 28°C and Humidity >= 70% triggers HIGH_HUMIDITY warning alert")
    void testGetDashboard_Alert_HighHumidity() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(3150)
                .dailyWaterTargetLitres(3.15)
                .temperatureCelsius(29.0)
                .humidityPercent(78)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Chennai", null, null))
                .thenReturn(hydrationMock);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Chennai", null, null);

        DashboardAlertDto alert = response.getAlerts().stream()
                .filter(a -> "HIGH_HUMIDITY".equals(a.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(alert);
        assertEquals("WARNING", alert.getSeverity());
        assertTrue(alert.getMessage().contains("78%"));
    }

    @Test
    @DisplayName("Weather failure falls back to baseline hydration target and sets isWeatherAdjusted=false")
    void testGetDashboard_WeatherFailure_FallsBackToBaseline() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        // Simulate weather provider failure
        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new RuntimeException("OpenWeatherMap 503 Service Unavailable"));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        DashboardResponse response = dashboardService.getDashboard("user@example.com", "Bengaluru", null, null);

        assertNotNull(response);
        // User summary remains intact
        assertEquals("Pavan Kalyan", response.getUser().getName());

        // Weather section is null
        assertNull(response.getWeather());

        // Hydration target falls back to baseline (70 * 35 = 2450 ml)
        assertEquals(2450, response.getHydration().getDailyTargetMl());
        assertEquals(2.45, response.getHydration().getDailyTargetLitres());
        assertFalse(response.getHydration().getIsWeatherAdjusted(), "Must be marked isWeatherAdjusted = false");

        // Alerts must contain WEATHER_UNAVAILABLE
        DashboardAlertDto weatherAlert = response.getAlerts().stream()
                .filter(a -> "WEATHER_UNAVAILABLE".equals(a.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(weatherAlert);
        assertEquals("INFO", weatherAlert.getSeverity());
        assertEquals("Current weather is unavailable. Showing your baseline hydration target.", weatherAlert.getMessage());
    }

    @Test
    @DisplayName("Missing both city and coordinates throws BadRequestException")
    void testGetDashboard_MissingLocation_ThrowsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> dashboardService.getDashboard("user@example.com", null, null, null));

        assertThrows(BadRequestException.class,
                () -> dashboardService.getDashboard("user@example.com", "   ", null, null));
    }

    @Test
    @DisplayName("Supplying only one coordinate throws BadRequestException")
    void testGetDashboard_IncompleteCoordinates_ThrowsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> dashboardService.getDashboard("user@example.com", null, 12.9716, null));

        assertThrows(BadRequestException.class,
                () -> dashboardService.getDashboard("user@example.com", null, null, 77.5946));
    }

    @Test
    @DisplayName("User not found throws ResourceNotFoundException")
    void testGetDashboard_UserNotFound_ThrowsNotFound() {
        when(userRepository.findByEmailAndIsActiveTrue("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dashboardService.getDashboard("unknown@example.com", "Bengaluru", null, null));
    }

    @Test
    @DisplayName("Validation exception from HydrationService is propagated directly as BadRequestException")
    void testGetDashboard_ValidationException_Propagated() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new BadRequestException("User profile is incomplete: weight is required"));

        assertThrows(BadRequestException.class,
                () -> dashboardService.getDashboard("user@example.com", "Bengaluru", null, null));
    }
}
