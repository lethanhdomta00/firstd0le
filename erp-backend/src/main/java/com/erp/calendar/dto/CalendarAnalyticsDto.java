package com.erp.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarAnalyticsDto {

    private long totalEvents;
    private long completedEvents;
    private long inProgressEvents;
    private long overdueEvents;
    private double overallCompletionRate;

    private List<BreakdownItem> byEventType;
    private List<BreakdownItem> byPriority;
    private List<AssigneeStat> assigneePerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownItem {
        private String key;
        private String label;
        private long count;
        private String colorHex;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssigneeStat {
        private String username;
        private String fullName;
        private long totalAssigned;
        private long completedCount;
        private long overdueCount;
        private double completionRate;
    }
}
