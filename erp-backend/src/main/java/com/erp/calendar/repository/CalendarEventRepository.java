package com.erp.calendar.repository;

import com.erp.calendar.model.CalendarEvent;
import com.erp.calendar.model.EventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /**
     * Tìm tất cả sự kiện giao thoa với khoảng thời gian [start, end]
     */
    @Query("SELECT e FROM CalendarEvent e WHERE e.startTime <= :endTime AND e.endTime >= :startTime ORDER BY e.startTime ASC")
    List<CalendarEvent> findEventsInRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Tìm các sự kiện sắp diễn ra tính từ thời điểm hiện tại
     */
    @Query("SELECT e FROM CalendarEvent e WHERE e.endTime >= :now ORDER BY e.startTime ASC")
    List<CalendarEvent> findUpcomingEvents(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    /**
     * Đếm sự kiện trong một khoảng thời gian
     */
    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Đếm sự kiện theo loại và khoảng thời gian
     */
    long countByEventTypeAndStartTimeBetween(EventType eventType, LocalDateTime start, LocalDateTime end);

    /**
     * Đếm sự kiện theo người tạo và loại
     */
    long countByCreatorUsernameAndEventType(String creatorUsername, EventType eventType);

    /**
     * Tìm các sự kiện cần kiểm tra gửi nhắc hẹn trong khung thời gian tới
     */
    @Query("SELECT e FROM CalendarEvent e WHERE (e.reminderSent = false OR e.reminderSent IS NULL) " +
           "AND e.status <> com.erp.calendar.model.EventStatus.COMPLETED " +
           "AND e.reminderMinutesBefore > 0 " +
           "AND e.startTime >= :windowStart AND e.startTime <= :windowEnd")
    List<CalendarEvent> findEventsPendingReminder(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );
}

