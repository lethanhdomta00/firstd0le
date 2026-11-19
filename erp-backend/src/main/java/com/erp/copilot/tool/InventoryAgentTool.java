package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.model.Product;
import com.erp.order.dto.InventoryForecastDto;
import com.erp.order.service.InventoryForecastService;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAgentTool implements AgentTool {

    private final ProductRepository productRepository;
    private final InventoryForecastService inventoryForecastService;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "checkProductStock";
    }

    @Override
    public String getDescription() {
        return "Tra cứu tồn kho thực tế, tồn khả dụng, hàng giữ chỗ và cảnh báo cạn kho của sản phẩm theo SKU hoặc tên.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> keyword = new HashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "Mã SKU hoặc tên sản phẩm cần kiểm tra tồn kho (vd: 'POLO-NAM-BLACK', 'Áo Polo', 'Quần Âu')");
        properties.put("keyword", keyword);

        Map<String, Object> checkForecast = new HashMap<>();
        checkForecast.put("type", "boolean");
        checkForecast.put("description", "Có kiểm tra kèm dự báo nguy cơ cạn kho AI hay không");
        properties.put("checkForecast", checkForecast);

        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("keyword"));
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String keyword = args.get("keyword") != null ? String.valueOf(args.get("keyword")).trim() : "";
        if (keyword.isEmpty()) {
            return ToolExecutionResult.failure(getToolName(), "Vui lòng cung cấp mã SKU hoặc tên sản phẩm cần kiểm tra!");
        }

        // Tìm kiếm theo SKU chính xác trước, sau đó tìm theo tên chứa từ khóa
        Optional<Product> productOpt = productRepository.findBySku(keyword);
        if (productOpt.isEmpty()) {
            List<Product> products = productRepository.findAll().stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(keyword.toLowerCase())
                            || (p.getSku() != null && p.getSku().toLowerCase().contains(keyword.toLowerCase())))
                    .limit(5)
                    .toList();
            if (!products.isEmpty()) {
                productOpt = Optional.of(products.get(0));
            }
        }

        if (productOpt.isEmpty()) {
            return ToolExecutionResult.failure(getToolName(), "Không tìm thấy sản phẩm nào khớp với từ khóa: '" + keyword + "' trong hệ thống.");
        }

        Product p = productOpt.get();
        int physical = p.getPhysicalStock() != null ? p.getPhysicalStock() : (p.getStock() != null ? p.getStock() : 0);
        int reserved = p.getReservedStock() != null ? p.getReservedStock() : 0;
        int available = p.getStock() != null ? p.getStock() : Math.max(0, physical - reserved);
        int incoming = p.getIncomingStock() != null ? p.getIncomingStock() : 0;
        int minAlert = p.getMinStockAlert() != null ? p.getMinStockAlert() : 10;

        boolean isLowStock = available <= minAlert;
        int suggestedRestock = Math.max(20, (minAlert * 2) - available);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productId", p.getId());
        data.put("sku", p.getSku());
        data.put("name", p.getName());
        data.put("physicalStock", physical);
        data.put("reservedStock", reserved);
        data.put("availableStock", available);
        data.put("incomingStock", incoming);
        data.put("minAlert", minAlert);
        data.put("price", p.getPrice());
        data.put("baseUnit", p.getBaseUnit() != null ? p.getBaseUnit() : "Cái");
        data.put("isLowStock", isLowStock);

        // Tạo Widget STOCK_ALERT_CARD phục vụ Generative UI
        Map<String, Object> widgetData = new LinkedHashMap<>(data);
        widgetData.put("suggestedRestockQty", suggestedRestock);
        widgetData.put("imageUrl", p.getImageUrl());
        widgetData.put("riskLevel", isLowStock ? (available <= 3 ? "CRITICAL" : "WARNING") : "SAFE");

        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);
        String priceFormatted = p.getPrice() != null ? currFmt.format(p.getPrice()) : "0 đ";
        widgetData.put("priceFormatted", priceFormatted);

        UiWidgetDto widget = UiWidgetDto.builder()
                .type("STOCK_ALERT_CARD")
                .data(widgetData)
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Sản phẩm **%s** (SKU: `%s`):\n", p.getName(), p.getSku()));
        sb.append(String.format("- **Tồn kho thực tế (vật lý)**: %d %s\n", physical, p.getBaseUnit()));
        sb.append(String.format("- **Hàng chờ giao (đã giữ chỗ)**: %d %s\n", reserved, p.getBaseUnit()));
        sb.append(String.format("- **Tồn khả dụng bán ngay**: %d %s\n", available, p.getBaseUnit()));
        if (incoming > 0) {
            sb.append(String.format("- **Hàng đang trên đường về**: %d %s\n", incoming, p.getBaseUnit()));
        }
        sb.append(String.format("- **Đơn giá niêm yết**: %s\n", priceFormatted));

        if (isLowStock) {
            sb.append(String.format("\n⚠️ **CẢNH BÁO TỒN KHO THẤP**: Tồn khả dụng (%d) đã chạm ngưỡng an toàn (%d). Đề xuất bổ sung gấp tối thiểu **%d %s**!",
                    available, minAlert, suggestedRestock, p.getBaseUnit()));
        } else {
            sb.append("\n✅ **Trạng thái kho an toàn**: Đáp ứng tốt các đơn hàng bán lẻ và đại lý trong tuần.");
        }

        return ToolExecutionResult.success(getToolName(), sb.toString(), data, widget);
    }
}
