package com.erp.calendar.dto;

import com.erp.calendar.model.EventPriority;
import com.erp.calendar.model.EventStatus;
import com.erp.calendar.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {

    @NotBlank(message = "Tiêu đề sự kiện không được để trống")
    @Size(max = 200, message = "Tiêu đề không quá 200 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    private Boolean isAllDay = false;

    private String location;

    private String meetingUrl;

    private EventType eventType = EventType.MEETING;

    private EventPriority priority = EventPriority.MEDIUM;

    private EventStatus status = EventStatus.SCHEDULED;

    private String colorHex;

    private String department;

    private List<String> attendees; // Danh sách username người tham gia

    private Integer reminderMinutesBefore = 15;

    private Boolean isPublic = true;
}

