package com.caresync.service;

import com.caresync.dto.dashboard.DashboardResponse;

public interface DashboardService {

    /**
     * Retrieve aggregated healthcare dashboard metrics for the authenticated user.
     *
     * @param userEmail the authenticated user's email
     * @param city      optional city name for current weather
     * @param latitude  optional latitude coordinate (-90 to +90)
     * @param longitude optional longitude coordinate (-180 to +180)
     * @return composite DashboardResponse
     */
    DashboardResponse getDashboard(String userEmail, String city, Double latitude, Double longitude);
}
