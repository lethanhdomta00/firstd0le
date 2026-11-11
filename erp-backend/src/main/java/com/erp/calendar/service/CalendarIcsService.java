package com.erp.calendar.service;

import com.erp.calendar.model.CalendarEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Service tạo tệp tin lịch chuẩn quốc tế iCalendar (.ics - RFC 5545)
 * Hỗ trợ đồng bộ trực tiếp vào Google Calendar, Apple Calendar và Microsoft Outlook.
 */
@Service
@Slf4j
public class CalendarIcsService {

    private static final DateTimeFormatter ICS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ICS_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICS_UTC_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    /**
     * Xuất một sự kiện duy nhất ra định dạng .ics
     */
    public String generateIcsForSingleEvent(CalendarEvent event) {
        return generateIcsForEvents(Collections.singletonList(event), "ERP Cloud Event");
    }

    /**
     * Xuất danh sách nhiều sự kiện ra định dạng .ics
     */
    public String generateIcsForEvents(List<CalendarEvent> events, String calendarName) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//ERP Cloud Enterprise//Calendar Module//VI\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:").append(escapeText(calendarName != null ? calendarName : "ERP Cloud Calendar")).append("\r\n");
        sb.append("X-WR-TIMEZONE:Asia/Ho_Chi_Minh\r\n");

        // Định nghĩa VTIMEZONE chuẩn Việt Nam (GMT+7) để Google/Outlook hiển thị chuẩn giờ
        sb.append("BEGIN:VTIMEZONE\r\n");
        sb.append("TZID:Asia/Ho_Chi_Minh\r\n");
        sb.append("BEGIN:STANDARD\r\n");
        sb.append("DTSTART:19700101T000000\r\n");
        sb.append("TZOFFSETFROM:+0700\r\n");
        sb.append("TZOFFSETTO:+0700\r\n");
        sb.append("TZNAME:+07\r\n");
        sb.append("END:STANDARD\r\n");
        sb.append("END:VTIMEZONE\r\n");

        String dtStamp = LocalDateTime.now(ZoneOffset.UTC).format(ICS_UTC_FORMAT);

        if (events != null) {
            for (CalendarEvent evt : events) {
                appendEventBlock(sb, evt, dtStamp);
            }
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private void appendEventBlock(StringBuilder sb, CalendarEvent evt, String dtStamp) {
        sb.append("BEGIN:VEVENT\r\n");
        sb.append("UID:erp-calendar-event-").append(evt.getId()).append("@erp-cloud.local\r\n");
        sb.append("DTSTAMP:").append(dtStamp).append("\r\n");

        if (Boolean.TRUE.equals(evt.getIsAllDay())) {
            sb.append("DTSTART;VALUE=DATE:").append(evt.getStartTime().toLocalDate().format(ICS_DATE_FORMAT)).append("\r\n");
            // Theo chuẩn RFC 5545, DTEND cho All-Day event là ngày kế tiếp (exclusive)
            sb.append("DTEND;VALUE=DATE:").append(evt.getEndTime().toLocalDate().plusDays(1).format(ICS_DATE_FORMAT)).append("\r\n");
        } else {
            sb.append("DTSTART;TZID=Asia/Ho_Chi_Minh:").append(evt.getStartTime().format(ICS_DATETIME_FORMAT)).append("\r\n");
            sb.append("DTEND;TZID=Asia/Ho_Chi_Minh:").append(evt.getEndTime().format(ICS_DATETIME_FORMAT)).append("\r\n");
        }

        sb.append("SUMMARY:").append(escapeText(evt.getTitle())).append("\r\n");

        StringBuilder descBuilder = new StringBuilder();
        if (evt.getDescription() != null && !evt.getDescription().isEmpty()) {
            descBuilder.append(evt.getDescription()).append("\n");
        }
        if (evt.getMeetingUrl() != null && !evt.getMeetingUrl().isEmpty()) {
            descBuilder.append("Link phòng họp: ").append(evt.getMeetingUrl()).append("\n");
        }
        if (evt.getAssigneeFullName() != null && !evt.getAssigneeFullName().isEmpty()) {
            descBuilder.append("Người phụ trách: ").append(evt.getAssigneeFullName()).append("\n");
        }
        if (descBuilder.length() > 0) {
            sb.append("DESCRIPTION:").append(escapeText(descBuilder.toString().trim())).append("\r\n");
        }

        String location = evt.getLocation();
        if ((location == null || location.isEmpty()) && evt.getMeetingUrl() != null) {
            location = evt.getMeetingUrl();
        }
        if (location != null && !location.isEmpty()) {
            sb.append("LOCATION:").append(escapeText(location)).append("\r\n");
        }

        if (evt.getMeetingUrl() != null && !evt.getMeetingUrl().isEmpty()) {
            sb.append("URL:").append(evt.getMeetingUrl().trim()).append("\r\n");
        }

        sb.append("STATUS:").append(evt.getStatus() != null && evt.getStatus().name().equals("CANCELLED") ? "CANCELLED" : "CONFIRMED").append("\r\n");

        // Đặt nhắc hẹn VALARM nếu sự kiện có reminderMinutesBefore > 0
        if (evt.getReminderMinutesBefore() != null && evt.getReminderMinutesBefore() > 0) {
            sb.append("BEGIN:VALARM\r\n");
            sb.append("TRIGGER:-PT").append(evt.getReminderMinutesBefore()).append("M\r\n");
            sb.append("ACTION:DISPLAY\r\n");
            sb.append("DESCRIPTION:Nhắc hẹn ERP Cloud: ").append(escapeText(evt.getTitle())).append("\r\n");
            sb.append("END:VALARM\r\n");
        }

        sb.append("END:VEVENT\r\n");
    }

    private String escapeText(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}
