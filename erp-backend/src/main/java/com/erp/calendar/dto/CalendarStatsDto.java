package com.erp.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarStatsDto {
    private long todayEventsCount;
    private long thisWeekEventsCount;
    private long myMeetingsCount;
    private long pendingDeadlinesCount;
}

