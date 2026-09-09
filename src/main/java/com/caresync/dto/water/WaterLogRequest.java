package com.caresync.dto.water;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for logging water consumption")
public class WaterLogRequest {

    @NotNull(message = "Water intake amount is required")
    @Min(value = 1, message = "Water intake amount must be at least 1 ml")
    @Max(value = 5000, message = "Water intake amount cannot exceed 5000 ml per log entry")
    @Schema(description = "Water intake volume in milliliters", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer amountMl;
}
