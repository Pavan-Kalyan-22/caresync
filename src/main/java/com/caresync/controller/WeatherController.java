package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Weather", description = "Current weather APIs for personalized health and wellness")
@SecurityRequirement(name = "Bearer Token")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/current")
    @Operation(
            summary = "Get current weather",
            description = "Retrieve current weather conditions for a specified city or geographic coordinates (latitude and longitude). " +
                    "Requires authentication. If both city and coordinates are provided, coordinates take precedence."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Current weather retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or missing location parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Location not found by weather provider",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "External weather service unavailable",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<WeatherResponse>> getCurrentWeather(
            @Parameter(description = "City name (e.g. 'Bengaluru' or 'Bengaluru,IN')", example = "Bengaluru")
            @RequestParam(required = false) String city,

            @Parameter(description = "Latitude (-90 to +90)", example = "12.9716")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Longitude (-180 to +180)", example = "77.5946")
            @RequestParam(required = false) Double lon
    ) {
        log.info("Received request for current weather: city='{}', lat={}, lon={}", city, lat, lon);
        WeatherResponse weatherResponse = weatherService.getCurrentWeather(city, lat, lon);
        return ResponseEntity.ok(ApiResponse.success("Current weather retrieved successfully", weatherResponse));
    }
}
