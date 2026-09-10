package com.caresync.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hydration metrics and daily progress for today")
public class HydrationSummaryDto {

    @Schema(description = "Daily water intake target in milliliters", example = "2500")
    private Integer dailyTargetMl;

    @Schema(description = "Daily water intake target in litres", example = "2.50")
    private Double dailyTargetLitres;

    @Schema(description = "Total water consumed today in milliliters", example = "1500")
    private Integer consumedMl;

    @Schema(description = "Remaining water to consume today in milliliters (never negative)", example = "1000")
    private Integer remainingMl;

    @Schema(description = "Hydration progress percentage (uncapped, rounded to 2 decimal places)", example = "60.0")
    private Double progressPercentage;

    @Schema(description = "Indicates whether the hydration target incorporated live ambient weather", example = "true")
    private Boolean isWeatherAdjusted;
}
