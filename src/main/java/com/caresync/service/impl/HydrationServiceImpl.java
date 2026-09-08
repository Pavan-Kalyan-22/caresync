package com.caresync.service.impl;

import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.entity.User;
import com.caresync.exception.BadRequestException;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.repository.UserRepository;
import com.caresync.service.HydrationService;
import com.caresync.service.WeatherService;
import com.caresync.service.hydration.HydrationCalculationEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HydrationServiceImpl implements HydrationService {

    private static final int MIN_ADULT_AGE = 18;
    private static final int MAX_VALID_AGE = 120;

    private final UserRepository userRepository;
    private final WeatherService weatherService;
    private final HydrationCalculationEngine calculationEngine;

    @Override
    public HydrationResponse getRecommendation(String userEmail, String city, Double latitude, Double longitude) {
        log.info("Calculating hydration recommendation for user: {}, city: {}, lat: {}, lon: {}",
                userEmail, city, latitude, longitude);

        // 1. Strict location parameter validation (no country fallback)
        validateLocationParameters(city, latitude, longitude);

        // 2. Load authenticated active user
        User user = userRepository.findByEmailAndIsActiveTrue(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // 3. Validate user profile attributes
        validateUserProfile(user);

        // 4. Retrieve live weather data using existing Weather module
        WeatherResponse weather = weatherService.getCurrentWeather(city, latitude, longitude);

        // 5. Execute deterministic hydration calculation engine
        HydrationResponse response = calculationEngine.calculate(user, weather);

        log.info("Hydration recommendation calculated successfully for user: {}. Target: {} ml ({} L)",
                userEmail, response.getDailyWaterTargetMl(), response.getDailyWaterTargetLitres());

        return response;
    }

    private void validateLocationParameters(String city, Double latitude, Double longitude) {
        boolean hasCity = city != null && !city.trim().isEmpty();
        boolean hasLat = latitude != null;
        boolean hasLon = longitude != null;

        if (hasLat ^ hasLon) {
            throw new BadRequestException("Both latitude and longitude must be provided for coordinate-based hydration calculation");
        }

        boolean hasCoordinates = hasLat && hasLon;

        if (!hasCity && !hasCoordinates) {
            throw new BadRequestException("Either city or coordinates (latitude and longitude) must be provided for hydration calculation");
        }
    }

    private void validateUserProfile(User user) {
        if (user.getWeight() == null) {
            throw new BadRequestException("User profile is incomplete: weight is required for hydration calculation. Please update your profile.");
        }

        if (user.getAge() != null) {
            if (user.getAge() < MIN_ADULT_AGE) {
                throw new BadRequestException("Hydration recommendation is currently tailored for adult users (18+)");
            }
            if (user.getAge() > MAX_VALID_AGE) {
                throw new BadRequestException("Invalid age in user profile");
            }
        }
    }
}
