package com.caresync.service;

import com.caresync.dto.water.WaterHistoryResponse;
import com.caresync.dto.water.WaterLogRequest;
import com.caresync.dto.water.WaterLogResponse;

public interface WaterService {

    /**
     * Log a water consumption entry for the authenticated user.
     *
     * @param userEmail the authenticated user's email
     * @param request   the intake payload (amount in ml)
     * @return confirmation of the logged entry
     */
    WaterLogResponse logWaterIntake(String userEmail, WaterLogRequest request);

    /**
     * Retrieve 7-day water consumption history for the authenticated user.
     *
     * @param userEmail the authenticated user's email
     * @param city      optional city name for today's weather
     * @param latitude  optional latitude for today's weather
     * @param longitude optional longitude for today's weather
     * @return 7-day history with daily targets, consumed ml, and progress percentage
     */
    WaterHistoryResponse getWaterHistory(String userEmail, String city, Double latitude, Double longitude);
}
