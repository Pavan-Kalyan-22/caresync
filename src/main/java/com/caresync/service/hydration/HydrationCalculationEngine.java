package com.caresync.service.hydration;

import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.entity.User;
import com.caresync.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Smart Hydration Calculation Engine.
 *
 * <p><b>DISCLAIMER / PRODUCT NOTICE:</b>
 * This calculation is a configurable software product heuristic designed for general wellness
 * guidance. It is <b>NOT</b> a clinical medical diagnosis, healthcare prescription, or medical advice.
 *
 * <p><b>Formula Overview:</b>
 * <pre>
 *   Daily Water Target (ml) = Clamp(Base Requirement + Activity Adj + Temp Adj + Humidity Adj, Min 1500, Max 4500)
 * </pre>
 */
@Component
@Slf4j
public class HydrationCalculationEngine {

    // =========================================================================
    // Centralized Calculation Constants (Product Heuristic Values)
    // =========================================================================

    /** Base water requirement factor in milliliters per kilogram of body weight. */
    public static final double BASE_WATER_ML_PER_KG = 35.0;

    /** Activity adjustments in milliliters based on occupational physical demand. */
    public static final int ACTIVITY_SEDENTARY_ML = 0;
    public static final int ACTIVITY_LIGHT_ML = 300;
    public static final int ACTIVITY_MODERATE_ML = 600;
    public static final int ACTIVITY_VERY_ACTIVE_ML = 900;

    /** Temperature thresholds in degrees Celsius. */
    public static final double TEMP_THRESHOLD_WARM = 25.0;
    public static final double TEMP_THRESHOLD_HOT = 30.0;
    public static final double TEMP_THRESHOLD_EXTREME = 35.0;

    /** Temperature adjustments in milliliters. */
    public static final int TEMP_ADJUSTMENT_WARM_ML = 250;
    public static final int TEMP_ADJUSTMENT_HOT_ML = 500;
    public static final int TEMP_ADJUSTMENT_EXTREME_ML = 750;

    /** Humidity thresholds and adjustments. */
    public static final double HUMIDITY_HOT_TEMP_THRESHOLD = 28.0;
    public static final int HUMIDITY_HIGH_THRESHOLD = 70;
    public static final int HUMIDITY_ADJUSTMENT_HOT_HUMID_ML = 200;

    public static final double HUMIDITY_DRY_TEMP_THRESHOLD = 25.0;
    public static final int HUMIDITY_LOW_THRESHOLD = 30;
    public static final int HUMIDITY_ADJUSTMENT_DRY_HEAT_ML = 150;

    /** Application-level safety boundaries (clamping limits) in milliliters. */
    public static final int MIN_DAILY_TARGET_ML = 1500;
    public static final int MAX_DAILY_TARGET_ML = 4500;

    /** Validation bounds. */
    public static final double MIN_WEIGHT_KG = 20.0;
    public static final double MAX_WEIGHT_KG = 300.0;
    public static final double MIN_TEMP_CELSIUS = -50.0;
    public static final double MAX_TEMP_CELSIUS = 60.0;
    public static final int MIN_HUMIDITY_PERCENT = 0;
    public static final int MAX_HUMIDITY_PERCENT = 100;

    /**
     * Calculate a personalized daily hydration recommendation.
     *
     * @param user    the authenticated user profile (weight required)
     * @param weather the current weather data (temperature and humidity required)
     * @return a complete HydrationResponse breakdown
     */
    public HydrationResponse calculate(User user, WeatherResponse weather) {
        validateInputs(user, weather);

        double weight = user.getWeight();
        String occupation = user.getOccupation();
        Double temperature = weather.getTemperature();
        Integer humidity = weather.getHumidity();

        // 1. Base requirement from body weight
        int baseRequirementMl = (int) Math.round(weight * BASE_WATER_ML_PER_KG);

        // 2. Activity adjustment from occupation
        ActivityClassification activity = classifyOccupation(occupation);
        int activityAdjustmentMl = activity.getAdjustmentMl();

        // 3. Ambient temperature adjustment
        int temperatureAdjustmentMl = calculateTemperatureAdjustment(temperature);

        // 4. Ambient humidity adjustment
        int humidityAdjustmentMl = calculateHumidityAdjustment(temperature, humidity);

        // 5. Raw total before guardrails
        int rawTargetMl = baseRequirementMl + activityAdjustmentMl + temperatureAdjustmentMl + humidityAdjustmentMl;

        // 6. Application guardrail clamping
        int clampedTargetMl = Math.max(MIN_DAILY_TARGET_ML, Math.min(MAX_DAILY_TARGET_ML, rawTargetMl));

        // 7. Convert to litres (rounded to 2 decimal places)
        double targetLitres = BigDecimal.valueOf(clampedTargetMl)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP)
                .doubleValue();

        // 8. Generate transparent human-readable explanation
        String explanation = generateExplanation(
                baseRequirementMl, activity, activityAdjustmentMl,
                temperature, temperatureAdjustmentMl,
                humidity, humidityAdjustmentMl,
                rawTargetMl, clampedTargetMl
        );

        return HydrationResponse.builder()
                .dailyWaterTargetMl(clampedTargetMl)
                .dailyWaterTargetLitres(targetLitres)
                .baseRequirementMl(baseRequirementMl)
                .activityAdjustmentMl(activityAdjustmentMl)
                .temperatureAdjustmentMl(temperatureAdjustmentMl)
                .humidityAdjustmentMl(humidityAdjustmentMl)
                .weightKg(weight)
                .occupation(occupation)
                .activityLevel(activity.name())
                .temperatureCelsius(temperature)
                .humidityPercent(humidity)
                .weatherCondition(weather.getWeatherCondition())
                .location(weather.getLocation())
                .explanation(explanation)
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Validates that all necessary inputs for the calculation are present and within valid ranges.
     */
    private void validateInputs(User user, WeatherResponse weather) {
        if (user == null) {
            throw new BadRequestException("User profile must not be null");
        }
        if (user.getWeight() == null) {
            throw new BadRequestException("User profile is incomplete: weight is required for hydration calculation. Please update your profile.");
        }
        if (user.getWeight() < MIN_WEIGHT_KG || user.getWeight() > MAX_WEIGHT_KG) {
            throw new BadRequestException(String.format("Weight must be between %.1f kg and %.1f kg", MIN_WEIGHT_KG, MAX_WEIGHT_KG));
        }

        if (weather == null) {
            throw new BadRequestException("Weather information must not be null");
        }
        if (weather.getTemperature() == null) {
            throw new BadRequestException("Weather temperature is missing");
        }
        if (weather.getTemperature() < MIN_TEMP_CELSIUS || weather.getTemperature() > MAX_TEMP_CELSIUS) {
            throw new BadRequestException(String.format("Weather temperature must be between %.1f°C and %.1f°C", MIN_TEMP_CELSIUS, MAX_TEMP_CELSIUS));
        }
        if (weather.getHumidity() == null) {
            throw new BadRequestException("Weather humidity is missing");
        }
        if (weather.getHumidity() < MIN_HUMIDITY_PERCENT || weather.getHumidity() > MAX_HUMIDITY_PERCENT) {
            throw new BadRequestException(String.format("Weather humidity must be between %d%% and %d%%", MIN_HUMIDITY_PERCENT, MAX_HUMIDITY_PERCENT));
        }
    }

    /**
     * Classifies a user's occupation string into an activity level.
     */
    public ActivityClassification classifyOccupation(String occupation) {
        if (occupation == null || occupation.trim().isEmpty()) {
            return ActivityClassification.SEDENTARY;
        }

        String normalized = occupation.toLowerCase();

        // Tier 4: Very Active (+900 ml)
        if (containsAny(normalized, "construction worker", "athlete", "military", "miner", "manual laborer")) {
            return ActivityClassification.VERY_ACTIVE;
        }

        // Tier 3: Moderately Active (+600 ml)
        if (containsAny(normalized, "field worker", "delivery", "farmer", "factory worker", "police", "fitness trainer")) {
            return ActivityClassification.MODERATELY_ACTIVE;
        }

        // Tier 2: Lightly Active (+300 ml)
        if (containsAny(normalized, "teacher", "doctor", "nurse", "retail", "sales", "chef", "pharmacist")) {
            return ActivityClassification.LIGHTLY_ACTIVE;
        }

        // Tier 1: Sedentary (+0 ml) - explicitly represented approved occupations
        if (containsAny(normalized, "desk jobs", "software engineer", "developer", "student", "accountant",
                "driver", "office worker", "manager", "clerk")) {
            return ActivityClassification.SEDENTARY;
        }

        // Default for unrecognized occupations
        return ActivityClassification.SEDENTARY;
    }

    /**
     * Determines temperature adjustment in milliliters based on ambient Celsius.
     */
    public int calculateTemperatureAdjustment(double temperature) {
        if (temperature >= TEMP_THRESHOLD_EXTREME) {
            return TEMP_ADJUSTMENT_EXTREME_ML;
        } else if (temperature >= TEMP_THRESHOLD_HOT) {
            return TEMP_ADJUSTMENT_HOT_ML;
        } else if (temperature >= TEMP_THRESHOLD_WARM) {
            return TEMP_ADJUSTMENT_WARM_ML;
        }
        return 0;
    }

    /**
     * Determines humidity adjustment in milliliters based on temperature and relative humidity.
     */
    public int calculateHumidityAdjustment(double temperature, int humidity) {
        if (temperature >= HUMIDITY_HOT_TEMP_THRESHOLD && humidity >= HUMIDITY_HIGH_THRESHOLD) {
            return HUMIDITY_ADJUSTMENT_HOT_HUMID_ML;
        }
        if (temperature >= HUMIDITY_DRY_TEMP_THRESHOLD && humidity <= HUMIDITY_LOW_THRESHOLD) {
            return HUMIDITY_ADJUSTMENT_DRY_HEAT_ML;
        }
        return 0;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String generateExplanation(int baseRequirementMl, ActivityClassification activity, int activityAdj,
                                        double temp, int tempAdj,
                                        int humidity, int humidityAdj,
                                        int rawTarget, int clampedTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Base requirement of %d ml (35 ml/kg)", baseRequirementMl));

        if (activityAdj > 0) {
            sb.append(String.format(", adjusted for %s activity level (+%d ml)", activity.name(), activityAdj));
        }

        if (tempAdj > 0) {
            sb.append(String.format(", elevated ambient temperature of %.1f°C (+%d ml)", temp, tempAdj));
        }

        if (humidityAdj > 0) {
            if (humidity >= HUMIDITY_HIGH_THRESHOLD) {
                sb.append(String.format(", and high humidity of %d%% (+%d ml)", humidity, humidityAdj));
            } else {
                sb.append(String.format(", and dry conditions of %d%% humidity (+%d ml)", humidity, humidityAdj));
            }
        }

        if (rawTarget < MIN_DAILY_TARGET_ML) {
            sb.append(String.format(". Target clamped to application minimum limit of %d ml.", MIN_DAILY_TARGET_ML));
        } else if (rawTarget > MAX_DAILY_TARGET_ML) {
            sb.append(String.format(". Target clamped to application maximum limit of %d ml.", MAX_DAILY_TARGET_ML));
        } else {
            sb.append(". Recommendation is an application wellness guide.");
        }

        return sb.toString();
    }

    /**
     * Calculates the baseline daily hydration target for a user profile
     * based purely on body weight and occupational physical demand,
     * clamped to application safety boundaries [1500, 4500] ml.
     * Used when live weather is unavailable or omitted.
     *
     * @param user the user profile
     * @return baseline daily water target in ml
     */
    public int calculateBaselineTarget(User user) {
        if (user == null || user.getWeight() == null || user.getWeight() <= 0) {
            return MIN_DAILY_TARGET_ML;
        }
        int baseRequirementMl = (int) Math.round(user.getWeight() * BASE_WATER_ML_PER_KG);
        ActivityClassification activity = classifyOccupation(user.getOccupation());
        int rawTarget = baseRequirementMl + activity.getAdjustmentMl();
        return Math.max(MIN_DAILY_TARGET_ML, Math.min(MAX_DAILY_TARGET_ML, rawTarget));
    }

    public enum ActivityClassification {
        SEDENTARY(ACTIVITY_SEDENTARY_ML),
        LIGHTLY_ACTIVE(ACTIVITY_LIGHT_ML),
        MODERATELY_ACTIVE(ACTIVITY_MODERATE_ML),
        VERY_ACTIVE(ACTIVITY_VERY_ACTIVE_ML);

        private final int adjustmentMl;

        ActivityClassification(int adjustmentMl) {
            this.adjustmentMl = adjustmentMl;
        }

        public int getAdjustmentMl() {
            return adjustmentMl;
        }
    }
}
