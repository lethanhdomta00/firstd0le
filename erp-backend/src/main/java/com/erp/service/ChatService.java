package com.erp.service;

import com.erp.dto.ChatMessageDto;
import com.erp.model.Announcement;
import com.erp.model.ChatMessage;
import com.erp.model.Product;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.AnnouncementRepository;
import com.erp.repository.ChatMessageRepository;
import com.erp.repository.ProductRepository;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 2. Thử tạo câu trả lời thông qua Google Gemini AI (nếu có API Key hợp lệ)
        String botReply = null;
        if (geminiService.isConfigured()) {
            botReply = geminiService.generateReply(user, prompt, userHistory);
        }

        // 3. Fallback: Nếu chưa có API Key hoặc Gemini gặp lỗi mạng -> dùng bộ xử lý thông minh nội bộ
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
     * Xử lý câu trả lời thông minh tra cứu dữ liệu ERP nội bộ và hội thoại tự nhiên, thời gian thực
     */
    private String generateSmartReply(User user, String prompt) {
        String trimmedPrompt = prompt != null ? prompt.trim() : "";
        String lower = trimmedPrompt.toLowerCase();
        // Chuẩn hóa chuỗi loại bỏ dấu câu thừa ở cuối câu
        String normalized = lower.replaceAll("[?.!,;:]+$", "").trim();
        // Chuỗi không dấu để so khớp linh hoạt
        String unaccented = unaccent(normalized);

        String displayName = user.getFullName() != null && !user.getFullName().isEmpty() 
                ? user.getFullName() 
                : "@" + user.getUsername();

        // 1. Máy tính / Tính toán toán học cơ bản
        String mathResult = evaluateMathExpression(trimmedPrompt, unaccented);
        if (mathResult != null) {
            return mathResult;
        }

        // 2. Hỏi THỜI GIAN, NGÀY THÁNG, GIỜ GIẤC HIỆN TẠI ("Mấy giờ rồi", "Hôm nay thứ mấy",...)
        if (isTimeOrDateQuery(normalized, unaccented)) {
            return buildTimeAndDateResponse();
        }

        // 3. Hỏi THÔNG TIN TÀI KHOẢN, DANH TÍNH ("Tôi là ai", "Quyền của tôi là gì",...)
        if (isUserIdentityQuery(unaccented)) {
            return buildUserIdentityResponse(user, displayName);
        }

        // 4. Hỏi HỆ THỐNG, SERVER, HẠ TẦNG, PHIÊN BẢN ("Server ở đâu", "Hệ thống phiên bản mấy",...)
        if (isSystemInfoQuery(unaccented)) {
            return buildSystemInfoResponse();
        }

        // 5. Chuyện cười, giải trí, thư giãn ("Kể chuyện cười", "Truyện cười",...)
        if (isJokeQuery(unaccented)) {
            return buildJokeResponse(normalized);
        }

        // 6. Thời tiết, nhiệt độ ("Thời tiết hôm nay", "Trời mưa không",...)
        if (isWeatherQuery(unaccented)) {
            return "⛅ **Dự báo Thời tiết & Lời nhắn nhủ:**\n\n"
                    + "Dạ hiện tại mình là Trợ lý ảo chuyên trách quản trị nội bộ ERP (quản lý kho hàng, sản phẩm, đơn hàng và nhân sự) nên chưa kết nối trực tiếp với cảm biến thời tiết vệ tinh nè! 🛰️\n\n"
                    + "Tuy nhiên, bạn nhớ xem dự báo thời tiết trên điện thoại trước khi ra ngoài, nhớ mang theo ô (dù) và uống đủ nước để giữ gìn sức khỏe làm việc thật tốt nhé! 😊";
        }

        // 7. Chào hỏi thân thiện theo thời gian trong ngày
        if (isGreeting(normalized, unaccented)) {
            ZonedDateTime nowVn = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            int hour = nowVn.getHour();
            String timeGreeting = (hour >= 5 && hour < 12) ? "Chào buổi sáng! 🌅 Chúc bạn một ngày mới tràn đầy năng lượng và làm việc thật năng suất nhé."
                    : (hour >= 12 && hour < 14) ? "Chào buổi trưa bạn nha! 🍱 Bạn đã ăn trưa và nghỉ ngơi chút chưa?"
                    : (hour >= 14 && hour < 18) ? "Chào buổi chiều tốt lành! ☕ Chúc công việc buổi chiều của bạn thật suôn sẻ."
                    : (hour >= 18 && hour < 23) ? "Chào buổi tối an lành! 🌙 Bạn đã hoàn tất công việc hôm nay chưa nè?"
                    : "Đã khuya muộn rồi đó bạn ơi! 🦉 Bạn nhớ nghỉ ngơi sớm giữ gìn sức khỏe nhé.";

            return "Xin chào **" + displayName + "**! 👋 Rất vui được gặp lại bạn.\n\n"
                    + timeGreeting + "\n\n"
                    + "Mình luôn túc trực ở đây để hỗ trợ bạn kiểm tra kho hàng, tra cứu nhân sự, hỗ trợ tính toán hay hướng dẫn thao tác trên hệ thống nhé! 😊";
        }

        // 8. Lời cảm ơn
        if (isThanks(unaccented)) {
            String[] thanksReplies = {
                "Dạ không có gì đâu bạn ơi! 😊 Rất vui vì đã hỗ trợ được bạn. Khi nào cần kiểm tra số liệu hay hàng họ gì thì cứ ới mình bất kỳ lúc nào nhé! Chúc bạn một ngày làm việc thật vui vẻ và hiệu quả nha! ✨",
                "Dạ có chi đâu nè! Đồng nghiệp với nhau cả mà. 😉 Cứ yên tâm làm việc nhé, có thắc mắc gì trong ERP thì mình luôn sẵn sàng đồng hành cùng bạn!",
                "Dạ vâng ạ! 🥰 Rất vui khi câu trả lời có ích cho bạn. Chúc bạn một ca làm việc thật suôn sẻ nha!"
            };
            return thanksReplies[Math.abs(normalized.hashCode()) % thanksReplies.length];
        }

        // 9. Lời khen ngợi / Động viên
        if (isCompliment(unaccented)) {
            String[] complimentReplies = {
                "Hihi cảm ơn bạn nhiều nha! 🥰 Được bạn khen làm mình vui cả ngày luôn á! Mình vẫn đang học hỏi và hoàn thiện mỗi ngày để hỗ trợ công việc của bạn ngày càng mượt mà hơn đó. Cần gì thêm bạn cứ bảo mình nha!",
                "Dạ cảm ơn lời khen của bạn nhiều nhé! 🌟 Bạn nói thế làm mình có thêm nhiều động lực hỗ trợ hệ thống hơn rồi nè. Có cần kiểm tra thêm dữ liệu gì nữa không bạn ơi?",
                "Cảm ơn bạn nha! 😊 Được đồng hành và hỗ trợ bạn trong công việc là niềm vinh hạnh của mình đấy!"
            };
            return complimentReplies[Math.abs(normalized.hashCode()) % complimentReplies.length];
        }

        // 10. Đồng ý / Xác nhận / Nhận thông tin
        if (isAgreement(unaccented)) {
            return "Dạ vâng trí bạn nha! 👍 Bạn cứ thoải mái tiếp tục công việc nhé, mình luôn túc trực ở đây khi bạn cần hỗ trợ thêm.";
        }

        // 11. Hỏi thăm sức khỏe, công việc, tâm sự
        if (isSmallTalk(unaccented)) {
            return "Dạ mình là AI nên lúc nào cũng tràn đầy 100% năng lượng để phục vụ bạn nè! 😄 Còn bạn thì sao, hôm nay công việc có nhiều áp lực lắm không? Cần mình hỗ trợ tra cứu hay xử lý số liệu gì cho đỡ việc thì cứ nhắn mình nha!";
        }

        // 12. Hỏi năng lực / Khả năng của trợ lý ảo
        if (isCapabilityQuery(unaccented)) {
            return "Mình nắm rất rõ toàn bộ dữ liệu thời gian thực trong hệ thống ERP này đó! 🤖💼 Bạn có thể nhờ mình:\n\n"
                    + "• ⏰ **Thời gian thực:** Trả lời chính xác ngày, giờ, thứ hiện tại (ví dụ: *'Mấy giờ rồi?'*).\n"
                    + "• 👤 **Tài khoản cá nhân:** Xem thông tin tài khoản và quyền hạn (ví dụ: *'Tôi là ai?'*).\n"
                    + "• 📦 **Tra cứu kho hàng:** Xem tồn kho, giá bán của từng sản phẩm cụ thể (ví dụ: *'Doc còn hàng không?'*).\n"
                    + "• 📊 **Báo cáo hàng hóa:** Xem tổng quan kho, danh sách các mặt hàng sắp hết hoặc đã hết.\n"
                    + "• ➕ **Tạo sản phẩm tự động:** Nhắn lệnh như *'Tạo sản phẩm Áo polo giá 180k tồn 30'* là mình lưu thẳng vào Database luôn!\n"
                    + "• 👥 **Thống kê nhân sự:** Đếm số lượng tài khoản người dùng, xem ai đang hoạt động hay bị khóa.\n"
                    + "• 🏢 **Hạ tầng & Server:** Xem thông số máy chủ, công nghệ đang chạy.\n"
                    + "• 🧮 **Máy tính:** Tính toán nhanh các biểu thức số học (ví dụ: *'15% của 500k'*, *'Tính 12 * 8'*).\n"
                    + "• 📢 **Thông báo:** Xem các sự kiện, tin tức đang chạy trên thanh Header.\n"
                    + "• 💡 **Hướng dẫn nghiệp vụ:** Hỏi bất kỳ thao tác nào bạn chưa rõ trên giao diện ERP.\n\n"
                    + "Bạn cần mình hỗ trợ kiểm tra mục nào ngay bây giờ nè?";
        }

        // 13. Hỏi thăm danh tính / Tên tuổi Trợ lý ảo
        if (unaccented.contains("ban la ai") || unaccented.contains("ban ten gi") || unaccented.contains("ai tao ra ban") || unaccented.contains("gioi thieu")) {
            return "Chào bạn, mình là **Trợ lý ảo ERP AI** - người đồng nghiệp số luôn túc trực để hỗ trợ bạn vận hành doanh nghiệp mượt mà nhất! 🤖💼\n\n"
                    + "Mình được kết nối trực tiếp với Cơ sở dữ liệu ERP nội bộ nên mọi biến động về kho hàng, sản phẩm hay nhân sự mình đều nắm rất rõ. Bạn cần mình tra cứu số liệu gì hôm nay không nè?";
        }

        // 14. Tạm biệt
        if (isFarewell(unaccented)) {
            return "Tạm biệt bạn nhé! 👋 Chúc bạn một ngày làm việc thật nhiều năng lượng và niềm vui. Khi nào cần hỗ trợ thì cứ mở khung chat lên gọi mình nha!";
        }

        // 15. Thả thính / Trêu đùa vui vẻ
        if (unaccented.contains("yeu ban") || unaccented.contains("thich ban") || unaccented.contains("xinh") || unaccented.contains("dep trai") || unaccented.contains("lay toi")) {
            return "Hihi bạn làm mình ngại ghê á! 😊 Trái tim mình chỉ toàn là dữ liệu số thôi, nhưng tình cảm dành cho công việc hỗ trợ bạn là thật 100% đó nha! Cần mình hỗ trợ thêm gì trong ERP không nè?";
        }

        // 16. Than thở / Mệt mỏi
        if (unaccented.contains("met") || unaccented.contains("ap luc") || unaccented.contains("chan") || unaccented.contains("stress") || unaccented.contains("nhieu viec")) {
            return "Làm việc với số liệu sổ sách cả ngày dễ căng thẳng lắm á bạn ơi! 🥺 Bạn nhớ nghỉ tay uống ngụm nước, vươn vai thư giãn một chút nhé. Còn những việc tra cứu kho bãi hay thống kê cứ để mình lo đỡ cho bạn một tay nha!";
        }

        // 17. Nhóm câu hỏi HƯỚNG DẪN THAO TÁC / CÁCH LÀM (HOW-TO)
        boolean isHowTo = containsAny(unaccented, "cach", "lam sao", "huong dan", "the nao", "nhu the nao", "buoc", "tao moi", "them moi");

        if (isHowTo && containsAny(unaccented, "san pham", "hang", "mon")) {
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

        if (isHowTo && containsAny(unaccented, "tai khoan", "nguoi dung", "nhan vien", "user")) {
            return "💡 **Hướng dẫn Quản lý Tài khoản trong ERP:**\n\n"
                    + "1. 👥 Vào menu **Quản lý Tài khoản** trên Sidebar (dành riêng cho Quản trị viên ADMIN).\n"
                    + "2. ➕ Bấm nút **\"+ Thêm người dùng mới\"** ở góc trên.\n"
                    + "3. 📝 Điền thông tin: Username, Email, Mật khẩu ban đầu và Phân quyền (`ADMIN` hoặc `USER`).\n"
                    + "4. 🔒 Bạn cũng có thể bấm nút **Khóa / Mở khóa** hoặc **Reset mật khẩu** trực tiếp trên bảng danh sách!";
        }

        if (isHowTo && containsAny(unaccented, "thong bao", "su kien", "banner")) {
            return "📢 **Hướng dẫn Cài đặt Thông báo Hệ thống:**\n\n"
                    + "1. 🔔 Vào mục **Quản lý Thông báo** trên thanh Sidebar.\n"
                    + "2. ✏️ Soạn tiêu đề và nội dung thông báo cần phát hành cho toàn doanh nghiệp.\n"
                    + "3. 🚀 Bật công tắc **\"Kích hoạt\"** ➔ Dòng chữ chạy (Marquee) trên Header sẽ hiển thị ngay lập tức!";
        }

        if (containsAny(unaccented, "doi mat khau", "doi avatar", "thong tin ca nhan", "ho so")) {
            return "👤 **Hướng dẫn cập nhật Thông tin Cá nhân:**\n\n"
                    + "1. Nhấp vào ảnh đại diện hoặc tên của bạn ở góc trên bên phải thanh Header.\n"
                    + "2. Chọn **\"Thông tin tài khoản\"** trong menu thả xuống.\n"
                    + "3. Bạn có thể thay đổi ảnh đại diện (avatar), cập nhật Họ tên, Số điện thoại và Tiểu sử.\n"
                    + "4. Bấm **\"Lưu thay đổi\"** để hoàn tất!";
        }

        // 18. Ra lệnh tạo sản phẩm trực tiếp (Function Calling nội bộ)
        if ((unaccented.startsWith("tao san pham") || unaccented.startsWith("them san pham") || unaccented.startsWith("tao sp")) 
                && !isHowTo) {
            return handleDirectCreateProduct(prompt);
        }

        // 19. Tra cứu chi tiết một sản phẩm cụ thể theo Tên hoặc Mã SKU trong Database
        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            String prodName = p.getName() != null ? p.getName().trim() : "";
            String prodSku = p.getSku() != null ? p.getSku().trim() : "";
            String uName = unaccent(prodName);
            String uSku = unaccent(prodSku);

            boolean nameMatched = !uName.isEmpty() && (unaccented.contains(uName) || containsWord(unaccented, uName));
            boolean skuMatched = !uSku.isEmpty() && (unaccented.contains(uSku) || containsWord(unaccented, uSku));

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

        // 20. Tra cứu toàn bộ tồn kho / danh sách sản phẩm tổng quan
        if (containsAny(unaccented, "san pham", "ton kho", "kho hang", "hang hoa", "mat hang", "kho con gi")) {
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

        // 21. Tra cứu người dùng / tài khoản nhân sự
        if (containsAny(unaccented, "tai khoan", "nguoi dung", "user", "nhan vien", "nhan su")) {
            long totalUsers = userRepository.count();
            List<User> users = userRepository.findAll();
            long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
            long lockedUsers = users.stream().filter(u -> "LOCKED".equalsIgnoreCase(u.getStatus())).count();

            return "👥 **Thống kê Tài khoản Nhân sự trên Hệ thống:**\n\n"
                    + "• Tổng số người dùng: **" + totalUsers + " tài khoản**\n"
                    + "• Đang hoạt động bình thường: **" + activeUsers + " tài khoản**\n"
                    + "• Đang bị khóa truy cập: **" + lockedUsers + " tài khoản**\n\n"
                    + (user.getRole() == Role.ADMIN 
                        ? "💡 *Bạn có quyền Quản trị viên (ADMIN) nên có thể vào mục **Quản lý Tài khoản** trên Sidebar để thêm mới hoặc mở khóa cho nhân viên nhé.*" 
                        : "💡 *Bạn đang thao tác với vai trò Nhân viên.*");
        }

        // 22. Tra cứu thông báo / sự kiện trên Header
        if (containsAny(unaccented, "thong bao", "su kien", "banner", "tin tuc")) {
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

        // 23. Phản hồi thông minh linh hoạt (Không dập khuôn máy móc)
        return buildSmartFallback(trimmedPrompt);
    }

    // ==================================================================================
    // CÁC HÀM XỬ LÝ TRỢ LÝ THỜI GIAN THỰC & HỘI THOẠI THÔNG MINH
    // ==================================================================================

    /**
     * Nhận diện câu hỏi liên quan đến THỜI GIAN, NGÀY THÁNG, GIỜ GIẤC
     */
    private boolean isTimeOrDateQuery(String text, String unaccented) {
        if (text == null || unaccented == null) return false;
        String u = unaccented.trim();
        if (u.equals("gio") || u.equals("time") || u.equals("clock") || u.equals("may gio") 
                || u.equals("may h") || u.equals("may gio roi") || u.equals("may h roi") || u.equals("bay gio la may gio")) {
            return true;
        }
        return containsAny(u, 
                "may gio", "may h", "bay gio la", "thoi gian hien tai", "thoi gian", 
                "dong ho", "gio hien tai", "cho biet gio", "xem gio", "gio giac",
                "hom nay ngay", "ngay may", "ngay bao nhieu", "thu may", "hom nay la thu may", 
                "hom nay ngay gi", "nam nay nam", "thang may", "ngay thang nam", "lich hom nay");
    }

    /**
     * Tạo câu trả lời thời gian thực tế chính xác đến từng giây kèm lời chào theo ca làm việc
     */
    private String buildTimeAndDateResponse() {
        ZonedDateTime nowVn = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dayOfWeekVn = switch (nowVn.getDayOfWeek()) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };

        int hour = nowVn.getHour();
        String periodGreeting;
        if (hour >= 5 && hour < 12) {
            periodGreeting = "🌅 **Chào buổi sáng!** Chúc bạn một ngày mới tràn đầy năng lượng và giải quyết công việc thật hiệu quả nhé!";
        } else if (hour >= 12 && hour < 14) {
            periodGreeting = "🍱 **Đã đến giờ nghỉ trưa rồi nè!** Bạn nhớ ăn trưa đầy đủ và chợp mắt một lát để nạp lại năng lượng nhé.";
        } else if (hour >= 14 && hour < 18) {
            periodGreeting = "☕ **Buổi chiều tốt lành!** Chúc công việc buổi chiều của bạn thật hanh thông và suôn sẻ!";
        } else if (hour >= 18 && hour < 23) {
            periodGreeting = "🌙 **Buổi tối an lành!** Nếu còn tăng ca thì cố gắng hoàn thành sớm để về nghỉ ngơi bên gia đình nhé.";
        } else {
            periodGreeting = "🦉 **Đã khuya lắm rồi đó bạn ơi!** Bạn nhớ giữ gìn sức khỏe và đi ngủ sớm nha, ERP lúc nào cũng sẵn sàng hỗ trợ bạn!";
        }

        return String.format("⏰ **Thời gian thực tế trên hệ thống ERP:**\n\n"
                + "• Bây giờ là: **%s** (Giờ Việt Nam, GMT+7)\n"
                + "• Hôm nay là: **%s**, ngày **%s**\n\n"
                + "%s",
                nowVn.format(timeFmt), dayOfWeekVn, nowVn.format(dateFmt), periodGreeting);
    }

    /**
     * Nhận diện câu hỏi danh tính người dùng ("Tôi là ai", "Quyền của tôi",...)
     */
    private boolean isUserIdentityQuery(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented,
                "toi la ai", "ai day", "tai khoan cua toi", "thong tin cua toi", 
                "ho so cua toi", "vai tro cua toi", "quyen cua toi", "toi co quyen gi", 
                "ten toi la gi", "email cua toi", "ban biet toi khong", "ai dang dang nhap",
                "profile cua toi", "thong tin toi");
    }

    /**
     * Phản hồi thông tin hồ sơ và phân quyền của người dùng đang đăng nhập
     */
    private String buildUserIdentityResponse(User user, String displayName) {
        String roleName = user.getRole() == Role.ADMIN
                ? "Quản trị viên cấp cao (ADMIN)"
                : "Nhân viên tác nghiệp (USER / STAFF)";
        String permissions = user.getRole() == Role.ADMIN
                ? "Toàn quyền quản trị: Thêm/Sửa/Xóa sản phẩm, duyệt kho hàng, quản lý và phân quyền nhân viên, cấu hình thông báo hệ thống."
                : "Quyền hạn tác nghiệp: Tra cứu kho hàng, xem giá niêm yết sản phẩm và cập nhật thông tin cá nhân.";

        return String.format("👤 **Thông tin tài khoản của bạn trên hệ thống ERP:**\n\n"
                + "• **Họ và tên:** **%s**\n"
                + "• **Tên đăng nhập:** `@%s`\n"
                + "• **Địa chỉ Email:** `%s`\n"
                + "• **Vai trò hệ thống:** **%s**\n"
                + "• **Trạng thái tài khoản:** 🟢 %s\n\n"
                + "🔑 **Phân quyền truy cập:**\n%s",
                displayName,
                user.getUsername(),
                user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "Chưa liên kết email",
                roleName,
                "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "Đang hoạt động (ACTIVE)" : user.getStatus(),
                permissions);
    }

    /**
     * Nhận diện câu hỏi về hệ thống, hạ tầng, server
     */
    private boolean isSystemInfoQuery(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented,
                "phien ban", "version", "he thong", "server", "may chu", "ha tang", 
                "sla", "cong nghe", "kien truc", "may chu o dau", "datacenter", "data center");
    }

    /**
     * Phản hồi thông số kỹ thuật và hạ tầng máy chủ
     */
    private String buildSystemInfoResponse() {
        return "🏢 **Thông số Kỹ thuật & Hạ tầng ERP Cloud Enterprise:**\n\n"
                + "• **Phiên bản hệ thống:** ERP Cloud Enterprise Suite v4.2.0\n"
                + "• **Backend Framework:** Java 17 LTS, Spring Boot 3.3.4, Spring Security 6 (Stateless JWT)\n"
                + "• **Giao thức Thời gian thực:** Spring WebSocket STOMP & SockJS (Full-Duplex Realtime)\n"
                + "• **Frontend Client:** Vue 3 Composition API, Vite, Pinia, Element Plus UI\n"
                + "• **Cơ sở dữ liệu:** MySQL 8.0 Community Edition & Redis In-Memory Cache\n"
                + "• **Cụm máy chủ (Server Cluster):** Data Center VN-Central (Hạ tầng Tier 4, Băng thông 10 Gbps)\n"
                + "• **Cam kết vận hành (SLA):** 99.98% High Availability, Tiêu chuẩn an ninh ISO 27001\n\n"
                + "💡 *Hệ thống đang vận hành ổn định và luôn sẵn sàng xử lý các giao dịch của doanh nghiệp 24/7!*";
    }

    /**
     * Tính toán số học cơ bản và phần trăm (Máy tính thông minh)
     */
    private String evaluateMathExpression(String text, String unaccented) {
        if (text == null) return null;

        // Tính phần trăm: ví dụ "20% của 500k", "15% cua 200000"
        Pattern percentPattern = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*%\\s*(?:của|cua|nhân|x|\\*)?\\s*(\\d+(?:\\.\\d+)?)\\s*([kK]?)");
        Matcher pMatcher = percentPattern.matcher(text);
        if (pMatcher.find()) {
            try {
                double rate = Double.parseDouble(pMatcher.group(1));
                double base = Double.parseDouble(pMatcher.group(2));
                if ("k".equalsIgnoreCase(pMatcher.group(3))) {
                    base *= 1000;
                }
                double result = (base * rate) / 100.0;
                return String.format("🧮 **Kết quả tính toán phần trăm:**\n\n"
                        + "• Biểu thức: **%.1f%% của %,.0f**\n"
                        + "• Đáp số: **%,.2f** ✨", rate, base, result);
            } catch (Exception ignored) {}
        }

        // Tính 4 phép tính cơ bản: +, -, *, /, x, :
        Pattern mathPattern = Pattern.compile("(?i)(?:tính\\s*|ket qua\\s*)?(\\d+(?:\\.\\d+)?)\\s*([kK]?)\\s*([+\\-*xX/÷:])\\s*(\\d+(?:\\.\\d+)?)\\s*([kK]?)");
        Matcher mMatcher = mathPattern.matcher(text);
        if (mMatcher.find()) {
            try {
                double num1 = Double.parseDouble(mMatcher.group(1));
                if ("k".equalsIgnoreCase(mMatcher.group(2))) num1 *= 1000;
                String op = mMatcher.group(3);
                double num2 = Double.parseDouble(mMatcher.group(4));
                if ("k".equalsIgnoreCase(mMatcher.group(5))) num2 *= 1000;

                double res;
                String opSymbol = op;
                switch (op.toLowerCase()) {
                    case "+": res = num1 + num2; opSymbol = "+"; break;
                    case "-": res = num1 - num2; opSymbol = "-"; break;
                    case "*": case "x": res = num1 * num2; opSymbol = "×"; break;
                    case "/": case "÷": case ":":
                        if (num2 == 0) return "⚠️ Không thể chia cho số 0 bạn nha!";
                        res = num1 / num2;
                        opSymbol = "÷";
                        break;
                    default: return null;
                }

                return String.format("🧮 **Kết quả tính toán:**\n\n"
                        + "• Biểu thức: **%,.2f %s %,.2f**\n"
                        + "• Kết quả: **%,.2f** ✨",
                        num1, opSymbol, num2, res);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Nhận diện yêu cầu kể chuyện cười, giải trí
     */
    private boolean isJokeQuery(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "ke chuyen cuoi", "truyen cuoi", "hai huoc", "do vui", "ke chuyen nghe", "chuyen vui");
    }

    private String buildJokeResponse(String seed) {
        String[] jokes = {
            "😄 **Chuyện cười lập trình:**\n\nSếp hỏi bạn IT: *'Em ơi, chức năng này test kỹ chưa?'*\nBạn IT vui vẻ đáp: *'Dạ em test trên máy em chạy mượt 100% rồi sếp ơi!'*\nSếp gật gù: *'Tốt lắm, vậy để anh ship nguyên cái máy của em cho khách hàng dùng luôn nhé!'* 😂",
            "☕ **Tâm sự Developer:**\n\nTại sao các lập trình viên Java luôn thích uống cà phê đen không đường?\nBởi vì họ sợ 'Java' bị bug đường huyết! ☕😆",
            "🤖 **Câu đố vui công nghệ:**\n\nTrên thế giới chỉ có 10 loại người:\n• Loại thứ nhất: Hiểu hệ nhị phân (Binary).\n• Loại thứ hai: Không hiểu hệ nhị phân!\n*(Hihi bạn có thuộc loại thứ nhất không nè? 😉)*",
            "🐞 **Triết lý kiểm thử:**\n\n'Không có phần mềm nào là hoàn toàn không có lỗi, chỉ có lỗi đã được khách hàng tìm thấy hay chưa mà thôi!' 🤣"
        };
        return jokes[Math.abs(seed.hashCode()) % jokes.length];
    }

    private boolean isWeatherQuery(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "thoi tiet", "troi mua", "troi nang", "nhiet do", "du bao thoi tiet");
    }

    /**
     * Câu trả lời thay thế thông minh (Fallback) có ngữ cảnh, không rập khuôn
     */
    private String buildSmartFallback(String trimmedPrompt) {
        String clean = trimmedPrompt.length() > 60 ? trimmedPrompt.substring(0, 60) + "..." : trimmedPrompt;
        return "Dạ mình lắng nghe câu hỏi của bạn rồi nè! 😊\n\n"
                + "Về câu hỏi *\"" + clean + "\"*, do hiện tại mình đang hoạt động ở chế độ Trợ lý Doanh nghiệp nội bộ nên chưa có đủ dữ liệu để giải đáp chi tiết cho câu hỏi này.\n\n"
                + "💡 **Bạn có thể thử hỏi mình những điều thú vị và hữu ích như:**\n"
                + "• ⏰ **Thời gian thực:** *\"Mấy giờ rồi?\"*, *\"Hôm nay thứ mấy?\"*\n"
                + "• 👤 **Tài khoản cá nhân:** *\"Tôi là ai?\"*, *\"Quyền hạn của tôi là gì?\"*\n"
                + "• 📦 **Kho & Sản phẩm:** *\"Doc còn bao nhiêu cái?\"*, *\"Kho còn hàng gì?\"*\n"
                + "• ➕ **Lệnh tự động:** *\"Tạo sản phẩm Bút bi giá 15k tồn 50\"*\n"
                + "• 👥 **Nhân sự & User:** *\"Hệ thống có bao nhiêu tài khoản?\"*\n"
                + "• 🏢 **Hạ tầng & Server:** *\"Server ở đâu?\"*, *\"Hệ thống phiên bản mấy?\"*\n"
                + "• 🧮 **Máy tính:** *\"Tính 25 * 40\"*, *\"15% của 500k\"*\n"
                + "• 😄 **Giải trí:** *\"Kể chuyện cười đi\"*\n\n"
                + "Bạn muốn mình kiểm tra hoặc giải đáp mục nào thì cứ nhắn mình nha! 👇";
    }

    private String unaccent(String text) {
        if (text == null) return "";
        String temp = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toLowerCase()
                .trim();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean isGreeting(String text, String unaccented) {
        if (text == null || unaccented == null) return false;
        String u = unaccented.trim();
        if (u.equals("hi") || u.equals("hello") || u.equals("alo") || u.equals("chao") 
                || u.equals("hey") || u.equals("e") || u.equals("ban oi") || u.equals("bot oi") || u.equals("ai oi")) {
            return true;
        }
        return u.startsWith("chao ") || u.startsWith("xin chao") || u.startsWith("hello ") 
                || u.startsWith("hi ") || u.startsWith("alo ") || u.startsWith("hey ");
    }

    private boolean isThanks(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "cam on", "thanks", "thank you", "tks", "cmon", "thank");
    }

    private boolean isCompliment(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "gioi", "thong minh", "tot lam", "hay qua", "duoc day", "xin", "dinh", "pro", "tuyet voi", "chat luong", "good job");
    }

    private boolean isAgreement(String unaccented) {
        if (unaccented == null) return false;
        String u = unaccented.trim();
        return u.equals("ok") || u.equals("okee") || u.equals("oke") || u.equals("okie") || u.equals("oki")
                || u.equals("ok nha") || u.equals("okie nha") || u.equals("duoc roi") || u.equals("hieu roi") 
                || u.equals("ro roi") || u.equals("chuan") || u.equals("dung roi") || u.equals("da vang")
                || u.equals("vang") || u.equals("u") || u.equals("uh") || u.equals("uhm")
                || u.startsWith("ok ") || u.startsWith("okee ") || u.startsWith("oke ") || u.startsWith("okie ");
    }

    private boolean isSmallTalk(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "khoe khong", "hom nay the nao", "dang lam gi", "tam su", "met khong");
    }

    private boolean isCapabilityQuery(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "lam duoc gi", "giup duoc gi", "biet lam gi", "chuc nang", "tinh nang", "co tac dung gi");
    }

    private boolean isFarewell(String unaccented) {
        if (unaccented == null) return false;
        return containsAny(unaccented, "tam biet", "bye", "goodbye", "hen gap lai", "ngu ngon", "off day");
    }

    private boolean containsWord(String fullText, String word) {
        if (fullText == null || word == null) return false;
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS)
                .matcher(fullText).find();
    }

    private String handleDirectCreateProduct(String prompt) {
        try {
            String clean = prompt.replaceAll("^(?i)(tạo sản phẩm|thêm sản phẩm|tạo sp)\\s*", "").trim();
            String name = clean;
            BigDecimal price = BigDecimal.valueOf(100000);
            int stock = 10;

            Pattern pricePattern = Pattern.compile("(?i)(giá\\s*[:=]?\\s*)(\\d+[kK]?)");
            Matcher priceMatcher = pricePattern.matcher(clean);
            if (priceMatcher.find()) {
                String priceStr = priceMatcher.group(2).toLowerCase();
                double pVal = priceStr.endsWith("k") 
                        ? Double.parseDouble(priceStr.replace("k", "")) * 1000 
                        : Double.parseDouble(priceStr);
                price = BigDecimal.valueOf(pVal);
                name = name.replace(priceMatcher.group(0), "");
            }

            Pattern stockPattern = Pattern.compile("(?i)(tồn\\s*(kho)?\\s*[:=]?\\s*)(\\d+)");
            Matcher stockMatcher = stockPattern.matcher(clean);
            if (stockMatcher.find()) {
                stock = Integer.parseInt(stockMatcher.group(3));
                name = name.replace(stockMatcher.group(0), "");
            }

            name = name.replaceAll("[,;\\s]+", " ").trim();
            if (name.isEmpty()) {
                name = "Sản phẩm mới " + (100 + new Random().nextInt(900));
            }

            String sku = "SKU-" + (1000 + new Random().nextInt(9000));

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
