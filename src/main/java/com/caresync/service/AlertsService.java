package com.caresync.service;

import com.caresync.dto.alert.AlertsResponse;

public interface AlertsService {

    /**
     * Retrieve active hydration alerts and reminders for the authenticated user.
     *
     * @param userEmail the authenticated user's email
     * @param city      optional city name for ambient weather evaluation
     * @param latitude  optional latitude coordinate (-90 to +90)
     * @param longitude optional longitude coordinate (-180 to +180)
     * @return AlertsResponse containing list of active alerts and count
     */
    AlertsResponse getAlerts(String userEmail, String city, Double latitude, Double longitude);
}
