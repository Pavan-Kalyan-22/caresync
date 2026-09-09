package com.caresync.controller;

import com.caresync.dto.water.DailyWaterHistoryDto;
import com.caresync.dto.water.WaterHistoryResponse;
import com.caresync.dto.water.WaterLogRequest;
import com.caresync.dto.water.WaterLogResponse;
import com.caresync.exception.GlobalExceptionHandler;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.service.WaterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WaterControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private WaterService waterService;

    @InjectMocks
    private WaterController waterController;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(waterController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password", Collections.emptyList());
    }

    @Test
    @DisplayName("POST /api/v1/water/log with valid amount returns 201 Created")
    void testLogWater_Success_Returns201() throws Exception {
        WaterLogRequest request = WaterLogRequest.builder().amountMl(500).build();
        WaterLogResponse mockResponse = WaterLogResponse.builder()
                .id(1L)
                .amountMl(500)
                .loggedAt(LocalDateTime.now())
                .build();

        when(waterService.logWaterIntake(eq("user@example.com"), any(WaterLogRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/water/log")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Water intake logged successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.amountMl").value(500));
    }

    @Test
    @DisplayName("POST /api/v1/water/log with null amount returns 400 Bad Request")
    void testLogWater_NullAmount_Returns400() throws Exception {
        String jsonPayload = "{}";

        mockMvc.perform(post("/api/v1/water/log")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amountMl").exists());
    }

    @Test
    @DisplayName("POST /api/v1/water/log with zero or negative amount returns 400 Bad Request")
    void testLogWater_ZeroOrNegativeAmount_Returns400() throws Exception {
        WaterLogRequest zeroRequest = WaterLogRequest.builder().amountMl(0).build();

        mockMvc.perform(post("/api/v1/water/log")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.amountMl").exists());
    }

    @Test
    @DisplayName("POST /api/v1/water/log with amount exceeding 5000 ml returns 400 Bad Request")
    void testLogWater_ExcessiveAmount_Returns400() throws Exception {
        WaterLogRequest excessiveRequest = WaterLogRequest.builder().amountMl(5001).build();

        mockMvc.perform(post("/api/v1/water/log")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.amountMl").exists());
    }

    @Test
    @DisplayName("GET /api/v1/water/history returns 200 OK with 7 calendar days")
    void testGetWaterHistory_Success_Returns200() throws Exception {
        List<DailyWaterHistoryDto> mockDays = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            mockDays.add(DailyWaterHistoryDto.builder()
                    .date(today.minusDays(i))
                    .dailyTargetMl(2500)
                    .consumedMl(i == 0 ? 1500 : 0)
                    .progressPercentage(i == 0 ? 60.0 : 0.0)
                    .build());
        }
        WaterHistoryResponse mockResponse = WaterHistoryResponse.builder().days(mockDays).build();

        when(waterService.getWaterHistory("user@example.com", null, null, null))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/water/history")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Water history retrieved successfully"))
                .andExpect(jsonPath("$.data.days").isArray())
                .andExpect(jsonPath("$.data.days.length()").value(7))
                .andExpect(jsonPath("$.data.days[0].dailyTargetMl").value(2500))
                .andExpect(jsonPath("$.data.days[0].consumedMl").value(1500))
                .andExpect(jsonPath("$.data.days[0].progressPercentage").value(60.0));
    }

    @Test
    @DisplayName("GET /api/v1/water/history when user not found returns 404 Not Found")
    void testGetWaterHistory_UserNotFound_Returns404() throws Exception {
        when(waterService.getWaterHistory("user@example.com", null, null, null))
                .thenThrow(new ResourceNotFoundException("User not found with email: user@example.com"));

        mockMvc.perform(get("/api/v1/water/history")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found with email: user@example.com"));
    }
}
