package com.caresync.service;

import com.caresync.dto.alert.AlertsResponse;
import com.caresync.dto.dashboard.DashboardAlertDto;
import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.entity.User;
import com.caresync.entity.WaterIntake;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.repository.UserRepository;
import com.caresync.repository.WaterIntakeRepository;
import com.caresync.service.alert.AlertRuleEngine;
import com.caresync.service.hydration.HydrationCalculationEngine;
import com.caresync.service.impl.AlertsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HydrationService hydrationService;

    @Spy
    private HydrationCalculationEngine calculationEngine = new HydrationCalculationEngine();

    @Spy
    private AlertRuleEngine alertRuleEngine = new AlertRuleEngine();

    @Mock
    private WaterIntakeRepository waterIntakeRepository;

    @InjectMocks
    private AlertsServiceImpl alertsService;

    private User sampleUser;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-09-10T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        alertsService.setClock(fixedClock);
        today = LocalDate.now(fixedClock);

        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Pavan Kalyan")
                .weight(70.0)
                .age(25)
                .occupation("Software Engineer")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("1. No water logged today returns HYDRATION_REMINDER alert")
    void testGetAlerts_NoWaterLoggedToday_ReturnsHydrationReminder() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(24.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertNotNull(response);
        assertEquals(1, response.getCount());
        assertEquals(1, response.getAlerts().size());

        DashboardAlertDto alert = response.getAlerts().get(0);
        assertEquals("HYDRATION_REMINDER", alert.getType());
        assertEquals("INFO", alert.getSeverity());
        assertEquals("Hydration reminder", alert.getTitle());
        assertEquals("You have not logged any water intake today.", alert.getMessage());
    }

    @Test
    @DisplayName("2. Temperature >= 35°C returns HIGH_TEMPERATURE warning alert")
    void testGetAlerts_TemperatureAtOrAbove35_ReturnsHighTemperature() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(3200)
                .temperatureCelsius(36.5)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder().amountMl(1000).loggedAt(today.atTime(10, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertEquals(1, response.getCount());
        DashboardAlertDto alert = response.getAlerts().get(0);
        assertEquals("HIGH_TEMPERATURE", alert.getType());
        assertEquals("WARNING", alert.getSeverity());
        assertEquals("High temperature", alert.getTitle());
        assertEquals("Stay hydrated today.", alert.getMessage());
    }

    @Test
    @DisplayName("3. Temperature below 35°C does NOT return HIGH_TEMPERATURE")
    void testGetAlerts_TemperatureBelow35_HighTemperatureNotReturned() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(34.9)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder().amountMl(1200).loggedAt(today.atTime(10, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertEquals(0, response.getCount());
        assertTrue(response.getAlerts().isEmpty());
    }

    @Test
    @DisplayName("4. Water consumption >= target returns HYDRATION_GOAL_REACHED alert")
    void testGetAlerts_ConsumptionAtOrAboveTarget_ReturnsHydrationGoalReached() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(25.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake1 = WaterIntake.builder().amountMl(1500).loggedAt(today.atTime(10, 0)).build();
        WaterIntake intake2 = WaterIntake.builder().amountMl(1200).loggedAt(today.atTime(14, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Arrays.asList(intake1, intake2));

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertEquals(1, response.getCount());
        DashboardAlertDto alert = response.getAlerts().get(0);
        assertEquals("HYDRATION_GOAL_REACHED", alert.getType());
        assertEquals("INFO", alert.getSeverity());
        assertEquals("Daily goal achieved", alert.getTitle());
        assertEquals("You have reached your daily hydration goal.", alert.getMessage());
    }

    @Test
    @DisplayName("5. Water consumption below target does NOT return HYDRATION_GOAL_REACHED")
    void testGetAlerts_ConsumptionBelowTarget_HydrationGoalReachedNotReturned() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(25.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder().amountMl(2400).loggedAt(today.atTime(10, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertEquals(0, response.getCount());
        assertTrue(response.getAlerts().isEmpty());
    }

    @Test
    @DisplayName("6. Multiple alerts can be returned together: High Temperature + Hydration Reminder")
    void testGetAlerts_MultipleAlerts_HighTemperatureAndHydrationReminder() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(3000)
                .temperatureCelsius(37.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertEquals(2, response.getCount());
        assertEquals(2, response.getAlerts().size());

        assertTrue(response.getAlerts().stream().anyMatch(a -> "HIGH_TEMPERATURE".equals(a.getType())));
        assertTrue(response.getAlerts().stream().anyMatch(a -> "HYDRATION_REMINDER".equals(a.getType())));
    }

    @Test
    @DisplayName("6b. Multiple alerts can be returned together: High Temperature + Daily Goal Achieved")
    void testGetAlerts_MultipleAlerts_HighTemperatureAndGoalReached() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(36.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder().amountMl(2700).loggedAt(today.atTime(15, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertEquals(2, response.getCount());
        assertTrue(response.getAlerts().stream().anyMatch(a -> "HIGH_TEMPERATURE".equals(a.getType())));
        assertTrue(response.getAlerts().stream().anyMatch(a -> "HYDRATION_GOAL_REACHED".equals(a.getType())));
    }

    @Test
    @DisplayName("7. No alert conditions met returns empty list and count 0")
    void testGetAlerts_NoAlertConditions_ReturnsEmptyListAndCountZero() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(22.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        WaterIntake intake = WaterIntake.builder().amountMl(1500).loggedAt(today.atTime(12, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertNotNull(response);
        assertEquals(0, response.getCount());
        assertTrue(response.getAlerts().isEmpty());
    }

    @Test
    @DisplayName("8. Weather unavailable gracefully degrades, omits HIGH_TEMPERATURE, returns hydration alerts")
    void testGetAlerts_WeatherUnavailable_GracefulDegradation() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new RuntimeException("OpenWeatherMap service unreachable"));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        AlertsResponse response = alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        assertNotNull(response);
        assertEquals(1, response.getCount());
        // Must contain HYDRATION_REMINDER based on baseline target; must NOT fabricate HIGH_TEMPERATURE
        assertEquals("HYDRATION_REMINDER", response.getAlerts().get(0).getType());
        assertFalse(response.getAlerts().stream().anyMatch(a -> "HIGH_TEMPERATURE".equals(a.getType())));
    }

    @Test
    @DisplayName("8b. No location provided uses baseline target and omits HIGH_TEMPERATURE")
    void testGetAlerts_NoLocationProvided_UsesBaseline() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        WaterIntake intake = WaterIntake.builder().amountMl(3000).loggedAt(today.atTime(12, 0)).build();
        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        // When city and lat/lon are null
        AlertsResponse response = alertsService.getAlerts("user@example.com", null, null, null);

        assertNotNull(response);
        // baseline target is 70 * 35 = 2450 ml; consumed 3000 ml >= 2450 ml -> GOAL_REACHED
        assertEquals(1, response.getCount());
        assertEquals("HYDRATION_GOAL_REACHED", response.getAlerts().get(0).getType());
        verifyNoInteractions(hydrationService);
    }

    @Test
    @DisplayName("9. Today's consumption strictly uses [startOfDay, endOfDay) date boundaries")
    void testGetAlerts_DateBoundaries_CorrectIntervalQueried() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        HydrationResponse hydrationMock = HydrationResponse.builder()
                .dailyWaterTargetMl(2500)
                .temperatureCelsius(24.0)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(hydrationMock);

        LocalDateTime expectedStart = today.atStartOfDay();
        LocalDateTime expectedEnd = today.plusDays(1).atStartOfDay();

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(
                eq(sampleUser), eq(expectedStart), eq(expectedEnd)))
                .thenReturn(Collections.emptyList());

        alertsService.getAlerts("user@example.com", "Bengaluru", null, null);

        verify(waterIntakeRepository).findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(
                sampleUser, expectedStart, expectedEnd);
    }

    @Test
    @DisplayName("10. Authenticated user data isolation")
    void testGetAlerts_UserIsolation() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        alertsService.getAlerts("user@example.com", null, null, null);

        verify(waterIntakeRepository).findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any());
    }

    @Test
    @DisplayName("11. Partial coordinates throws BadRequestException (lat without lon)")
    void testGetAlerts_PartialCoordinates_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () ->
                alertsService.getAlerts("user@example.com", null, 12.9716, null));
    }

    @Test
    @DisplayName("12. User not found throws ResourceNotFoundException")
    void testGetAlerts_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmailAndIsActiveTrue("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                alertsService.getAlerts("unknown@example.com", "Bengaluru", null, null));
    }
}
