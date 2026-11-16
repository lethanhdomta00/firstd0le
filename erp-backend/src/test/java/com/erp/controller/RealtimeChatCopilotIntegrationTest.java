package com.erp.controller;

import com.erp.dto.RealtimeMessageDto;
import com.erp.model.RealtimeMessage;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.RealtimeMessageRepository;
import com.erp.repository.UserRepository;
import com.erp.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RealtimeChatCopilotIntegrationTest {

    @Autowired
    private RealtimeChatController realtimeChatController;

    @Autowired
    private RealtimeMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatService chatService;

    private User testUser;
    private Principal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("test_chat_user").orElseGet(() -> {
            User u = new User();
            u.setUsername("test_chat_user");
            u.setFullName("Lê Văn Test");
            u.setPassword("password");
            u.setRole(Role.USER);
            u.setStatus("ACTIVE");
            return userRepository.save(u);
        });

        userPrincipal = () -> testUser.getUsername();
    }

    @Test
    @DisplayName("1. Mention @copilot trong phòng chat công khai kích hoạt phản hồi tự động của AI Bot")
    void testCopilotMentionInPublicChatCreatesBotReply() {
        String testRoomId = "TEST_ROOM_" + System.currentTimeMillis();
        RealtimeMessageDto sendDto = RealtimeMessageDto.builder()
                .roomId(testRoomId)
                .senderUsername(testUser.getUsername())
                .content("Xin chào @copilot cho tôi xem tình hình doanh thu hôm nay")
                .build();

        realtimeChatController.sendPublicMessage(sendDto, userPrincipal);

        List<RealtimeMessage> messages = messageRepository.findByRoomIdOrderByCreatedAtAsc(testRoomId);
        assertThat(messages).isNotEmpty();

        RealtimeMessage userMsg = messages.stream()
                .filter(m -> testUser.getUsername().equals(m.getSenderUsername()))
                .findFirst()
                .orElse(null);
        assertThat(userMsg).isNotNull();

        RealtimeMessage botReply = messages.stream()
                .filter(m -> "copilot".equals(m.getSenderUsername()))
                .findFirst()
                .orElse(null);

        assertThat(botReply).isNotNull();
        assertThat(botReply.getReplyToId()).isEqualTo(userMsg.getId());
        assertThat(botReply.getContent()).contains("Báo cáo Doanh thu");
        assertThat(botReply.getSenderFullName()).contains("Copilot");
    }

    @Test
    @DisplayName("2. Nhắn tin riêng 1-1 tới copilot được xử lý và phản hồi riêng tư")
    void testCopilotPrivateMessageCreatesBotReply() {
        RealtimeMessageDto sendDto = RealtimeMessageDto.builder()
                .recipientUsername("copilot")
                .senderUsername(testUser.getUsername())
                .content("Cho tôi biết các vai trò trong hệ thống")
                .build();

        realtimeChatController.sendPrivateMessage(sendDto, userPrincipal);

        String privateRoomId = RealtimeChatController.buildPrivateRoomId(testUser.getUsername(), "copilot");
        List<RealtimeMessage> privateMessages = messageRepository.findByRoomIdOrderByCreatedAtAsc(privateRoomId);

        assertThat(privateMessages).isNotEmpty();
        RealtimeMessage botReply = privateMessages.stream()
                .filter(m -> "copilot".equals(m.getSenderUsername()))
                .findFirst()
                .orElse(null);

        assertThat(botReply).isNotNull();
        assertThat(botReply.getRecipientUsername()).isEqualTo(testUser.getUsername());
        assertThat(botReply.getContent()).contains("Ma trận Phân quyền");
        assertThat(botReply.getContent()).contains("ADMIN");
        assertThat(botReply.getContent()).contains("ACCOUNTANT");
    }

    @Test
    @DisplayName("3. ChatService phản hồi thông minh đầy đủ các nghiệp vụ doanh nghiệp đa chiều")
    void testChatServiceEnterpriseKnowledge() {
        // Query Doanh thu
        String revReply = chatService.generateBotReplyForPrompt(testUser, "báo cáo doanh thu");
        assertThat(revReply).contains("Doanh thu");

        // Query Vai trò RBAC
        String rbacReply = chatService.generateBotReplyForPrompt(testUser, "các vai trò phân quyền");
        assertThat(rbacReply).contains("Ma trận Phân quyền");
        assertThat(rbacReply).contains("MANAGER");

        // Query Lịch biểu
        String calReply = chatService.generateBotReplyForPrompt(testUser, "hôm nay tôi có lịch gì");
        assertThat(calReply).contains("Lịch biểu");
    }
}
