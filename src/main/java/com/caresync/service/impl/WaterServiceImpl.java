package com.caresync.service.impl;

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
import com.caresync.service.HydrationService;
import com.caresync.service.WaterService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaterServiceImpl implements WaterService {

    private final UserRepository userRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final HydrationService hydrationService;
    private final HydrationCalculationEngine calculationEngine;

    @Setter
    private Clock clock = Clock.systemDefaultZone();

    @Override
    @Transactional
    public WaterLogResponse logWaterIntake(String userEmail, WaterLogRequest request) {
        log.info("Logging water intake of {} ml for user: {}", request.getAmountMl(), userEmail);

        User user = userRepository.findByEmailAndIsActiveTrue(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        WaterIntake intake = WaterIntake.builder()
                .user(user)
                .amountMl(request.getAmountMl())
                .loggedAt(LocalDateTime.now(clock))
                .build();

        WaterIntake saved = waterIntakeRepository.save(intake);
        log.info("Water intake logged successfully with id: {} for user: {}", saved.getId(), userEmail);

        return WaterLogResponse.builder()
                .id(saved.getId())
                .amountMl(saved.getAmountMl())
                .loggedAt(saved.getLoggedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WaterHistoryResponse getWaterHistory(String userEmail, String city, Double latitude, Double longitude) {
        log.info("Fetching 7-day water history for user: {}, city: {}, lat: {}, lon: {}",
                userEmail, city, latitude, longitude);

        User user = userRepository.findByEmailAndIsActiveTrue(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.minusDays(6);

        // Half-open interval [startOfDay(today - 6), startOfDay(today + 1))
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();

        List<WaterIntake> records = waterIntakeRepository.findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(
                user, startDateTime, endDateTime);

        // Aggregate total consumed ml per calendar day
        Map<LocalDate, Integer> dailyConsumptionMap = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getLoggedAt().toLocalDate(),
                        Collectors.summingInt(WaterIntake::getAmountMl)
                ));

        // For today: calculate weather-adjusted target if location is provided; otherwise profile baseline
        int todayTargetMl = calculateTodayTarget(user, userEmail, city, latitude, longitude);

        // For previous 6 days: use profile baseline target (Weight * 35 ml/kg + Activity Adj, clamped 1500-4500 ml)
        // Architectural notice: Historical weather and past daily targets are not stored in CareSync.
        int historicalBaselineTargetMl = calculateBaselineTarget(user);

        // Build exactly 7 calendar days, newest day first
        List<DailyWaterHistoryDto> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            int consumed = dailyConsumptionMap.getOrDefault(date, 0);
            int target = (i == 0) ? todayTargetMl : historicalBaselineTargetMl;
            double progressPercentage = calculateProgressPercentage(consumed, target);

            days.add(DailyWaterHistoryDto.builder()
                    .date(date)
                    .dailyTargetMl(target)
                    .consumedMl(consumed)
                    .progressPercentage(progressPercentage)
                    .build());
        }

        return WaterHistoryResponse.builder()
                .days(days)
                .build();
    }

    private int calculateTodayTarget(User user, String userEmail, String city, Double latitude, Double longitude) {
        boolean hasCity = city != null && !city.trim().isEmpty();
        boolean hasCoordinates = latitude != null && longitude != null;

        if (hasCity || hasCoordinates) {
            try {
                HydrationResponse hydration = hydrationService.getRecommendation(userEmail, city, latitude, longitude);
                if (hydration != null && hydration.getDailyWaterTargetMl() != null) {
                    return hydration.getDailyWaterTargetMl();
                }
            } catch (Exception e) {
                log.warn("Could not retrieve live weather hydration target for today, using baseline: {}", e.getMessage());
            }
        }
        return calculateBaselineTarget(user);
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

    private double calculateProgressPercentage(int consumedMl, int dailyTargetMl) {
        if (dailyTargetMl <= 0 || consumedMl <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(((double) consumedMl / dailyTargetMl) * 100.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
