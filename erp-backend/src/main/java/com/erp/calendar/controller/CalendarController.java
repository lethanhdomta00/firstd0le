package com.erp.calendar.controller;

import com.erp.calendar.dto.CalendarAnalyticsDto;
import com.erp.calendar.dto.CalendarEventDto;
import com.erp.calendar.dto.CalendarStatsDto;
import com.erp.calendar.dto.CreateEventRequest;
import com.erp.calendar.dto.RescheduleEventRequest;
import com.erp.calendar.dto.UpdateEventRequest;
import com.erp.calendar.dto.UpdateProgressRequest;
import com.erp.calendar.dto.UpdateSubtasksRequest;
import com.erp.calendar.model.CalendarEvent;
import com.erp.calendar.model.EventType;
import com.erp.calendar.service.CalendarEventService;
import com.erp.calendar.service.CalendarIcsService;
import com.erp.calendar.service.CalendarReminderScheduler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarEventService calendarEventService;
    private final CalendarIcsService calendarIcsService;
    private final CalendarReminderScheduler calendarReminderScheduler;

    /**
     * Lấy danh sách sự kiện theo khoảng thời gian và bộ lọc
     */
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventType type,
            Authentication authentication
    ) {
        String currentUsername = authentication != null ? authentication.getName() : "anonymous";
        List<CalendarEventDto> events = calendarEventService.getEvents(start, end, search, type, currentUsername);
        return ResponseEntity.ok(events);
    }

    /**
     * Lấy chi tiết 1 sự kiện
     */
    @GetMapping("/events/{id}")
    public ResponseEntity<CalendarEventDto> getEventById(@PathVariable Long id) {
        CalendarEventDto event = calendarEventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Tạo mới sự kiện trên lịch
     */
    @PostMapping("/events")
    public ResponseEntity<CalendarEventDto> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        CalendarEventDto created = calendarEventService.createEvent(request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Cập nhật sự kiện trên lịch
     */
    @PutMapping("/events/{id}")
    public ResponseEntity<CalendarEventDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        CalendarEventDto updated = calendarEventService.updateEvent(id, request, currentUsername);
        return ResponseEntity.ok(updated);
    }

    /**
     * Xóa sự kiện khỏi lịch
     */
    @DeleteMapping("/events/{id}")
    public ResponseEntity<Map<String, Object>> deleteEvent(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        calendarEventService.deleteEvent(id, currentUsername);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xóa sự kiện thành công!",
                "deletedId", id
        ));
    }

    /**
     * Chuyển đổi trạng thái hoàn thành nhanh (Toggle Completed)
     */
    @PatchMapping("/events/{id}/toggle-status")
    public ResponseEntity<CalendarEventDto> toggleStatus(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        CalendarEventDto updated = calendarEventService.toggleEventStatus(id, currentUsername);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cập nhật tiến độ % của sự kiện / công việc
     */
    @PatchMapping("/events/{id}/progress")
    public ResponseEntity<CalendarEventDto> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProgressRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        CalendarEventDto updated = calendarEventService.updateProgress(id, request.getProgress(), currentUsername);
        return ResponseEntity.ok(updated);
    }

    /**
     * Cập nhật danh sách công việc con (Subtasks Checklist)
     */
    @PatchMapping("/events/{id}/subtasks")
    public ResponseEntity<CalendarEventDto> updateSubtasks(
            @PathVariable Long id,
            @RequestBody UpdateSubtasksRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        CalendarEventDto updated = calendarEventService.updateSubtasks(id, request.getSubtasks(), currentUsername);
        return ResponseEntity.ok(updated);
    }

    /**
     * Dời ngày sự kiện (Drag & Drop Reschedule)
     */
    @PatchMapping("/events/{id}/reschedule")
    public ResponseEntity<CalendarEventDto> rescheduleEvent(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleEventRequest request,
            Authentication authentication
    ) {
        String currentUsername = authentication.getName();
        CalendarEventDto updated = calendarEventService.rescheduleEvent(id, request.getStartTime(), request.getEndTime(), currentUsername);
        return ResponseEntity.ok(updated);
    }

    /**
     * Lấy danh sách các sự kiện sắp diễn ra
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<CalendarEventDto>> getUpcomingEvents(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<CalendarEventDto> upcoming = calendarEventService.getUpcomingEvents(limit);
        return ResponseEntity.ok(upcoming);
    }

    /**
     * Thống kê tổng quan Lịch biểu
     */
    @GetMapping("/stats")
    public ResponseEntity<CalendarStatsDto> getCalendarStats(Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : "anonymous";
        CalendarStatsDto stats = calendarEventService.getStats(currentUsername);
        return ResponseEntity.ok(stats);
    }

    /**
     * Báo cáo & Phân tích KPI hiệu suất Lịch biểu & Công việc
     */
    @GetMapping("/analytics")
    public ResponseEntity<CalendarAnalyticsDto> getCalendarAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        CalendarAnalyticsDto analytics = calendarEventService.getAnalytics(start, end);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Xuất 1 sự kiện ra file iCalendar (.ics) chuẩn RFC 5545 (import Google/Outlook Calendar)
     */
    @GetMapping({"/events/{id}/export.ics", "/events/{id}/export-ics"})
    public ResponseEntity<byte[]> exportSingleEventToIcs(@PathVariable Long id) {
        CalendarEvent event = calendarEventService.getEventEntityById(id);
        String icsContent = calendarIcsService.generateIcsForSingleEvent(event);
        byte[] bytes = icsContent.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar; charset=utf-8"));
        headers.setContentDispositionFormData("attachment", "erp-event-" + id + ".ics");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Xuất toàn bộ lịch trong khoảng thời gian ra file iCalendar (.ics)
     */
    @GetMapping({"/export.ics", "/export-ics"})
    public ResponseEntity<byte[]> exportCalendarToIcs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) EventType type
    ) {
        List<CalendarEvent> events = calendarEventService.getEventsEntitiesInRange(start, end, type);
        String icsContent = calendarIcsService.generateIcsForEvents(events, "ERP Cloud - Lich bieu & Cong viec");
        byte[] bytes = icsContent.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar; charset=utf-8"));
        headers.setContentDispositionFormData("attachment", "erp-calendar-sync.ics");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Gửi thông báo Nhắc hẹn tức thì qua WebSocket tới người tham gia
     */
    @PostMapping("/events/{id}/remind-now")
    public ResponseEntity<Map<String, Object>> sendImmediateReminder(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String currentUsername = authentication != null ? authentication.getName() : "admin";
        CalendarEvent event = calendarEventService.getEventEntityById(id);
        int recipientsCount = calendarReminderScheduler.sendImmediateReminder(event, currentUsername);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi thông báo nhắc hẹn tới " + recipientsCount + " thành viên!",
                "recipientsCount", recipientsCount
        ));
    }
}

