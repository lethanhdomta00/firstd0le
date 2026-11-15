package com.erp.scheduler.task;

import com.erp.dto.RealtimeMessageDto;
import com.erp.model.MessageStatus;
import com.erp.model.MessageType;
import com.erp.model.Product;
import com.erp.model.RealtimeMessage;
import com.erp.order.dto.OrderStatsAggregate;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.ProductRepository;
import com.erp.repository.RealtimeMessageRepository;
import com.erp.service.TelegramService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailySalesSummaryTaskHandler implements ScheduledTaskHandler {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TelegramService telegramService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeMessageRepository realtimeMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getJobType() {
        return "DAILY_SALES_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "Tổng hợp Báo cáo Kinh doanh, Đơn hàng & Kho vận";
    }

    @Override
    public String execute(String paramsJson, String triggeredBy) throws Exception {
        boolean notifyTelegram = true;
        boolean broadcastChat = true;

        if (paramsJson != null && !paramsJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(paramsJson);
                if (root.has("notifyTelegram")) {
                    notifyTelegram = root.get("notifyTelegram").asBoolean(true);
                }
                if (root.has("broadcastChat")) {
                    broadcastChat = root.get("broadcastChat").asBoolean(true);
                }
            } catch (Exception e) {
                log.warn("Lỗi parse paramsJson cho DAILY_SALES_REPORT: {}", e.getMessage());
            }
        }

        // 1. Thống kê Kinh doanh & Doanh thu thực tế (OrderStatsAggregate)
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        OrderStatsAggregate stats = orderRepository.getOrderStatsAggregate(startOfToday);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        long totalOrders = stats != null && stats.totalOrders() != null ? stats.totalOrders() : 0L;
        long todayOrders = stats != null && stats.todayOrders() != null ? stats.todayOrders() : 0L;
        long completedOrders = stats != null && stats.completedOrders() != null ? stats.completedOrders() : 0L;
        BigDecimal activeRevenue = stats != null && stats.activeRevenue() != null ? stats.activeRevenue() : BigDecimal.ZERO;
        BigDecimal paidRevenue = stats != null && stats.paidRevenue() != null ? stats.paidRevenue() : BigDecimal.ZERO;

        // 2. Thống kê Kho vận & Tài sản hàng hóa
        List<Product> products = productRepository.findAll();
        int totalSkus = products.size();
        long totalStockQty = 0;
        int outOfStockCount = 0;
        BigDecimal totalInventoryValue = BigDecimal.ZERO;

        for (Product p : products) {
            int qty = p.getStock() != null ? p.getStock() : 0;
            totalStockQty += qty;
            if (qty == 0) {
                outOfStockCount++;
            }
            if (p.getPrice() != null && qty > 0) {
                totalInventoryValue = totalInventoryValue.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
            }
        }

        String formattedRevenue = currencyFormat.format(activeRevenue);
        String formattedPaid = currencyFormat.format(paidRevenue);
        String formattedInventory = currencyFormat.format(totalInventoryValue);

        String summary = String.format("Doanh thu: %s (Đã thu: %s) | Đơn hàng: %d (Hôm nay: %d, Hoàn tất: %d) | SKUs: %d (Hết hàng: %d) | Định giá kho: %s",
                formattedRevenue, formattedPaid, totalOrders, todayOrders, completedOrders, totalSkus, outOfStockCount, formattedInventory);

        // 3. Gửi thông báo Telegram (nếu bật)
        if (notifyTelegram) {
            String telegramText = "📊 <b>[ERP BÁO CÁO KINH DOANH & KHO VẬN TỔNG HỢP]</b>\n\n"
                    + "💰 <b>Tổng doanh thu:</b> <b>" + formattedRevenue + "</b> (Đã thu: " + formattedPaid + ")\n"
                    + "📦 <b>Lượng đơn hàng:</b> " + totalOrders + " đơn (Hôm nay: " + todayOrders + " | Hoàn tất: " + completedOrders + ")\n"
                    + "🏷️ <b>Danh mục hàng hóa (SKU):</b> " + totalSkus + " mã (Hết hàng: " + outOfStockCount + ")\n"
                    + "🏭 <b>Định giá tồn kho:</b> <b>" + formattedInventory + "</b> (Tổng: " + totalStockQty + " chiếc)\n\n"
                    + "<i>Tác vụ tự động định kỳ • Người kích hoạt: " + triggeredBy + "</i>";
            telegramService.sendMessage(telegramText);
        }

        // 4. Broadcast thông báo vào Kênh Chat Chung (WebSocket STOMP) từ Bot Copilot
        if (broadcastChat) {
            try {
                String botChatContent = String.format("📢 **[BÁO CÁO TỔNG HỢP TỰ ĐỘNG CUỐI NGÀY]**\n"
                        + "• **Doanh thu hợp lệ:** %s (Đã thu: %s)\n"
                        + "• **Tổng số đơn hàng:** %d đơn (Hôm nay: %d đơn | Hoàn tất: %d đơn)\n"
                        + "• **Tồn kho vật lý:** %d chiếc trên %d mã SKU (Hết hàng: %d mã)\n"
                        + "• **Tổng định giá kho:** %s\n"
                        + "*(Tiến trình nền thực thi tự động bởi %s)*",
                        formattedRevenue, formattedPaid, totalOrders, todayOrders, completedOrders,
                        totalStockQty, totalSkus, outOfStockCount, formattedInventory, triggeredBy);

                RealtimeMessage botMsg = RealtimeMessage.builder()
                        .roomId("GLOBAL")
                        .senderUsername("copilot")
                        .senderFullName("🤖 ERP AI Copilot")
                        .senderAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=copilot")
                        .content(botChatContent)
                        .type(MessageType.CHAT)
                        .status(MessageStatus.SENT)
                        .createdAt(LocalDateTime.now())
                        .build();

                RealtimeMessage saved = realtimeMessageRepository.save(botMsg);
                messagingTemplate.convertAndSend("/topic/room.GLOBAL", RealtimeMessageDto.fromEntity(saved));
                log.info("Broadcasted Daily Summary message to /topic/room.GLOBAL");
            } catch (Exception ex) {
                log.warn("Không thể broadcast báo cáo qua WebSocket chat: {}", ex.getMessage());
            }
        }

        log.info("DAILY_SALES_REPORT hoàn thành: {}", summary);
        return summary;
    }
}
