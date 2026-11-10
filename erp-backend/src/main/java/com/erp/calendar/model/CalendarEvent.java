package com.erp.calendar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "calendar_events", indexes = {
        @Index(name = "idx_cal_start_time", columnList = "start_time"),
        @Index(name = "idx_cal_end_time", columnList = "end_time"),
        @Index(name = "idx_cal_creator", columnList = "creator_username"),
        @Index(name = "idx_cal_event_type", columnList = "event_type")
})
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_all_day", nullable = false)
    @Builder.Default
    private Boolean isAllDay = false;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    @Builder.Default
    private EventType eventType = EventType.MEETING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private EventPriority priority = EventPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.SCHEDULED;

    @Column(name = "color_hex", length = 20)
    @Builder.Default
    private String colorHex = "#2563eb";

    @Column(name = "creator_username", nullable = false, length = 50)
    private String creatorUsername;

    @Column(name = "creator_full_name", length = 100)
    private String creatorFullName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "attendees_json", columnDefinition = "TEXT")
    private String attendeesJson; // Danh sách username người tham gia dạng JSON mảng hoặc chuỗi phân cách

    @Column(name = "reminder_minutes_before")
    @Builder.Default
    private Integer reminderMinutesBefore = 15;

    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0; // Tiến độ công việc (0 - 100%)

    @Column(name = "assignee_username", length = 50)
    private String assigneeUsername; // Người phụ trách chính

    @Column(name = "assignee_full_name", length = 100)
    private String assigneeFullName;

    @Column(name = "subtasks_json", columnDefinition = "TEXT")
    private String subtasksJson; // Danh sách công việc con (Checklist JSON)

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isAllDay == null) this.isAllDay = false;
        if (this.eventType == null) this.eventType = EventType.MEETING;
        if (this.priority == null) this.priority = EventPriority.MEDIUM;
        if (this.status == null) this.status = EventStatus.SCHEDULED;
        if (this.colorHex == null || this.colorHex.trim().isEmpty()) this.colorHex = "#2563eb";
        if (this.progress == null) this.progress = 0;
        if (this.isPublic == null) this.isPublic = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}

