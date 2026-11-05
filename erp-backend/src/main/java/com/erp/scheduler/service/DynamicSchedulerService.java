package com.erp.scheduler.service;

import com.erp.scheduler.dto.CreateJobRequest;
import com.erp.scheduler.dto.ScheduledJobDto;
import com.erp.scheduler.dto.UpdateJobRequest;
import com.erp.scheduler.model.ExecutionStatus;
import com.erp.scheduler.model.JobExecutionLog;
import com.erp.scheduler.model.JobStatus;
import com.erp.scheduler.model.ScheduledJob;
import com.erp.scheduler.repository.JobExecutionLogRepository;
import com.erp.scheduler.repository.ScheduledJobRepository;
import com.erp.scheduler.task.ScheduledTaskHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class DynamicSchedulerService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final ScheduledJobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final Map<String, ScheduledTaskHandler> taskHandlerMap = new HashMap<>();

    // Map lưu trữ các ScheduledFuture đang chạy trong RAM
    private final Map<String, ScheduledFuture<?>> activeScheduledFutures = new ConcurrentHashMap<>();

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public DynamicSchedulerService(
            @Qualifier("erpTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            ScheduledJobRepository jobRepository,
            JobExecutionLogRepository logRepository,
            List<ScheduledTaskHandler> handlers
    ) {
        this.taskScheduler = taskScheduler;
        this.jobRepository = jobRepository;
        this.logRepository = logRepository;
        for (ScheduledTaskHandler handler : handlers) {
            this.taskHandlerMap.put(handler.getJobType(), handler);
            log.info("Đã đăng ký Task Handler: [{}] - {}", handler.getJobType(), handler.getDisplayName());
        }
    }

    @PostConstruct
    public void init() {
        log.info("Khởi động Dynamic Scheduler Engine...");
        List<ScheduledJob> activeJobs = jobRepository.findByStatus(JobStatus.ACTIVE);
        for (ScheduledJob job : activeJobs) {
            try {
                scheduleJobInRuntime(job);
            } catch (Exception e) {
                log.error("Không thể lập lịch cho job [{}] lúc khởi động: {}", job.getJobCode(), e.getMessage());
            }
        }
        log.info("Dynamic Scheduler Engine đã kích hoạt thành công {} tác vụ tự động.", activeScheduledFutures.size());
    }

    /**
     * Lập lịch tác vụ vào bộ nhớ RAM của Spring TaskScheduler
     */
    private synchronized void scheduleJobInRuntime(ScheduledJob job) {
        cancelScheduledFuture(job.getJobCode());

        if (job.getStatus() != JobStatus.ACTIVE) {
            log.info("Job [{}] đang ở trạng thái PAUSED, không nạp vào Scheduler.", job.getJobCode());
            return;
        }

        if (!isValidCron(job.getCronExpression())) {
            log.error("Biểu thức Cron không hợp lệ cho job [{}]: {}", job.getJobCode(), job.getCronExpression());
            return;
        }

        CronTrigger trigger = new CronTrigger(job.getCronExpression(), VIETNAM_ZONE);
        Runnable task = () -> executeTaskWrapped(job.getId(), "SYSTEM_CRON");

        ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
        activeScheduledFutures.put(job.getJobCode(), future);

        // Tính toán và cập nhật nextRunAt
        updateNextRunTime(job);
        log.info("Đã lập lịch thành công cho job [{}] với cron [{}]", job.getJobCode(), job.getCronExpression());
    }

    /**
     * Hủy tác vụ trong bộ nhớ RAM
     */
    private synchronized void cancelScheduledFuture(String jobCode) {
        ScheduledFuture<?> existing = activeScheduledFutures.remove(jobCode);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel(false);
            log.info("Đã hủy ScheduledFuture trong RAM cho job: [{}]", jobCode);
        }
    }

    /**
     * Cập nhật thời điểm chạy kế tiếp theo Cron
     */
    private void updateNextRunTime(ScheduledJob job) {
        try {
            CronExpression cron = CronExpression.parse(job.getCronExpression());
            ZonedDateTime next = cron.next(ZonedDateTime.now(VIETNAM_ZONE));
            if (next != null) {
                job.setNextRunAt(next.toLocalDateTime());
                jobRepository.save(job);
            }
        } catch (Exception e) {
            log.warn("Không thể tính nextRunAt cho job [{}]: {}", job.getJobCode(), e.getMessage());
        }
    }

    /**
     * Wrapper thực thi tác vụ: Đo thời gian, bắt lỗi và ghi nhật ký JobExecutionLog
     */
    @Transactional
    public String executeTaskWrapped(Long jobId, String triggeredBy) {
        Optional<ScheduledJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            log.warn("Không tìm thấy job ID [{}] để thực thi!", jobId);
            return "Job không tồn tại";
        }

        ScheduledJob job = jobOpt.get();
        ScheduledTaskHandler handler = taskHandlerMap.get(job.getJobType());
        if (handler == null) {
            String err = "Không tìm thấy Handler xử lý cho jobType: " + job.getJobType();
            log.error(err);
            saveFailedLog(job, triggeredBy, err, null);
            return err;
        }

        LocalDateTime startTime = LocalDateTime.now(VIETNAM_ZONE);
        long startMs = System.currentTimeMillis();

        JobExecutionLog execLog = JobExecutionLog.builder()
                .jobId(job.getId())
                .jobCode(job.getJobCode())
                .jobName(job.getJobName())
                .startTime(startTime)
                .status(ExecutionStatus.RUNNING)
                .triggeredBy(triggeredBy)
                .paramsJson(job.getParamsJson())
                .build();
        execLog = logRepository.save(execLog);

        try {
            log.info("BẮT ĐẦU CHẠY JOB: [{}] ({}) - Kích hoạt bởi: {}", job.getJobCode(), job.getJobName(), triggeredBy);
            String summary = handler.execute(job.getParamsJson(), triggeredBy);

            long duration = System.currentTimeMillis() - startMs;
            LocalDateTime endTime = LocalDateTime.now(VIETNAM_ZONE);

            execLog.setEndTime(endTime);
            execLog.setDurationMs(duration);
            execLog.setStatus(ExecutionStatus.SUCCESS);
            execLog.setResultSummary(summary);
            logRepository.save(execLog);

            job.setLastRunAt(startTime);
            job.setLastRunStatus(ExecutionStatus.SUCCESS);
            job.setLastRunDurationMs(duration);
            updateNextRunTime(job);

            log.info("HOÀN TẤT JOB: [{}] trong {}ms. Kết quả: {}", job.getJobCode(), duration, summary);
            return summary;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startMs;
            LocalDateTime endTime = LocalDateTime.now(VIETNAM_ZONE);

            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            execLog.setEndTime(endTime);
            execLog.setDurationMs(duration);
            execLog.setStatus(ExecutionStatus.FAILED);
            execLog.setErrorLog(t.getMessage() + "\n" + stackTrace);
            logRepository.save(execLog);

            job.setLastRunAt(startTime);
            job.setLastRunStatus(ExecutionStatus.FAILED);
            job.setLastRunDurationMs(duration);
            updateNextRunTime(job);

            log.error("LỖI KHI CHẠY JOB: [{}] sau {}ms: {}", job.getJobCode(), duration, t.getMessage());
            return "Lỗi: " + t.getMessage();
        }
    }

    private void saveFailedLog(ScheduledJob job, String triggeredBy, String errorMsg, String stackTrace) {
        JobExecutionLog execLog = JobExecutionLog.builder()
                .jobId(job.getId())
                .jobCode(job.getJobCode())
                .jobName(job.getJobName())
                .startTime(LocalDateTime.now(VIETNAM_ZONE))
                .endTime(LocalDateTime.now(VIETNAM_ZONE))
                .durationMs(0L)
                .status(ExecutionStatus.FAILED)
                .triggeredBy(triggeredBy)
                .paramsJson(job.getParamsJson())
                .errorLog(errorMsg + (stackTrace != null ? "\n" + stackTrace : ""))
                .build();
        logRepository.save(execLog);
    }

    // =========================================================================
    //                        CÁC THAO TÁC QUẢN TRỊ JOB
    // =========================================================================

    public List<ScheduledJobDto> getAllJobs() {
        return jobRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ScheduledJobDto::fromEntity)
                .toList();
    }

    public ScheduledJobDto getJobById(Long id) {
        return jobRepository.findById(id)
                .map(ScheduledJobDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác vụ với ID: " + id));
    }

    @Transactional
    public ScheduledJobDto createJob(CreateJobRequest request) {
        if (jobRepository.existsByJobCode(request.getJobCode().trim())) {
            throw new RuntimeException("Mã jobCode đã tồn tại: " + request.getJobCode());
        }
        if (!isValidCron(request.getCronExpression().trim())) {
            throw new RuntimeException("Biểu thức Cron không hợp lệ: " + request.getCronExpression());
        }

        ScheduledJob job = ScheduledJob.builder()
                .jobCode(request.getJobCode().trim().toUpperCase())
                .jobName(request.getJobName().trim())
                .jobGroup(request.getJobGroup() != null ? request.getJobGroup().trim().toUpperCase() : "GENERAL")
                .description(request.getDescription())
                .cronExpression(request.getCronExpression().trim())
                .jobType(request.getJobType().trim())
                .paramsJson(request.getParamsJson())
                .status(JobStatus.ACTIVE)
                .build();

        job = jobRepository.save(job);
        scheduleJobInRuntime(job);
        return ScheduledJobDto.fromEntity(job);
    }

    @Transactional
    public ScheduledJobDto updateJob(Long id, UpdateJobRequest request) {
        ScheduledJob job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác vụ với ID: " + id));

        if (request.getJobName() != null) job.setJobName(request.getJobName().trim());
        if (request.getJobGroup() != null) job.setJobGroup(request.getJobGroup().trim().toUpperCase());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getParamsJson() != null) job.setParamsJson(request.getParamsJson());

        if (request.getCronExpression() != null && !request.getCronExpression().trim().equals(job.getCronExpression())) {
            String newCron = request.getCronExpression().trim();
            if (!isValidCron(newCron)) {
                throw new RuntimeException("Biểu thức Cron mới không hợp lệ: " + newCron);
            }
            job.setCronExpression(newCron);
        }

        job = jobRepository.save(job);
        scheduleJobInRuntime(job);
        return ScheduledJobDto.fromEntity(job);
    }

    @Transactional
    public ScheduledJobDto pauseJob(Long id) {
        ScheduledJob job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác vụ với ID: " + id));

        job.setStatus(JobStatus.PAUSED);
        job.setNextRunAt(null);
        job = jobRepository.save(job);

        cancelScheduledFuture(job.getJobCode());
        log.info("Đã tạm dừng (PAUSE) tác vụ: [{}]", job.getJobCode());
        return ScheduledJobDto.fromEntity(job);
    }

    @Transactional
    public ScheduledJobDto resumeJob(Long id) {
        ScheduledJob job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác vụ với ID: " + id));

        job.setStatus(JobStatus.ACTIVE);
        job = jobRepository.save(job);

        scheduleJobInRuntime(job);
        log.info("Đã kích hoạt lại (RESUME) tác vụ: [{}]", job.getJobCode());
        return ScheduledJobDto.fromEntity(job);
    }

    /**
     * Kích hoạt chạy ngay lập tức (Trigger Now) mà không ảnh hưởng tới lịch Cron
     */
    public String triggerNow(Long id, String triggeredBy) {
        ScheduledJob job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác vụ với ID: " + id));

        // Chạy bất đồng bộ trong ThreadPool để không block request HTTP
        taskScheduler.execute(() -> executeTaskWrapped(job.getId(), triggeredBy != null ? triggeredBy : "MANUAL"));
        return "Đã phát lệnh kích hoạt chạy tác vụ [" + job.getJobCode() + "] thành công.";
    }

    @Transactional
    public void deleteJob(Long id) {
        ScheduledJob job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác vụ với ID: " + id));

        cancelScheduledFuture(job.getJobCode());
        jobRepository.delete(job);
        log.info("Đã xóa vĩnh viễn tác vụ: [{}]", job.getJobCode());
    }

    public List<Map<String, String>> getAvailableJobTypes() {
        return taskHandlerMap.values().stream()
                .map(h -> Map.of(
                        "jobType", h.getJobType(),
                        "displayName", h.getDisplayName()
                ))
                .toList();
    }

    public boolean isValidCron(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

