package com.caresync.controller;

import com.caresync.dto.ApiResponse;
import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.service.HydrationService;
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
@RequestMapping("/api/v1/hydration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Hydration", description = "Smart Hydration Engine APIs for personalized wellness")
@SecurityRequirement(name = "Bearer Token")
public class HydrationController {

    private final HydrationService hydrationService;

    @GetMapping("/recommendation")
    @Operation(
            summary = "Get personalized hydration recommendation",
            description = "Calculate a personalized daily water intake recommendation for the authenticated user based on " +
                    "their profile (weight, occupation) and current ambient weather (temperature, humidity). " +
                    "Requires authentication. Provide either a city name or latitude and longitude coordinates. " +
                    "Notice: This is a software product wellness heuristic and not a medical prescription."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Hydration recommendation calculated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input, missing location, incomplete user profile (e.g. missing weight), or underage user",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Valid JWT token required",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User or location not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "External weather service unavailable",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<HydrationResponse>> getRecommendation(
            @Parameter(description = "City name (e.g. 'Bengaluru' or 'Bengaluru,IN')", example = "Bengaluru")
            @RequestParam(required = false) String city,

            @Parameter(description = "Latitude (-90 to +90)", example = "12.9716")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Longitude (-180 to +180)", example = "77.5946")
            @RequestParam(required = false) Double lon,

            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        log.info("Received request for hydration recommendation from user: {}, city: '{}', lat: {}, lon: {}",
                userEmail, city, lat, lon);

        HydrationResponse response = hydrationService.getRecommendation(userEmail, city, lat, lon);
        return ResponseEntity.ok(ApiResponse.success("Hydration recommendation calculated successfully", response));
    }
}
