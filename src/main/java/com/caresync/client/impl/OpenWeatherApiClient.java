package com.caresync.client.impl;

import com.caresync.client.WeatherApiClient;
import com.caresync.client.dto.OpenWeatherResponse;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.exception.WeatherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenWeatherApiClient implements WeatherApiClient {

    private final RestClient weatherRestClient;

    @Value("${weather.api-key:}")
    private String apiKey;

    @Value("${weather.base-url:https://api.openweathermap.org/data/2.5}")
    private String baseUrl;

    @Override
    public WeatherResponse fetchWeatherByCity(String city) {
        log.info("Fetching current weather from OpenWeatherMap for city: {}", city);
        validateApiKeyConfigured();

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/weather")
                .queryParam("q", city)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .build()
                .toUri();

        return executeWeatherRequest(uri, city);
    }

    @Override
    public WeatherResponse fetchWeatherByCoordinates(double latitude, double longitude) {
        log.info("Fetching current weather from OpenWeatherMap for coordinates: lat={}, lon={}", latitude, longitude);
        validateApiKeyConfigured();

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/weather")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .build()
                .toUri();

        return executeWeatherRequest(uri, String.format("(%.4f, %.4f)", latitude, longitude));
    }

    private void validateApiKeyConfigured() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Weather API key is not configured");
            throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Weather service is currently unavailable. API key is not configured.");
        }
    }

    private WeatherResponse executeWeatherRequest(URI uri, String locationIdentifier) {
        try {
            OpenWeatherResponse response = weatherRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        log.warn("Location not found in weather provider: {}", locationIdentifier);
                        throw new ResourceNotFoundException("Weather data not found for location: " + locationIdentifier);
                    })
                    .onStatus(status -> status.value() == 401 || status.value() == 403, (req, resp) -> {
                        log.error("External weather provider authentication failed with status: {}", resp.getStatusCode());
                        throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Weather service is currently unavailable. Please try again later.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (req, resp) -> {
                        log.error("External weather provider returned server error: {}", resp.getStatusCode());
                        throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                                "External weather service is temporarily unavailable. Please try again later.");
                    })
                    .body(OpenWeatherResponse.class);

            if (response == null) {
                log.error("Received null body from external weather provider for location: {}", locationIdentifier);
                throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Unable to parse weather service response. Please try again later.");
            }

            return mapToWeatherResponse(response);

        } catch (ResourceNotFoundException | WeatherException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error when calling external weather service: {}", e.getMessage());
            throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Weather service request timed out. Please try again later.", e);
        } catch (RestClientResponseException e) {
            log.error("Unexpected response from weather service: {}", e.getStatusCode());
            if (e.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Weather data not found for location: " + locationIdentifier);
            }
            throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Weather service is currently unavailable. Please try again later.", e);
        } catch (Exception e) {
            log.error("Unexpected error during weather data processing: {}", e.getMessage());
            throw new WeatherException(HttpStatus.SERVICE_UNAVAILABLE,
                    "An unexpected error occurred while fetching weather data.", e);
        }
    }

    private WeatherResponse mapToWeatherResponse(OpenWeatherResponse response) {
        String weatherCondition = null;
        String weatherDescription = null;
        if (response.getWeather() != null && !response.getWeather().isEmpty()) {
            weatherCondition = response.getWeather().get(0).getMain();
            weatherDescription = response.getWeather().get(0).getDescription();
        }

        LocalDateTime observedAt;
        if (response.getDt() != null) {
            observedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(response.getDt()), ZoneId.systemDefault());
        } else {
            observedAt = LocalDateTime.now();
        }

        Double temp = response.getMain() != null ? response.getMain().getTemp() : null;
        Double feelsLike = response.getMain() != null ? response.getMain().getFeelsLike() : null;
        Double tempMin = response.getMain() != null ? response.getMain().getTempMin() : null;
        Double tempMax = response.getMain() != null ? response.getMain().getTempMax() : null;
        Integer humidity = response.getMain() != null ? response.getMain().getHumidity() : null;

        Double windSpeed = response.getWind() != null ? response.getWind().getSpeed() : null;
        String country = response.getSys() != null ? response.getSys().getCountry() : null;
        Double lat = response.getCoord() != null ? response.getCoord().getLat() : null;
        Double lon = response.getCoord() != null ? response.getCoord().getLon() : null;

        return WeatherResponse.builder()
                .location(response.getName())
                .country(country)
                .latitude(lat)
                .longitude(lon)
                .temperature(temp)
                .feelsLikeTemperature(feelsLike)
                .minimumTemperature(tempMin)
                .maximumTemperature(tempMax)
                .humidity(humidity)
                .weatherCondition(weatherCondition)
                .weatherDescription(weatherDescription)
                .windSpeed(windSpeed)
                .observedAt(observedAt)
                .build();
    }
}
