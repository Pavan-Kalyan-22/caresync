package com.caresync.service;

import com.caresync.dto.weather.WeatherResponse;

public interface WeatherService {

    /**
     * Retrieve current weather for a city or coordinates.
     * If both city and coordinates are provided, coordinates take precedence.
     *
     * @param city      optional city name
     * @param latitude  optional latitude (-90 to +90)
     * @param longitude optional longitude (-180 to +180)
     * @return current WeatherResponse
     */
    WeatherResponse getCurrentWeather(String city, Double latitude, Double longitude);
}
