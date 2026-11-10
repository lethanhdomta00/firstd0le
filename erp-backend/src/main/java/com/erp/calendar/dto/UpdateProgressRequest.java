package com.erp.calendar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgressRequest {

    @NotNull(message = "Tiến độ không được để trống")
    @Min(value = 0, message = "Tiến độ tối thiểu là 0%")
    @Max(value = 100, message = "Tiến độ tối đa là 100%")
    private Integer progress;
}
