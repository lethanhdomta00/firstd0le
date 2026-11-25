package com.erp.workflow.service;

import com.erp.workflow.dto.AuditLogDto;
import com.erp.workflow.model.ImmutableAuditLog;
import com.erp.workflow.repository.ImmutableAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final ImmutableAuditLogRepository auditLogRepository;

    /**
     * Ghi nhật ký kiểm toán bất biến (Append-Only)
     * Sử dụng Propagation.REQUIRED để đảm bảo nằm chung giao dịch nghiệp vụ hoặc độc lập
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public ImmutableAuditLog logAction(String username,
                                       String userRole,
                                       String action,
                                       String entityType,
                                       Long entityId,
                                       String entityCode,
                                       String oldValueJson,
                                       String newValueJson,
                                       String reason,
                                       String ipAddress,
                                       String userAgent) {
        String traceId = "TRC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ImmutableAuditLog logEntry = ImmutableAuditLog.builder()
                .traceId(traceId)
                .username(username != null ? username : "system")
                .userRole(userRole != null ? userRole : "SYSTEM")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityCode(entityCode)
                .oldValueJson(oldValueJson)
                .newValueJson(newValueJson)
                .reason(reason)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .userAgent(userAgent != null ? userAgent : "ERP Cloud Internal")
                .build();

        ImmutableAuditLog saved = auditLogRepository.save(logEntry);
        log.info("[AUDIT-TRAIL] Trace: {} | User: {} ({}) | Action: {} | Entity: {} #{} | Code: {}",
                traceId, username, userRole, action, entityType, entityId, entityCode);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> searchAuditLogs(String entityType,
                                             String username,
                                             String action,
                                             LocalDateTime fromDate,
                                             LocalDateTime toDate,
                                             int page,
                                             int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ImmutableAuditLog> logs = auditLogRepository.searchAuditLogs(entityType, username, action, fromDate, toDate, pageable);
        return logs.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogsForEntity(String entityType, Long entityId) {
        List<ImmutableAuditLog> logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
        return logs.stream().map(this::mapToDto).toList();
    }

    public AuditLogDto mapToDto(ImmutableAuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .traceId(log.getTraceId())
                .username(log.getUsername())
                .userRole(log.getUserRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityCode(log.getEntityCode())
                .oldValueJson(log.getOldValueJson())
                .newValueJson(log.getNewValueJson())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .reason(log.getReason())
                .timestamp(log.getTimestamp())
                .build();
    }
}
