package com.caresync.dto.water;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hydration metrics for a single calendar day")
public class DailyWaterHistoryDto {

    @Schema(description = "Calendar date (YYYY-MM-DD)", example = "2026-09-09")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "Target hydration volume for the day in milliliters", example = "2800")
    private Integer dailyTargetMl;

    @Schema(description = "Total volume of water consumed on this day in milliliters", example = "2100")
    private Integer consumedMl;

    @Schema(description = "Percentage of daily target achieved (uncapped, e.g. 75.0 or 120.0)", example = "75.0")
    private Double progressPercentage;
}
