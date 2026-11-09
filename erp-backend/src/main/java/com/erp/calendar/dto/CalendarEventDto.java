package com.erp.calendar.dto;

import com.erp.calendar.model.CalendarEvent;
import com.erp.calendar.model.EventPriority;
import com.erp.calendar.model.EventStatus;
import com.erp.calendar.model.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAllDay;
    private String location;
    private String meetingUrl;
    private EventType eventType;
    private EventPriority priority;
    private EventStatus status;
    private String colorHex;
    private String creatorUsername;
    private String creatorFullName;
    private String department;
    private String attendeesJson;
    private List<String> attendees;
    private Integer reminderMinutesBefore;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CalendarEventDto fromEntity(CalendarEvent entity, List<String> attendeeList) {
        if (entity == null) return null;
        return CalendarEventDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .isAllDay(entity.getIsAllDay())
                .location(entity.getLocation())
                .meetingUrl(entity.getMeetingUrl())
                .eventType(entity.getEventType())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .colorHex(entity.getColorHex())
                .creatorUsername(entity.getCreatorUsername())
                .creatorFullName(entity.getCreatorFullName())
                .department(entity.getDepartment())
                .attendeesJson(entity.getAttendeesJson())
                .attendees(attendeeList != null ? attendeeList : Collections.emptyList())
                .reminderMinutesBefore(entity.getReminderMinutesBefore())
                .isPublic(entity.getIsPublic())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

