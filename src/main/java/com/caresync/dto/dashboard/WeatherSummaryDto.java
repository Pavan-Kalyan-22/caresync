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
@Schema(description = "Current ambient weather summary")
public class WeatherSummaryDto {

    @Schema(description = "Location or city name", example = "Bengaluru")
    private String location;

    @Schema(description = "Current temperature in Celsius", example = "32.0")
    private Double temperature;

    @Schema(description = "Relative humidity percentage", example = "65")
    private Integer humidity;

    @Schema(description = "Primary weather condition", example = "Clear")
    private String weatherCondition;
}
