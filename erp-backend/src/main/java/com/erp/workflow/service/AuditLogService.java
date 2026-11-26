package com.erp.workflow.service;

import com.erp.workflow.dto.AuditLogDto;
import com.erp.workflow.model.ImmutableAuditLog;
import com.erp.workflow.repository.ImmutableAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    /**
     * Ghi nhật ký an toàn trong giao dịch riêng biệt (REQUIRES_NEW)
     * Đảm bảo không làm lỗi luồng nghiệp vụ chính nếu có sự cố ghi log
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActionSafe(String username,
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
        try {
            logAction(username, userRole, action, entityType, entityId, entityCode,
                    oldValueJson, newValueJson, reason, ipAddress, userAgent);
        } catch (Exception ex) {
            log.warn("[AUDIT-SAFE] Không thể ghi nhận log thao tác ({} - {}): {}", action, entityType, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> searchAuditLogsAdvanced(String keyword,
                                                     String entityType,
                                                     String username,
                                                     String action,
                                                     LocalDateTime fromDate,
                                                     LocalDateTime toDate,
                                                     int page,
                                                     int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        String normKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String normEntityType = (entityType != null && !entityType.isBlank()) ? entityType.trim() : null;
        String normUsername = (username != null && !username.isBlank()) ? username.trim() : null;
        String normAction = (action != null && !action.isBlank()) ? action.trim() : null;

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ImmutableAuditLog> logs = auditLogRepository.searchAuditLogsAdvanced(
                normKeyword, normEntityType, normUsername, normAction, fromDate, toDate, pageable);
        return logs.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public AuditLogDto getAuditLogById(Long id) {
        ImmutableAuditLog entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi nhật ký #" + id));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAuditStatistics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        long totalToday = auditLogRepository.countLogsSince(startOfToday);
        long riskyToday = auditLogRepository.countRiskyLogsSince(startOfToday,
                List.of("DELETE", "REJECT", "CANCEL", "LOGIN_FAILED", "LOCK"));
        long activeUsersToday = auditLogRepository.countDistinctUsersSince(startOfToday);
        long totalAll = auditLogRepository.count();

        List<Object[]> byEntity = auditLogRepository.countByEntityTypeSince(last7Days);
        List<Map<String, Object>> entityBreakdown = new ArrayList<>();
        String topEntity = "N/A";
        long topEntityCount = 0;
        for (Object[] row : byEntity) {
            String type = String.valueOf(row[0]);
            long cnt = ((Number) row[1]).longValue();
            if (cnt > topEntityCount) {
                topEntityCount = cnt;
                topEntity = type;
            }
            entityBreakdown.add(Map.of("entityType", type, "count", cnt));
        }

        List<Object[]> byAction = auditLogRepository.countByActionSince(last7Days);
        List<Map<String, Object>> actionBreakdown = new ArrayList<>();
        for (Object[] row : byAction) {
            actionBreakdown.add(Map.of(
                    "action", String.valueOf(row[0]),
                    "count", ((Number) row[1]).longValue()
            ));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalToday", totalToday);
        stats.put("riskyToday", riskyToday);
        stats.put("activeUsersToday", activeUsersToday);
        stats.put("totalAll", totalAll);
        stats.put("topEntity", topEntity);
        stats.put("topEntityCount", topEntityCount);
        stats.put("entityBreakdown", entityBreakdown);
        stats.put("actionBreakdown", actionBreakdown);
        return stats;
    }

    @Transactional(readOnly = true)
    public byte[] exportAuditLogsToExcel(String keyword,
                                         String entityType,
                                         String username,
                                         String action,
                                         LocalDateTime fromDate,
                                         LocalDateTime toDate) throws IOException {
        Pageable pageable = PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<ImmutableAuditLog> logs = auditLogRepository.searchAuditLogsAdvanced(
                (keyword != null && !keyword.isBlank()) ? keyword.trim() : null,
                (entityType != null && !entityType.isBlank()) ? entityType.trim() : null,
                (username != null && !username.isBlank()) ? username.trim() : null,
                (action != null && !action.isBlank()) ? action.trim() : null,
                fromDate,
                toDate,
                pageable
        ).getContent();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Nhat_Ky_Thao_Tac");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columns = {"ID", "Mã Trace", "Thời Gian", "Tài Khoản", "Vai Trò",
                    "Hành Động", "Phân Hệ", "Mã Đối Tượng", "Mô Tả / Lý Do", "Địa Chỉ IP", "Thiết Bị"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ImmutableAuditLog item : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getId() != null ? item.getId() : 0);
                row.createCell(1).setCellValue(item.getTraceId() != null ? item.getTraceId() : "");
                row.createCell(2).setCellValue(item.getTimestamp() != null ? item.getTimestamp().format(dtf) : "");
                row.createCell(3).setCellValue(item.getUsername() != null ? item.getUsername() : "");
                row.createCell(4).setCellValue(item.getUserRole() != null ? item.getUserRole() : "");
                row.createCell(5).setCellValue(item.getAction() != null ? item.getAction() : "");
                row.createCell(6).setCellValue(item.getEntityType() != null ? item.getEntityType() : "");
                row.createCell(7).setCellValue(item.getEntityCode() != null ? item.getEntityCode() : (item.getEntityId() != null ? "#" + item.getEntityId() : ""));
                row.createCell(8).setCellValue(item.getReason() != null ? item.getReason() : "");
                row.createCell(9).setCellValue(item.getIpAddress() != null ? item.getIpAddress() : "");
                row.createCell(10).setCellValue(item.getUserAgent() != null ? item.getUserAgent() : "");
            }

            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
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
