package com.erp.service;

import com.erp.dto.ChatMessageDto;
import com.erp.model.Announcement;
import com.erp.model.ChatMessage;
import com.erp.model.Product;
import com.erp.model.User;
import com.erp.repository.AnnouncementRepository;
import com.erp.repository.ChatMessageRepository;
import com.erp.repository.ProductRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;

    /**
     * Lấy toàn bộ lịch sử trò chuyện RIÊNG BIỆT của tài khoản đang đăng nhập
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(User user) {
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .map(ChatMessageDto::fromEntity)
                .toList();
    }

    /**
     * Gửi tin nhắn mới và nhận phản hồi trợ lý thông minh theo từng tài khoản
     */
    @Transactional
    public ChatMessageDto sendMessage(User user, String userContent) {
        String prompt = userContent != null ? userContent.trim() : "";

        // 1. Lưu tin nhắn của người dùng vào DB với user_id của chính người đó
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .role("USER")
                .content(prompt)
                .build();
        chatMessageRepository.save(userMsg);

        // 2. Tạo câu trả lời thông minh dựa trên dữ liệu ERP
        String botReply = generateSmartReply(user, prompt);

        // 3. Lưu câu trả lời của trợ lý ảo vào DB với user_id của chính người đó
        ChatMessage botMsg = ChatMessage.builder()
                .user(user)
                .role("ASSISTANT")
                .content(botReply)
                .build();
        ChatMessage savedBotMsg = chatMessageRepository.save(botMsg);

        return ChatMessageDto.fromEntity(savedBotMsg);
    }

    /**
     * Xóa sạch lịch sử trò chuyện của tài khoản đang đăng nhập
     */
    @Transactional
    public void clearHistory(User user) {
        chatMessageRepository.deleteByUserId(user.getId());
    }

    /**
     * Xử lý câu trả lời thông minh tra cứu dữ liệu ERP nội bộ
     */
    private String generateSmartReply(User user, String prompt) {
        String lower = prompt.toLowerCase();
        String displayName = user.getFullName() != null && !user.getFullName().isEmpty() 
                ? user.getFullName() 
                : "@" + user.getUsername();

        // 1. Chào hỏi
        if (lower.contains("chào") || lower.contains("hello") || lower.contains("hi") || lower.contains("alo")) {
            return "Xin chào **" + displayName + "**! 👋 Tôi là **Trợ lý ảo ERP AI** của bạn.\n\n"
                    + "Tôi có thể hỗ trợ bạn tra cứu trực tiếp:\n"
                    + "• 📦 **Tồn kho sản phẩm** & các mặt hàng sắp hết\n"
                    + "• 👥 **Tài khoản người dùng** trong hệ thống\n"
                    + "• 📢 **Thông báo & sự kiện** đang phát hành\n\n"
                    + "Bạn cần tôi kiểm tra thông tin gì ngay bây giờ?";
        }

        // 2. Tra cứu tồn kho / sản phẩm
        if (lower.contains("sản phẩm") || lower.contains("tồn kho") || lower.contains("kho hàng") || lower.contains("hàng hóa")) {
            long totalProducts = productRepository.count();
            List<Product> allProducts = productRepository.findAll();
            long outOfStockCount = allProducts.stream().filter(p -> p.getStock() == null || p.getStock() == 0).count();
            long lowStockCount = allProducts.stream().filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 10).count();

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **Báo cáo Kho hàng ERP:**\n\n");
            sb.append("• Tổng số mặt hàng: **").append(totalProducts).append(" sản phẩm**\n");
            sb.append("• Sản phẩm đã hết hàng (0 cái): **").append(outOfStockCount).append(" mặt hàng**\n");
            sb.append("• Cảnh báo sắp hết (1 - 10 cái): **").append(lowStockCount).append(" mặt hàng**\n\n");

            if (outOfStockCount > 0) {
                sb.append("⚠️ *Lưu ý: Có mặt hàng đang hết hàng, bạn vui lòng vào mục **Quản lý Sản phẩm** để bổ sung thêm tồn kho nhé!*");
            } else {
                sb.append("✅ *Tình trạng kho hàng hiện tại đang được duy trì rất tốt!*");
            }
            return sb.toString();
        }

        // 3. Tra cứu người dùng / tài khoản
        if (lower.contains("tài khoản") || lower.contains("người dùng") || lower.contains("user") || lower.contains("nhân viên")) {
            long totalUsers = userRepository.count();
            List<User> users = userRepository.findAll();
            long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
            long lockedUsers = users.stream().filter(u -> "LOCKED".equalsIgnoreCase(u.getStatus())).count();

            return "👥 **Thống kê Tài khoản Hệ thống:**\n\n"
                    + "• Tổng số tài khoản: **" + totalUsers + " người dùng**\n"
                    + "• Đang hoạt động: **" + activeUsers + " tài khoản**\n"
                    + "• Đang bị khóa: **" + lockedUsers + " tài khoản**\n\n"
                    + (user.getRole() == com.erp.model.Role.ADMIN 
                        ? "💡 *Bạn có quyền Quản trị viên (ADMIN) nên có thể vào mục **Quản lý Tài khoản** để thêm, sửa hoặc mở khóa cho nhân viên.*" 
                        : "💡 *Bạn đang đăng nhập với vai trò Nhân viên.*");
        }

        // 4. Tra cứu thông báo / sự kiện
        if (lower.contains("thông báo") || lower.contains("sự kiện") || lower.contains("banner")) {
            Announcement active = announcementRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElse(null);
            if (active != null) {
                return "📢 **Thông báo đang chạy trên Header:**\n\n"
                        + "• **Tiêu đề:** " + active.getTitle() + "\n"
                        + "• **Nội dung:** \"" + active.getContent() + "\"\n"
                        + "• **Loại thông điệp:** " + active.getType();
            } else {
                return "Hiện tại hệ thống chưa kích hoạt thông báo nào trên thanh Header.";
            }
        }

        // 5. Câu trả lời mặc định thông minh
        return "Tôi đã nhận được câu hỏi: *\"" + prompt + "\"*.\n\n"
                + "Hiện tại tôi đã được liên kết với cơ sở dữ liệu nội bộ của bạn. Bạn có thể thử các câu hỏi nhanh như:\n"
                + "• *\"Kiểm tra tồn kho sản phẩm\"*\n"
                + "• *\"Thống kê tài khoản người dùng\"*\n"
                + "• *\"Thông báo hệ thống hiện tại là gì?\"*\n\n"
                + "*(Ở buổi học tiếp theo, chúng ta sẽ kết nối trực tiếp với **Google Gemini AI API** để tôi có thể trò chuyện tự do và giải đáp mọi tình huống phức tạp hơn nữa!)*";
    }
}

