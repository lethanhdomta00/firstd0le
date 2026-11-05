package com.erp.scheduler.task;

import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import com.erp.service.TelegramService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailySalesSummaryTaskHandler implements ScheduledTaskHandler {

    private final ProductRepository productRepository;
    private final TelegramService telegramService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getJobType() {
        return "DAILY_SALES_REPORT";
    }

    @Override
    public String getDisplayName() {
        return "Tổng hợp Báo cáo Kho vận & Giá trị hàng hóa";
    }

    @Override
    public String execute(String paramsJson, String triggeredBy) throws Exception {
        boolean notifyTelegram = true;

        if (paramsJson != null && !paramsJson.trim().isEmpty()) {
            try {
                JsonNode root = objectMapper.readTree(paramsJson);
                if (root.has("notifyTelegram")) {
                    notifyTelegram = root.get("notifyTelegram").asBoolean(true);
                }
            } catch (Exception e) {
                log.warn("Lỗi parse paramsJson cho DAILY_SALES_REPORT: {}", e.getMessage());
            }
        }

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

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedValue = currencyFormat.format(totalInventoryValue);

        String summary = String.format("Tổng SKUs: %d | Tổng tồn kho vật lý: %d sản phẩm | Hết hàng: %d | Tổng giá trị kho: %s",
                totalSkus, totalStockQty, outOfStockCount, formattedValue);

        if (notifyTelegram) {
            String telegramText = "📊 <b>[ERP BÁO CÁO KHO VẬN TỔNG HỢP]</b>\n\n"
                    + "• <b>Tổng số danh mục (SKU):</b> " + totalSkus + "\n"
                    + "• <b>Tổng lượng hàng trong kho:</b> " + totalStockQty + " chiếc\n"
                    + "• <b>Sản phẩm đã hết hàng (0):</b> " + outOfStockCount + "\n"
                    + "• <b>Ước tính giá trị kho:</b> <b>" + formattedValue + "</b>\n\n"
                    + "<i>Báo cáo định kỳ tự động - Người kích hoạt: " + triggeredBy + "</i>";
            telegramService.sendMessage(telegramText);
        }

        log.info("DAILY_SALES_REPORT hoàn thành: {}", summary);
        return summary;
    }
}

