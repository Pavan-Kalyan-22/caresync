package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.dashboard.DashboardResponse;
import com.caresync.service.DashboardService;
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
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Healthcare Dashboard", description = "Aggregated health and hydration overview APIs")
@SecurityRequirement(name = "Bearer Token")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(
            summary = "Get aggregated healthcare dashboard",
            description = "Retrieve an aggregated overview combining user summary, current ambient weather, " +
                    "recommended hydration target, today's consumed water, remaining water, progress percentage, " +
                    "and active wellness/environmental alerts. Requires authentication. " +
                    "Provide either a city name or latitude and longitude coordinates."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dashboard retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Missing or invalid location parameters, incomplete user profile, or underage user",
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
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @Parameter(description = "City name (e.g. 'Bengaluru' or 'Bengaluru,IN')", example = "Bengaluru")
            @RequestParam(required = false) String city,

            @Parameter(description = "Latitude (-90 to +90)", example = "12.9716")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Longitude (-180 to +180)", example = "77.5946")
            @RequestParam(required = false) Double lon,

            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("Received request for dashboard from user: {}, city: '{}', lat: {}, lon: {}",
                userEmail, city, lat, lon);

        DashboardResponse response = dashboardService.getDashboard(userEmail, city, lat, lon);
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", response));
    }
}
