package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.water.WaterHistoryResponse;
import com.caresync.dto.water.WaterLogRequest;
import com.caresync.dto.water.WaterLogResponse;
import com.caresync.service.WaterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/water")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Water Management", description = "Water consumption logging and hydration history APIs")
@SecurityRequirement(name = "Bearer Token")
public class WaterController {

    private final WaterService waterService;

    @PostMapping("/log")
    @Operation(
            summary = "Log water intake",
            description = "Log a water consumption amount in milliliters for the authenticated user. " +
                    "Amount must be positive and between 1 ml and 5000 ml per log entry."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Water intake logged successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or amount out of bounds (1 - 5000 ml)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<WaterLogResponse>> logWater(
            @Valid @RequestBody WaterLogRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("Received request to log water: {} ml for user: {}", request.getAmountMl(), userEmail);

        WaterLogResponse response = waterService.logWaterIntake(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Water intake logged successfully", response));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Get 7-day water history",
            description = "Retrieve water consumption history for the past 7 calendar days (newest day first). " +
                    "For today, a weather-adjusted target is used if location is supplied; for previous days, " +
                    "the user's profile baseline target is used as past weather conditions are not stored."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Water history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<WaterHistoryResponse>> getHistory(
            @Parameter(description = "Optional city name for today's weather adjustment", example = "Bengaluru")
            @RequestParam(required = false) String city,

            @Parameter(description = "Optional latitude coordinate (-90 to +90)", example = "12.9716")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Optional longitude coordinate (-180 to +180)", example = "77.5946")
            @RequestParam(required = false) Double lon,

            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("Received request for water history from user: {}, city: {}, lat: {}, lon: {}",
                userEmail, city, lat, lon);

        WaterHistoryResponse response = waterService.getWaterHistory(userEmail, city, lat, lon);
        return ResponseEntity.ok(ApiResponse.success("Water history retrieved successfully", response));
    }
}
