package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.alert.AlertsResponse;
import com.caresync.service.AlertsService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alerts & Reminders", description = "Hydration alert and reminder APIs")
@SecurityRequirement(name = "Bearer Token")
public class AlertsController {

    private final AlertsService alertsService;

    @GetMapping
    @Operation(
            summary = "Get active hydration alerts and reminders",
            description = "Retrieve current hydration-related alerts dynamically evaluated for the authenticated user. " +
                    "Evaluates hydration progress, reminder on zero intake, and high ambient temperature warnings. " +
                    "Requires JWT authentication. Location parameters (city or lat/lon) are optional."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Alerts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid coordinate parameters or incomplete profile",
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
    public ResponseEntity<ApiResponse<AlertsResponse>> getAlerts(
            @Parameter(description = "Optional city name for current weather evaluation (e.g. 'Bengaluru')", example = "Bengaluru")
            @RequestParam(required = false) String city,

            @Parameter(description = "Optional latitude coordinate (-90 to +90)", example = "12.9716")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Optional longitude coordinate (-180 to +180)", example = "77.5946")
            @RequestParam(required = false) Double lon,

            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("Received request for alerts from user: {}, city: '{}', lat: {}, lon: {}",
                userEmail, city, lat, lon);

        AlertsResponse response = alertsService.getAlerts(userEmail, city, lat, lon);
        return ResponseEntity.ok(ApiResponse.success("Alerts retrieved successfully", response));
    }
}
