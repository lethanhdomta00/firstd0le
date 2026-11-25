package com.erp.workflow.controller;

import com.erp.workflow.dto.*;
import com.erp.workflow.model.ApprovalDocumentType;
import com.erp.workflow.model.ApprovalStatus;
import com.erp.workflow.service.AuditLogService;
import com.erp.workflow.service.WorkflowApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowApprovalController {

    private final WorkflowApprovalService workflowService;
    private final AuditLogService auditLogService;

    /**
     * Danh sách yêu cầu phê duyệt (có lọc và phân trang)
     */
    @GetMapping("/approvals")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES')")
    public ResponseEntity<Page<ApprovalRequestDto>> getApprovals(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) ApprovalDocumentType docType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(workflowService.getApprovals(status, docType, search, page, size));
    }

    /**
     * Chi tiết yêu cầu phê duyệt
     */
    @GetMapping("/approvals/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES')")
    public ResponseEntity<ApprovalRequestDto> getApprovalById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getApprovalById(id));
    }

    /**
     * Lịch sử các bước duyệt
     */
    @GetMapping("/approvals/{id}/steps")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES')")
    public ResponseEntity<List<ApprovalRequestDto.ApprovalStepHistoryDto>> getStepHistories(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getStepHistories(id));
    }

    /**
     * Gửi yêu cầu phê duyệt mới
     */
    @PostMapping("/approvals/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES')")
    public ResponseEntity<ApprovalRequestDto> submitApproval(
            @Valid @RequestBody SubmitApprovalRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        String role = extractPrimaryRole(authentication);
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        ApprovalRequestDto result = workflowService.submitForApproval(
                request, username, username, role, ipAddress, userAgent);
        return ResponseEntity.ok(result);
    }

    /**
     * Quyết định phê duyệt hoặc từ chối (In-App)
     */
    @PostMapping("/approvals/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApprovalRequestDto> processDecision(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionRequest decision,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        String role = extractPrimaryRole(authentication);
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        ApprovalRequestDto result = workflowService.processDecision(
                id, decision, username, username, role, ipAddress, userAgent);
        return ResponseEntity.ok(result);
    }

    /**
     * Phê duyệt nhanh 1-Click qua One-Time Token (Public endpoint từ Email / Telegram webhook)
     */
    @GetMapping("/approvals/quick-token-decision")
    public ResponseEntity<?> quickTokenDecision(
            @RequestParam String token,
            @RequestParam String action,
            @RequestParam(required = false, defaultValue = "") String reason,
            HttpServletRequest servletRequest) {
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");

        try {
            ApprovalRequestDto result = workflowService.processTokenDecision(token, action, reason, ipAddress, userAgent);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xử lý thành công! Trạng thái yêu cầu: " + result.getStatusLabel(),
                    "data", result
            ));
        } catch (Exception e) {
            log.error("Lỗi khi xử lý quick token decision: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Thống kê Dashboard Phê duyệt
     */
    @GetMapping("/approvals/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'WAREHOUSE', 'SALES')")
    public ResponseEntity<Map<String, Object>> getApprovalStats() {
        return ResponseEntity.ok(workflowService.getApprovalStatistics());
    }

    // ==========================================
    // MA TRẬN QUY TẮC PHÊ DUYỆT (RULES)
    // ==========================================

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ApprovalMatrixRuleDto>> getAllRules() {
        return ResponseEntity.ok(workflowService.getAllRules());
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalMatrixRuleDto> createOrUpdateRule(@Valid @RequestBody ApprovalMatrixRuleDto dto) {
        return ResponseEntity.ok(workflowService.createOrUpdateRule(dto));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRule(@PathVariable Long id) {
        workflowService.deleteRule(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa quy tắc phê duyệt thành công"));
    }

    // ==========================================
    // SỔ NHẬT KÝ KIỂM TOÁN BẤT BIẾN (AUDIT LOGS)
    // ==========================================

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.searchAuditLogs(entityType, username, action, fromDate, toDate, page, size));
    }

    @GetMapping("/audit-logs/entity")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<AuditLogDto>> getEntityAuditLogs(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        return ResponseEntity.ok(auditLogService.getAuditLogsForEntity(entityType, entityId));
    }

    private String extractPrimaryRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            return "ROLE_USER";
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            if (role.equals("ROLE_ADMIN") || role.equals("ADMIN")) return "ROLE_ADMIN";
            if (role.equals("ROLE_MANAGER") || role.equals("MANAGER")) return "ROLE_MANAGER";
            if (role.equals("ROLE_ACCOUNTANT") || role.equals("ACCOUNTANT")) return "ROLE_ACCOUNTANT";
        }
        return auth.getAuthorities().iterator().next().getAuthority();
    }
}
