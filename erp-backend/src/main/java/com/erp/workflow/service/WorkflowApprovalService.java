package com.erp.workflow.service;

import com.erp.finance.model.FinanceVoucher;
import com.erp.finance.model.VoucherStatus;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.procurement.model.PurchaseOrder;
import com.erp.procurement.model.PurchaseOrderStatus;
import com.erp.procurement.repository.PurchaseOrderRepository;
import com.erp.workflow.dto.*;
import com.erp.workflow.model.*;
import com.erp.workflow.repository.ApprovalMatrixRuleRepository;
import com.erp.workflow.repository.ApprovalRequestRepository;
import com.erp.workflow.repository.ApprovalStepHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowApprovalService {

    private final ApprovalMatrixRuleRepository ruleRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepHistoryRepository stepHistoryRepository;
    private final AuditLogService auditLogService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final FinanceVoucherRepository financeVoucherRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi yêu cầu phê duyệt mới
     */
    @Transactional
    public ApprovalRequestDto submitForApproval(SubmitApprovalRequest request,
                                                String username,
                                                String fullName,
                                                String role,
                                                String ipAddress,
                                                String userAgent) {
        log.info("Khởi tạo yêu cầu phê duyệt: DocType={}, DocId={}, Amount={}",
                request.getDocumentType(), request.getDocumentId(), request.getAmount());

        // 1. Tìm các quy tắc ma trận phù hợp
        List<ApprovalMatrixRule> matchingRules = ruleRepository.findMatchingRules(
                request.getDocumentType(), request.getAmount());

        String requestCode = generateRequestCode();
        boolean autoApprove = false;

        // Nếu có ít nhất 1 quy tắc khớp và có đánh dấu autoApprove
        if (!matchingRules.isEmpty() && Boolean.TRUE.equals(matchingRules.get(0).getAutoApprove())) {
            autoApprove = true;
        }

        if (autoApprove) {
            // TỰ ĐỘNG PHÊ DUYỆT NGAY LẬP TỨC
            ApprovalRequest apr = ApprovalRequest.builder()
                    .requestCode(requestCode)
                    .documentType(request.getDocumentType())
                    .documentId(request.getDocumentId())
                    .documentCode(request.getDocumentCode())
                    .title(request.getTitle())
                    .amount(request.getAmount())
                    .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                    .currentLevel(1)
                    .totalLevels(1)
                    .status(ApprovalStatus.APPROVED)
                    .currentApproverRole("SYSTEM_AUTO")
                    .requesterUsername(username)
                    .requesterFullName(fullName)
                    .decisionReason("Tự động phê duyệt theo định mức ma trận quy tắc (< " + request.getAmount() + " VND)")
                    .payloadSnapshot(request.getPayloadSnapshot())
                    .build();

            ApprovalStepHistory step = ApprovalStepHistory.builder()
                    .stepLevel(1)
                    .approverUsername("SYSTEM")
                    .approverFullName("Hệ Thống Tự Động")
                    .approverRole("SYSTEM_AUTO")
                    .action("AUTO_APPROVE")
                    .comment("Tự động duyệt do giá trị chứng từ nằm trong ngưỡng tự động phê duyệt")
                    .build();
            apr.addStepHistory(step);

            ApprovalRequest saved = requestRepository.save(apr);

            // Cập nhật chứng từ gốc
            applyDocumentApproval(request.getDocumentType(), request.getDocumentId());

            // Ghi Audit Trail
            auditLogService.logAction(username, role, "AUTO_APPROVE",
                    request.getDocumentType().name(), request.getDocumentId(), request.getDocumentCode(),
                    null, "{\"status\":\"APPROVED\",\"auto\":true}",
                    "Tự động duyệt theo ma trận định mức", ipAddress, userAgent);

            // Phát thông báo Realtime STOMP
            broadcastApprovalUpdate(saved, "AUTO_APPROVED");

            return mapToDto(saved);
        }

        // CẦN DUYỆT THỦ CÔNG QUA CÁC CẤP
        int totalLevels = matchingRules.isEmpty() ? 1 : matchingRules.size();
        String initialApproverRole = !matchingRules.isEmpty()
                ? matchingRules.get(0).getApproverRole()
                : "ROLE_MANAGER";

        String oneTimeToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusHours(24);

        ApprovalRequest apr = ApprovalRequest.builder()
                .requestCode(requestCode)
                .documentType(request.getDocumentType())
                .documentId(request.getDocumentId())
                .documentCode(request.getDocumentCode())
                .title(request.getTitle())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .currentLevel(1)
                .totalLevels(totalLevels)
                .status(ApprovalStatus.PENDING)
                .currentApproverRole(initialApproverRole)
                .requesterUsername(username)
                .requesterFullName(fullName)
                .oneTimeToken(oneTimeToken)
                .tokenExpiresAt(tokenExpiresAt)
                .payloadSnapshot(request.getPayloadSnapshot())
                .build();

        ApprovalStepHistory step = ApprovalStepHistory.builder()
                .stepLevel(0)
                .approverUsername(username)
                .approverFullName(fullName)
                .approverRole(role)
                .action("SUBMIT")
                .comment("Khởi tạo và gửi yêu cầu phê duyệt cấp 1")
                .build();
        apr.addStepHistory(step);

        ApprovalRequest saved = requestRepository.save(apr);

        // Đánh dấu chứng từ gốc sang trạng thái PENDING_APPROVAL nếu có
        markDocumentPending(request.getDocumentType(), request.getDocumentId());

        // Ghi Audit Trail
        auditLogService.logAction(username, role, "SUBMIT_APPROVAL",
                request.getDocumentType().name(), request.getDocumentId(), request.getDocumentCode(),
                null, "{\"status\":\"PENDING\",\"requestCode\":\"" + requestCode + "\"}",
                "Gửi yêu cầu phê duyệt đa cấp", ipAddress, userAgent);

        // Bắn WebSocket Realtime
        broadcastApprovalUpdate(saved, "APPROVAL_SUBMITTED");

        return mapToDto(saved);
    }

    /**
     * Xử lý phê duyệt hoặc từ chối từ người có thẩm quyền
     */
    @Transactional
    public ApprovalRequestDto processDecision(Long requestId,
                                              ApprovalDecisionRequest decision,
                                              String username,
                                              String fullName,
                                              String role,
                                              String ipAddress,
                                              String userAgent) {
        ApprovalRequest apr = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu phê duyệt với ID: " + requestId));

        if (apr.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu phê duyệt này đã được xử lý (trạng thái hiện tại: " + apr.getStatus().getLabel() + ")");
        }

        // Kiểm tra thẩm quyền (Người duyệt phải có role phù hợp hoặc là ADMIN)
        boolean isAdmin = role != null && (role.contains("ADMIN") || role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN"));
        String cleanUserRole = role != null && !role.startsWith("ROLE_") ? "ROLE_" + role : role;
        String cleanApproverRole = apr.getCurrentApproverRole() != null && !apr.getCurrentApproverRole().startsWith("ROLE_")
                ? "ROLE_" + apr.getCurrentApproverRole() : apr.getCurrentApproverRole();

        if (!isAdmin && cleanApproverRole != null && !cleanApproverRole.equalsIgnoreCase(cleanUserRole)) {
            throw new SecurityException("Bạn không có quyền duyệt ở cấp độ này! Yêu cầu vai trò: " + apr.getCurrentApproverRole());
        }

        String action = decision.getAction().trim().toUpperCase();
        String comment = decision.getComment() != null ? decision.getComment().trim() : "";

        if ("REJECT".equals(action)) {
            // TỪ CHỐI DUYỆT
            apr.setStatus(ApprovalStatus.REJECTED);
            apr.setDecisionReason(comment.isEmpty() ? "Bị từ chối bởi " + fullName : comment);

            ApprovalStepHistory step = ApprovalStepHistory.builder()
                    .stepLevel(apr.getCurrentLevel())
                    .approverUsername(username)
                    .approverFullName(fullName)
                    .approverRole(role)
                    .action("REJECT")
                    .comment(comment)
                    .build();
            apr.addStepHistory(step);

            ApprovalRequest saved = requestRepository.save(apr);

            // Cập nhật chứng từ gốc bị từ chối
            applyDocumentRejection(apr.getDocumentType(), apr.getDocumentId(), comment);

            // Ghi Audit Trail
            auditLogService.logAction(username, role, "REJECT_APPROVAL",
                    apr.getDocumentType().name(), apr.getDocumentId(), apr.getDocumentCode(),
                    "{\"status\":\"PENDING\"}", "{\"status\":\"REJECTED\",\"reason\":\"" + comment + "\"}",
                    comment, ipAddress, userAgent);

            broadcastApprovalUpdate(saved, "REJECTED");
            return mapToDto(saved);
        } else if ("APPROVE".equals(action)) {
            // PHÊ DUYỆT CẤP HIỆN TẠI
            ApprovalStepHistory step = ApprovalStepHistory.builder()
                    .stepLevel(apr.getCurrentLevel())
                    .approverUsername(username)
                    .approverFullName(fullName)
                    .approverRole(role)
                    .action("APPROVE")
                    .comment(comment)
                    .build();
            apr.addStepHistory(step);

            // Kiểm tra xem có cần cấp tiếp theo không
            if (apr.getCurrentLevel() < apr.getTotalLevels()) {
                int nextLevel = apr.getCurrentLevel() + 1;
                apr.setCurrentLevel(nextLevel);

                // Tìm rule tương ứng với cấp kế tiếp
                List<ApprovalMatrixRule> rules = ruleRepository.findMatchingRules(apr.getDocumentType(), apr.getAmount());
                String nextRole = "ROLE_ADMIN";
                for (ApprovalMatrixRule r : rules) {
                    if (r.getLevel().equals(nextLevel)) {
                        nextRole = r.getApproverRole();
                        break;
                    }
                }
                apr.setCurrentApproverRole(nextRole);

                ApprovalRequest saved = requestRepository.save(apr);

                auditLogService.logAction(username, role, "APPROVE_STEP_" + (nextLevel - 1),
                        apr.getDocumentType().name(), apr.getDocumentId(), apr.getDocumentCode(),
                        "{\"level\":" + (nextLevel - 1) + "}", "{\"level\":" + nextLevel + ",\"nextRole\":\"" + nextRole + "\"}",
                        comment, ipAddress, userAgent);

                broadcastApprovalUpdate(saved, "STEP_APPROVED");
                return mapToDto(saved);
            } else {
                // ĐÃ HOÀN TẤT TẤT CẢ CÁC CẤP DUYỆT
                apr.setStatus(ApprovalStatus.APPROVED);
                apr.setDecisionReason(comment.isEmpty() ? "Đã phê duyệt hoàn tất bởi " + fullName : comment);

                ApprovalRequest saved = requestRepository.save(apr);

                // Cập nhật chứng từ gốc sang APPROVED
                applyDocumentApproval(apr.getDocumentType(), apr.getDocumentId());

                auditLogService.logAction(username, role, "APPROVE_FINAL",
                        apr.getDocumentType().name(), apr.getDocumentId(), apr.getDocumentCode(),
                        "{\"status\":\"PENDING\"}", "{\"status\":\"APPROVED\"}",
                        comment, ipAddress, userAgent);

                broadcastApprovalUpdate(saved, "FINAL_APPROVED");
                return mapToDto(saved);
            }
        } else {
            throw new IllegalArgumentException("Hành động không hợp lệ: " + action + " (chỉ chấp nhận APPROVE hoặc REJECT)");
        }
    }

    /**
     * Phê duyệt nhanh 1-click qua One-Time Token (dành cho Email / Telegram / Webhook)
     */
    @Transactional
    public ApprovalRequestDto processTokenDecision(String token, String action, String reason, String ipAddress, String userAgent) {
        ApprovalRequest apr = requestRepository.findByOneTimeToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Mã Token phê duyệt không tồn tại hoặc không hợp lệ!"));

        if (apr.getTokenExpiresAt() != null && apr.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Mã Token phê duyệt đã hết hạn hiệu lực (24h)! Vui lòng đăng nhập vào ERP để duyệt trực tiếp.");
        }

        if (apr.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu phê duyệt này đã hoàn tất trước đó với trạng thái: " + apr.getStatus().getLabel());
        }

        ApprovalDecisionRequest decision = ApprovalDecisionRequest.builder()
                .action(action)
                .comment(reason != null && !reason.isBlank() ? reason : "Phê duyệt 1-Click qua Token Link")
                .build();

        return processDecision(apr.getId(), decision, "token-user", "Người duyệt qua Token Link",
                apr.getCurrentApproverRole(), ipAddress, userAgent);
    }

    /**
     * Lấy danh sách yêu cầu phê duyệt có phân trang và bộ lọc
     */
    @Transactional(readOnly = true)
    public Page<ApprovalRequestDto> getApprovals(ApprovalStatus status,
                                                 ApprovalDocumentType docType,
                                                 String search,
                                                 int page,
                                                 int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApprovalRequest> requests = requestRepository.searchApprovals(status, docType, search, pageable);
        return requests.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestDto getApprovalById(Long id) {
        ApprovalRequest apr = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu phê duyệt với ID: " + id));
        return mapToDto(apr);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestDto.ApprovalStepHistoryDto> getStepHistories(Long approvalRequestId) {
        List<ApprovalStepHistory> steps = stepHistoryRepository.findByApprovalRequestIdOrderByStepLevelAscActionTimeAsc(approvalRequestId);
        return steps.stream().map(s -> ApprovalRequestDto.ApprovalStepHistoryDto.builder()
                .id(s.getId())
                .stepLevel(s.getStepLevel())
                .approverUsername(s.getApproverUsername())
                .approverFullName(s.getApproverFullName())
                .approverRole(s.getApproverRole())
                .action(s.getAction())
                .comment(s.getComment())
                .actionTime(s.getActionTime())
                .build()).toList();
    }

    /**
     * Thống kê Dashboard Phê duyệt
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getApprovalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        long pending = requestRepository.countByStatus(ApprovalStatus.PENDING);
        long approved = requestRepository.countByStatus(ApprovalStatus.APPROVED);
        long rejected = requestRepository.countByStatus(ApprovalStatus.REJECTED);
        long cancelled = requestRepository.countByStatus(ApprovalStatus.CANCELLED);
        long total = pending + approved + rejected + cancelled;

        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("cancelled", cancelled);
        stats.put("total", total);

        return stats;
    }

    // ==========================================
    // QUẢN TRỊ MA TRẬN QUY TẮC (RULES CRUD)
    // ==========================================

    @Transactional(readOnly = true)
    public List<ApprovalMatrixRuleDto> getAllRules() {
        return ruleRepository.findByActiveTrueOrderByDocumentTypeAscPriorityAscLevelAsc()
                .stream().map(this::mapRuleToDto).toList();
    }

    @Transactional
    public ApprovalMatrixRuleDto createOrUpdateRule(ApprovalMatrixRuleDto dto) {
        ApprovalMatrixRule rule;
        if (dto.getId() != null) {
            rule = ruleRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc ma trận với ID: " + dto.getId()));
        } else {
            rule = new ApprovalMatrixRule();
        }

        rule.setRuleName(dto.getRuleName());
        rule.setDocumentType(dto.getDocumentType());
        rule.setMinAmount(dto.getMinAmount());
        rule.setMaxAmount(dto.getMaxAmount());
        rule.setLevel(dto.getLevel());
        rule.setApproverRole(dto.getApproverRole());
        rule.setAutoApprove(Boolean.TRUE.equals(dto.getAutoApprove()));
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 1);
        rule.setActive(dto.getActive() != null ? dto.getActive() : true);

        ApprovalMatrixRule saved = ruleRepository.save(rule);
        return mapRuleToDto(saved);
    }

    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.findById(id).ifPresent(r -> {
            r.setActive(false);
            ruleRepository.save(r);
        });
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void markDocumentPending(ApprovalDocumentType type, Long docId) {
        try {
            if (type == ApprovalDocumentType.PURCHASE_ORDER) {
                purchaseOrderRepository.findById(docId).ifPresent(po -> {
                    po.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
                    purchaseOrderRepository.save(po);
                });
            }
        } catch (Exception e) {
            log.warn("Không thể cập nhật trạng thái PENDING cho chứng từ {} #{}: {}", type, docId, e.getMessage());
        }
    }

    private void applyDocumentApproval(ApprovalDocumentType type, Long docId) {
        try {
            if (type == ApprovalDocumentType.PURCHASE_ORDER) {
                purchaseOrderRepository.findById(docId).ifPresent(po -> {
                    po.setStatus(PurchaseOrderStatus.APPROVED);
                    purchaseOrderRepository.save(po);
                    log.info("PurchaseOrder #{} đã được cập nhật sang APPROVED thành công!", docId);
                });
            } else if (type == ApprovalDocumentType.PAYMENT_VOUCHER) {
                financeVoucherRepository.findById(docId).ifPresent(v -> {
                    v.setStatus(VoucherStatus.APPROVED);
                    financeVoucherRepository.save(v);
                    log.info("FinanceVoucher #{} đã được cập nhật sang APPROVED thành công!", docId);
                });
            }
        } catch (Exception e) {
            log.error("Lỗi khi áp dụng phê duyệt cho chứng từ {} #{}: {}", type, docId, e.getMessage());
        }
    }

    private void applyDocumentRejection(ApprovalDocumentType type, Long docId, String reason) {
        try {
            if (type == ApprovalDocumentType.PURCHASE_ORDER) {
                purchaseOrderRepository.findById(docId).ifPresent(po -> {
                    po.setStatus(PurchaseOrderStatus.CANCELLED);
                    purchaseOrderRepository.save(po);
                });
            } else if (type == ApprovalDocumentType.PAYMENT_VOUCHER) {
                financeVoucherRepository.findById(docId).ifPresent(v -> {
                    v.setStatus(VoucherStatus.CANCELLED);
                    financeVoucherRepository.save(v);
                });
            }
        } catch (Exception e) {
            log.error("Lỗi khi áp dụng từ chối cho chứng từ {} #{}: {}", type, docId, e.getMessage());
        }
    }

    private void broadcastApprovalUpdate(ApprovalRequest apr, String eventType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("requestId", apr.getId());
            payload.put("requestCode", apr.getRequestCode());
            payload.put("documentType", apr.getDocumentType().name());
            payload.put("documentCode", apr.getDocumentCode());
            payload.put("title", apr.getTitle());
            payload.put("amount", apr.getAmount());
            payload.put("status", apr.getStatus().name());
            payload.put("currentLevel", apr.getCurrentLevel());
            payload.put("totalLevels", apr.getTotalLevels());
            payload.put("currentApproverRole", apr.getCurrentApproverRole());
            payload.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/approvals", (Object) payload);
            log.info("[REALTIME-STOMP] Đã phát sóng sự kiện /topic/approvals: {} cho {}", eventType, apr.getRequestCode());
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo WebSocket /topic/approvals: {}", e.getMessage());
        }
    }

    private String generateRequestCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "APR-" + dateStr + "-" + randomSuffix;
    }

    public ApprovalRequestDto mapToDto(ApprovalRequest apr) {
        List<ApprovalRequestDto.ApprovalStepHistoryDto> steps = apr.getStepHistories() != null
                ? apr.getStepHistories().stream().map(s -> ApprovalRequestDto.ApprovalStepHistoryDto.builder()
                .id(s.getId())
                .stepLevel(s.getStepLevel())
                .approverUsername(s.getApproverUsername())
                .approverFullName(s.getApproverFullName())
                .approverRole(s.getApproverRole())
                .action(s.getAction())
                .comment(s.getComment())
                .actionTime(s.getActionTime())
                .build()).toList()
                : Collections.emptyList();

        return ApprovalRequestDto.builder()
                .id(apr.getId())
                .requestCode(apr.getRequestCode())
                .documentType(apr.getDocumentType())
                .documentTypeLabel(apr.getDocumentType() != null ? apr.getDocumentType().getLabel() : "")
                .documentId(apr.getDocumentId())
                .documentCode(apr.getDocumentCode())
                .title(apr.getTitle())
                .amount(apr.getAmount())
                .currency(apr.getCurrency())
                .currentLevel(apr.getCurrentLevel())
                .totalLevels(apr.getTotalLevels())
                .status(apr.getStatus())
                .statusLabel(apr.getStatus() != null ? apr.getStatus().getLabel() : "")
                .currentApproverRole(apr.getCurrentApproverRole())
                .requesterUsername(apr.getRequesterUsername())
                .requesterFullName(apr.getRequesterFullName())
                .oneTimeToken(apr.getOneTimeToken())
                .tokenExpiresAt(apr.getTokenExpiresAt())
                .decisionReason(apr.getDecisionReason())
                .payloadSnapshot(apr.getPayloadSnapshot())
                .createdAt(apr.getCreatedAt())
                .updatedAt(apr.getUpdatedAt())
                .stepHistories(steps)
                .build();
    }

    public ApprovalMatrixRuleDto mapRuleToDto(ApprovalMatrixRule rule) {
        return ApprovalMatrixRuleDto.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .documentType(rule.getDocumentType())
                .minAmount(rule.getMinAmount())
                .maxAmount(rule.getMaxAmount())
                .level(rule.getLevel())
                .approverRole(rule.getApproverRole())
                .autoApprove(rule.getAutoApprove())
                .priority(rule.getPriority())
                .active(rule.getActive())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
