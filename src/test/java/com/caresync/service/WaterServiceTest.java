package com.caresync.service;

import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.dto.water.DailyWaterHistoryDto;
import com.caresync.dto.water.WaterHistoryResponse;
import com.caresync.dto.water.WaterLogRequest;
import com.caresync.dto.water.WaterLogResponse;
import com.caresync.entity.User;
import com.caresync.entity.WaterIntake;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.repository.UserRepository;
import com.caresync.repository.WaterIntakeRepository;
import com.caresync.service.hydration.HydrationCalculationEngine;
import com.caresync.service.impl.WaterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WaterIntakeRepository waterIntakeRepository;

    @Mock
    private HydrationService hydrationService;

    @Spy
    private HydrationCalculationEngine calculationEngine = new HydrationCalculationEngine();

    @InjectMocks
    private WaterServiceImpl waterService;

    private User sampleUser;
    private Clock fixedClock;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        // Fix clock to 2026-09-09 12:00:00 UTC
        Instant fixedInstant = Instant.parse("2026-09-09T12:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        waterService.setClock(fixedClock);
        today = LocalDate.now(fixedClock);

        sampleUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .weight(70.0)
                .occupation("Software Engineer")
                .age(28)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Log water intake successfully persists record and returns response")
    void testLogWaterIntake_Success() {
        WaterLogRequest request = WaterLogRequest.builder().amountMl(500).build();

        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(waterIntakeRepository.save(any(WaterIntake.class)))
                .thenAnswer(invocation -> {
                    WaterIntake saved = invocation.getArgument(0);
                    saved.setId(101L);
                    return saved;
                });

        WaterLogResponse response = waterService.logWaterIntake("user@example.com", request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(500, response.getAmountMl());
        assertEquals(LocalDateTime.now(fixedClock), response.getLoggedAt());

        verify(waterIntakeRepository).save(any(WaterIntake.class));
    }

    @Test
    @DisplayName("Log water intake with unknown user throws ResourceNotFoundException")
    void testLogWaterIntake_UserNotFound() {
        WaterLogRequest request = WaterLogRequest.builder().amountMl(300).build();

        when(userRepository.findByEmailAndIsActiveTrue("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> waterService.logWaterIntake("unknown@example.com", request));

        verify(waterIntakeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get water history returns exactly 7 calendar days, newest day first")
    void testGetWaterHistory_Exact7Days_NewestFirst() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", null, null, null);

        assertNotNull(response);
        assertNotNull(response.getDays());
        assertEquals(7, response.getDays().size(), "Must return exactly 7 calendar days");

        // Verify ordering: newest first
        assertEquals(today, response.getDays().get(0).getDate(), "Day 0 must be today");
        assertEquals(today.minusDays(1), response.getDays().get(1).getDate());
        assertEquals(today.minusDays(2), response.getDays().get(2).getDate());
        assertEquals(today.minusDays(3), response.getDays().get(3).getDate());
        assertEquals(today.minusDays(4), response.getDays().get(4).getDate());
        assertEquals(today.minusDays(5), response.getDays().get(5).getDate());
        assertEquals(today.minusDays(6), response.getDays().get(6).getDate(), "Day 6 must be 6 days ago");
    }

    @Test
    @DisplayName("Get water history aggregates multiple water intakes on the same calendar day")
    void testGetWaterHistory_AggregatesSameDayIntakes() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        // Two intakes today: 500 ml at 09:00 and 750 ml at 14:00 (Total = 1250 ml)
        WaterIntake intake1 = WaterIntake.builder()
                .id(1L)
                .user(sampleUser)
                .amountMl(500)
                .loggedAt(today.atTime(9, 0))
                .build();
        WaterIntake intake2 = WaterIntake.builder()
                .id(2L)
                .user(sampleUser)
                .amountMl(750)
                .loggedAt(today.atTime(14, 0))
                .build();

        // One intake yesterday: 800 ml
        WaterIntake intake3 = WaterIntake.builder()
                .id(3L)
                .user(sampleUser)
                .amountMl(800)
                .loggedAt(today.minusDays(1).atTime(10, 30))
                .build();

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Arrays.asList(intake2, intake1, intake3));

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", null, null, null);

        DailyWaterHistoryDto day0 = response.getDays().get(0);
        assertEquals(today, day0.getDate());
        assertEquals(1250, day0.getConsumedMl(), "Today's consumption must be sum of 500 + 750");

        DailyWaterHistoryDto day1 = response.getDays().get(1);
        assertEquals(today.minusDays(1), day1.getDate());
        assertEquals(800, day1.getConsumedMl());
    }

    @Test
    @DisplayName("Days with zero water consumption are retained with consumedMl 0 and progress 0.0")
    void testGetWaterHistory_DaysWithZeroConsumptionRetained() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", null, null, null);

        for (DailyWaterHistoryDto day : response.getDays()) {
            assertEquals(0, day.getConsumedMl());
            assertEquals(0.0, day.getProgressPercentage());
            assertEquals(2450, day.getDailyTargetMl(), "Baseline for 70kg Software Engineer (70*35 + 0 = 2450)");
        }
    }

    @Test
    @DisplayName("Progress percentage can exceed 100% when consumed water exceeds target")
    void testGetWaterHistory_ProgressExceeds100() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        // Baseline target is 2450 ml. Consumed: 3000 ml -> Progress = (3000 / 2450) * 100 = 122.45%
        WaterIntake intake = WaterIntake.builder()
                .id(1L)
                .user(sampleUser)
                .amountMl(3000)
                .loggedAt(today.atTime(12, 0))
                .build();

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.singletonList(intake));

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", null, null, null);

        DailyWaterHistoryDto day0 = response.getDays().get(0);
        assertEquals(3000, day0.getConsumedMl());
        assertEquals(2450, day0.getDailyTargetMl());
        assertEquals(122.45, day0.getProgressPercentage(), "Progress must be uncapped");
    }

    @Test
    @DisplayName("Today uses weather-adjusted target when location is provided while historical days use baseline")
    void testGetWaterHistory_TodayWeatherTarget_HistoricalBaselineTarget() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        // Mock weather-adjusted recommendation for today in Bengaluru: 2950 ml
        HydrationResponse weatherRecommendation = HydrationResponse.builder()
                .dailyWaterTargetMl(2950)
                .build();

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenReturn(weatherRecommendation);

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", "Bengaluru", null, null);

        // Day 0 (Today) must have weather-adjusted target: 2950 ml
        assertEquals(2950, response.getDays().get(0).getDailyTargetMl());

        // Previous 6 days must have baseline target (70 * 35 = 2450 ml)
        for (int i = 1; i < 7; i++) {
            assertEquals(2450, response.getDays().get(i).getDailyTargetMl(),
                    "Historical day " + i + " must use baseline target without fabricating weather");
        }
    }

    @Test
    @DisplayName("Today gracefully falls back to baseline target if weather service fails")
    void testGetWaterHistory_WeatherServiceFailureFallsBackToBaseline() {
        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(hydrationService.getRecommendation("user@example.com", "Bengaluru", null, null))
                .thenThrow(new RuntimeException("OpenWeatherMap service unreachable"));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", "Bengaluru", null, null);

        // Even though weather service failed, history succeeds and today uses baseline 2450 ml
        assertEquals(2450, response.getDays().get(0).getDailyTargetMl());
        assertEquals(2450, response.getDays().get(1).getDailyTargetMl());
    }

    @Test
    @DisplayName("User with missing weight defaults to minimum daily target guardrail (1500 ml)")
    void testGetWaterHistory_UserWithoutWeight_DefaultsToMinTarget() {
        sampleUser.setWeight(null);

        when(userRepository.findByEmailAndIsActiveTrue("user@example.com"))
                .thenReturn(Optional.of(sampleUser));

        when(waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(eq(sampleUser), any(), any()))
                .thenReturn(Collections.emptyList());

        WaterHistoryResponse response = waterService.getWaterHistory("user@example.com", null, null, null);

        for (DailyWaterHistoryDto day : response.getDays()) {
            assertEquals(1500, day.getDailyTargetMl(), "Missing weight must default to MIN_DAILY_TARGET_ML");
        }
    }
}
