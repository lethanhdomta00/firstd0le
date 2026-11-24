package com.erp.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Dịch vụ Đồng bộ Trạng thái Thời gian thực (Realtime State Synchronizer)
 * Phát các tín hiệu WebSocket STOMP tới các kênh /topic/... để Frontend tự động cập nhật
 * mà không cần người dùng phải bấm F5 (Optimistic UI & Zero-Lag UX).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeStateSyncService {

    private final SimpMessagingTemplate messagingTemplate;

    @Data
    @Builder
    public static class RealtimeEvent<T> {
        private String topic;
        private String action; // CREATED, UPDATED, DELETED, STATUS_CHANGED
        private LocalDateTime timestamp;
        private T data;
    }

    public <T> void publishEvent(String topic, String action, T data) {
        try {
            RealtimeEvent<T> event = RealtimeEvent.<T>builder()
                    .topic(topic)
                    .action(action)
                    .timestamp(LocalDateTime.now())
                    .data(data)
                    .build();

            messagingTemplate.convertAndSend(topic, event);
            log.debug("Đã phát sự kiện Realtime tới [{}]: action={}, data={}", topic, action, data);
        } catch (Exception e) {
            log.warn("Không thể phát sự kiện Realtime tới [{}]: {}", topic, e.getMessage());
        }
    }

    public void publishOrderEvent(String action, Object data) {
        publishEvent("/topic/orders", action, data);
    }

    public void publishInventoryEvent(String action, Object data) {
        publishEvent("/topic/inventory", action, data);
    }

    public void publishFinanceEvent(String action, Object data) {
        publishEvent("/topic/finance", action, data);
    }
}
