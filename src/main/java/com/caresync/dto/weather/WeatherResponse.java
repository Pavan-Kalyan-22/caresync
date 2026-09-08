package com.caresync.dto.weather;

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
@Schema(description = "CareSync Current Weather Information")
public class WeatherResponse {

    @Schema(description = "Location or city name", example = "Bengaluru")
    private String location;

    @Schema(description = "Two-letter country code", example = "IN")
    private String country;

    @Schema(description = "Geographic latitude", example = "12.9716")
    private Double latitude;

    @Schema(description = "Geographic longitude", example = "77.5946")
    private Double longitude;

    @Schema(description = "Current temperature in Celsius", example = "28.5")
    private Double temperature;

    @Schema(description = "Human perception of weather in Celsius", example = "29.8")
    private Double feelsLikeTemperature;

    @Schema(description = "Minimum temperature at the moment in Celsius", example = "26.0")
    private Double minimumTemperature;

    @Schema(description = "Maximum temperature at the moment in Celsius", example = "30.5")
    private Double maximumTemperature;

    @Schema(description = "Humidity percentage", example = "65")
    private Integer humidity;

    @Schema(description = "Primary weather condition group", example = "Clear")
    private String weatherCondition;

    @Schema(description = "Detailed weather description", example = "clear sky")
    private String weatherDescription;

    @Schema(description = "Wind speed in meters per second", example = "3.6")
    private Double windSpeed;

    @Schema(description = "Time of data observation", example = "2026-09-07T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime observedAt;
}
