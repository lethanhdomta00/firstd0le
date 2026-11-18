package com.erp.order.service;

import com.erp.model.Product;
import com.erp.order.dto.InventoryForecastDto;
import com.erp.order.model.OrderStatus;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryForecastService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    /**
     * Phân tích tốc độ bán hàng & dự báo cạn kho (AI Inventory Forecast)
     */
    @Transactional(readOnly = true)
    public List<InventoryForecastDto> getInventoryForecast() {
        List<Product> products = productRepository.findAll();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Lấy toàn bộ đơn hàng trong 30 ngày qua (trừ đơn CANCELLED)
        List<com.erp.order.model.Order> recentOrders = orderRepository.findRecentOrders(thirtyDaysAgo, OrderStatus.CANCELLED);

        // Gom tổng số lượng bán ra theo productId
        Map<Long, Integer> sales30DaysMap = new HashMap<>();
        for (com.erp.order.model.Order order : recentOrders) {
            for (com.erp.order.model.OrderItem item : order.getItems()) {
                if (item.getProductId() != null) {
                    int rate = item.getUomConversionRate() != null ? item.getUomConversionRate().intValue() : 1;
                    int baseQty = (item.getQuantity() != null ? item.getQuantity() : 0) * rate;
                    sales30DaysMap.merge(item.getProductId(), baseQty, Integer::sum);
                }
            }
        }

        List<InventoryForecastDto> forecastList = new ArrayList<>();

        for (Product p : products) {
            int sold30Days = sales30DaysMap.getOrDefault(p.getId(), 0);
            double avgDaily = Math.max(0.05, sold30Days / 30.0);

            int availableStock = p.getAvailableStock();
            int minAlert = p.getMinStockAlert() != null ? p.getMinStockAlert() : 10;

            int daysRemaining;
            String riskLevel;
            String riskLevelLabel;

            if (availableStock <= 0) {
                daysRemaining = 0;
                riskLevel = "CRITICAL";
                riskLevelLabel = "Hết hàng (Khẩn cấp)";
            } else {
                daysRemaining = (int) Math.round(availableStock / avgDaily);
                if (daysRemaining <= 3 || availableStock <= minAlert) {
                    riskLevel = "CRITICAL";
                    riskLevelLabel = "Sắp cạn kho (≤ 3 ngày)";
                } else if (daysRemaining <= 7) {
                    riskLevel = "WARNING";
                    riskLevelLabel = "Cần chú ý (4 - 7 ngày)";
                } else {
                    riskLevel = "SAFE";
                    riskLevelLabel = "An toàn (> 7 ngày)";
                }
            }

            // Đề xuất số lượng đặt hàng từ Nhà cung cấp
            int targetStock = Math.max(minAlert * 3, (int) Math.ceil(avgDaily * 30));
            int suggestedRestock = Math.max(0, targetStock - availableStock);

            BigDecimal unitCost = p.getMovingAvgCost() != null && p.getMovingAvgCost().compareTo(BigDecimal.ZERO) > 0
                    ? p.getMovingAvgCost() : (p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            BigDecimal estimatedCost = unitCost.multiply(BigDecimal.valueOf(suggestedRestock));

            forecastList.add(InventoryForecastDto.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .productSku(p.getSku())
                    .productImageUrl(p.getImageUrl())
                    .baseUnit(p.getBaseUnit() != null ? p.getBaseUnit() : "Cái")
                    .availableStock(availableStock)
                    .minStockAlert(minAlert)
                    .totalSoldLast30Days(sold30Days)
                    .avgDailySales(Math.round(avgDaily * 100.0) / 100.0)
                    .daysOfStockRemaining(daysRemaining)
                    .riskLevel(riskLevel)
                    .riskLevelLabel(riskLevelLabel)
                    .suggestedRestockQty(suggestedRestock)
                    .estimatedRestockCost(estimatedCost)
                    .build());
        }

        // Sắp xếp: Ưu tiên CRITICAL trước, sau đó WARNING, cuối cùng SAFE
        forecastList.sort((a, b) -> {
            int rankA = "CRITICAL".equals(a.getRiskLevel()) ? 0 : ("WARNING".equals(a.getRiskLevel()) ? 1 : 2);
            int rankB = "CRITICAL".equals(b.getRiskLevel()) ? 0 : ("WARNING".equals(b.getRiskLevel()) ? 1 : 2);
            if (rankA != rankB) return Integer.compare(rankA, rankB);
            return Integer.compare(a.getDaysOfStockRemaining(), b.getDaysOfStockRemaining());
        });

        return forecastList;
    }
}
