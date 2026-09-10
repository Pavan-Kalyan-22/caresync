package com.caresync.dto.alert;

import com.caresync.dto.dashboard.DashboardAlertDto;
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
@Schema(description = "Hydration alerts response")
public class AlertsResponse {

    @Schema(description = "List of active hydration alerts")
    private List<DashboardAlertDto> alerts;

    @Schema(description = "Total count of active alerts", example = "2")
    private Integer count;
}
