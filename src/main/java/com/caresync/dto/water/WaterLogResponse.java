package com.caresync.dto.water;

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
@Schema(description = "Response confirming a water consumption log entry")
public class WaterLogResponse {

    @Schema(description = "Unique ID of the water intake log entry", example = "1")
    private Long id;

    @Schema(description = "Volume of water consumed in milliliters", example = "500")
    private Integer amountMl;

    @Schema(description = "Timestamp when the intake was logged", example = "2026-09-09T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime loggedAt;
}
