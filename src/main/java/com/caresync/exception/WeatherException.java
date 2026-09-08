package com.caresync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WeatherException extends RuntimeException {

    private final HttpStatus status;

    public WeatherException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public WeatherException(String message) {
        super(message);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public WeatherException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public WeatherException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
