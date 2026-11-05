package com.erp.scheduler.controller;

import com.erp.scheduler.dto.CreateJobRequest;
import com.erp.scheduler.dto.JobExecutionLogDto;
import com.erp.scheduler.dto.ScheduledJobDto;
import com.erp.scheduler.dto.UpdateJobRequest;
import com.erp.scheduler.model.JobExecutionLog;
import com.erp.scheduler.repository.JobExecutionLogRepository;
import com.erp.scheduler.service.DynamicSchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
@Slf4j
public class SchedulerController {

    private final DynamicSchedulerService schedulerService;
    private final JobExecutionLogRepository logRepository;

    /**
     * 1. Lấy danh sách toàn bộ các tác vụ tự động
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<ScheduledJobDto>> getAllJobs() {
        return ResponseEntity.ok(schedulerService.getAllJobs());
    }

    /**
     * 2. Lấy chi tiết một tác vụ
     */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<ScheduledJobDto> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(schedulerService.getJobById(id));
    }

    /**
     * 3. Tạo tác vụ tự động mới
     */
    @PostMapping("/jobs")
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobRequest request) {
        try {
            ScheduledJobDto created = schedulerService.createJob(request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 4. Cập nhật tác vụ / Đổi lịch Cron
     */
    @PutMapping("/jobs/{id}")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody UpdateJobRequest request) {
        try {
            ScheduledJobDto updated = schedulerService.updateJob(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 5. Tạm dừng tác vụ (PAUSE)
     */
    @PostMapping("/jobs/{id}/pause")
    public ResponseEntity<?> pauseJob(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(schedulerService.pauseJob(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 6. Kích hoạt lại tác vụ (RESUME)
     */
    @PostMapping("/jobs/{id}/resume")
    public ResponseEntity<?> resumeJob(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(schedulerService.resumeJob(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 7. Chạy ngay lập tức (TRIGGER NOW)
     */
    @PostMapping("/jobs/{id}/trigger")
    public ResponseEntity<?> triggerNow(@PathVariable Long id, Authentication authentication) {
        try {
            String triggeredBy = authentication != null ? authentication.getName() : "ADMIN_MANUAL";
            String result = schedulerService.triggerNow(id, triggeredBy);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 8. Xóa vĩnh viễn một tác vụ
     */
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id) {
        try {
            schedulerService.deleteJob(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa tác vụ thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 9. Lấy danh sách các loại Task Handler có sẵn trong hệ thống
     */
    @GetMapping("/job-types")
    public ResponseEntity<List<Map<String, String>>> getAvailableJobTypes() {
        return ResponseEntity.ok(schedulerService.getAvailableJobTypes());
    }

    /**
     * 10. Lấy danh sách nhật ký thực thi (Execution Logs) phân trang
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getExecutionLogs(
            @RequestParam(required = false) String jobCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<JobExecutionLog> logPage;

        if (jobCode != null && !jobCode.trim().isEmpty()) {
            logPage = logRepository.findByJobCodeOrderByStartTimeDesc(jobCode.trim(), pageable);
        } else {
            logPage = logRepository.findAllByOrderByStartTimeDesc(pageable);
        }

        Page<JobExecutionLogDto> dtoPage = logPage.map(JobExecutionLogDto::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }
}

