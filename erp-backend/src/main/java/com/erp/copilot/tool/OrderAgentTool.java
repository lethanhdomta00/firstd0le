package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAgentTool implements AgentTool {

    private final OrderRepository orderRepository;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "findRecentOrders";
    }

    @Override
    public String getDescription() {
        return "Truy vấn danh sách đơn hàng mới nhất theo trạng thái, từ khóa khách hàng hoặc mã đơn.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> keyword = new HashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "Từ khóa tìm kiếm (mã đơn, tên khách, số điện thoại)");
        properties.put("keyword", keyword);

        Map<String, Object> limit = new HashMap<>();
        limit.put("type", "integer");
        limit.put("description", "Số lượng đơn cần lấy (mặc định 5 đơn)");
        properties.put("limit", limit);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String keyword = args.get("keyword") != null ? String.valueOf(args.get("keyword")).trim() : "";
        int limit = 5;
        if (args.get("limit") != null) {
            try {
                limit = Integer.parseInt(String.valueOf(args.get("limit")));
            } catch (Exception ignored) {
            }
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Order> orders = orderRepository.findRecentOrders(thirtyDaysAgo, OrderStatus.CANCELLED);

        if (!keyword.isEmpty()) {
            String kwLower = keyword.toLowerCase();
            orders = orders.stream()
                    .filter(o -> (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(kwLower))
                            || (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(kwLower))
                            || (o.getCustomerPhone() != null && o.getCustomerPhone().contains(kwLower)))
                    .toList();
        }

        if (orders.isEmpty()) {
            return ToolExecutionResult.success(getToolName(), "Không tìm thấy đơn hàng nào khớp với yêu cầu.", Map.of("count", 0), null);
        }

        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

        int take = Math.min(orders.size(), limit);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📦 **DANH SÁCH %d ĐƠN HÀNG GẦN NHẤT**:\n\n", take));

        for (int i = 0; i < take; i++) {
            Order o = orders.get(i);
            String statusBadge = switch (o.getStatus()) {
                case PENDING -> "⏳ Chờ xác nhận";
                case CONFIRMED -> "📋 Đã xác nhận";
                case SHIPPING -> "🚚 Đang giao";
                case COMPLETED -> "✅ Hoàn thành";
                case CANCELLED -> "❌ Đã hủy";
            };

            sb.append(String.format("%d. **%s** — Khách: **%s** (`%s`)\n",
                    i + 1, o.getOrderCode(), o.getCustomerName(), o.getCustomerPhone() != null ? o.getCustomerPhone() : "---"));
            sb.append(String.format("   - Tổng tiền: **%s** (%s) | Trạng thái: %s\n",
                    currFmt.format(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO),
                    o.getPaymentStatus() == com.erp.order.model.PaymentStatus.PAID ? "Đã trả đủ" : "Chưa thanh toán",
                    statusBadge));
            sb.append(String.format("   - Ngày tạo: %s\n", o.getCreatedAt() != null ? o.getCreatedAt().format(timeFmt) : ""));
        }

        return ToolExecutionResult.success(getToolName(), sb.toString(), Map.of("count", take), null);
    }
}
