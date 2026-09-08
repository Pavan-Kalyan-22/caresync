package com.caresync.service.impl;

import com.caresync.client.WeatherApiClient;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.exception.BadRequestException;
import com.caresync.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final int MAX_CITY_LENGTH = 100;

    private final WeatherApiClient weatherApiClient;

    @Override
    public WeatherResponse getCurrentWeather(String city, Double latitude, Double longitude) {
        // Validate coordinate combination
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;

        if (hasLatitude ^ hasLongitude) {
            log.warn("Incomplete coordinates supplied: lat={}, lon={}", latitude, longitude);
            throw new BadRequestException("Both latitude and longitude must be provided for coordinate-based weather lookup");
        }

        boolean hasCoordinates = hasLatitude && hasLongitude;
        boolean hasCity = city != null && !city.trim().isEmpty();

        if (!hasCoordinates && !hasCity) {
            log.warn("Weather request missing both city and coordinate parameters");
            throw new BadRequestException("Either city or coordinates (latitude and longitude) must be provided");
        }

        // Coordinates take precedence when both are provided
        if (hasCoordinates) {
            validateCoordinates(latitude, longitude);
            log.info("Processing weather request by coordinates: lat={}, lon={}", latitude, longitude);
            return weatherApiClient.fetchWeatherByCoordinates(latitude, longitude);
        }

        // City lookup
        String trimmedCity = city.trim();
        validateCity(trimmedCity);
        log.info("Processing weather request by city: {}", trimmedCity);
        return weatherApiClient.fetchWeatherByCity(trimmedCity);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            log.warn("Invalid latitude: {}", latitude);
            throw new BadRequestException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            log.warn("Invalid longitude: {}", longitude);
            throw new BadRequestException("Longitude must be between -180 and 180 degrees");
        }
    }

    private void validateCity(String city) {
        if (city.isEmpty()) {
            throw new BadRequestException("City name cannot be blank");
        }
        if (city.length() > MAX_CITY_LENGTH) {
            log.warn("City name exceeds maximum allowed length: {}", city.length());
            throw new BadRequestException("City name exceeds maximum allowed length of " + MAX_CITY_LENGTH + " characters");
        }
    }
}
