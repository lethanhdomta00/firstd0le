package com.erp.scheduler.dto;

import com.erp.scheduler.model.ExecutionStatus;
import com.erp.scheduler.model.JobExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionLogDto {
    private Long id;
    private Long jobId;
    private String jobCode;
    private String jobName;
    private String startTime;
    private String endTime;
    private Long durationMs;
    private ExecutionStatus status;
    private String triggeredBy;
    private String paramsJson;
    private String resultSummary;
    private String errorLog;
    private String createdAt;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss+07:00");

    public static JobExecutionLogDto fromEntity(JobExecutionLog entity) {
        if (entity == null) return null;
        return JobExecutionLogDto.builder()
                .id(entity.getId())
                .jobId(entity.getJobId())
                .jobCode(entity.getJobCode())
                .jobName(entity.getJobName())
                .startTime(entity.getStartTime() != null ? entity.getStartTime().format(ISO_FORMATTER) : null)
                .endTime(entity.getEndTime() != null ? entity.getEndTime().format(ISO_FORMATTER) : null)
                .durationMs(entity.getDurationMs())
                .status(entity.getStatus())
                .triggeredBy(entity.getTriggeredBy())
                .paramsJson(entity.getParamsJson())
                .resultSummary(entity.getResultSummary())
                .errorLog(entity.getErrorLog())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(ISO_FORMATTER) : null)
                .build();
    }
}

