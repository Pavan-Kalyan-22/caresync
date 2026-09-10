package com.caresync.service.impl;

import com.caresync.dto.alert.AlertsResponse;
import com.caresync.dto.dashboard.DashboardAlertDto;
import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.entity.User;
import com.caresync.entity.WaterIntake;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.repository.UserRepository;
import com.caresync.repository.WaterIntakeRepository;
import com.caresync.service.AlertsService;
import com.caresync.service.HydrationService;
import com.caresync.service.alert.AlertRuleEngine;
import com.caresync.service.hydration.HydrationCalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertsServiceImpl implements AlertsService {

    private final UserRepository userRepository;
    private final HydrationService hydrationService;
    private final HydrationCalculationEngine calculationEngine;
    private final AlertRuleEngine alertRuleEngine;
    private final WaterIntakeRepository waterIntakeRepository;

    @Setter
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional(readOnly = true)
    public AlertsResponse getAlerts(String userEmail, String city, Double latitude, Double longitude) {
        log.info("Evaluating hydration alerts for user: {}, city: {}, lat: {}, lon: {}",
                userEmail, city, latitude, longitude);

        // 1. Validate coordinate symmetry if provided
        validateLocationParameters(city, latitude, longitude);

        // 2. Load authenticated active user
        User user = userRepository.findByEmailAndIsActiveTrue(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // 3. Determine target and ambient temperature (weather-adjusted if location available and provider healthy)
        int targetMl;
        Double temperature = null;

        boolean hasCity = city != null && !city.trim().isEmpty();
        boolean hasCoordinates = latitude != null && longitude != null;

        if (hasCity || hasCoordinates) {
            try {
                HydrationResponse hydration = hydrationService.getRecommendation(userEmail, city, latitude, longitude);
                if (hydration != null && hydration.getDailyWaterTargetMl() != null) {
                    targetMl = hydration.getDailyWaterTargetMl();
                    temperature = hydration.getTemperatureCelsius();
                } else {
                    targetMl = calculationEngine.calculateBaselineTarget(user);
                }
            } catch (BadRequestException | ResourceNotFoundException ex) {
                // Propagate client validation / user profile errors directly
                throw ex;
            } catch (Exception ex) {
                log.warn("External weather provider failed while determining alerts for user: {}. Falling back to baseline target: {}",
                        userEmail, ex.getMessage());
                targetMl = calculationEngine.calculateBaselineTarget(user);
                temperature = null;
            }
        } else {
            // Location omitted: calculate baseline target without weather
            targetMl = calculationEngine.calculateBaselineTarget(user);
            temperature = null;
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

        // 5. Evaluate active hydration alerts via AlertRuleEngine
        List<DashboardAlertDto> alerts = alertRuleEngine.evaluateHydrationAlerts(temperature, consumedMl, targetMl);

        log.info("Evaluated {} hydration alerts for user: {}", alerts.size(), userEmail);

        return AlertsResponse.builder()
                .alerts(alerts)
                .count(alerts.size())
                .build();
    }

    private void validateLocationParameters(String city, Double latitude, Double longitude) {
        boolean hasLat = latitude != null;
        boolean hasLon = longitude != null;

        if (hasLat ^ hasLon) {
            throw new BadRequestException("Both latitude and longitude must be provided for coordinate-based alerts lookup");
        }
    }
}
