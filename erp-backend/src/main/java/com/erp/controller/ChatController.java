package com.erp.controller;

import com.erp.dto.ChatMessageDto;
import com.erp.dto.ChatSendRequest;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import com.erp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // Bắt buộc đã đăng nhập token JWT
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    /**
     * Lấy đúng thực thể User của tài khoản đang gửi request từ Token JWT
     * Đảm bảo không thể giả mạo hoặc lẫn lộn giữa các tài khoản khác nhau
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Chưa xác thực người dùng!");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + auth.getName()));
    }

    // GET /api/chat/history -> Lấy lịch sử chat của CHÍNH tài khoản đang đăng nhập
    @GetMapping("/history")
    public List<ChatMessageDto> getHistory() {
        User currentUser = getCurrentUser();
        return chatService.getChatHistory(currentUser);
    }

    // POST /api/chat/send -> Gửi tin nhắn và nhận phản hồi trợ lý ảo
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatSendRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nội dung câu hỏi không được để trống!"));
        }
        User currentUser = getCurrentUser();
        ChatMessageDto botReply = chatService.sendMessage(currentUser, request.getMessage());
        return ResponseEntity.ok(botReply);
    }

    // DELETE /api/chat/history -> Xóa sạch lịch sử chat của CHÍNH tài khoản đang đăng nhập
    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory() {
        User currentUser = getCurrentUser();
        chatService.clearHistory(currentUser);
        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ lịch sử trò chuyện của tài khoản @" + currentUser.getUsername() + "!"));
    }
}

