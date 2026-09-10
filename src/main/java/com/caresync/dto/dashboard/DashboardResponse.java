package com.caresync.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated Healthcare Dashboard response")
public class DashboardResponse {

    @Schema(description = "User profile summary")
    private UserSummaryDto user;

    @Schema(description = "Current ambient weather summary (null if weather provider is unavailable)")
    private WeatherSummaryDto weather;

    @Schema(description = "Hydration metrics and daily progress for today")
    private HydrationSummaryDto hydration;

    @Schema(description = "Active wellness and environmental alerts")
    private List<DashboardAlertDto> alerts;

    @Schema(description = "Timestamp when the dashboard was calculated", example = "2026-09-10T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime calculatedAt;
}
