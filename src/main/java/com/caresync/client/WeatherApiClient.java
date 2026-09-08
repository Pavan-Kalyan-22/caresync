package com.caresync.client;

import com.caresync.dto.weather.WeatherResponse;

/**
 * Client abstraction layer for retrieving current weather information
 * from an external weather provider.
 */
public interface WeatherApiClient {

    /**
     * Retrieve current weather information for a specified city or location name.
     *
     * @param city the city name (e.g. "Bengaluru" or "Bengaluru,IN")
     * @return standardized WeatherResponse DTO
     */
    WeatherResponse fetchWeatherByCity(String city);

    /**
     * Retrieve current weather information for specified geographic coordinates.
     *
     * @param latitude  the latitude (-90 to +90)
     * @param longitude the longitude (-180 to +180)
     * @return standardized WeatherResponse DTO
     */
    WeatherResponse fetchWeatherByCoordinates(double latitude, double longitude);
}
