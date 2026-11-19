package com.erp.copilot.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý kho lưu trữ báo cáo tạm thời được sinh bởi AI Copilot.
 * Tự động dọn dẹp các file quá hạn 24 giờ để tránh rò rỉ dung lượng.
 */
@Slf4j
@Service
public class TempReportCacheManager {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedReport {
        private String fileId;
        private String fileName;
        private String contentType;
        private byte[] contentBytes;
        private long fileSizeBytes;
        private LocalDateTime createdAt;
    }

    private final Map<String, CachedReport> cache = new ConcurrentHashMap<>();

    /**
     * Lưu trữ một file báo cáo vào cache và sinh fileId duy nhất
     */
    public String storeReport(String fileName, String contentType, byte[] contentBytes) {
        String fileId = UUID.randomUUID().toString().replace("-", "");
        CachedReport report = CachedReport.builder()
                .fileId(fileId)
                .fileName(fileName)
                .contentType(contentType)
                .contentBytes(contentBytes)
                .fileSizeBytes(contentBytes != null ? contentBytes.length : 0)
                .createdAt(LocalDateTime.now())
                .build();
        cache.put(fileId, report);
        log.info("Cached generated AI report [{}]: {} ({})", fileId, fileName, formatSize(report.getFileSizeBytes()));
        return fileId;
    }

    /**
     * Lấy file báo cáo theo fileId
     */
    public Optional<CachedReport> getReport(String fileId) {
        if (fileId == null) return Optional.empty();
        return Optional.ofNullable(cache.get(fileId.trim()));
    }

    /**
     * Định dạng kích thước file người dùng dễ đọc
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Quét và dọn dẹp báo cáo tạo quá 24h
     */
    @Scheduled(fixedRate = 3600000) // Chạy mỗi giờ
    public void cleanupOldReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        cache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().getCreatedAt().isBefore(cutoff);
            if (expired) {
                log.info("Purged expired AI report cache: {}", entry.getKey());
            }
            return expired;
        });
    }
}
