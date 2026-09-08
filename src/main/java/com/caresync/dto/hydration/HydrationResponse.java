package com.caresync.dto.hydration;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "CareSync Personalized Daily Hydration Recommendation")
public class HydrationResponse {

    @Schema(description = "Recommended daily water intake target in milliliters", example = "2950")
    private Integer dailyWaterTargetMl;

    @Schema(description = "Recommended daily water intake target in litres", example = "2.95")
    private Double dailyWaterTargetLitres;

    @Schema(description = "Base water requirement calculated from body weight in milliliters", example = "2450")
    private Integer baseRequirementMl;

    @Schema(description = "Hydration adjustment based on occupation/activity level in milliliters", example = "0")
    private Integer activityAdjustmentMl;

    @Schema(description = "Hydration adjustment based on ambient temperature in milliliters", example = "500")
    private Integer temperatureAdjustmentMl;

    @Schema(description = "Hydration adjustment based on ambient humidity in milliliters", example = "0")
    private Integer humidityAdjustmentMl;

    @Schema(description = "User body weight in kilograms used for calculation", example = "70.0")
    private Double weightKg;

    @Schema(description = "User occupation from profile", example = "Software Engineer")
    private String occupation;

    @Schema(description = "Activity level classified from occupation", example = "SEDENTARY")
    private String activityLevel;

    @Schema(description = "Ambient temperature in Celsius used for calculation", example = "31.5")
    private Double temperatureCelsius;

    @Schema(description = "Ambient humidity percentage used for calculation", example = "60")
    private Integer humidityPercent;

    @Schema(description = "Current weather condition", example = "Clouds")
    private String weatherCondition;

    @Schema(description = "Location name where weather was observed", example = "Bengaluru")
    private String location;

    @Schema(description = "Human-readable explanation of how the recommendation was derived",
            example = "Base requirement of 2450 ml adjusted for elevated ambient temperature of 31.5°C (+500 ml). Recommendation is an application wellness guide.")
    private String explanation;

    @Schema(description = "Timestamp when the recommendation was calculated", example = "2026-09-08T20:45:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;
}
