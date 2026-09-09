package com.caresync.dto.water;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "7-day hydration history container")
public class WaterHistoryResponse {

    @Schema(description = "List of daily water records for the past 7 calendar days (newest day first)")
    private List<DailyWaterHistoryDto> days;
}
