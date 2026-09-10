package com.caresync.service.alert;

import com.caresync.dto.dashboard.DashboardAlertDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable Rule Engine for evaluating and generating health and hydration alerts.
 * Serves as the single source of truth for alert thresholds and evaluation logic across CareSync
 * (Task 6 Healthcare Dashboard & Task 7 Alerts / Reminders).
 */
@Component
public class AlertRuleEngine {

    // Threshold constants
    public static final double HIGH_TEMP_THRESHOLD_CELSIUS = 35.0;
    public static final double WARM_TEMP_THRESHOLD_CELSIUS = 30.0;
    public static final double HIGH_HUMIDITY_TEMP_THRESHOLD_CELSIUS = 28.0;
    public static final int HIGH_HUMIDITY_PERCENT_THRESHOLD = 70;

    // Severity constants
    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_WARNING = "WARNING";

    // Alert Types
    public static final String TYPE_HIGH_TEMPERATURE = "HIGH_TEMPERATURE";
    public static final String TYPE_WARM_TEMPERATURE = "WARM_TEMPERATURE";
    public static final String TYPE_HIGH_HUMIDITY = "HIGH_HUMIDITY";
    public static final String TYPE_HYDRATION_GOAL_REACHED = "HYDRATION_GOAL_REACHED";
    public static final String TYPE_HYDRATION_REMINDER = "HYDRATION_REMINDER";
    public static final String TYPE_WEATHER_UNAVAILABLE = "WEATHER_UNAVAILABLE";

    /**
     * Determines whether the ambient temperature qualifies as high temperature (>= 35°C).
     */
    public boolean isHighTemperature(Double temperature) {
        return temperature != null && temperature >= HIGH_TEMP_THRESHOLD_CELSIUS;
    }

    /**
     * Determines whether the ambient temperature qualifies as warm temperature (30°C to < 35°C).
     */
    public boolean isWarmTemperature(Double temperature) {
        return temperature != null && temperature >= WARM_TEMP_THRESHOLD_CELSIUS && temperature < HIGH_TEMP_THRESHOLD_CELSIUS;
    }

    /**
     * Determines whether the conditions qualify as high humidity (temp >= 28°C and humidity >= 70%).
     */
    public boolean isHighHumidity(Double temperature, Integer humidity) {
        return temperature != null && humidity != null
                && temperature >= HIGH_HUMIDITY_TEMP_THRESHOLD_CELSIUS
                && humidity >= HIGH_HUMIDITY_PERCENT_THRESHOLD;
    }

    /**
     * Determines whether daily water consumption has reached or exceeded the target.
     */
    public boolean isHydrationGoalReached(int consumedMl, int targetMl) {
        return targetMl > 0 && consumedMl >= targetMl;
    }

    /**
     * Determines whether zero water has been logged today.
     */
    public boolean isHydrationReminder(int consumedMl) {
        return consumedMl == 0;
    }

    /**
     * Evaluates active alerts specifically for Task 7 (Alerts & Reminders API).
     * Supports: HIGH_TEMPERATURE, HYDRATION_GOAL_REACHED, and HYDRATION_REMINDER.
     *
     * @param temperature current temperature in Celsius (null if weather unavailable)
     * @param consumedMl  total water consumed today in ml
     * @param targetMl    daily hydration target in ml
     * @return list of active hydration alerts
     */
    public List<DashboardAlertDto> evaluateHydrationAlerts(Double temperature, int consumedMl, int targetMl) {
        List<DashboardAlertDto> alerts = new ArrayList<>();

        if (isHighTemperature(temperature)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HIGH_TEMPERATURE)
                    .severity(SEVERITY_WARNING)
                    .title("High temperature")
                    .message("Stay hydrated today.")
                    .build());
        }

        if (isHydrationGoalReached(consumedMl, targetMl)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HYDRATION_GOAL_REACHED)
                    .severity(SEVERITY_INFO)
                    .title("Daily goal achieved")
                    .message("You have reached your daily hydration goal.")
                    .build());
        } else if (isHydrationReminder(consumedMl)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HYDRATION_REMINDER)
                    .severity(SEVERITY_INFO)
                    .title("Hydration reminder")
                    .message("You have not logged any water intake today.")
                    .build());
        }

        return alerts;
    }

    /**
     * Evaluates ambient weather alerts for Task 6 (Healthcare Dashboard).
     *
     * @param temperature current temperature in Celsius
     * @param humidity    current relative humidity percentage
     * @return list of active dashboard weather alerts
     */
    public List<DashboardAlertDto> evaluateDashboardWeatherAlerts(Double temperature, Integer humidity) {
        List<DashboardAlertDto> alerts = new ArrayList<>();
        if (temperature == null) {
            return alerts;
        }

        if (isHighTemperature(temperature)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HIGH_TEMPERATURE)
                    .severity(SEVERITY_WARNING)
                    .title("High temperature")
                    .message(String.format("High ambient temperature of %.1f°C observed. Perspiration rate increases fluid loss; remember to drink water regularly.", temperature))
                    .build());
        } else if (isWarmTemperature(temperature)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_WARM_TEMPERATURE)
                    .severity(SEVERITY_INFO)
                    .title("Warm temperature")
                    .message(String.format("Warm ambient temperature of %.1f°C observed. Target adjusted to compensate for heat.", temperature))
                    .build());
        }

        if (isHighHumidity(temperature, humidity)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HIGH_HUMIDITY)
                    .severity(SEVERITY_WARNING)
                    .title("High humidity")
                    .message(String.format("High humidity (%d%%) combined with warm temperature restricts sweat evaporation. Extra fluid intake recommended.", humidity))
                    .build());
        }

        return alerts;
    }

    /**
     * Evaluates hydration alerts for Task 6 (Healthcare Dashboard).
     *
     * @param consumedMl total water consumed today in ml
     * @param targetMl   daily hydration target in ml
     * @return list of active dashboard hydration alerts
     */
    public List<DashboardAlertDto> evaluateDashboardHydrationAlerts(int consumedMl, int targetMl) {
        List<DashboardAlertDto> alerts = new ArrayList<>();

        if (isHydrationGoalReached(consumedMl, targetMl)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HYDRATION_GOAL_REACHED)
                    .severity(SEVERITY_INFO)
                    .title("Daily goal achieved")
                    .message("Daily hydration goal achieved! Great job staying hydrated today.")
                    .build());
        } else if (isHydrationReminder(consumedMl)) {
            alerts.add(DashboardAlertDto.builder()
                    .type(TYPE_HYDRATION_REMINDER)
                    .severity(SEVERITY_INFO)
                    .title("Hydration reminder")
                    .message("You have not logged any water yet today. Remember to stay hydrated.")
                    .build());
        }

        return alerts;
    }

    /**
     * Builds the standard weather unavailable alert for Task 6 (Healthcare Dashboard).
     */
    public DashboardAlertDto buildWeatherUnavailableAlert() {
        return DashboardAlertDto.builder()
                .type(TYPE_WEATHER_UNAVAILABLE)
                .severity(SEVERITY_INFO)
                .title("Weather unavailable")
                .message("Current weather is unavailable. Showing your baseline hydration target.")
                .build();
    }
}
