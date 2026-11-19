package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.order.dto.InventoryForecastDto;
import com.erp.order.service.InventoryForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryForecastAgentTool implements AgentTool {

    private final InventoryForecastService inventoryForecastService;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "forecastDepletedStock";
    }

    @Override
    public String getDescription() {
        return "Quét toàn bộ danh mục sản phẩm, phát hiện các mặt hàng có nguy cơ cạn kho AI trong 3-7 ngày tới và tính toán ngân sách nhập hàng đề xuất.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> riskLevel = new HashMap<>();
        riskLevel.put("type", "string");
        riskLevel.put("enum", Arrays.asList("CRITICAL", "WARNING", "ALL"));
        riskLevel.put("description", "Mức độ rủi ro: CRITICAL (cực gấp <= 3 ngày), WARNING (cần lưu ý 4-7 ngày), ALL (toàn bộ)");
        properties.put("riskLevel", riskLevel);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String riskLevel = args.get("riskLevel") != null ? String.valueOf(args.get("riskLevel")).toUpperCase() : "ALL";

        List<InventoryForecastDto> allForecast = inventoryForecastService.getInventoryForecast();
        List<InventoryForecastDto> allRiskItems = allForecast.stream()
                .filter(i -> !"SAFE".equalsIgnoreCase(i.getRiskLevel()))
                .toList();

        if ("CRITICAL".equals(riskLevel)) {
            allRiskItems = allRiskItems.stream().filter(i -> "CRITICAL".equalsIgnoreCase(i.getRiskLevel())).toList();
        } else if ("WARNING".equals(riskLevel)) {
            allRiskItems = allRiskItems.stream().filter(i -> "WARNING".equalsIgnoreCase(i.getRiskLevel())).toList();
        }

        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);

        if (allRiskItems.isEmpty()) {
            Map<String, Object> data = Map.of("count", 0, "message", "Kho hàng hiện đang trong trạng thái rất an toàn!");
            return ToolExecutionResult.success(getToolName(), "🎉 Tuyệt vời! Hiện không có sản phẩm nào thuộc diện nguy cơ cạn kho theo tiêu chí yêu cầu.", data, null);
        }

        BigDecimal totalBudget = BigDecimal.ZERO;
        for (InventoryForecastDto item : allRiskItems) {
            if (item.getEstimatedRestockCost() != null) {
                totalBudget = totalBudget.add(item.getEstimatedRestockCost());
            }
        }

        // Lấy sản phẩm nguy cấp nhất để hiển thị STOCK_ALERT_CARD
        InventoryForecastDto topCritical = allRiskItems.get(0);

        Map<String, Object> widgetData = new LinkedHashMap<>();
        widgetData.put("productId", topCritical.getProductId());
        widgetData.put("sku", topCritical.getProductSku());
        widgetData.put("name", topCritical.getProductName());
        widgetData.put("availableStock", topCritical.getAvailableStock());
        widgetData.put("daysLeft", topCritical.getDaysOfStockRemaining());
        widgetData.put("riskLevel", topCritical.getRiskLevel());
        widgetData.put("suggestedRestockQty", topCritical.getSuggestedRestockQty());
        widgetData.put("dailySalesVelocity", topCritical.getAvgDailySales());
        widgetData.put("estimatedBudgetFormatted", topCritical.getEstimatedRestockCost() != null ? currFmt.format(topCritical.getEstimatedRestockCost()) : "0 đ");
        widgetData.put("imageUrl", topCritical.getProductImageUrl());
        widgetData.put("totalRiskCount", allRiskItems.size());
        widgetData.put("totalBudgetFormatted", currFmt.format(totalBudget));

        UiWidgetDto widget = UiWidgetDto.builder()
                .type("STOCK_ALERT_CARD")
                .data(widgetData)
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🚨 **PHÁT HIỆN %d SẢN PHẨM CÓ NGUY CƠ CẠN KHO** (Ngân sách đề xuất nhập: **%s**):\n\n",
                allRiskItems.size(), currFmt.format(totalBudget)));

        int limit = Math.min(allRiskItems.size(), 5);
        for (int i = 0; i < limit; i++) {
            InventoryForecastDto it = allRiskItems.get(i);
            String icon = "CRITICAL".equalsIgnoreCase(it.getRiskLevel()) ? "🔴" : "🟡";
            sb.append(String.format("%s **%s** (`%s`): Còn tồn **%d**, tốc độ bán **%.1f c/ngày** ➔ Hết hàng sau **%d ngày**. Cần nhập: **%d** (%s)\n",
                    icon, it.getProductName(), it.getProductSku(), it.getAvailableStock(),
                    it.getAvgDailySales() != null ? it.getAvgDailySales() : 0.0,
                    it.getDaysOfStockRemaining() != null ? it.getDaysOfStockRemaining() : 0,
                    it.getSuggestedRestockQty() != null ? it.getSuggestedRestockQty() : 0,
                    it.getEstimatedRestockCost() != null ? currFmt.format(it.getEstimatedRestockCost()) : "0 đ"));
        }

        if (allRiskItems.size() > limit) {
            sb.append(String.format("\n_...và còn %d sản phẩm khác có nguy cơ tương tự._\n", allRiskItems.size() - limit));
        }
        sb.append("\n👉 Bạn có thể yêu cầu tôi: *\"Xuất file Excel dự báo cạn kho\"* để tải toàn bộ bảng phân tích chi tiết!");

        Map<String, Object> resData = new LinkedHashMap<>();
        resData.put("totalItems", allRiskItems.size());
        resData.put("totalBudget", totalBudget);

        return ToolExecutionResult.success(getToolName(), sb.toString(), resData, widget);
    }
}
