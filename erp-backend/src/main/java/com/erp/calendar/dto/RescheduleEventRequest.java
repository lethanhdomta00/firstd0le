package com.erp.calendar.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleEventRequest {

    @NotNull(message = "Thời gian bắt đầu mới không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc mới không được để trống")
    private LocalDateTime endTime;
}
