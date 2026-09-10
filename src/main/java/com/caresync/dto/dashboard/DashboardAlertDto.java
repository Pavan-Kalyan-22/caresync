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
@Schema(description = "Structured alert for health and hydration conditions")
public class DashboardAlertDto {

    @Schema(description = "Alert classification type", example = "HIGH_TEMPERATURE")
    private String type;

    @Schema(description = "Alert severity level: INFO, WARNING", example = "WARNING")
    private String severity;

    @Schema(description = "Alert headline or short title", example = "High temperature")
    private String title;

    @Schema(description = "Human-readable descriptive alert message",
            example = "High ambient temperature of 36.0°C observed. Perspiration rate increases fluid loss; remember to drink water regularly.")
    private String message;
}
