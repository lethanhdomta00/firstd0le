package com.erp.calendar.service;

import com.erp.calendar.model.CalendarEvent;
import com.erp.calendar.repository.CalendarEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Bộ quét & gửi thông báo Nhắc hẹn Thông minh Realtime qua WebSocket (STOMP)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CalendarReminderScheduler {

    private final CalendarEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /**
     * Quét định kỳ mỗi 30 giây để kiểm tra các sự kiện sắp tới hạn nhắc hẹn
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkAndSendReminders() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        // Quét trong cửa sổ 25 giờ tới (bao gồm cả cấu hình nhắc trước 1 ngày = 1440 phút)
        List<CalendarEvent> candidates = eventRepository.findEventsPendingReminder(
                now.minusMinutes(5),
                now.plusHours(25)
        );

        if (candidates.isEmpty()) {
            return;
        }

        for (CalendarEvent event : candidates) {
            int minsBefore = event.getReminderMinutesBefore() != null ? event.getReminderMinutesBefore() : 0;
            if (minsBefore <= 0) continue;

            LocalDateTime triggerTime = event.getStartTime().minusMinutes(minsBefore);
            if (!now.isBefore(triggerTime)) {
                dispatchReminderWebSocket(event, false, null);
                event.setReminderSent(true);
                eventRepository.save(event);
                log.info("🔔 Đã tự động gửi nhắc hẹn cho sự kiện [ID: {} - {}] trước {} phút",
                        event.getId(), event.getTitle(), minsBefore);
            }
        }
    }

    /**
     * Gửi nhắc hẹn tức thì (khi người dùng bấm nút Nhắc ngay trên giao diện)
     */
    public int sendImmediateReminder(CalendarEvent event, String triggeredBy) {
        Set<String> recipients = dispatchReminderWebSocket(event, true, triggeredBy);
        log.info("🔔 [{}] đã gửi thông báo nhắc ngay sự kiện [ID: {} - {}] tới {} người",
                triggeredBy, event.getId(), event.getTitle(), recipients.size());
        return recipients.size();
    }

    private Set<String> dispatchReminderWebSocket(CalendarEvent event, boolean isManual, String triggeredBy) {
        Set<String> recipients = collectRecipients(event);
        if (recipients.isEmpty()) {
            return recipients;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", event.getId());
        payload.put("title", event.getTitle());
        payload.put("startTime", event.getStartTime() != null ? event.getStartTime().toString() : null);
        payload.put("formattedTime", event.getStartTime() != null ? event.getStartTime().format(TIME_FMT) : "");
        payload.put("location", event.getLocation());
        payload.put("meetingUrl", event.getMeetingUrl());
        payload.put("eventType", event.getEventType() != null ? event.getEventType().name() : "MEETING");
        payload.put("priority", event.getPriority() != null ? event.getPriority().name() : "MEDIUM");
        payload.put("colorHex", event.getColorHex());
        payload.put("reminderMinutesBefore", event.getReminderMinutesBefore());
        payload.put("isManualPing", isManual);
        payload.put("triggeredBy", triggeredBy);
        payload.put("timestamp", LocalDateTime.now(VIETNAM_ZONE).toString());

        for (String username : recipients) {
            try {
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/calendar-reminders",
                        payload
                );
            } catch (Exception e) {
                log.warn("Không thể gửi nhắc hẹn WebSocket tới user [{}]: {}", username, e.getMessage());
            }
        }

        return recipients;
    }

    private Set<String> collectRecipients(CalendarEvent event) {
        Set<String> recipients = new LinkedHashSet<>();
        if (event.getCreatorUsername() != null && !event.getCreatorUsername().trim().isEmpty()) {
            recipients.add(event.getCreatorUsername().trim());
        }
        if (event.getAssigneeUsername() != null && !event.getAssigneeUsername().trim().isEmpty()) {
            recipients.add(event.getAssigneeUsername().trim());
        }
        String json = event.getAttendeesJson();
        if (json != null && !json.trim().isEmpty() && !"[]".equals(json.trim())) {
            try {
                List<String> attendees = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                for (String att : attendees) {
                    if (att != null && !att.trim().isEmpty()) {
                        recipients.add(att.trim());
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi parse attendeesJson khi thu thập người nhận nhắc hẹn: {}", e.getMessage());
            }
        }
        return recipients;
    }
}
