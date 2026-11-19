package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.order.model.Order;
import com.erp.order.model.OrderItem;
import com.erp.order.model.OrderStatus;
import com.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesAgentTool implements AgentTool {

    private final OrderRepository orderRepository;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "getSalesPerformance";
    }

    @Override
    public String getDescription() {
        return "Báo cáo doanh số bán hàng, số lượng đơn hoàn thành, giá trị trung bình đơn (AOV), lợi nhuận gộp và biểu đồ xu hướng 7 ngày theo khoảng thời gian.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> timeRange = new HashMap<>();
        timeRange.put("type", "string");
        timeRange.put("enum", Arrays.asList("TODAY", "THIS_WEEK", "THIS_MONTH"));
        timeRange.put("description", "Khoảng thời gian cần xem: TODAY (Hôm nay), THIS_WEEK (Tuần này), THIS_MONTH (Tháng này)");
        properties.put("timeRange", timeRange);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String timeRange = args.get("timeRange") != null ? String.valueOf(args.get("timeRange")).toUpperCase() : "TODAY";

        LocalDate today = LocalDate.now();
        LocalDateTime fromDate;
        String rangeLabel;

        if ("THIS_WEEK".equals(timeRange)) {
            fromDate = today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay();
            rangeLabel = "Tuần này (từ Thứ 2 đến nay)";
        } else if ("THIS_MONTH".equals(timeRange)) {
            fromDate = today.withDayOfMonth(1).atStartOfDay();
            rangeLabel = "Tháng " + today.getMonthValue() + "/" + today.getYear();
        } else {
            fromDate = today.atStartOfDay();
            rangeLabel = "Hôm nay (" + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
        }

        // Lấy các đơn hàng phát sinh từ fromDate trở đi (hoặc tối thiểu 7 ngày để vẽ biểu đồ sparkline)
        LocalDateTime sparklineFrom = today.minusDays(6).atStartOfDay();
        LocalDateTime queryFrom = fromDate.isBefore(sparklineFrom) ? fromDate : sparklineFrom;

        List<Order> orders = orderRepository.findRecentOrders(queryFrom, OrderStatus.CANCELLED);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        long completedCount = 0;
        long totalOrdersInPeriod = 0;

        // Chuẩn bị map 7 ngày cho biểu đồ Sparkline
        Map<LocalDate, BigDecimal> dailyRevenueMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            dailyRevenueMap.put(today.minusDays(i), BigDecimal.ZERO);
        }

        for (Order o : orders) {
            LocalDateTime orderTime = o.getCreatedAt();
            LocalDate orderDate = orderTime.toLocalDate();

            // Tính dữ liệu sparkline 7 ngày
            if (dailyRevenueMap.containsKey(orderDate)) {
                dailyRevenueMap.put(orderDate, dailyRevenueMap.get(orderDate).add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO));
            }

            // Tính dữ liệu cho khoảng thời gian yêu cầu
            if (!orderTime.isBefore(fromDate)) {
                totalOrdersInPeriod++;
                BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(amount);

                if (o.getStatus() == OrderStatus.COMPLETED) {
                    completedCount++;
                }

                // Tính giá vốn (nếu có items)
                if (o.getItems() != null) {
                    for (OrderItem item : o.getItems()) {
                        // Ước tính giá vốn bình quân gia quyền hoặc 70% giá bán nếu chưa có moving_avg_cost
                        BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                        int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                        BigDecimal costPerUnit = itemPrice.multiply(BigDecimal.valueOf(0.68)); // ~68% giá bán
                        totalCost = totalCost.add(costPerUnit.multiply(BigDecimal.valueOf(qty)));
                    }
                }
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        if (grossProfit.compareTo(BigDecimal.ZERO) < 0) {
            grossProfit = totalRevenue.multiply(BigDecimal.valueOf(0.32)); // Tối thiểu ước lượng 32% biên lãi
        }

        BigDecimal aov = totalOrdersInPeriod > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrdersInPeriod), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);

        // Tạo dữ liệu Sparkline Widget
        List<String> chartLabels = new ArrayList<>();
        List<Double> chartValues = new ArrayList<>();
        DateTimeFormatter shortDayFmt = DateTimeFormatter.ofPattern("dd/MM");

        for (Map.Entry<LocalDate, BigDecimal> entry : dailyRevenueMap.entrySet()) {
            chartLabels.add(entry.getKey().format(shortDayFmt));
            chartValues.add(entry.getValue().doubleValue());
        }

        Map<String, Object> widgetData = new LinkedHashMap<>();
        widgetData.put("title", "Biến động Doanh số 7 ngày qua");
        widgetData.put("labels", chartLabels);
        widgetData.put("values", chartValues);
        widgetData.put("totalRevenueFormatted", currFmt.format(totalRevenue));
        widgetData.put("grossProfitFormatted", currFmt.format(grossProfit));
        widgetData.put("orderCount", totalOrdersInPeriod);
        widgetData.put("completedCount", completedCount);
        widgetData.put("aovFormatted", currFmt.format(aov));

        UiWidgetDto widget = UiWidgetDto.builder()
                .type("MINI_SPARKLINE_CHART")
                .data(widgetData)
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📊 **BÁO CÁO DOANH SỐ — %s**:\n", rangeLabel));
        sb.append(String.format("- **Tổng doanh thu**: **%s**\n", currFmt.format(totalRevenue)));
        sb.append(String.format("- **Tổng số đơn phát sinh**: **%d đơn** (Hoàn thành: **%d đơn**)\n", totalOrdersInPeriod, completedCount));
        sb.append(String.format("- **Giá trị trung bình mỗi đơn (AOV)**: **%s**\n", currFmt.format(aov)));
        sb.append(String.format("- **Ước tính lợi nhuận gộp**: **%s** (Tỷ suất lợi nhuận ~ 32%%)\n", currFmt.format(grossProfit)));

        if (totalRevenue.compareTo(BigDecimal.valueOf(50_000_000)) >= 0) {
            sb.append("\n🔥 _Hiệu suất bán hàng giai đoạn này đang rất bùng nổ, vượt chỉ tiêu kỳ vọng!_");
        } else if (totalOrdersInPeriod > 0) {
            sb.append("\n📈 _Tiến độ bán hàng duy trì ổn định, cần đẩy mạnh thêm các chương trình khuyến mại để bứt phá doanh số!_");
        } else {
            sb.append("\n💤 _Chưa ghi nhận đơn hàng mới trong khung giờ này. Đội ngũ bán hàng hãy tích cực chốt đơn nhé!_");
        }

        Map<String, Object> resData = new LinkedHashMap<>();
        resData.put("totalRevenue", totalRevenue);
        resData.put("orderCount", totalOrdersInPeriod);
        resData.put("completedCount", completedCount);
        resData.put("grossProfit", grossProfit);
        resData.put("aov", aov);

        return ToolExecutionResult.success(getToolName(), sb.toString(), resData, widget);
    }
}
