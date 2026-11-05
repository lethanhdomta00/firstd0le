package com.erp.controller;

import com.erp.config.WebSocketEventListener;
import com.erp.dto.RealtimeMessageDto;
import com.erp.dto.UserResponseDto;
import com.erp.model.ChatNickname;
import com.erp.model.MessageStatus;
import com.erp.model.MessageType;
import com.erp.model.RealtimeMessage;
import com.erp.model.User;
import com.erp.repository.ChatGroupMemberRepository;
import com.erp.repository.ChatNicknameRepository;
import com.erp.repository.RealtimeMessageRepository;
import com.erp.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RealtimeChatController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatNicknameRepository nicknameRepository;
    private final ChatGroupMemberRepository memberRepository;

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
        String senderUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : messageDto.getSenderUsername();
        String roomId = (messageDto.getRoomId() != null && !messageDto.getRoomId().trim().isEmpty())
                ? messageDto.getRoomId().trim()
                : "GLOBAL";

        Optional<User> senderOpt = (senderUsername != null) ? userRepository.findByUsername(senderUsername) : Optional.empty();
        String fullName = senderOpt.map(User::getFullName).orElse(senderUsername != null ? senderUsername : "Thành viên");
        String avatarUrl = senderOpt.map(User::getAvatarUrl).orElse(null);

        RealtimeMessage entity = RealtimeMessage.builder()
                .roomId(roomId)
                .senderUsername(senderUsername != null ? senderUsername : "system")
                .senderFullName(fullName)
                .senderAvatarUrl(avatarUrl)
                .recipientUsername(null)
                .content(messageDto.getContent())
                .type(messageDto.getType() != null ? messageDto.getType() : MessageType.CHAT)
                .status(MessageStatus.SENT)
                .replyToId(messageDto.getReplyToId())
                .replyToSender(messageDto.getReplyToSender())
                .replyToContent(messageDto.getReplyToContent())
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
        String senderUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : messageDto.getSenderUsername();
        String recipientUsername = messageDto.getRecipientUsername();

        if (recipientUsername == null || recipientUsername.trim().isEmpty()) {
            log.warn("Gửi tin nhắn riêng tư thất bại: Không có recipientUsername!");
            return;
        }

        recipientUsername = recipientUsername.trim();
        if (senderUsername == null || senderUsername.trim().isEmpty()) {
            senderUsername = "unknown";
        } else {
            senderUsername = senderUsername.trim();
        }

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
                .replyToId(messageDto.getReplyToId())
                .replyToSender(messageDto.getReplyToSender())
                .replyToContent(messageDto.getReplyToContent())
                .build();

        RealtimeMessage saved = messageRepository.save(entity);
        RealtimeMessageDto responseDto = RealtimeMessageDto.fromEntity(saved);

        // Gửi trực tiếp tới người nhận (Recipient) qua User Queue
        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/messages", responseDto);

        // Gửi xác nhận lại cho người gửi (Sender) để cập nhật UI ngay lập tức
        messagingTemplate.convertAndSendToUser(senderUsername, "/queue/messages", responseDto);

        // Kênh bổ trợ phòng riêng: Đảm bảo cả hai bên nhận được ngay lập tức bất kể môi trường mạng
        messagingTemplate.convertAndSend("/topic/private." + privateRoomId, responseDto);

        log.info("WebSocket Private: [{}] -> [{}]: {}", senderUsername, recipientUsername, messageDto.getContent());
    }

    /**
     * 3. Báo hiệu trạng thái đang gõ phím (Typing Indicator)
     * Client gửi lên: /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void sendTypingStatus(@Payload RealtimeMessageDto messageDto, Principal principal) {
        String senderUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : messageDto.getSenderUsername();
        messageDto.setSenderUsername(senderUsername);
        messageDto.setType(MessageType.TYPING);

        if (messageDto.getRecipientUsername() != null && !messageDto.getRecipientUsername().trim().isEmpty()) {
            // Đang gõ trong chat riêng 1-1
            messagingTemplate.convertAndSendToUser(messageDto.getRecipientUsername().trim(), "/queue/typing", messageDto);
            String privateRoomId = buildPrivateRoomId(senderUsername, messageDto.getRecipientUsername().trim());
            messagingTemplate.convertAndSend("/topic/private." + privateRoomId + ".typing", messageDto);
        } else {
            // Đang gõ trong phòng chat chung
            String roomId = messageDto.getRoomId() != null ? messageDto.getRoomId() : "GLOBAL";
            messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", messageDto);
        }
    }

    /**
     * 4. Thả cảm xúc (Reaction) tin nhắn qua WebSocket STOMP
     * Client gửi lên: /app/chat.react
     */
    @MessageMapping("/chat.react")
    public void reactMessageStomp(@Payload Map<String, Object> payload, Principal principal) {
        Long messageId = payload.get("messageId") != null ? Long.valueOf(payload.get("messageId").toString()) : null;
        String emoji = (String) payload.get("emoji");
        String username = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : (String) payload.get("username");
        processReaction(messageId, emoji, username);
    }

    /**
     * Thả cảm xúc qua REST API fallback
     */
    @PostMapping("/api/realtime-chat/react")
    public ResponseEntity<?> reactMessageRest(@RequestBody Map<String, Object> payload) {
        Long messageId = payload.get("messageId") != null ? Long.valueOf(payload.get("messageId").toString()) : null;
        String emoji = (String) payload.get("emoji");
        String username = getCurrentUsername();
        Map<String, List<String>> updatedReactions = processReaction(messageId, emoji, username);
        return ResponseEntity.ok(Map.of("messageId", messageId, "reactions", updatedReactions));
    }

    @Transactional
    public Map<String, List<String>> processReaction(Long messageId, String emoji, String username) {
        if (messageId == null || emoji == null || username == null) return Collections.emptyMap();

        Optional<RealtimeMessage> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isEmpty()) return Collections.emptyMap();

        RealtimeMessage message = msgOpt.get();
        Map<String, List<String>> reactions = new HashMap<>();

        if (message.getReactionsJson() != null && !message.getReactionsJson().trim().isEmpty()) {
            try {
                reactions = OBJECT_MAPPER.readValue(message.getReactionsJson(), new TypeReference<Map<String, List<String>>>() {});
            } catch (Exception e) {
                reactions = new HashMap<>();
            }
        }

        List<String> users = reactions.getOrDefault(emoji, new ArrayList<>());
        // Toggle: nếu đã thả emoji này thì bỏ thả, nếu chưa thì thêm vào
        if (users.contains(username)) {
            users.remove(username);
            if (users.isEmpty()) {
                reactions.remove(emoji);
            } else {
                reactions.put(emoji, users);
            }
        } else {
            users.add(username);
            reactions.put(emoji, users);
        }

        try {
            message.setReactionsJson(OBJECT_MAPPER.writeValueAsString(reactions));
            messageRepository.save(message);
        } catch (Exception e) {
            log.error("Lỗi lưu reactionsJson:", e);
        }

        Map<String, Object> event = new HashMap<>();
        event.put("type", "REACTION");
        event.put("messageId", messageId);
        event.put("roomId", message.getRoomId());
        event.put("reactions", reactions);

        // Broadcast sự kiện reaction tới phòng
        messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), (Object) event);
        if (message.getRecipientUsername() != null) {
            messagingTemplate.convertAndSend("/topic/private." + message.getRoomId(), (Object) event);
            messagingTemplate.convertAndSendToUser(message.getRecipientUsername(), "/queue/messages", event);
            messagingTemplate.convertAndSendToUser(message.getSenderUsername(), "/queue/messages", event);
        }

        return reactions;
    }

    /**
     * 5. Chuyển tiếp tín hiệu Gọi thoại / Video nội bộ (WebRTC Signaling)
     * Client gửi lên: /app/chat.callSignal
     * Server gửi đích danh qua STOMP User Destination: /user/{targetUsername}/queue/call
     */
    @MessageMapping("/chat.callSignal")
    public void forwardCallSignal(@Payload Map<String, Object> signal, Principal principal) {
        String senderUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : (signal.get("senderUsername") != null ? signal.get("senderUsername").toString() : null);
        String targetUsername = signal.get("targetUsername") != null ? signal.get("targetUsername").toString() : null;

        if (StringUtils.hasText(targetUsername) && StringUtils.hasText(senderUsername)) {
            signal.put("senderUsername", senderUsername);
            String finalTarget = WebSocketEventListener.getOnlineUsernames().stream()
                    .filter(u -> u.equalsIgnoreCase(targetUsername.trim()))
                    .findFirst()
                    .orElse(targetUsername.trim());
            messagingTemplate.convertAndSendToUser(finalTarget, "/queue/call", signal);
            log.info("WebRTC Signal [{}] -> [{}]: action={}", senderUsername, finalTarget, signal.get("type"));
        } else {
            log.warn("WebRTC Signal bị bỏ qua vì thiếu sender hoặc targetUsername: sender={}, target={}", senderUsername, targetUsername);
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
     * Lấy lịch sử tin nhắn phòng chat chung có phân trang (Lazy Loading kéo lên trên)
     */
    @GetMapping("/api/realtime-chat/history/{roomId}/page")
    public ResponseEntity<List<RealtimeMessageDto>> getRoomHistoryPaged(
            @PathVariable String roomId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
        List<RealtimeMessage> messages;
        if (beforeId == null) {
            messages = messageRepository.findByRoomIdOrderByIdDesc(roomId, pageable);
        } else {
            messages = messageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, beforeId, pageable);
        }

        List<RealtimeMessageDto> result = messages.stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(RealtimeMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy lịch sử chat riêng 1-1 giữa tôi và đồng nghiệp
     */
    @GetMapping("/api/realtime-chat/private-history/{targetUsername}")
    public ResponseEntity<List<RealtimeMessageDto>> getPrivateHistory(@PathVariable String targetUsername) {
        String currentUsername = getCurrentUsername();
        List<RealtimeMessageDto> history = messageRepository.findPrivateMessagesBetween(currentUsername, targetUsername)
                .stream()
                .map(RealtimeMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * Lấy lịch sử chat riêng 1-1 có phân trang (Lazy Loading kéo lên trên)
     */
    @GetMapping("/api/realtime-chat/private-history/{targetUsername}/page")
    public ResponseEntity<List<RealtimeMessageDto>> getPrivateHistoryPaged(
            @PathVariable String targetUsername,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") int limit) {
        String currentUsername = getCurrentUsername();
        String roomId = buildPrivateRoomId(currentUsername, targetUsername);

        Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
        List<RealtimeMessage> messages;
        if (beforeId == null) {
            messages = messageRepository.findPrivateHistoryLatest(roomId, currentUsername, targetUsername, pageable);
        } else {
            messages = messageRepository.findPrivateHistoryBeforeId(roomId, currentUsername, targetUsername, beforeId, pageable);
        }

        List<RealtimeMessageDto> result = messages.stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(RealtimeMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Đánh dấu đã đọc tất cả tin nhắn trong 1 phòng gửi tới tôi
     */
    @PutMapping("/api/realtime-chat/read/{roomId}")
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable String roomId) {
        String currentUsername = getCurrentUsername();
        String partnerUsername = "";
        if (roomId.contains("_") && !roomId.startsWith("GROUP_")) {
            String[] parts = roomId.split("_");
            partnerUsername = parts[0].equalsIgnoreCase(currentUsername) ? parts[1] : parts[0];
        }
        int updatedCount = messageRepository.markMessagesAsReadInRoomWithPartner(roomId, currentUsername, partnerUsername, MessageStatus.READ);
        if (updatedCount > 0) {
            broadcastReadReceipt(roomId, currentUsername);
        }
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "markedCount", updatedCount
        ));
    }

    /**
     * Đánh dấu TẤT CẢ tin nhắn chưa đọc của tôi là ĐÃ ĐỌC
     */
    @PutMapping("/api/realtime-chat/read-all")
    @Transactional
    public ResponseEntity<?> markAllAsRead() {
        String currentUsername = getCurrentUsername();
        int updatedCount = messageRepository.markAllMessagesAsReadForUser(currentUsername, MessageStatus.READ);
        log.info("Người dùng [{}] đã đánh dấu tất cả ({}) tin nhắn là ĐÃ ĐỌC", currentUsername, updatedCount);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "markedCount", updatedCount
        ));
    }

    /**
     * Đánh dấu đã đọc qua WebSocket STOMP
     */
    @MessageMapping("/chat.markRead")
    @Transactional
    public void handleMarkReadWs(@Payload Map<String, String> payload, Principal principal) {
        String currentUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : payload.get("readerUsername");
        String roomId = payload.get("roomId");
        if (StringUtils.hasText(roomId) && StringUtils.hasText(currentUsername)) {
            String partnerUsername = "";
            if (roomId.contains("_") && !roomId.startsWith("GROUP_")) {
                String[] parts = roomId.split("_");
                partnerUsername = parts[0].equalsIgnoreCase(currentUsername) ? parts[1] : parts[0];
            }
            int updatedCount = messageRepository.markMessagesAsReadInRoomWithPartner(roomId, currentUsername, partnerUsername, MessageStatus.READ);
            if (updatedCount > 0) {
                broadcastReadReceipt(roomId, currentUsername);
            }
        }
    }

    private void broadcastReadReceipt(String roomId, String readerUsername) {
        Map<String, Object> readEvent = new HashMap<>();
        readEvent.put("type", "READ_RECEIPT");
        readEvent.put("roomId", roomId);
        readEvent.put("readerUsername", readerUsername);
        readEvent.put("readAt", java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString());

        messagingTemplate.convertAndSend("/topic/room." + roomId, (Object) readEvent);
        messagingTemplate.convertAndSend("/topic/private." + roomId, (Object) readEvent);

        if (roomId.contains("_") && !roomId.startsWith("GROUP_")) {
            String[] parts = roomId.split("_");
            String partner = parts[0].equalsIgnoreCase(readerUsername) ? parts[1] : parts[0];
            messagingTemplate.convertAndSendToUser(partner, "/queue/messages", readEvent);
        }
    }

    // =========================================================================
    //                        THU HỒI / XÓA TIN NHẮN
    // =========================================================================

    @MessageMapping("/chat.deleteMessage")
    @Transactional
    public void deleteMessageWs(@Payload Map<String, Object> payload, Principal principal) {
        String currentUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : (String) payload.get("username");
        Long messageId = payload.get("messageId") != null ? Long.valueOf(payload.get("messageId").toString()) : null;
        if (messageId != null && currentUsername != null) {
            executeDeleteMessage(messageId, currentUsername);
        }
    }

    @DeleteMapping("/api/realtime-chat/messages/{messageId}")
    @Transactional
    public ResponseEntity<?> deleteMessageRest(@PathVariable Long messageId) {
        String currentUsername = getCurrentUsername();
        executeDeleteMessage(messageId, currentUsername);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Đã thu hồi tin nhắn"));
    }

    private void executeDeleteMessage(Long messageId, String currentUsername) {
        RealtimeMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn!"));

        boolean isSender = message.getSenderUsername().equalsIgnoreCase(currentUsername);
        boolean isGroupAdmin = false;
        if (message.getRoomId().startsWith("GROUP_")) {
            try {
                Long groupId = Long.valueOf(message.getRoomId().replace("GROUP_", ""));
                isGroupAdmin = memberRepository.findByGroupId(groupId).stream()
                        .anyMatch(m -> m.getUsername().equalsIgnoreCase(currentUsername) && "ADMIN".equalsIgnoreCase(m.getRole()));
            } catch (Exception ignored) {}
        }

        if (!isSender && !isGroupAdmin) {
            throw new RuntimeException("Bạn không có quyền thu hồi tin nhắn này!");
        }

        message.setIsDeleted(true);
        message.setDeletedAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        message.setContent("Tin nhắn đã được thu hồi");
        messageRepository.save(message);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "MESSAGE_DELETED");
        event.put("messageId", messageId);
        event.put("roomId", message.getRoomId());
        event.put("deletedBy", currentUsername);

        messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), (Object) event);
        messagingTemplate.convertAndSend("/topic/private." + message.getRoomId(), (Object) event);
        if (message.getRecipientUsername() != null) {
            messagingTemplate.convertAndSendToUser(message.getRecipientUsername(), "/queue/messages", event);
            messagingTemplate.convertAndSendToUser(message.getSenderUsername(), "/queue/messages", event);
        }
    }

    // =========================================================================
    //                        CHỈNH SỬA TIN NHẮN
    // =========================================================================

    @MessageMapping("/chat.editMessage")
    @Transactional
    public void editMessageWs(@Payload Map<String, Object> payload, Principal principal) {
        String currentUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : (String) payload.get("username");
        Long messageId = payload.get("messageId") != null ? Long.valueOf(payload.get("messageId").toString()) : null;
        String newContent = (String) payload.get("newContent");
        if (messageId != null && currentUsername != null && StringUtils.hasText(newContent)) {
            executeEditMessage(messageId, currentUsername, newContent.trim());
        }
    }

    @PutMapping("/api/realtime-chat/messages/{messageId}")
    @Transactional
    public ResponseEntity<?> editMessageRest(@PathVariable Long messageId, @RequestBody Map<String, String> body) {
        String currentUsername = getCurrentUsername();
        String newContent = body.get("content");
        if (!StringUtils.hasText(newContent)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nội dung tin nhắn không được để trống!"));
        }
        executeEditMessage(messageId, currentUsername, newContent.trim());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Đã cập nhật tin nhắn"));
    }

    private void executeEditMessage(Long messageId, String currentUsername, String newContent) {
        RealtimeMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn!"));

        if (!message.getSenderUsername().equalsIgnoreCase(currentUsername)) {
            throw new RuntimeException("Bạn chỉ có thể chỉnh sửa tin nhắn của chính mình!");
        }

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Không thể chỉnh sửa tin nhắn đã bị thu hồi!");
        }

        if (message.getType() != MessageType.CHAT) {
            throw new RuntimeException("Chỉ có thể chỉnh sửa tin nhắn văn bản!");
        }

        message.setContent(newContent);
        message.setIsEdited(true);
        message.setEditedAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        messageRepository.save(message);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "MESSAGE_EDITED");
        event.put("messageId", messageId);
        event.put("roomId", message.getRoomId());
        event.put("newContent", newContent);
        event.put("editedAt", message.getEditedAt() != null ? message.getEditedAt().toString() : null);

        messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), (Object) event);
        messagingTemplate.convertAndSend("/topic/private." + message.getRoomId(), (Object) event);
        if (message.getRecipientUsername() != null) {
            messagingTemplate.convertAndSendToUser(message.getRecipientUsername(), "/queue/messages", event);
            messagingTemplate.convertAndSendToUser(message.getSenderUsername(), "/queue/messages", event);
        }
    }

    // =========================================================================
    //                        GHIM TIN NHẮN (PIN / UNPIN)
    // =========================================================================

    /**
     * Lấy danh sách tin nhắn đã ghim trong một phòng
     */
    @GetMapping("/api/realtime-chat/rooms/{roomId}/pinned")
    public ResponseEntity<List<RealtimeMessageDto>> getPinnedMessages(@PathVariable String roomId) {
        List<RealtimeMessage> pinned;
        if ("GLOBAL".equalsIgnoreCase(roomId) || roomId.startsWith("GROUP_")) {
            pinned = messageRepository.findByRoomIdAndIsPinnedTrueOrderByPinnedAtDesc(roomId);
        } else {
            // Chat riêng 1-1
            String[] parts = roomId.split("_");
            if (parts.length >= 2) {
                pinned = messageRepository.findPinnedMessagesBetween(roomId, parts[0], parts[1]);
            } else {
                pinned = messageRepository.findByRoomIdAndIsPinnedTrueOrderByPinnedAtDesc(roomId);
            }
        }
        List<RealtimeMessageDto> dtos = pinned.stream()
                .map(RealtimeMessageDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * WebSocket: Ghim hoặc Bỏ ghim tin nhắn
     */
    @MessageMapping("/chat.pinMessage")
    @Transactional
    public void pinMessageWs(@Payload Map<String, Object> payload, Principal principal) {
        String currentUsername = (principal != null && StringUtils.hasText(principal.getName()))
                ? principal.getName()
                : (String) payload.get("username");
        Long messageId = payload.get("messageId") != null ? Long.valueOf(payload.get("messageId").toString()) : null;
        Boolean isPinned = payload.containsKey("isPinned") && payload.get("isPinned") != null 
                ? Boolean.valueOf(payload.get("isPinned").toString()) 
                : null;
        if (messageId != null && currentUsername != null) {
            executeTogglePin(messageId, currentUsername, isPinned);
        }
    }

    /**
     * REST: Ghim hoặc Bỏ ghim tin nhắn
     */
    @PutMapping("/api/realtime-chat/messages/{messageId}/pin")
    @Transactional
    public ResponseEntity<?> togglePinMessageRest(@PathVariable Long messageId, @RequestBody(required = false) Map<String, Object> body) {
        String currentUsername = getCurrentUsername();
        Boolean targetPinned = null;
        if (body != null && body.containsKey("isPinned") && body.get("isPinned") != null) {
            targetPinned = Boolean.valueOf(body.get("isPinned").toString());
        }
        RealtimeMessageDto updated = executeTogglePin(messageId, currentUsername, targetPinned);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", Boolean.TRUE.equals(updated.getIsPinned()) ? "Đã ghim tin nhắn" : "Đã bỏ ghim tin nhắn",
                "data", updated
        ));
    }

    private RealtimeMessageDto executeTogglePin(Long messageId, String currentUsername, Boolean targetPinned) {
        RealtimeMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn!"));

        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            throw new RuntimeException("Không thể ghim tin nhắn đã bị thu hồi!");
        }

        boolean newPinState = targetPinned != null ? targetPinned : !Boolean.TRUE.equals(message.getIsPinned());
        message.setIsPinned(newPinState);
        message.setPinnedAt(newPinState ? java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : null);
        message.setPinnedBy(newPinState ? currentUsername : null);
        RealtimeMessage saved = messageRepository.save(message);

        RealtimeMessageDto dto = RealtimeMessageDto.fromEntity(saved);

        Map<String, Object> event = new HashMap<>();
        event.put("type", newPinState ? "MESSAGE_PINNED" : "MESSAGE_UNPINNED");
        event.put("messageId", messageId);
        event.put("roomId", message.getRoomId());
        event.put("isPinned", newPinState);
        event.put("pinnedBy", currentUsername);
        event.put("pinnedAt", saved.getPinnedAt() != null ? saved.getPinnedAt().toString() : null);
        event.put("message", dto);

        messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), (Object) event);
        messagingTemplate.convertAndSend("/topic/private." + message.getRoomId(), (Object) event);
        if (message.getRecipientUsername() != null) {
            messagingTemplate.convertAndSendToUser(message.getRecipientUsername(), "/queue/messages", event);
            messagingTemplate.convertAndSendToUser(message.getSenderUsername(), "/queue/messages", event);
        }

        log.info("Message Pin: [{}] đã {} tin nhắn #{} trong phòng [{}]", 
                currentUsername, newPinState ? "GHIM" : "BỎ GHIM", messageId, message.getRoomId());
        return dto;
    }

    // =========================================================================
    //                        BIỆT DANH (NICKNAMES)
    // =========================================================================

    @GetMapping("/api/realtime-chat/rooms/{roomId}/nicknames")
    public ResponseEntity<Map<String, String>> getNicknames(@PathVariable String roomId) {
        List<ChatNickname> list = nicknameRepository.findByRoomId(roomId);
        Map<String, String> map = list.stream()
                .collect(Collectors.toMap(ChatNickname::getUsername, ChatNickname::getNickname, (a, b) -> b));
        return ResponseEntity.ok(map);
    }

    @PostMapping("/api/realtime-chat/rooms/{roomId}/nicknames")
    @Transactional
    public ResponseEntity<?> setNickname(@PathVariable String roomId, @RequestBody Map<String, String> body) {
        String currentUsername = getCurrentUsername();
        String targetUsername = body.get("username");
        String nickname = body.get("nickname");

        if (!StringUtils.hasText(targetUsername)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu username!"));
        }

        ChatNickname nick = nicknameRepository.findByRoomIdAndUsername(roomId, targetUsername)
                .orElse(ChatNickname.builder().roomId(roomId).username(targetUsername).build());

        String cleanNick = StringUtils.hasText(nickname) ? nickname.trim() : "";
        nick.setNickname(cleanNick);
        nick.setUpdatedBy(currentUsername);
        nicknameRepository.save(nick);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "NICKNAME_UPDATED");
        event.put("roomId", roomId);
        event.put("username", targetUsername);
        event.put("nickname", cleanNick);
        event.put("updatedBy", currentUsername);

        messagingTemplate.convertAndSend("/topic/room." + roomId, (Object) event);
        messagingTemplate.convertAndSend("/topic/private." + roomId, (Object) event);
        if (roomId.contains("_") && !roomId.startsWith("GROUP_")) {
            String[] parts = roomId.split("_");
            messagingTemplate.convertAndSendToUser(parts[0], "/queue/messages", event);
            messagingTemplate.convertAndSendToUser(parts[1], "/queue/messages", event);
        }

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "nickname", cleanNick));
    }

    // =========================================================================
    //                        THĂM DÒ Ý KIẾN (POLL VOTE)
    // =========================================================================

    @PostMapping("/api/realtime-chat/messages/{messageId}/poll/vote")
    @Transactional
    public ResponseEntity<?> votePoll(@PathVariable Long messageId, @RequestBody Map<String, Object> body) {
        String currentUsername = getCurrentUsername();
        RealtimeMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin nhắn bình chọn!"));

        if (message.getType() != MessageType.POLL || !StringUtils.hasText(message.getContent())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tin nhắn không phải bình chọn!"));
        }

        Integer optionId = body.get("optionId") != null ? Integer.valueOf(body.get("optionId").toString()) : null;
        if (optionId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu optionId!"));
        }

        try {
            Map<String, Object> pollData = OBJECT_MAPPER.readValue(message.getContent(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");
            boolean isMultiple = Boolean.TRUE.equals(pollData.get("isMultiple"));

            if (!isMultiple) {
                // Đơn chọn: Hủy vote ở các option khác trước
                for (Map<String, Object> opt : options) {
                    Integer optId = opt.get("id") != null ? Integer.valueOf(opt.get("id").toString()) : null;
                    if (!Objects.equals(optId, optionId)) {
                        List<String> votes = (List<String>) opt.get("votes");
                        if (votes != null) {
                            votes.remove(currentUsername);
                        }
                    }
                }
            }

            for (Map<String, Object> opt : options) {
                Integer optId = opt.get("id") != null ? Integer.valueOf(opt.get("id").toString()) : null;
                if (Objects.equals(optId, optionId)) {
                    List<String> votes = (List<String>) opt.get("votes");
                    if (votes == null) {
                        votes = new ArrayList<>();
                        opt.put("votes", votes);
                    }
                    if (votes.contains(currentUsername)) {
                        votes.remove(currentUsername);
                    } else {
                        votes.add(currentUsername);
                    }
                }
            }

            String updatedJson = OBJECT_MAPPER.writeValueAsString(pollData);
            message.setContent(updatedJson);
            messageRepository.save(message);

            Map<String, Object> event = new HashMap<>();
            event.put("type", "POLL_VOTED");
            event.put("messageId", messageId);
            event.put("roomId", message.getRoomId());
            event.put("pollData", pollData);
            event.put("votedBy", currentUsername);

            messagingTemplate.convertAndSend("/topic/room." + message.getRoomId(), (Object) event);
            messagingTemplate.convertAndSend("/topic/private." + message.getRoomId(), (Object) event);
            if (message.getRecipientUsername() != null) {
                messagingTemplate.convertAndSendToUser(message.getRecipientUsername(), "/queue/messages", event);
                messagingTemplate.convertAndSendToUser(message.getSenderUsername(), "/queue/messages", event);
            }

            return ResponseEntity.ok(pollData);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý vote poll: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi xử lý bình chọn: " + e.getMessage()));
        }
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

    /**
     * Lấy danh sách đồng nghiệp trong doanh nghiệp để hiển thị trên danh bạ chat 1-1
     */
    @GetMapping("/api/realtime-chat/colleagues")
    public ResponseEntity<List<UserResponseDto>> getColleagues() {
        String currentUsername = getCurrentUsername();
        List<UserResponseDto> colleagues = userRepository.findAll().stream()
                .filter(u -> !u.getUsername().equalsIgnoreCase(currentUsername))
                .filter(u -> !"LOCKED".equalsIgnoreCase(u.getStatus()))
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());

        Set<String> onlineUsernames = WebSocketEventListener.getOnlineUsernames();

        for (UserResponseDto dto : colleagues) {
            dto.setIsOnline(onlineUsernames.contains(dto.getUsername()));

            String privateRoomId = buildPrivateRoomId(currentUsername, dto.getUsername());
            nicknameRepository.findByRoomIdAndUsername(privateRoomId, dto.getUsername())
                    .ifPresent(n -> dto.setNickname(n.getNickname()));

            // Đếm số tin nhắn chưa đọc mà đồng nghiệp này gửi tới tôi
            long unread = messageRepository.countUnreadFromSender(dto.getUsername(), currentUsername, MessageStatus.SENT);
            dto.setUnreadCount((int) unread);

            List<RealtimeMessage> lastMsgs = messageRepository.findLastMessageBetween(currentUsername, dto.getUsername(), PageRequest.of(0, 1));
            if (!lastMsgs.isEmpty()) {
                RealtimeMessage m = lastMsgs.get(0);
                String preview = m.getContent();
                if (Boolean.TRUE.equals(m.getIsDeleted())) {
                    preview = "Tin nhắn đã được thu hồi";
                } else if (m.getType() == MessageType.IMAGE) {
                    preview = "📷 [Hình ảnh]";
                } else if (m.getType() == MessageType.VIDEO) {
                    preview = "🎥 [Video]";
                } else if (m.getType() == MessageType.VOICE) {
                    preview = "🎙️ [Tin nhắn thoại]";
                } else if (m.getType() == MessageType.POLL) {
                    preview = "📊 [Bình chọn]";
                } else if (m.getType() == MessageType.PRODUCT) {
                    preview = "📦 [Thẻ Sản phẩm]";
                } else if (m.getType() == MessageType.FILE) {
                    preview = "📎 [Tệp đính kèm]";
                }
                dto.setLastMessageContent(preview);
                dto.setLastMessageTime(m.getCreatedAt());
                dto.setLastMessageStatus(m.getStatus() != null ? m.getStatus().name() : "SENT");
                dto.setLastMessageSender(m.getSenderUsername());
            }
        }

        colleagues.sort((a, b) -> {
            int unreadA = a.getUnreadCount() != null ? a.getUnreadCount() : 0;
            int unreadB = b.getUnreadCount() != null ? b.getUnreadCount() : 0;
            if (unreadA > 0 && unreadB == 0) return -1;
            if (unreadB > 0 && unreadA == 0) return 1;

            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) {
                String nameA = a.getFullName() != null ? a.getFullName() : a.getUsername();
                String nameB = b.getFullName() != null ? b.getFullName() : b.getUsername();
                return nameA.compareToIgnoreCase(nameB);
            }
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return ResponseEntity.ok(colleagues);
    }

    /**
     * Lấy danh sách username đang trực tuyến qua kết nối WebSocket
     */
    @GetMapping("/api/realtime-chat/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        return ResponseEntity.ok(WebSocketEventListener.getOnlineUsernames());
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
        if (user1 == null) user1 = "";
        if (user2 == null) user2 = "";
        String u1 = user1.trim().toLowerCase();
        String u2 = user2.trim().toLowerCase();
        if (u1.compareTo(u2) <= 0) {
            return u1 + "_" + u2;
        } else {
            return u2 + "_" + u1;
        }
    }
}

