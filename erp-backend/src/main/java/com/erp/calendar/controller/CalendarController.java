package com.erp.calendar.controller;

import com.erp.calendar.dto.CalendarEventDto;
import com.erp.calendar.dto.CalendarStatsDto;
import com.erp.calendar.dto.CreateEventRequest;
import com.erp.calendar.dto.UpdateEventRequest;
import com.erp.calendar.model.EventType;
import com.erp.calendar.service.CalendarEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class CalendarController {

    private final CalendarEventService calendarEventService;

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
}

