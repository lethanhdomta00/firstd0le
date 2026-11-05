package com.erp.scheduler.dto;

import com.erp.scheduler.model.ExecutionStatus;
import com.erp.scheduler.model.JobStatus;
import com.erp.scheduler.model.ScheduledJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobDto {
    private Long id;
    private String jobCode;
    private String jobName;
    private String jobGroup;
    private String description;
    private String cronExpression;
    private JobStatus status;
    private String jobType;
    private String paramsJson;
    private String lastRunAt;
    private String nextRunAt;
    private ExecutionStatus lastRunStatus;
    private Long lastRunDurationMs;
    private String createdAt;
    private String updatedAt;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+07:00");

    public static ScheduledJobDto fromEntity(ScheduledJob entity) {
        if (entity == null) return null;
        return ScheduledJobDto.builder()
                .id(entity.getId())
                .jobCode(entity.getJobCode())
                .jobName(entity.getJobName())
                .jobGroup(entity.getJobGroup())
                .description(entity.getDescription())
                .cronExpression(entity.getCronExpression())
                .status(entity.getStatus())
                .jobType(entity.getJobType())
                .paramsJson(entity.getParamsJson())
                .lastRunAt(entity.getLastRunAt() != null ? entity.getLastRunAt().format(ISO_FORMATTER) : null)
                .nextRunAt(entity.getNextRunAt() != null ? entity.getNextRunAt().format(ISO_FORMATTER) : null)
                .lastRunStatus(entity.getLastRunStatus())
                .lastRunDurationMs(entity.getLastRunDurationMs())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(ISO_FORMATTER) : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(ISO_FORMATTER) : null)
                .build();
    }
}

