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
    private final GeminiService geminiService;

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

        // Lấy lịch sử hội thoại trước đó của user để nạp ngữ cảnh cho Gemini
        List<ChatMessage> userHistory = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId());

        // 1. Lưu tin nhắn của người dùng vào DB với user_id của chính người đó
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .role("USER")
                .content(prompt)
                .build();
        chatMessageRepository.save(userMsg);

        // 2. Thử tạo câu trả lời thông qua Google Gemini AI (nếu có API Key)
        String botReply = null;
        if (geminiService.isConfigured()) {
            botReply = geminiService.generateReply(user, prompt, userHistory);
        }

        // 3. Fallback: Nếu chưa có API Key hoặc Gemini gặp lỗi mạng -> dùng bộ xử lý ERP nội bộ
        if (botReply == null || botReply.trim().isEmpty()) {
            botReply = generateSmartReply(user, prompt);
        }

        // 4. Lưu câu trả lời của trợ lý ảo vào DB với user_id của chính người đó
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
     * Xử lý câu trả lời thông minh tra cứu dữ liệu ERP nội bộ và hội thoại tự nhiên như con người
     */
    private String generateSmartReply(User user, String prompt) {
        String trimmedPrompt = prompt != null ? prompt.trim() : "";
        String lower = trimmedPrompt.toLowerCase();
        // Chuẩn hóa chuỗi loại bỏ dấu câu thừa ở cuối câu để so khớp từ vựng mượt mà
        String normalized = lower.replaceAll("[?.!,;:]+$", "").trim();

        String displayName = user.getFullName() != null && !user.getFullName().isEmpty() 
                ? user.getFullName() 
                : "@" + user.getUsername();

        // 1. Chào hỏi thân thiện & linh hoạt
        if (isGreeting(normalized)) {
            return "Xin chào **" + displayName + "**! 👋 Rất vui được gặp lại bạn nè.\n\n"
                    + "Hôm nay công việc của bạn thế nào rồi? Mình luôn túc trực ở đây để hỗ trợ bạn kiểm tra kho hàng, tra cứu nhân sự hay hướng dẫn thao tác trên hệ thống nhé! 😊";
        }

        // 2. Lời cảm ơn (Giao tiếp tự nhiên, gần gũi, đa dạng)
        if (isThanks(normalized)) {
            String[] thanksReplies = {
                "Dạ không có gì đâu bạn ơi! 😊 Rất vui vì đã hỗ trợ được bạn. Khi nào cần kiểm tra số liệu hay hàng họ gì thì cứ ới mình bất kỳ lúc nào nhé! Chúc bạn một ngày làm việc thật vui vẻ và hiệu quả nha! ✨",
                "Dạ có chi đâu nè! Đồng nghiệp với nhau cả mà. 😉 Cứ yên tâm làm việc nhé, có thắc mắc gì trong ERP thì mình luôn sẵn sàng đồng hành cùng bạn!",
                "Dạ vâng ạ! 🥰 Rất vui khi câu trả lời có ích cho bạn. Chúc bạn một ca làm việc thật suôn sẻ nha!"
            };
            return thanksReplies[Math.abs(normalized.hashCode()) % thanksReplies.length];
        }

        // 3. Lời khen ngợi / Động viên
        if (isCompliment(normalized)) {
            String[] complimentReplies = {
                "Hihi cảm ơn bạn nhiều nha! 🥰 Được bạn khen làm mình vui cả ngày luôn á! Mình vẫn đang học hỏi và hoàn thiện mỗi ngày để hỗ trợ công việc của bạn ngày càng mượt mà hơn đó. Cần gì thêm bạn cứ bảo mình nha!",
                "Dạ cảm ơn lời khen của bạn nhiều nhé! 🌟 Bạn nói thế làm mình có thêm nhiều động lực hỗ trợ hệ thống hơn rồi nè. Có cần kiểm tra thêm dữ liệu gì nữa không bạn ơi?",
                "Cảm ơn bạn nha! 😊 Được đồng hành và hỗ trợ bạn trong công việc là niềm vinh hạnh của mình đấy!"
            };
            return complimentReplies[Math.abs(normalized.hashCode()) % complimentReplies.length];
        }

        // 4. Đồng ý / Xác nhận / Nhận thông tin
        if (isAgreement(normalized)) {
            return "Dạ vâng trí bạn nha! 👍 Bạn cứ thoải mái tiếp tục công việc nhé, mình luôn túc trực ở đây khi bạn cần hỗ trợ thêm.";
        }

        // 5. Hỏi thăm sức khỏe, công việc, tâm sự
        if (isSmallTalk(normalized)) {
            return "Dạ mình là AI nên lúc nào cũng tràn đầy 100% năng lượng để phục vụ bạn nè! 😄 Còn bạn thì sao, hôm nay công việc có nhiều áp lực lắm không? Cần mình hỗ trợ tra cứu hay xử lý số liệu gì cho đỡ việc thì cứ nhắn mình nha!";
        }

        // 6. Hỏi năng lực / Khả năng của trợ lý ảo
        if (isCapabilityQuery(normalized)) {
            return "Mình nắm rất rõ toàn bộ dữ liệu thời gian thực trong hệ thống ERP này đó! 🤖💼 Bạn có thể nhờ mình:\n\n"
                    + "• 📦 **Tra cứu kho hàng:** Xem tồn kho, giá bán của từng sản phẩm cụ thể (ví dụ: *'Doc còn hàng không?'*).\n"
                    + "• 📊 **Báo cáo hàng hóa:** Xem tổng quan kho, danh sách các mặt hàng sắp hết hoặc đã hết.\n"
                    + "• ➕ **Tạo sản phẩm tự động:** Nhắn lệnh như *'Tạo sản phẩm Áo polo giá 180k tồn 30'* là mình lưu thẳng vào Database luôn!\n"
                    + "• 👥 **Thống kê nhân sự:** Đếm số lượng tài khoản người dùng, xem ai đang hoạt động hay bị khóa.\n"
                    + "• 📢 **Thông báo:** Xem các sự kiện, tin tức đang chạy trên thanh Header.\n"
                    + "• 💡 **Hướng dẫn nghiệp vụ:** Hỏi bất kỳ thao tác nào bạn chưa rõ trên giao diện ERP.\n\n"
                    + "Bạn cần mình hỗ trợ kiểm tra mục nào ngay bây giờ nè?";
        }

        // 7. Hỏi thăm danh tính / Tên tuổi
        if (normalized.contains("bạn là ai") || normalized.contains("bạn tên gì") || normalized.contains("ai tạo ra bạn") || normalized.contains("giới thiệu")) {
            return "Chào bạn, mình là **Trợ lý ảo ERP AI** - người đồng nghiệp số luôn túc trực để hỗ trợ bạn vận hành doanh nghiệp mượt mà nhất! 🤖💼\n\n"
                    + "Mình được kết nối trực tiếp với Cơ sở dữ liệu ERP nội bộ nên mọi biến động về kho hàng, sản phẩm hay nhân sự mình đều nắm rất rõ. Bạn cần mình tra cứu số liệu gì hôm nay không nè?";
        }

        // 8. Tạm biệt
        if (isFarewell(normalized)) {
            return "Tạm biệt bạn nhé! 👋 Chúc bạn một ngày làm việc thật nhiều năng lượng và niềm vui. Khi nào cần hỗ trợ thì cứ mở khung chat lên gọi mình nha!";
        }

        // 9. Thả thính / Trêu đùa vui vẻ
        if (normalized.contains("yêu bạn") || normalized.contains("thích bạn") || normalized.contains("xinh") || normalized.contains("đẹp trai") || normalized.contains("lấy tôi")) {
            return "Hihi bạn làm mình ngại ghê á! 😊 Trái tim mình chỉ toàn là dữ liệu số thôi, nhưng tình cảm dành cho công việc hỗ trợ bạn là thật 100% đó nha! Cần mình hỗ trợ thêm gì trong ERP không nè?";
        }

        // 10. Than thở / Mệt mỏi
        if (normalized.contains("mệt") || normalized.contains("áp lực") || normalized.contains("chán") || normalized.contains("stress") || normalized.contains("nhiều việc")) {
            return "Làm việc với số liệu sổ sách cả ngày dễ căng thẳng lắm á bạn ơi! 🥺 Bạn nhớ nghỉ tay uống ngụm nước, vươn vai thư giãn một chút nhé. Còn những việc tra cứu kho bãi hay thống kê cứ để mình lo đỡ cho bạn một tay nha!";
        }

        // 11. Nhóm câu hỏi HƯỚNG DẪN THAO TÁC / CÁCH LÀM (HOW-TO)
        boolean isHowTo = normalized.contains("cách") || normalized.contains("làm sao") || normalized.contains("hướng dẫn") 
                || normalized.contains("thế nào") || normalized.contains("như thế nào") || normalized.contains("bước")
                || normalized.contains("tạo mới") || normalized.contains("thêm mới");

        if (isHowTo && (normalized.contains("sản phẩm") || normalized.contains("hàng") || normalized.contains("món"))) {
            return "💡 **Hướng dẫn tạo Sản phẩm mới trong ERP:**\n\n"
                    + "1. 📦 Nhấp vào menu **Quản lý Sản phẩm** ở thanh điều hướng bên trái (hoặc bấm nút thao tác nhanh ở Dashboard).\n"
                    + "2. ➕ Nhấn vào nút xanh **\"+ Thêm sản phẩm mới\"** ở góc trên bên phải màn hình.\n"
                    + "3. 📝 Điền các trường thông tin:\n"
                    + "   • **Tên sản phẩm**: Nhập tên mặt hàng (bắt buộc)\n"
                    + "   • **Mã SKU**: Mã phân loại sản phẩm (ví dụ: `SKU-001`)\n"
                    + "   • **Giá bán**: Giá niêm yết (VNĐ)\n"
                    + "   • **Tồn kho ban đầu**: Số lượng nhập vào kho\n"
                    + "   • **Hình ảnh**: Tải ảnh sản phẩm lên (Cloudinary)\n"
                    + "4. ✅ Bấm **\"Lưu / Xác nhận\"** để hoàn tất. Sản phẩm sẽ xuất hiện ngay trên danh sách và kho hàng!\n\n"
                    + "*(Mẹo nhỏ: Bạn cũng có thể nhắn thẳng cho mình lệnh ví dụ: 'Tạo sản phẩm Bút bi giá 15k tồn 50' là mình tự tạo luôn đó!)*";
        }

        if (isHowTo && (normalized.contains("tài khoản") || normalized.contains("người dùng") || normalized.contains("nhân viên") || normalized.contains("user"))) {
            return "💡 **Hướng dẫn Quản lý Tài khoản trong ERP:**\n\n"
                    + "1. 👥 Vào menu **Quản lý Tài khoản** trên Sidebar (dành riêng cho Quản trị viên ADMIN).\n"
                    + "2. ➕ Bấm nút **\"+ Thêm người dùng mới\"** ở góc trên.\n"
                    + "3. 📝 Điền thông tin: Username, Email, Mật khẩu ban đầu và Phân quyền (`ADMIN` hoặc `STAFF`).\n"
                    + "4. 🔒 Bạn cũng có thể bấm nút **Khóa / Mở khóa** hoặc **Reset mật khẩu** trực tiếp trên bảng danh sách!";
        }

        if (isHowTo && (normalized.contains("thông báo") || normalized.contains("sự kiện") || normalized.contains("banner"))) {
            return "📢 **Hướng dẫn Cài đặt Thông báo Hệ thống:**\n\n"
                    + "1. 🔔 Vào mục **Quản lý Thông báo** trên thanh Sidebar.\n"
                    + "2. ✏️ Soạn tiêu đề và nội dung thông báo cần phát hành cho toàn doanh nghiệp.\n"
                    + "3. 🚀 Bật công tắc **\"Kích hoạt\"** ➔ Dòng chữ chạy (Marquee) trên Header sẽ hiển thị ngay lập tức!";
        }

        if (normalized.contains("đổi mật khẩu") || normalized.contains("đổi avatar") || normalized.contains("thông tin cá nhân") || normalized.contains("hồ sơ")) {
            return "👤 **Hướng dẫn cập nhật Thông tin Cá nhân:**\n\n"
                    + "1. Nhấp vào ảnh đại diện hoặc tên của bạn ở góc trên bên phải thanh Header.\n"
                    + "2. Chọn **\"Thông tin tài khoản\"** trong menu thả xuống.\n"
                    + "3. Bạn có thể thay đổi ảnh đại diện (avatar), cập nhật Họ tên, Số điện thoại và Tiểu sử.\n"
                    + "4. Bấm **\"Lưu thay đổi\"** để hoàn tất!";
        }

        // 12. Ra lệnh tạo sản phẩm trực tiếp (Function Calling nội bộ)
        if ((normalized.startsWith("tạo sản phẩm") || normalized.startsWith("thêm sản phẩm") || normalized.startsWith("tạo sp")) 
                && !isHowTo) {
            return handleDirectCreateProduct(prompt);
        }

        // 13. Tra cứu chi tiết một sản phẩm cụ thể theo Tên hoặc Mã SKU trong Database
        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            String prodName = p.getName() != null ? p.getName().toLowerCase().trim() : "";
            String prodSku = p.getSku() != null ? p.getSku().toLowerCase().trim() : "";

            boolean nameMatched = !prodName.isEmpty() && (normalized.contains(prodName) || containsWord(normalized, prodName));
            boolean skuMatched = !prodSku.isEmpty() && (normalized.contains(prodSku) || containsWord(normalized, prodSku));

            if (nameMatched || skuMatched) {
                String stockAdvice;
                int currentStock = p.getStock() != null ? p.getStock() : 0;
                if (currentStock <= 0) {
                    stockAdvice = "❌ Hiện tại mặt hàng này đã **hết hàng** trong kho! Bạn nên báo kế toán hoặc phòng mua sắm để nhập thêm hàng nhé.";
                } else if (currentStock <= 10) {
                    stockAdvice = "⚠️ Số lượng trong kho chỉ còn **" + currentStock + " cái** (mức cảnh báo sắp hết). Bạn lưu ý theo dõi nhé!";
                } else {
                    stockAdvice = "✅ Số lượng tồn kho đang rất dồi dào (" + currentStock + " cái), sẵn sàng cung ứng cho các đơn hàng!";
                }

                return String.format("📦 **Thông tin chi tiết sản phẩm từ Database ERP:**\n\n"
                        + "• **Tên sản phẩm:** **%s**\n"
                        + "• **Mã SKU:** `%s`\n"
                        + "• **Giá niêm yết:** **%,.0f đ**\n"
                        + "• **Tồn kho hiện tại:** **%d cái**\n\n"
                        + "💡 *Đánh giá nhanh:* %s",
                        p.getName(),
                        p.getSku() != null ? p.getSku() : "N/A",
                        p.getPrice() != null ? p.getPrice() : 0.0,
                        currentStock,
                        stockAdvice);
            }
        }

        // 14. Tra cứu toàn bộ tồn kho / danh sách sản phẩm tổng quan
        if (normalized.contains("sản phẩm") || normalized.contains("tồn kho") || normalized.contains("kho hàng") 
                || normalized.contains("hàng hóa") || normalized.contains("mặt hàng") || normalized.contains("kho còn gì")) {
            long totalProducts = allProducts.size();
            long outOfStockCount = allProducts.stream().filter(p -> p.getStock() == null || p.getStock() == 0).count();
            long lowStockCount = allProducts.stream().filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 10).count();

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **Báo cáo Kho hàng thực tế từ Database:**\n\n");
            sb.append("• Tổng số mặt hàng: **").append(totalProducts).append(" sản phẩm**\n");
            sb.append("• Hết hàng (0 cái): **").append(outOfStockCount).append(" mặt hàng**\n");
            sb.append("• Cảnh báo sắp hết (1 - 10 cái): **").append(lowStockCount).append(" mặt hàng**\n\n");

            if (!allProducts.isEmpty()) {
                sb.append("**Danh sách mặt hàng trong kho:**\n");
                for (Product p : allProducts) {
                    sb.append(String.format("• **%s** (SKU: `%s`) - Giá: %,.0f đ | Tồn: **%d cái**\n",
                            p.getName(),
                            p.getSku() != null ? p.getSku() : "N/A",
                            p.getPrice() != null ? p.getPrice() : 0.0,
                            p.getStock() != null ? p.getStock() : 0));
                }
            } else {
                sb.append("*(Hiện tại chưa có sản phẩm nào trong kho hàng)*\n");
            }

            return sb.toString();
        }

        // 15. Tra cứu người dùng / tài khoản nhân sự
        if (normalized.contains("tài khoản") || normalized.contains("người dùng") || normalized.contains("user") 
                || normalized.contains("nhân viên") || normalized.contains("nhân sự")) {
            long totalUsers = userRepository.count();
            List<User> users = userRepository.findAll();
            long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
            long lockedUsers = users.stream().filter(u -> "LOCKED".equalsIgnoreCase(u.getStatus())).count();

            return "👥 **Thống kê Tài khoản Nhân sự trên Hệ thống:**\n\n"
                    + "• Tổng số người dùng: **" + totalUsers + " tài khoản**\n"
                    + "• Đang hoạt động bình thường: **" + activeUsers + " tài khoản**\n"
                    + "• Đang bị khóa truy cập: **" + lockedUsers + " tài khoản**\n\n"
                    + (user.getRole() == com.erp.model.Role.ADMIN 
                        ? "💡 *Bạn có quyền Quản trị viên (ADMIN) nên có thể vào mục **Quản lý Tài khoản** trên Sidebar để thêm mới hoặc mở khóa cho nhân viên nhé.*" 
                        : "💡 *Bạn đang thao tác với vai trò Nhân viên.*");
        }

        // 16. Tra cứu thông báo / sự kiện trên Header
        if (normalized.contains("thông báo") || normalized.contains("sự kiện") || normalized.contains("banner") || normalized.contains("tin tức")) {
            Announcement active = announcementRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElse(null);
            if (active != null) {
                return "📢 **Thông báo đang chạy trên thanh Header:**\n\n"
                        + "• **Tiêu đề:** " + active.getTitle() + "\n"
                        + "• **Nội dung:** \"" + active.getContent() + "\"\n"
                        + "• **Loại thông điệp:** " + active.getType() + "\n\n"
                        + "💡 *Thông báo này đang được phát hành rộng rãi cho toàn bộ nhân sự công ty.*";
            } else {
                return "Dạ hiện tại hệ thống chưa có thông báo nào được kích hoạt trên thanh Header bạn nhé! Nếu là ADMIN, bạn có thể vào mục **Quản lý Thông báo** để soạn tin mới.";
            }
        }

        // 17. Phản hồi tự nhiên, gần gũi khi không khớp (KHÔNG DÙNG CÂU MẪU MÁY MÓC)
        return "Dạ mình lắng nghe đây bạn ơi! 😊\n\n"
                + "Câu này mình chưa hiểu trọn vẹn ý bạn lắm nè. Có phải bạn đang muốn kiểm tra thông tin nào trong hệ thống ERP không? Bạn có thể thử:\n"
                + "• 📦 Tra cứu tồn kho hoặc giá sản phẩm (ví dụ: *'Sản phẩm Doc còn bao nhiêu cái?'*)\n"
                + "• 📊 Xem tổng quan toàn bộ hàng hóa trong kho\n"
                + "• 👥 Xem thống kê tài khoản người dùng\n"
                + "• ➕ Tự động tạo sản phẩm mới vào kho\n\n"
                + "Bạn muốn mình kiểm tra phần nào thì cứ bảo mình nha, hoặc bấm vào các nút gợi ý nhanh bên dưới nhé! 👇";
    }

    private boolean isGreeting(String text) {
        if (text == null) return false;
        String t = text.trim();
        if (t.equals("hi") || t.equals("hello") || t.equals("alo") || t.equals("chào") 
                || t.equals("chao") || t.equals("hey") || t.equals("ê") || t.equals("e")
                || t.equals("bạn ơi") || t.equals("bot ơi") || t.equals("ai ơi")) {
            return true;
        }
        return t.startsWith("chào ") || t.startsWith("xin chào") || t.startsWith("hello ") 
                || t.startsWith("hi ") || t.startsWith("alo ") || t.startsWith("hey ");
    }

    private boolean isThanks(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.contains("cảm ơn") || t.contains("cam on") || t.contains("thanks") 
                || t.contains("thank you") || t.contains("tks") || t.contains("cmon") 
                || t.contains("cảm ơn nha") || t.contains("cảm ơn nhiều") || t.contains("thank");
    }

    private boolean isCompliment(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.contains("giỏi") || t.contains("thông minh") || t.contains("tốt lắm") 
                || t.contains("hay quá") || t.contains("được đấy") || t.contains("xịn") 
                || t.contains("đỉnh") || t.contains("pro") || t.contains("tuyệt vời") 
                || t.contains("chất lượng") || t.contains("good job");
    }

    private boolean isAgreement(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.equals("ok") || t.equals("okee") || t.equals("oke") || t.equals("okie") || t.equals("oki")
                || t.equals("ok nha") || t.equals("okie nha") || t.equals("được rồi") || t.equals("hiểu rồi") 
                || t.equals("rõ rồi") || t.equals("chuẩn") || t.equals("đúng rồi") || t.equals("dạ vâng")
                || t.equals("vâng") || t.equals("ừ") || t.equals("uh") || t.equals("uhm")
                || t.startsWith("ok ") || t.startsWith("okee ") || t.startsWith("oke ") || t.startsWith("okie ");
    }

    private boolean isSmallTalk(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.contains("khỏe không") || t.contains("khoe khong") || t.contains("hôm nay thế nào")
                || t.contains("đang làm gì") || t.contains("tâm sự") || t.contains("mệt không");
    }

    private boolean isCapabilityQuery(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.contains("làm được gì") || t.contains("giúp được gì") || t.contains("biết làm gì")
                || t.contains("chức năng") || t.contains("tính năng") || t.contains("có tác dụng gì");
    }

    private boolean isFarewell(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.contains("tạm biệt") || t.contains("bye") || t.contains("goodbye") 
                || t.contains("hẹn gặp lại") || t.contains("ngủ ngon") || t.contains("off đây");
    }

    private boolean containsWord(String fullText, String word) {
        if (fullText == null || word == null) return false;
        return java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(fullText).find();
    }

    private String handleDirectCreateProduct(String prompt) {
        try {
            String clean = prompt.replaceAll("^(?i)(tạo sản phẩm|thêm sản phẩm|tạo sp)\\s*", "").trim();
            String name = clean;
            java.math.BigDecimal price = java.math.BigDecimal.valueOf(100000);
            int stock = 10;

            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("(?i)(giá\\s*[:=]?\\s*)(\\d+[kK]?)");
            java.util.regex.Matcher priceMatcher = pricePattern.matcher(clean);
            if (priceMatcher.find()) {
                String priceStr = priceMatcher.group(2).toLowerCase();
                double pVal = priceStr.endsWith("k") 
                        ? Double.parseDouble(priceStr.replace("k", "")) * 1000 
                        : Double.parseDouble(priceStr);
                price = java.math.BigDecimal.valueOf(pVal);
                name = name.replace(priceMatcher.group(0), "");
            }

            java.util.regex.Pattern stockPattern = java.util.regex.Pattern.compile("(?i)(tồn\\s*(kho)?\\s*[:=]?\\s*)(\\d+)");
            java.util.regex.Matcher stockMatcher = stockPattern.matcher(clean);
            if (stockMatcher.find()) {
                stock = Integer.parseInt(stockMatcher.group(3));
                name = name.replace(stockMatcher.group(0), "");
            }

            name = name.replaceAll("[,;\\s]+", " ").trim();
            if (name.isEmpty()) {
                name = "Sản phẩm mới " + (100 + new java.util.Random().nextInt(900));
            }

            String sku = "SKU-" + (1000 + new java.util.Random().nextInt(9000));

            Product product = new Product();
            product.setName(name);
            product.setSku(sku);
            product.setPrice(price);
            product.setStock(stock);
            product.setImageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60");

            Product saved = productRepository.save(product);

            return String.format("🎉 **Đã tạo thành công sản phẩm mới vào Database ERP!**\n\n"
                    + "• **ID:** %d\n"
                    + "• **Tên sản phẩm:** **%s**\n"
                    + "• **Mã SKU:** `%s`\n"
                    + "• **Giá bán:** **%,.0f đ**\n"
                    + "• **Tồn kho ban đầu:** **%d cái**\n\n"
                    + "✅ Sản phẩm đã được lưu trữ vĩnh viễn và hiển thị ngay trên trang **Quản lý Sản phẩm**!",
                    saved.getId(), saved.getName(), saved.getSku(), saved.getPrice(), saved.getStock());
        } catch (Exception e) {
            return "⚠️ Không thể tự động tạo sản phẩm vào Database: " + e.getMessage();
        }
    }
}

