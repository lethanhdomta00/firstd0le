package com.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    // Đếm số phiên kết nối của mỗi username (hỗ trợ trường hợp 1 user mở nhiều tab hoặc thiết bị)
    private static final Map<String, Integer> userSessionCount = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal != null && principal.getName() != null) {
            String username = principal.getName();
            userSessionCount.compute(username, (k, count) -> (count == null) ? 1 : count + 1);
            log.info("WebSocket STOMP: User [{}] trực tuyến (Số tab/thiết bị: {})", username, userSessionCount.get(username));
            broadcastOnlineUsers();
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal != null && principal.getName() != null) {
            String username = principal.getName();
            userSessionCount.computeIfPresent(username, (k, count) -> {
                int next = count - 1;
                return next > 0 ? next : null;
            });
            log.info("WebSocket STOMP: User [{}] ngắt kết nối", username);
            broadcastOnlineUsers();
        }
    }

    public static Set<String> getOnlineUsernames() {
        return Collections.unmodifiableSet(userSessionCount.keySet());
    }

    private void broadcastOnlineUsers() {
        Set<String> onlineList = userSessionCount.keySet();
        try {
            messagingTemplate.convertAndSend("/topic/online-users", onlineList);
        } catch (Exception e) {
            log.warn("Không thể broadcast danh sách online users: {}", e.getMessage());
        }
    }
}

