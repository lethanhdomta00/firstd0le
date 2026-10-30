package com.erp.controller;

import com.erp.dto.RealtimeMessageDto;
import com.erp.model.MessageStatus;
import com.erp.model.MessageType;
import com.erp.model.RealtimeMessage;
import com.erp.model.User;
import com.erp.repository.RealtimeMessageRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RealtimeChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeMessageRepository messageRepository;
    private final UserRepository userRepository;

    // =========================================================================
    //                        WEBSOCKET STOMP HANDLERS
    // =========================================================================

    /**
     * 1. Gửi tin nhắn vào Phòng chat chung (Toàn công ty)
     * Client gửi lên: /app/chat.sendPublic
     * Server phát lại tới: /topic/room.{roomId}
     */
    @MessageMapping("/chat.sendPublic")
    public void sendPublicMessage(@Payload RealtimeMessageDto messageDto, Principal principal) {
        String senderUsername = principal != null ? principal.getName() : messageDto.getSenderUsername();
        String roomId = (messageDto.getRoomId() != null && !messageDto.getRoomId().trim().isEmpty())
                ? messageDto.getRoomId().trim()
                : "GLOBAL";

        Optional<User> senderOpt = userRepository.findByUsername(senderUsername);
        String fullName = senderOpt.map(User::getFullName).orElse(senderUsername);
        String avatarUrl = senderOpt.map(User::getAvatarUrl).orElse(null);

        RealtimeMessage entity = RealtimeMessage.builder()
                .roomId(roomId)
                .senderUsername(senderUsername)
                .senderFullName(fullName)
                .senderAvatarUrl(avatarUrl)
                .recipientUsername(null)
                .content(messageDto.getContent())
                .type(messageDto.getType() != null ? messageDto.getType() : MessageType.CHAT)
                .status(MessageStatus.SENT)
                .build();

        RealtimeMessage saved = messageRepository.save(entity);
        RealtimeMessageDto responseDto = RealtimeMessageDto.fromEntity(saved);

        // Broadcast tin nhắn tới tất cả mọi người đang theo dõi phòng này
        messagingTemplate.convertAndSend("/topic/room." + roomId, responseDto);
        log.info("WebSocket Public: [{}] gửi vào phòng [{}]: {}", senderUsername, roomId, messageDto.getContent());
    }

    /**
     * 2. Gửi tin nhắn Riêng tư 1-1 giữa 2 nhân viên
     * Client gửi lên: /app/chat.sendPrivate
     * Server gửi đích danh qua STOMP User Destination: /user/{username}/queue/messages
     */
    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload RealtimeMessageDto messageDto, Principal principal) {
        String senderUsername = principal != null ? principal.getName() : messageDto.getSenderUsername();
        String recipientUsername = messageDto.getRecipientUsername();

        if (recipientUsername == null || recipientUsername.trim().isEmpty()) {
            log.warn("Gửi tin nhắn riêng tư thất bại: Không có recipientUsername!");
            return;
        }

        recipientUsername = recipientUsername.trim();

        // Tạo Room ID chuẩn giữa 2 người (được sắp xếp theo bảng chữ cái để cả 2 luôn chung 1 phòng)
        String privateRoomId = buildPrivateRoomId(senderUsername, recipientUsername);

        Optional<User> senderOpt = userRepository.findByUsername(senderUsername);
        String fullName = senderOpt.map(User::getFullName).orElse(senderUsername);
        String avatarUrl = senderOpt.map(User::getAvatarUrl).orElse(null);

        RealtimeMessage entity = RealtimeMessage.builder()
                .roomId(privateRoomId)
                .senderUsername(senderUsername)
                .senderFullName(fullName)
                .senderAvatarUrl(avatarUrl)
                .recipientUsername(recipientUsername)
                .content(messageDto.getContent())
                .type(messageDto.getType() != null ? messageDto.getType() : MessageType.CHAT)
                .status(MessageStatus.SENT)
                .build();

        RealtimeMessage saved = messageRepository.save(entity);
        RealtimeMessageDto responseDto = RealtimeMessageDto.fromEntity(saved);

        // Gửi trực tiếp tới người nhận (Recipient)
        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/messages", responseDto);

        // Gửi xác nhận lại cho người gửi (Sender) để cập nhật UI ngay lập tức
        messagingTemplate.convertAndSendToUser(senderUsername, "/queue/messages", responseDto);

        log.info("WebSocket Private: [{}] -> [{}]: {}", senderUsername, recipientUsername, messageDto.getContent());
    }

    /**
     * 3. Báo hiệu trạng thái đang gõ phím (Typing Indicator)
     * Client gửi lên: /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void sendTypingStatus(@Payload RealtimeMessageDto messageDto, Principal principal) {
        String senderUsername = principal != null ? principal.getName() : messageDto.getSenderUsername();
        messageDto.setSenderUsername(senderUsername);
        messageDto.setType(MessageType.TYPING);

        if (messageDto.getRecipientUsername() != null && !messageDto.getRecipientUsername().trim().isEmpty()) {
            // Đang gõ trong chat riêng 1-1
            messagingTemplate.convertAndSendToUser(messageDto.getRecipientUsername().trim(), "/queue/typing", messageDto);
        } else {
            // Đang gõ trong phòng chat chung
            String roomId = messageDto.getRoomId() != null ? messageDto.getRoomId() : "GLOBAL";
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", messageDto);
        }
    }

    // =========================================================================
    //                        REST API CHO LỊCH SỬ CHAT
    // =========================================================================

    /**
     * Lấy lịch sử tin nhắn phòng chat chung
     */
    @GetMapping("/api/realtime-chat/history/{roomId}")
    public ResponseEntity<List<RealtimeMessageDto>> getRoomHistory(@PathVariable String roomId) {
        List<RealtimeMessageDto> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(RealtimeMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * Lấy lịch sử chat riêng 1-1 giữa tôi và đồng nghiệp
     */
    @GetMapping("/api/realtime-chat/private-history/{targetUsername}")
    public ResponseEntity<List<RealtimeMessageDto>> getPrivateHistory(@PathVariable String targetUsername) {
        String currentUsername = getCurrentUsername();
        String roomId = buildPrivateRoomId(currentUsername, targetUsername);

        List<RealtimeMessageDto> history = messageRepository.findByRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .map(RealtimeMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * Đánh dấu đã đọc tất cả tin nhắn trong 1 phòng gửi tới tôi
     */
    @PutMapping("/api/realtime-chat/read/{roomId}")
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable String roomId) {
        String currentUsername = getCurrentUsername();
        int updatedCount = messageRepository.markMessagesAsReadInRoom(roomId, currentUsername, MessageStatus.READ);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "markedCount", updatedCount
        ));
    }

    /**
     * Đếm tổng số tin nhắn chưa đọc của tôi
     */
    @GetMapping("/api/realtime-chat/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        String currentUsername = getCurrentUsername();
        long unreadCount = messageRepository.countByRecipientUsernameAndStatus(currentUsername, MessageStatus.SENT);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    // =========================================================================
    //                        HELPER METHODS
    // =========================================================================

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Chưa đăng nhập!");
        }
        return auth.getName();
    }

    /**
     * Tạo ID phòng chat 1-1 cố định theo thứ tự chữ cái của 2 username
     * Ví dụ: giữa "admin" và "john" luôn là "admin_john" bất kể ai là người gửi trước.
     */
    public static String buildPrivateRoomId(String user1, String user2) {
        if (user1.compareTo(user2) <= 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }
}

