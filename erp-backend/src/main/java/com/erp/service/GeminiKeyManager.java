package com.erp.service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quản lý danh sách nhiều Key Gemini (Multi-Key Pool) và tự động Failover.
 * - Tự động nạp danh sách key từ file gemini-keys.yml hoặc application.yml hoặc biến môi trường.
 * - Hỗ trợ nhiều key (dạng danh sách YAML có dấu gạch ngang hoặc chuỗi phân tách dấu phẩy).
 * - Luân phiên key (Round-Robin) và tự động switch sang key tiếp theo khi 1 key gặp sự cố.
 */
@Component
@ConfigurationProperties(prefix = "gemini")
@Data
@Slf4j
public class GeminiKeyManager {

    /**
     * Danh sách key từ gemini-keys.yml
     */
    private List<String> apiKeys = new ArrayList<>();

    /**
     * Hỗ trợ key đơn lẻ từ application.yml (nếu có)
     */
    private String apiKey;

    private final List<String> pool = new CopyOnWriteArrayList<>();
    private final Map<String, Long> failedKeysCooldown = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    // Thời gian cách ly tạm thời key gặp sự cố (5 phút)
    private static final long COOLDOWN_DURATION_MS = 5 * 60 * 1000;

    @PostConstruct
    public void init() {
        reloadPool();
    }

    public synchronized void reloadPool() {
        pool.clear();

        // 1. Nạp từ danh sách api-keys
        if (apiKeys != null) {
            for (String raw : apiKeys) {
                if (raw != null && !raw.trim().isEmpty() && !raw.contains("AIzaSy_Key_Du_Phong")) {
                    for (String part : raw.split("[,;\\s]+")) {
                        String clean = part.trim();
                        if (!clean.isEmpty() && !pool.contains(clean)) {
                            pool.add(clean);
                        }
                    }
                }
            }
        }

        // 2. Nạp từ apiKey đơn lẻ (nếu có)
        if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("AIzaSy_Key_Du_Phong")) {
            for (String part : apiKey.split("[,;\\s]+")) {
                String clean = part.trim();
                if (!clean.isEmpty() && !pool.contains(clean)) {
                    pool.add(clean);
                }
            }
        }

        log.info("GeminiKeyManager: Đã nạp thành công {} API Key(s) vào danh sách sẵn sàng hoạt động.", pool.size());
    }

    public boolean hasKeys() {
        return !pool.isEmpty();
    }

    public int getKeyCount() {
        return pool.size();
    }

    /**
     * Lấy danh sách các key theo thứ tự ưu tiên thử nghiệm (luân phiên xoay vòng)
     */
    public List<String> getCandidateKeys() {
        cleanExpiredCooldowns();
        if (pool.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> candidates = new ArrayList<>();
        int size = pool.size();
        int start = Math.abs(roundRobinIndex.getAndIncrement() % size);

        for (int i = 0; i < size; i++) {
            String key = pool.get((start + i) % size);
            if (!failedKeysCooldown.containsKey(key)) {
                candidates.add(key);
            }
        }

        // Nếu tất cả các key đều đang bị cooldown, vẫn trả về danh sách gốc để thử lại
        if (candidates.isEmpty()) {
            return new ArrayList<>(pool);
        }

        return candidates;
    }

    /**
     * Đánh dấu key gặp sự cố để tạm thời chuyển sang key khác
     */
    public void markKeyAsFailed(String key, int httpStatus, String errorMessage) {
        String masked = key.length() > 6 ? key.substring(0, 3) + "..." + key.substring(key.length() - 4) : key;
        log.warn("Gemini API Key [{}] gặp sự cố (HTTP {}): {}. Tự động switch sang Key tiếp theo trong Pool.",
                masked, httpStatus, errorMessage);
        failedKeysCooldown.put(key, System.currentTimeMillis() + COOLDOWN_DURATION_MS);
    }

    private void cleanExpiredCooldowns() {
        long now = System.currentTimeMillis();
        failedKeysCooldown.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
