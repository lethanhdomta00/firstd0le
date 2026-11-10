package com.erp.calendar.service;

import com.erp.calendar.dto.CalendarEventDto;
import com.erp.calendar.dto.CalendarStatsDto;
import com.erp.calendar.dto.CreateEventRequest;
import com.erp.calendar.dto.UpdateEventRequest;
import com.erp.calendar.model.CalendarEvent;
import com.erp.calendar.model.EventPriority;
import com.erp.calendar.model.EventStatus;
import com.erp.calendar.model.EventType;
import com.erp.calendar.repository.CalendarEventRepository;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @PostConstruct
    public void initSampleData() {
        if (eventRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu mẫu cho Lịch biểu doanh nghiệp (Calendar Events)...");
            LocalDate today = LocalDate.now(VIETNAM_ZONE);

            List<CalendarEvent> sampleEvents = List.of(
                    CalendarEvent.builder()
                            .title("Họp giao ban tuần & Review KPI")
                            .description("Đánh giá tiến độ dự án ERP Sprint 4, tháo gỡ vướng mắc các phòng ban.")
                            .startTime(today.atTime(9, 0))
                            .endTime(today.atTime(10, 30))
                            .isAllDay(false)
                            .location("Phòng họp chính A2 / Google Meet")
                            .meetingUrl("https://meet.google.com/erp-sync-weekly")
                            .eventType(EventType.MEETING)
                            .priority(EventPriority.HIGH)
                            .status(EventStatus.SCHEDULED)
                            .colorHex("#2563eb")
                            .creatorUsername("admin")
                            .creatorFullName("Quản trị hệ thống")
                            .department("Toàn công ty")
                            .attendeesJson("[\"admin\",\"lethanhdo\",\"Thienbip\"]")
                            .reminderMinutesBefore(15)
                            .isPublic(true)
                            .build(),

                    CalendarEvent.builder()
                            .title("Deadline bàn giao Module Chat & Call WebRTC")
                            .description("Hoàn tất kiểm thử tính năng gọi thoại, video nội bộ và ghim tin nhắn.")
                            .startTime(today.plusDays(1).atTime(17, 0))
                            .endTime(today.plusDays(1).atTime(18, 0))
                            .isAllDay(false)
                            .location("Hệ thống ERP")
                            .eventType(EventType.DEADLINE)
                            .priority(EventPriority.URGENT)
                            .status(EventStatus.SCHEDULED)
                            .colorHex("#ef4444")
                            .creatorUsername("admin")
                            .creatorFullName("Quản trị hệ thống")
                            .department("Kỹ thuật")
                            .attendeesJson("[\"admin\",\"lethanhdo\"]")
                            .reminderMinutesBefore(30)
                            .isPublic(true)
                            .build(),

                    CalendarEvent.builder()
                            .title("Kiểm kê hàng hóa tồn kho tháng")
                            .description("Đối chiếu số liệu kho thực tế và hệ thống ERP SKU định kỳ.")
                            .startTime(today.plusDays(3).atTime(8, 30))
                            .endTime(today.plusDays(3).atTime(11, 30))
                            .isAllDay(false)
                            .location("Kho tổng Hà Nội")
                            .eventType(EventType.TASK)
                            .priority(EventPriority.MEDIUM)
                            .status(EventStatus.SCHEDULED)
                            .colorHex("#f59e0b")
                            .creatorUsername("admin")
                            .creatorFullName("Quản trị hệ thống")
                            .department("Kho vận")
                            .attendeesJson("[\"admin\"]")
                            .reminderMinutesBefore(15)
                            .isPublic(true)
                            .build(),

                    CalendarEvent.builder()
                            .title("Workshop chia sẻ kiến thức: Spring Boot 3 & AI Copilot")
                            .description("Buổi chia sẻ nội bộ kỹ năng áp dụng AI vào quy trình làm việc doanh nghiệp.")
                            .startTime(today.plusDays(5).atTime(14, 0))
                            .endTime(today.plusDays(5).atTime(16, 0))
                            .isAllDay(false)
                            .location("Hội trường B1")
                            .eventType(EventType.EVENT)
                            .priority(EventPriority.LOW)
                            .status(EventStatus.SCHEDULED)
                            .colorHex("#8b5cf6")
                            .creatorUsername("admin")
                            .creatorFullName("Quản trị hệ thống")
                            .department("Toàn công ty")
                            .attendeesJson("[]")
                            .reminderMinutesBefore(60)
                            .isPublic(true)
                            .build()
            );

            eventRepository.saveAll(sampleEvents);
            log.info("Đã tạo thành công {} sự kiện mẫu cho Calendar.", sampleEvents.size());
        }
    }

    /**
     * Lấy danh sách sự kiện trong khoảng thời gian [start, end] kèm bộ lọc
     */
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getEvents(
            LocalDateTime start,
            LocalDateTime end,
            String search,
            EventType eventType,
            String username
    ) {
        LocalDateTime queryStart = start != null ? start : LocalDate.now(VIETNAM_ZONE).withDayOfMonth(1).atStartOfDay();
        LocalDateTime queryEnd = end != null ? end : LocalDate.now(VIETNAM_ZONE).plusMonths(1).atTime(23, 59, 59);

        List<CalendarEvent> events = eventRepository.findEventsInRange(queryStart, queryEnd);

        String kw = search != null ? search.trim().toLowerCase() : "";

        return events.stream()
                .filter(e -> {
                    // Lọc theo từ khóa tìm kiếm
                    if (!kw.isEmpty()) {
                        boolean matchTitle = e.getTitle() != null && e.getTitle().toLowerCase().contains(kw);
                        boolean matchDesc = e.getDescription() != null && e.getDescription().toLowerCase().contains(kw);
                        boolean matchLoc = e.getLocation() != null && e.getLocation().toLowerCase().contains(kw);
                        if (!matchTitle && !matchDesc && !matchLoc) return false;
                    }
                    // Lọc theo loại sự kiện
                    if (eventType != null && e.getEventType() != eventType) {
                        return false;
                    }
                    return true;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết 1 sự kiện theo ID
     */
    @Transactional(readOnly = true)
    public CalendarEventDto getEventById(Long id) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));
        return toDto(event);
    }

    /**
     * Tạo mới sự kiện trên lịch
     */
    @Transactional
    public CalendarEventDto createEvent(CreateEventRequest req, String currentUsername) {
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
        }

        User creator = userRepository.findByUsername(currentUsername).orElse(null);
        String creatorName = creator != null && creator.getFullName() != null ? creator.getFullName() : currentUsername;

        String attendeesJson = serializeAttendees(req.getAttendees());
        String subtasksJson = serializeSubtasks(req.getSubtasks());

        String assigneeFullName = null;
        if (req.getAssigneeUsername() != null && !req.getAssigneeUsername().trim().isEmpty()) {
            User assignee = userRepository.findByUsername(req.getAssigneeUsername().trim()).orElse(null);
            assigneeFullName = assignee != null && assignee.getFullName() != null ? assignee.getFullName() : req.getAssigneeUsername();
        }

        // Tự động gán màu sắc mặc định theo loại sự kiện nếu người dùng không chọn
        String color = req.getColorHex();
        if (color == null || color.trim().isEmpty()) {
            color = getDefaultColorForType(req.getEventType());
        }

        Integer initialProgress = req.getProgress() != null ? req.getProgress() : 0;
        EventStatus initialStatus = req.getStatus() != null ? req.getStatus() : EventStatus.SCHEDULED;
        if (initialProgress >= 100) {
            initialStatus = EventStatus.COMPLETED;
        }

        CalendarEvent event = CalendarEvent.builder()
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .isAllDay(Boolean.TRUE.equals(req.getIsAllDay()))
                .location(req.getLocation())
                .meetingUrl(req.getMeetingUrl())
                .eventType(req.getEventType() != null ? req.getEventType() : EventType.MEETING)
                .priority(req.getPriority() != null ? req.getPriority() : EventPriority.MEDIUM)
                .status(initialStatus)
                .colorHex(color)
                .creatorUsername(currentUsername)
                .creatorFullName(creatorName)
                .department(req.getDepartment() != null ? req.getDepartment() : "Chung")
                .attendeesJson(attendeesJson)
                .reminderMinutesBefore(req.getReminderMinutesBefore() != null ? req.getReminderMinutesBefore() : 15)
                .progress(initialProgress)
                .assigneeUsername(req.getAssigneeUsername())
                .assigneeFullName(assigneeFullName)
                .subtasksJson(subtasksJson)
                .isPublic(req.getIsPublic() != null ? req.getIsPublic() : true)
                .build();

        CalendarEvent saved = eventRepository.save(event);
        log.info("Người dùng [{}] đã tạo sự kiện mới [{}] (ID: {})", currentUsername, saved.getTitle(), saved.getId());
        return toDto(saved);
    }

    /**
     * Cập nhật sự kiện trên lịch
     */
    @Transactional
    public CalendarEventDto updateEvent(Long id, UpdateEventRequest req, String currentUsername) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));

        // Kiểm tra quyền: Chỉ người tạo sự kiện hoặc ADMIN mới được cập nhật
        checkPermission(event, currentUsername);

        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
        }

        event.setTitle(req.getTitle().trim());
        event.setDescription(req.getDescription());
        event.setStartTime(req.getStartTime());
        event.setEndTime(req.getEndTime());
        if (req.getIsAllDay() != null) event.setIsAllDay(req.getIsAllDay());
        event.setLocation(req.getLocation());
        event.setMeetingUrl(req.getMeetingUrl());
        if (req.getEventType() != null) event.setEventType(req.getEventType());
        if (req.getPriority() != null) event.setPriority(req.getPriority());
        if (req.getStatus() != null) event.setStatus(req.getStatus());
        if (req.getColorHex() != null && !req.getColorHex().trim().isEmpty()) event.setColorHex(req.getColorHex());
        if (req.getDepartment() != null) event.setDepartment(req.getDepartment());
        if (req.getAttendees() != null) event.setAttendeesJson(serializeAttendees(req.getAttendees()));
        if (req.getReminderMinutesBefore() != null) event.setReminderMinutesBefore(req.getReminderMinutesBefore());
        if (req.getIsPublic() != null) event.setIsPublic(req.getIsPublic());

        if (req.getAssigneeUsername() != null) {
            event.setAssigneeUsername(req.getAssigneeUsername());
            if (!req.getAssigneeUsername().trim().isEmpty()) {
                User assignee = userRepository.findByUsername(req.getAssigneeUsername().trim()).orElse(null);
                event.setAssigneeFullName(assignee != null && assignee.getFullName() != null ? assignee.getFullName() : req.getAssigneeUsername());
            } else {
                event.setAssigneeFullName(null);
            }
        }

        if (req.getProgress() != null) {
            event.setProgress(req.getProgress());
            if (req.getProgress() >= 100) {
                event.setStatus(EventStatus.COMPLETED);
            } else if (event.getStatus() == EventStatus.COMPLETED) {
                event.setStatus(EventStatus.IN_PROGRESS);
            }
        }

        if (req.getSubtasks() != null) {
            event.setSubtasksJson(serializeSubtasks(req.getSubtasks()));
        }

        CalendarEvent updated = eventRepository.save(event);
        log.info("Người dùng [{}] đã cập nhật sự kiện [{}] (ID: {})", currentUsername, updated.getTitle(), updated.getId());
        return toDto(updated);
    }

    /**
     * Xóa sự kiện khỏi lịch
     */
    @Transactional
    public void deleteEvent(Long id, String currentUsername) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));

        // Kiểm tra quyền xóa
        checkPermission(event, currentUsername);

        eventRepository.delete(event);
        log.info("Người dùng [{}] đã xóa sự kiện [{}] (ID: {})", currentUsername, event.getTitle(), id);
    }

    /**
     * Lấy danh sách các sự kiện sắp diễn ra
     */
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getUpcomingEvents(int limit) {
        int take = limit > 0 && limit <= 50 ? limit : 5;
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        return eventRepository.findUpcomingEvents(now, PageRequest.of(0, take)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Thống kê tổng quan cho Lịch biểu
     */
    @Transactional(readOnly = true)
    public CalendarStatsDto getStats(String username) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(23, 59, 59);

        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59);

        long todayCount = eventRepository.countByStartTimeBetween(todayStart, todayEnd);
        long weekCount = eventRepository.countByStartTimeBetween(weekStart, weekEnd);
        long meetingsCount = eventRepository.countByEventTypeAndStartTimeBetween(EventType.MEETING, weekStart, weekEnd);
        long deadlinesCount = eventRepository.countByEventTypeAndStartTimeBetween(EventType.DEADLINE, todayStart, weekEnd);

        return CalendarStatsDto.builder()
                .todayEventsCount(todayCount)
                .thisWeekEventsCount(weekCount)
                .myMeetingsCount(meetingsCount)
                .pendingDeadlinesCount(deadlinesCount)
                .build();
    }

    /**
     * Chuyển đổi trạng thái hoàn thành nhanh (Toggle Status)
     */
    @Transactional
    public CalendarEventDto toggleEventStatus(Long id, String currentUsername) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));

        if (event.getStatus() == EventStatus.COMPLETED) {
            event.setStatus(EventStatus.IN_PROGRESS);
            event.setProgress(0);
        } else {
            event.setStatus(EventStatus.COMPLETED);
            event.setProgress(100);
        }

        CalendarEvent updated = eventRepository.save(event);
        log.info("Người dùng [{}] đã toggle trạng thái sự kiện [{}] sang {}", currentUsername, event.getTitle(), event.getStatus());
        return toDto(updated);
    }

    /**
     * Cập nhật tiến độ % công việc
     */
    @Transactional
    public CalendarEventDto updateProgress(Long id, Integer progress, String currentUsername) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));

        int p = Math.max(0, Math.min(100, progress != null ? progress : 0));
        event.setProgress(p);

        if (p >= 100) {
            event.setStatus(EventStatus.COMPLETED);
        } else if (event.getStatus() == EventStatus.COMPLETED) {
            event.setStatus(EventStatus.IN_PROGRESS);
        }

        CalendarEvent updated = eventRepository.save(event);
        log.info("Người dùng [{}] đã cập nhật tiến độ sự kiện [{}] thành {}%", currentUsername, event.getTitle(), p);
        return toDto(updated);
    }

    /**
     * Cập nhật danh sách công việc con (Subtasks) và tự động tính lại % tiến độ
     */
    @Transactional
    public CalendarEventDto updateSubtasks(Long id, List<Map<String, Object>> subtasks, String currentUsername) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));

        event.setSubtasksJson(serializeSubtasks(subtasks));

        if (subtasks != null && !subtasks.isEmpty()) {
            long completedCount = subtasks.stream()
                    .filter(s -> Boolean.TRUE.equals(s.get("completed")) || "true".equalsIgnoreCase(String.valueOf(s.get("completed"))))
                    .count();
            int calcProgress = (int) Math.round(((double) completedCount / subtasks.size()) * 100);
            event.setProgress(calcProgress);

            if (calcProgress >= 100) {
                event.setStatus(EventStatus.COMPLETED);
            } else if (event.getStatus() == EventStatus.COMPLETED) {
                event.setStatus(EventStatus.IN_PROGRESS);
            }
        }

        CalendarEvent updated = eventRepository.save(event);
        log.info("Người dùng [{}] đã cập nhật subtasks sự kiện [{}] ({} việc con)", currentUsername, event.getTitle(), subtasks != null ? subtasks.size() : 0);
        return toDto(updated);
    }

    /**
     * Dời ngày sự kiện (Drag & Drop Reschedule)
     */
    @Transactional
    public CalendarEventDto rescheduleEvent(Long id, LocalDateTime startTime, LocalDateTime endTime, String currentUsername) {
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện với ID: " + id));

        checkPermission(event, currentUsername);

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
        }

        event.setStartTime(startTime);
        event.setEndTime(endTime);

        CalendarEvent updated = eventRepository.save(event);
        log.info("Người dùng [{}] đã dời lịch sự kiện [{}] sang [{} -> {}]", currentUsername, event.getTitle(), startTime, endTime);
        return toDto(updated);
    }

    // =========================================================================
    //                            HELPER METHODS
    // =========================================================================

    private void checkPermission(CalendarEvent event, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isCreator = event.getCreatorUsername().equalsIgnoreCase(currentUsername);

        if (!isAdmin && !isCreator) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền chỉnh sửa hoặc xóa sự kiện do đồng nghiệp khác tạo!"
            );
        }
    }

    private CalendarEventDto toDto(CalendarEvent event) {
        List<String> attendeesList = parseAttendees(event.getAttendeesJson());
        List<Map<String, Object>> subtasksList = parseSubtasks(event.getSubtasksJson());
        return CalendarEventDto.fromEntity(event, attendeesList, subtasksList);
    }

    private String serializeAttendees(List<String> attendees) {
        if (attendees == null || attendees.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attendees);
        } catch (Exception e) {
            log.warn("Không thể serialize danh sách attendees: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> parseAttendees(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Không thể parse danh sách attendees json: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serializeSubtasks(List<Map<String, Object>> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(subtasks);
        } catch (Exception e) {
            log.warn("Không thể serialize danh sách subtasks: {}", e.getMessage());
            return "[]";
        }
    }

    private List<Map<String, Object>> parseSubtasks(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("Không thể parse danh sách subtasks json: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getDefaultColorForType(EventType type) {
        if (type == null) return "#2563eb";
        return switch (type) {
            case MEETING -> "#2563eb";          // Xanh dương
            case CLIENT_MEETING -> "#0284c7";   // Xanh da trời
            case DEADLINE -> "#ef4444";         // Đỏ
            case TASK -> "#f59e0b";             // Vàng cam
            case TRAINING -> "#8b5cf6";         // Tím violet
            case INTERVIEW -> "#06b6d4";        // Xanh ngọc lam
            case BUSINESS_TRIP -> "#0d9488";    // Xanh lục đậm
            case REVIEW -> "#ea580c";           // Cam đậm
            case TEAM_BUILDING -> "#ec4899";    // Hồng cánh sen
            case MAINTENANCE -> "#64748b";      // Xám đá
            case HOLIDAY -> "#10b981";          // Xanh lá cây
            case EVENT -> "#6366f1";            // Xanh chàm
        };
    }
}

