package com.caresync.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile summary information")
public class UserSummaryDto {

    @Schema(description = "User full name", example = "Pavan Kalyan")
    private String name;
}
