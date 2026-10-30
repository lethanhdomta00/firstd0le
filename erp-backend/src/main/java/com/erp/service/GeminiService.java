package com.erp.service;

import com.erp.model.Announcement;
import com.erp.model.ChatMessage;
import com.erp.model.Product;
import com.erp.model.User;
import com.erp.repository.AnnouncementRepository;
import com.erp.repository.ProductRepository;
import com.erp.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Service tích hợp Google Gemini AI kết hợp kiến trúc RAG, Function Calling và Multi-Key Failover.
 * 
 * Đối chiếu tư duy lập trình PHP/Laravel:
 * - Multi-Key Failover: Giống như cấu hình danh sách backup connections trong database.php hoặc Guzzle retry middleware.
 * - Function Calling: AI nhận diện ý định và tự động gọi các hàm Model/Service bên dưới để INSERT/UPDATE dữ liệu thực tế.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final ObjectMapper objectMapper;
    private final GeminiKeyManager geminiKeyManager;

    @Value("${gemini.model:gemini-2.5-flash-lite}")
    private String model;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    /**
     * Kiểm tra xem có ít nhất 1 Key sẵn sàng trong Pool hay không
     */
    public boolean isConfigured() {
        return geminiKeyManager.hasKeys();
    }

    /**
     * Gửi câu hỏi và ngữ cảnh hội thoại lên Google Gemini AI với cơ chế tự động luân phiên và chuyển đổi Key (Failover)
     */
    public String generateReply(User user, String currentPrompt, List<ChatMessage> history) {
        if (!isConfigured()) {
            log.info("Không có Gemini API Key nào trong Key Pool. Hệ thống sẽ sử dụng bộ xử lý thông minh nội bộ.");
            return null;
        }

        List<String> candidateKeys = geminiKeyManager.getCandidateKeys();
        if (candidateKeys.isEmpty()) {
            log.warn("Toàn bộ Gemini API Key hiện tại đang trong thời gian chờ cooldown.");
            return null;
        }

        // 1. Tạo System Prompt chứa dữ liệu thời gian thực của ERP (RAG Context)
        String systemInstructionText = buildSystemInstructionWithERPContext(user);

        // 2. Tạo Request Body JSON theo chuẩn Google Gemini API
        ObjectNode root = objectMapper.createObjectNode();

        // System Instruction theo chuẩn Google Gemini API v1beta
        ObjectNode sysInstruction = root.putObject("system_instruction");
        sysInstruction.putArray("parts").addObject().put("text", systemInstructionText);

        // Khai báo công cụ Function Calling cho Gemini (AI tự động tạo sản phẩm khi được yêu cầu)
        ArrayNode toolsArray = root.putArray("tools");
        ObjectNode toolObj = toolsArray.addObject();
        ArrayNode funcDecls = toolObj.putArray("function_declarations");

        ObjectNode createProdFunc = funcDecls.addObject();
        createProdFunc.put("name", "create_product");
        createProdFunc.put("description", "Tạo một sản phẩm mới vào cơ sở dữ liệu kho hàng ERP khi người dùng yêu cầu tạo hoặc thêm sản phẩm.");
        ObjectNode params = createProdFunc.putObject("parameters");
        params.put("type", "OBJECT");
        ObjectNode props = params.putObject("properties");
        props.putObject("name").put("type", "STRING").put("description", "Tên sản phẩm");
        props.putObject("sku").put("type", "STRING").put("description", "Mã SKU của sản phẩm (ví dụ: SKU-1234)");
        props.putObject("price").put("type", "NUMBER").put("description", "Giá niêm yết (VNĐ)");
        props.putObject("stock").put("type", "INTEGER").put("description", "Số lượng tồn kho ban đầu");
        ArrayNode required = params.putArray("required");
        required.add("name");
        required.add("price");

        // Contents (Bao gồm lịch sử hội thoại hợp lệ + câu hỏi hiện tại)
        ArrayNode contentsArray = root.putArray("contents");

        java.util.List<ObjectNode> validTurns = new java.util.ArrayList<>();
        String expectedRole = "user";

        int startIdx = Math.max(0, history.size() - 8);
        for (int i = startIdx; i < history.size(); i++) {
            ChatMessage pastMsg = history.get(i);
            if (pastMsg.getContent() == null || pastMsg.getContent().trim().isEmpty()) continue;
            String geminiRole = "USER".equalsIgnoreCase(pastMsg.getRole()) ? "user" : "model";

            if (geminiRole.equals(expectedRole)) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("role", geminiRole);
                node.putArray("parts").addObject().put("text", pastMsg.getContent());
                validTurns.add(node);
                expectedRole = "user".equals(geminiRole) ? "model" : "user";
            }
        }

        if (!validTurns.isEmpty() && "user".equals(validTurns.get(validTurns.size() - 1).get("role").asText())) {
            validTurns.remove(validTurns.size() - 1);
        }

        for (ObjectNode turn : validTurns) {
            contentsArray.add(turn);
        }

        // Thêm câu hỏi mới nhất của user (role: 'user')
        ObjectNode currentTurnNode = contentsArray.addObject();
        currentTurnNode.put("role", "user");
        ArrayNode currentParts = currentTurnNode.putArray("parts");
        currentParts.addObject().put("text", currentPrompt);

        // Cấu hình sinh văn bản (Generation Config)
        ObjectNode genConfig = root.putObject("generationConfig");
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 1200);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(root.toString(), headers);

        // 3. VÒNG LẶP FAILOVER: Thử lần lượt các key trong Key Pool
        for (String candidateKey : candidateKeys) {
            try {
                String endpoint = String.format("%s/%s:generateContent?key=%s", apiUrl, model, candidateKey.trim());
                ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    JsonNode candidates = responseJson.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode firstCandidate = candidates.get(0);
                        JsonNode partsNode = firstCandidate.path("content").path("parts");
                        if (partsNode.isArray() && !partsNode.isEmpty()) {
                            JsonNode firstPart = partsNode.get(0);

                            // Trường hợp 1: Gemini gọi hàm Function Calling
                            if (firstPart.has("functionCall")) {
                                JsonNode funcCall = firstPart.path("functionCall");
                                String funcName = funcCall.path("name").asText();
                                if ("create_product".equalsIgnoreCase(funcName)) {
                                    return executeCreateProduct(funcCall.path("args"));
                                }
                            }

                            // Trường hợp 2: Gemini trả lời văn bản thông thường
                            if (firstPart.has("text")) {
                                return firstPart.path("text").asText();
                            }
                        }
                    }
                }
            } catch (HttpStatusCodeException e) {
                log.warn("Gemini API HTTP Error {}: {}. Đang tự động chuyển sang Key tiếp theo...", 
                        e.getStatusCode(), e.getResponseBodyAsString());
                geminiKeyManager.markKeyAsFailed(candidateKey, e.getStatusCode().value(), e.getMessage());
                // Tiếp tục thử key dự phòng tiếp theo
                continue;
            } catch (Exception e) {
                log.warn("Lỗi kết nối Gemini với key [{}]: {}. Thử key tiếp theo...", candidateKey, e.getMessage());
                geminiKeyManager.markKeyAsFailed(candidateKey, 500, e.getMessage());
                continue;
            }
        }

        log.warn("Tất cả các Gemini API Key trong Pool đều gặp lỗi. Tự động chuyển về bộ xử lý nội bộ.");
        return null;
    }

    /**
     * Thực thi hàm Function Calling: Tự động lưu sản phẩm vào MySQL
     */
    private String executeCreateProduct(JsonNode args) {
        try {
            String name = args.has("name") ? args.path("name").asText() : "Sản phẩm mới";
            BigDecimal price = args.has("price") ? BigDecimal.valueOf(args.path("price").asDouble()) : BigDecimal.valueOf(100000);
            int stock = args.has("stock") ? args.path("stock").asInt() : 10;
            String sku = args.has("sku") && !args.path("sku").asText().isEmpty() 
                    ? args.path("sku").asText() 
                    : "SKU-" + (1000 + new Random().nextInt(9000));

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
                    + "• **Giá niêm yết:** **%,.0f đ**\n"
                    + "• **Tồn kho ban đầu:** **%d cái**\n\n"
                    + "✅ Dữ liệu đã được lưu trữ vĩnh viễn và hiển thị ngay tại trang **Quản lý Sản phẩm**!",
                    saved.getId(), saved.getName(), saved.getSku(), saved.getPrice(), saved.getStock());
        } catch (Exception e) {
            log.error("Lỗi khi Function Calling tạo sản phẩm: {}", e.getMessage(), e);
            return "⚠️ Đã xảy ra lỗi khi tạo sản phẩm vào Database: " + e.getMessage();
        }
    }

    /**
     * Thu thập dữ liệu thực tế từ cơ sở dữ liệu nội bộ để nạp vào System Instruction (RAG).
     */
    private String buildSystemInstructionWithERPContext(User user) {
        StringBuilder sb = new StringBuilder();

        // 0. Thời gian thực tế theo múi giờ Việt Nam (GMT+7)
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
        String periodVn = (hour >= 5 && hour < 12) ? "Buổi sáng" :
                          (hour >= 12 && hour < 14) ? "Buổi trưa" :
                          (hour >= 14 && hour < 18) ? "Buổi chiều" :
                          (hour >= 18 && hour < 23) ? "Buổi tối" : "Đêm khuya";

        String displayName = user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName()
                : "@" + user.getUsername();

        sb.append("Bạn là Trợ lý ảo AI thông minh của hệ thống quản lý doanh nghiệp ERP Cloud System.\n");
        sb.append("--- MÔI TRƯỜNG THỜI GIAN THỰC TẾ & HỆ THỐNG ---\n");
        sb.append("• Thời gian thực tế hiện tại: ").append(nowVn.format(timeFmt)).append(" (GMT+7, Giờ Việt Nam)\n");
        sb.append("• Ngày hiện tại: ").append(dayOfWeekVn).append(", ngày ").append(nowVn.format(dateFmt)).append("\n");
        sb.append("• Buổi trong ngày: ").append(periodVn).append("\n");
        sb.append("• Hệ thống: ERP Cloud Enterprise Suite v4.2.0\n");
        sb.append("• Hạ tầng kỹ thuật: Java 17 LTS, Spring Boot 3.3.4, Spring Security 6, Spring WebSocket STOMP, Vue 3, MySQL 8, Redis\n");
        sb.append("• Cụm máy chủ: Data Center VN-Central, Cam kết SLA 99.98%, Tiêu chuẩn ISO 27001\n\n");

        sb.append("--- THÔNG TIN TÀI KHOẢN ĐANG GIAO TIẾP VỚI BẠN ---\n");
        sb.append("• Họ và tên: ").append(displayName).append("\n");
        sb.append("• Tên đăng nhập (Username): @").append(user.getUsername()).append("\n");
        sb.append("• Email: ").append(user.getEmail() != null ? user.getEmail() : "Chưa cập nhật").append("\n");
        sb.append("• Vai trò phân quyền: ").append(user.getRole()).append(" (").append(user.getRole() == com.erp.model.Role.ADMIN ? "Quản trị viên toàn quyền" : "Nhân viên tác nghiệp").append(")\n");
        sb.append("• Trạng thái tài khoản: ").append(user.getStatus()).append("\n\n");

        sb.append("--- DỮ LIỆU THỜI GIAN THỰC TRONG DATABASE DOANH NGHIỆP (RAG CONTEXT) ---\n\n");

        // 1. Kho hàng & Sản phẩm
        List<Product> products = productRepository.findAll();
        long totalProducts = products.size();
        long outOfStock = products.stream().filter(p -> p.getStock() == null || p.getStock() == 0).count();
        long lowStock = products.stream().filter(p -> p.getStock() != null && p.getStock() > 0 && p.getStock() <= 10).count();

        sb.append("1. THÔNG TIN KHO HÀNG & SẢN PHẨM:\n");
        sb.append("- Tổng số mặt hàng: ").append(totalProducts).append(" sản phẩm.\n");
        sb.append("- Hết hàng (tồn 0): ").append(outOfStock).append(" mặt hàng.\n");
        sb.append("- Sắp hết hàng (tồn 1-10): ").append(lowStock).append(" mặt hàng.\n");
        sb.append("- Danh sách toàn bộ sản phẩm thực tế trong cơ sở dữ liệu kho hàng:\n");
        for (Product p : products) {
            String stockStatus = (p.getStock() != null && p.getStock() > 0) ? "Còn hàng" : "Hết hàng";
            sb.append(String.format("  * [ID: %d] %s | Mã SKU: %s | Giá: %,.0f đ | Tồn kho: %d cái (%s)\n",
                    p.getId(),
                    p.getName(),
                    p.getSku() != null ? p.getSku() : "N/A",
                    p.getPrice() != null ? p.getPrice() : 0.0,
                    p.getStock() != null ? p.getStock() : 0,
                    stockStatus));
        }
        sb.append("\n");

        // 2. Tài khoản người dùng
        List<User> users = userRepository.findAll();
        long totalUsers = users.size();
        long activeUsers = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long lockedUsers = users.stream().filter(u -> "LOCKED".equalsIgnoreCase(u.getStatus())).count();

        sb.append("2. THÔNG TIN TÀI KHOẢN HỆ THỐNG:\n");
        sb.append("- Tổng số tài khoản: ").append(totalUsers).append(" người dùng.\n");
        sb.append("- Đang hoạt động: ").append(activeUsers).append(" tài khoản.\n");
        sb.append("- Đang bị khóa: ").append(lockedUsers).append(" tài khoản.\n\n");

        // 3. Thông báo hệ thống trên Header
        Announcement activeAnnouncement = announcementRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElse(null);
        sb.append("3. THÔNG BÁO HỆ THỐNG ĐANG PHÁT HÀNH:\n");
        if (activeAnnouncement != null) {
            sb.append("- Tiêu đề: ").append(activeAnnouncement.getTitle()).append("\n");
            sb.append("- Nội dung: ").append(activeAnnouncement.getContent()).append("\n");
        } else {
            sb.append("- Hiện không có thông báo nào đang kích hoạt trên thanh Header.\n");
        }
        sb.append("\n");

        sb.append("--- NGUYÊN TẮC VÀ QUY TẮC PHẢN HỒI CỦA TRỢ LÝ AI ---\n");
        sb.append("1. Giao tiếp bằng Tiếng Việt tự nhiên, linh hoạt, thân thiện và gần gũi như một người đồng nghiệp thông minh, nhiệt tình (xưng hô 'mình' - 'bạn', dùng emoji vui vẻ 😊, 📦, ✨, 👍, ⏰).\n");
        sb.append("2. Trả lời THỜI GIAN & NGÀY THÁNG: Khi người dùng hỏi 'Mấy giờ rồi', 'Bây giờ là mấy giờ', 'Hôm nay thứ mấy', 'Hôm nay ngày mấy', hãy trả lời chính xác giờ:phút:giây, thứ ngày tháng năm dựa theo dữ liệu thời gian thực ở mục trên, kèm lời chào/lời chúc phù hợp buổi sáng/trưa/chiều/tối/đêm.\n");
        sb.append("3. Trả lời DANH TÍNH NGƯỜI DÙNG: Khi người dùng hỏi 'Tôi là ai', 'Tài khoản của tôi', 'Thông tin của tôi', 'Quyền hạn của tôi', hãy trình bày rõ ràng tên, username, email, vai trò và quyền hạn của họ trong hệ thống ERP.\n");
        sb.append("4. Trả lời HỆ THỐNG & SERVER: Khi hỏi 'Hệ thống phiên bản gì', 'Server ở đâu', 'Công nghệ gì', hãy trình bày thông tin kỹ thuật chính xác từ mục môi trường ở trên.\n");
        sb.append("5. Hỗ trợ TÍNH TOÁN: Khi người dùng nhờ tính toán (cộng trừ nhân chia, tính tỷ lệ % tồn kho, doanh số), hãy tính toán chuẩn xác và giải thích ngắn gọn.\n");
        sb.append("6. Khi người dùng chào hỏi, cảm ơn, khen ngợi, đồng ý hoặc nói chuyện phiếm (chitchat, hỏi thăm sức khỏe, đùa vui), hãy đối đáp duyên dáng, chân thành, hóm hỉnh và uyển chuyển như con người thực thụ. TUYỆT ĐỐI KHÔNG trả lời máy móc hay lặp lại các câu dập khuôn.\n");
        sb.append("7. Nếu người dùng hỏi hướng dẫn (ví dụ: 'cách tạo sản phẩm', 'làm sao thêm tài khoản'), hãy giải thích từng bước rõ ràng, sinh động.\n");
        sb.append("8. Nếu người dùng ra lệnh tạo sản phẩm (ví dụ: 'Tạo sản phẩm Áo thun giá 100k tồn 20'), hãy kích hoạt công cụ 'create_product' (Function Calling) để tự động lưu vào Database!\n");
        sb.append("9. Sử dụng chính xác dữ liệu thực tế ở trên để giải đáp mọi câu hỏi về số liệu kho hàng, giá bán, nhân sự.\n");
        sb.append("10. Tuyệt đối không bịa đặt số liệu sai lệch về doanh nghiệp.");

        return sb.toString();
    }
}
