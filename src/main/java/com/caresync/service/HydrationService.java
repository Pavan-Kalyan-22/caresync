package com.caresync.service;

import com.caresync.dto.hydration.HydrationResponse;

public interface HydrationService {

    /**
     * Calculate daily hydration recommendation for the authenticated user
     * based on their profile and live weather at the specified location.
     *
     * @param userEmail the authenticated user's email
     * @param city      optional city name
     * @param latitude  optional latitude (-90 to +90)
     * @param longitude optional longitude (-180 to +180)
     * @return personalized HydrationResponse
     */
    HydrationResponse getRecommendation(String userEmail, String city, Double latitude, Double longitude);
}
