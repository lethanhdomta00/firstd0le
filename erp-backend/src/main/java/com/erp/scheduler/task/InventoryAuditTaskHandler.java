package com.erp.scheduler.task;

import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import com.erp.service.TelegramService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryAuditTaskHandler implements ScheduledTaskHandler {

    private final ProductRepository productRepository;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getJobType() {
        return "INVENTORY_AUDIT";
    }

    @Override
    public String getDisplayName() {
        return "Quét & Cảnh báo Tồn kho an toàn";
    }

    @Override
    public String execute(String paramsJson, String triggeredBy) throws Exception {
        int threshold = 10;
        boolean notifyTelegram = true;

        if (paramsJson != null && !paramsJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(paramsJson);
                if (root.has("threshold")) {
                    threshold = root.get("threshold").asInt(10);
                }
                if (root.has("notifyTelegram")) {
                    notifyTelegram = root.get("notifyTelegram").asBoolean(true);
                }
            } catch (Exception e) {
                log.warn("Không thể parse paramsJson cho INVENTORY_AUDIT, dùng giá trị mặc định: {}", e.getMessage());
            }
        }

        List<Product> allProducts = productRepository.findAll();
        int finalThreshold = threshold;
        List<Product> lowStockProducts = allProducts.stream()
                .filter(p -> p.getStock() != null && p.getStock() <= finalThreshold)
                .collect(Collectors.toList());

        String summary = String.format("Kiểm tra %d sản phẩm. Phát hiện %d sản phẩm dưới ngưỡng tồn kho an toàn (<= %d).",
                allProducts.size(), lowStockProducts.size(), threshold);

        if (!lowStockProducts.isEmpty() && notifyTelegram) {
            StringBuilder msg = new StringBuilder();
            msg.append("⚠️ <b>[ERP CẢNH BÁO TỒN KHO]</b>\n\n");
            msg.append("Phát hiện <b>").append(lowStockProducts.size()).append("</b> mặt hàng sắp hết hàng (ngưỡng &lt;= ").append(threshold).append("):\n\n");

            int count = 0;
            for (Product p : lowStockProducts) {
                if (count++ >= 8) {
                    msg.append("... và ").append(lowStockProducts.size() - 8).append(" sản phẩm khác.\n");
                    break;
                }
                msg.append("• <b>").append(p.getName()).append("</b> (SKU: <code>").append(p.getSku()).append("</code>) - Còn lại: <b>").append(p.getStock()).append("</b> cái\n");
            }
            msg.append("\nKích hoạt bởi: <i>").append(triggeredBy).append("</i>");
            telegramService.sendMessage(msg.toString());
        }

        log.info("INVENTORY_AUDIT hoàn thành: {}", summary);
        return summary;
    }
}

