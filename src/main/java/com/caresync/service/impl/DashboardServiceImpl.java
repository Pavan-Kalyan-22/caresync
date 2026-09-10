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
import com.caresync.service.alert.AlertRuleEngine;
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
    private final AlertRuleEngine alertRuleEngine;
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

            // Evaluate environmental weather alerts via AlertRuleEngine
            alerts.addAll(alertRuleEngine.evaluateDashboardWeatherAlerts(hydration.getTemperatureCelsius(), hydration.getHumidityPercent()));

        } catch (BadRequestException | ResourceNotFoundException ex) {
            // Re-throw client validation / user errors directly
            throw ex;
        } catch (Exception ex) {
            log.warn("External weather provider failed while generating dashboard for user: {}. Falling back to baseline target: {}",
                    userEmail, ex.getMessage());

            targetMl = calculationEngine.calculateBaselineTarget(user);
            targetLitres = BigDecimal.valueOf(targetMl).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).doubleValue();
            isWeatherAdjusted = false;
            weatherSummary = null;

            alerts.add(alertRuleEngine.buildWeatherUnavailableAlert());
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

        // 7. Evaluate hydration intake alerts via AlertRuleEngine
        alerts.addAll(alertRuleEngine.evaluateDashboardHydrationAlerts(consumedMl, targetMl));

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
}
