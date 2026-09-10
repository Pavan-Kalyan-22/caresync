package com.caresync.service.impl;

import com.caresync.dto.dashboard.*;
import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.entity.User;
import com.caresync.entity.WaterIntake;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.repository.UserRepository;
import com.caresync.repository.WaterIntakeRepository;
import com.caresync.service.DashboardService;
import com.caresync.service.HydrationService;
import com.caresync.service.hydration.HydrationCalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final HydrationService hydrationService;
    private final HydrationCalculationEngine calculationEngine;
    private final WaterIntakeRepository waterIntakeRepository;

    @Setter
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String userEmail, String city, Double latitude, Double longitude) {
        log.info("Generating dashboard for user: {}, city: {}, lat: {}, lon: {}",
                userEmail, city, latitude, longitude);

        // 1. Validate location parameters (Dashboard requires current weather)
        validateLocationParameters(city, latitude, longitude);

        // 2. Load authenticated active user
        User user = userRepository.findByEmailAndIsActiveTrue(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // 3. Obtain recommended hydration target & current weather (with graceful fallback)
        WeatherSummaryDto weatherSummary = null;
        int targetMl;
        double targetLitres;
        boolean isWeatherAdjusted;
        List<DashboardAlertDto> alerts = new ArrayList<>();

        try {
            HydrationResponse hydration = hydrationService.getRecommendation(userEmail, city, latitude, longitude);
            targetMl = hydration.getDailyWaterTargetMl();
            targetLitres = hydration.getDailyWaterTargetLitres() != null
                    ? hydration.getDailyWaterTargetLitres()
                    : BigDecimal.valueOf(targetMl).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).doubleValue();
            isWeatherAdjusted = true;

            weatherSummary = WeatherSummaryDto.builder()
                    .location(hydration.getLocation())
                    .temperature(hydration.getTemperatureCelsius())
                    .humidity(hydration.getHumidityPercent())
                    .weatherCondition(hydration.getWeatherCondition())
                    .build();

            // Evaluate environmental weather alerts
            evaluateWeatherAlerts(hydration.getTemperatureCelsius(), hydration.getHumidityPercent(), alerts);

        } catch (BadRequestException | ResourceNotFoundException ex) {
            // Re-throw client validation / user errors directly
            throw ex;
        } catch (Exception ex) {
            log.warn("External weather provider failed while generating dashboard for user: {}. Falling back to baseline target: {}",
                    userEmail, ex.getMessage());

            targetMl = calculateBaselineTarget(user);
            targetLitres = BigDecimal.valueOf(targetMl).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).doubleValue();
            isWeatherAdjusted = false;
            weatherSummary = null;

            alerts.add(DashboardAlertDto.builder()
                    .type("WEATHER_UNAVAILABLE")
                    .severity("INFO")
                    .message("Current weather is unavailable. Showing your baseline hydration target.")
                    .build());
        }

        // 4. Calculate today's water consumption for the authenticated user
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<WaterIntake> todayRecords = waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(
                user, startOfDay, endOfDay);

        int consumedMl = todayRecords.stream()
                .mapToInt(WaterIntake::getAmountMl)
                .sum();

        // 5. Calculate remaining water (never negative)
        int remainingMl = Math.max(targetMl - consumedMl, 0);

        // 6. Calculate progress percentage (uncapped, rounded to 2 decimal places)
        double progressPercentage = 0.0;
        if (targetMl > 0 && consumedMl > 0) {
            progressPercentage = BigDecimal.valueOf(((double) consumedMl / targetMl) * 100.0)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 7. Evaluate hydration intake alerts
        evaluateHydrationAlerts(consumedMl, targetMl, alerts);

        // 8. Assemble composite dashboard response
        UserSummaryDto userSummary = UserSummaryDto.builder()
                .name(user.getFullName())
                .build();

        HydrationSummaryDto hydrationSummary = HydrationSummaryDto.builder()
                .dailyTargetMl(targetMl)
                .dailyTargetLitres(targetLitres)
                .consumedMl(consumedMl)
                .remainingMl(remainingMl)
                .progressPercentage(progressPercentage)
                .isWeatherAdjusted(isWeatherAdjusted)
                .build();

        return DashboardResponse.builder()
                .user(userSummary)
                .weather(weatherSummary)
                .hydration(hydrationSummary)
                .alerts(alerts)
                .calculatedAt(LocalDateTime.now(clock))
                .build();
    }

    private void validateLocationParameters(String city, Double latitude, Double longitude) {
        boolean hasCity = city != null && !city.trim().isEmpty();
        boolean hasLat = latitude != null;
        boolean hasLon = longitude != null;

        if (hasLat ^ hasLon) {
            throw new BadRequestException("Both latitude and longitude must be provided for coordinate-based dashboard lookup");
        }

        boolean hasCoordinates = hasLat && hasLon;
        if (!hasCity && !hasCoordinates) {
            throw new BadRequestException("Either city or coordinates (latitude and longitude) must be provided for dashboard");
        }
    }

    private int calculateBaselineTarget(User user) {
        if (user.getWeight() == null || user.getWeight() <= 0) {
            return HydrationCalculationEngine.MIN_DAILY_TARGET_ML;
        }
        int baseRequirementMl = (int) Math.round(user.getWeight() * HydrationCalculationEngine.BASE_WATER_ML_PER_KG);
        HydrationCalculationEngine.ActivityClassification activity = calculationEngine.classifyOccupation(user.getOccupation());
        int rawTarget = baseRequirementMl + activity.getAdjustmentMl();
        return Math.max(HydrationCalculationEngine.MIN_DAILY_TARGET_ML,
                Math.min(HydrationCalculationEngine.MAX_DAILY_TARGET_ML, rawTarget));
    }

    private void evaluateWeatherAlerts(Double temperature, Integer humidity, List<DashboardAlertDto> alerts) {
        if (temperature == null) {
            return;
        }

        if (temperature >= 35.0) {
            alerts.add(DashboardAlertDto.builder()
                    .type("HIGH_TEMPERATURE")
                    .severity("WARNING")
                    .message(String.format("High ambient temperature of %.1f°C observed. Perspiration rate increases fluid loss; remember to drink water regularly.", temperature))
                    .build());
        } else if (temperature >= 30.0) {
            alerts.add(DashboardAlertDto.builder()
                    .type("WARM_TEMPERATURE")
                    .severity("INFO")
                    .message(String.format("Warm ambient temperature of %.1f°C observed. Target adjusted to compensate for heat.", temperature))
                    .build());
        }

        if (humidity != null && temperature >= 28.0 && humidity >= 70) {
            alerts.add(DashboardAlertDto.builder()
                    .type("HIGH_HUMIDITY")
                    .severity("WARNING")
                    .message(String.format("High humidity (%d%%) combined with warm temperature restricts sweat evaporation. Extra fluid intake recommended.", humidity))
                    .build());
        }
    }

    private void evaluateHydrationAlerts(int consumedMl, int targetMl, List<DashboardAlertDto> alerts) {
        if (consumedMl >= targetMl) {
            alerts.add(DashboardAlertDto.builder()
                    .type("HYDRATION_GOAL_REACHED")
                    .severity("INFO")
                    .message("Daily hydration goal achieved! Great job staying hydrated today.")
                    .build());
        } else if (consumedMl == 0) {
            alerts.add(DashboardAlertDto.builder()
                    .type("HYDRATION_REMINDER")
                    .severity("INFO")
                    .message("You have not logged any water yet today. Remember to stay hydrated.")
                    .build());
        }
    }
}
