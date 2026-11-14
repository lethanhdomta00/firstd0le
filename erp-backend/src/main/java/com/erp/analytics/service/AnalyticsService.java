package com.erp.analytics.service;

import com.erp.analytics.dto.*;
import com.erp.analytics.projection.*;
import com.erp.analytics.repository.AnalyticsRepository;
import com.erp.model.Product;
import com.erp.order.dto.OrderStatsAggregate;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentMethod;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM");

    @Transactional(readOnly = true)
    public AnalyticsOverviewDto getOverview(LocalDate fromDate, LocalDate toDate) {
        if (toDate == null) {
            toDate = LocalDate.now();
        }
        if (fromDate == null) {
            fromDate = toDate.minusDays(29);
        }
        if (fromDate.isAfter(toDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(LocalTime.MAX);

        // 1. KPI cơ bản từ OrderStatsAggregate
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        OrderStatsAggregate statsAgg = orderRepository.getOrderStatsAggregate(startOfToday);

        long totalOrders = statsAgg.totalOrders();
        long completedOrders = statsAgg.completedOrders();
        long cancelledOrders = statsAgg.cancelledOrders();
        long activeOrders = totalOrders - cancelledOrders;

        BigDecimal activeRevenue = statsAgg.activeRevenue();
        BigDecimal avgOrderValue = activeOrders > 0
                ? activeRevenue.divide(BigDecimal.valueOf(activeOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double completionRate = totalOrders > 0
                ? Math.round((completedOrders * 1000.0) / totalOrders) / 10.0
                : 0.0;

        // 2. Định giá kho từ Product
        InventoryValuationProjection valuationProj = analyticsRepository.getInventoryValuation();
        BigDecimal inventoryValuation = valuationProj != null && valuationProj.getTotalValuation() != null
                ? valuationProj.getTotalValuation()
                : BigDecimal.ZERO;
        long totalStockUnits = valuationProj != null && valuationProj.getTotalUnits() != null
                ? valuationProj.getTotalUnits()
                : 0L;
        long lowStockCount = valuationProj != null && valuationProj.getLowStockCount() != null
                ? valuationProj.getLowStockCount()
                : 0L;

        // 3. Biểu đồ xu hướng doanh thu theo ngày (Lấp đầy các ngày 0 đồng để biểu đồ liên tục)
        List<RevenueTrendProjection> rawTrends = analyticsRepository.getDailyRevenueTrends(fromDateTime, toDateTime);
        Map<String, RevenueTrendProjection> trendMap = new HashMap<>();
        for (RevenueTrendProjection p : rawTrends) {
            if (p.getOrderDate() != null) {
                trendMap.put(p.getOrderDate(), p);
            }
        }

        List<TimeSeriesPointDto> trends = new ArrayList<>();
        LocalDate cur = fromDate;
        while (!cur.isAfter(toDate)) {
            String dateKey = cur.format(DATE_FMT);
            String displayLabel = cur.format(DISPLAY_FMT);

            RevenueTrendProjection item = trendMap.get(dateKey);
            BigDecimal dayRevenue = (item != null && item.getRevenue() != null) ? item.getRevenue() : BigDecimal.ZERO;
            long dayOrders = (item != null && item.getOrderCount() != null) ? item.getOrderCount() : 0L;

            trends.add(TimeSeriesPointDto.builder()
                    .date(dateKey)
                    .label(displayLabel)
                    .revenue(dayRevenue)
                    .orderCount(dayOrders)
                    .build());

            cur = cur.plusDays(1);
        }

        // 4. Phân bổ trạng thái đơn hàng (Donut)
        List<StatusCountProjection> statusRaw = analyticsRepository.getOrderStatusBreakdown(fromDateTime, toDateTime);
        long rangeTotalStatusCount = statusRaw.stream().mapToLong(s -> s.getCount() != null ? s.getCount() : 0L).sum();
        List<OrderStatusDistributionDto> statusBreakdown = new ArrayList<>();
        for (StatusCountProjection s : statusRaw) {
            long count = s.getCount() != null ? s.getCount() : 0L;
            double pct = rangeTotalStatusCount > 0 ? Math.round((count * 1000.0) / rangeTotalStatusCount) / 10.0 : 0.0;
            String st = s.getStatus();
            String label = resolveStatusLabel(st);
            String color = resolveStatusColor(st);

            statusBreakdown.add(OrderStatusDistributionDto.builder()
                    .status(st)
                    .label(label)
                    .count(count)
                    .percentage(pct)
                    .color(color)
                    .build());
        }

        // 5. Phân bổ phương thức thanh toán
        List<PaymentMethodProjection> payRaw = analyticsRepository.getPaymentMethodBreakdown(fromDateTime, toDateTime);
        long rangeTotalPayCount = payRaw.stream().mapToLong(p -> p.getCount() != null ? p.getCount() : 0L).sum();
        List<PaymentMethodDistributionDto> payBreakdown = new ArrayList<>();
        for (PaymentMethodProjection p : payRaw) {
            long count = p.getCount() != null ? p.getCount() : 0L;
            double pct = rangeTotalPayCount > 0 ? Math.round((count * 1000.0) / rangeTotalPayCount) / 10.0 : 0.0;
            String method = p.getPaymentMethod();
            String label = resolvePaymentMethodLabel(method);
            String color = resolvePaymentMethodColor(method);

            payBreakdown.add(PaymentMethodDistributionDto.builder()
                    .method(method)
                    .label(label)
                    .count(count)
                    .totalAmount(p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                    .percentage(pct)
                    .color(color)
                    .build());
        }

        // 6. Top sản phẩm bán chạy (Top 8)
        List<TopProductProjection> topRaw = analyticsRepository.getTopSellingProducts(fromDateTime, toDateTime, 8);
        List<TopProductDto> topProducts = new ArrayList<>();
        for (TopProductProjection t : topRaw) {
            topProducts.add(TopProductDto.builder()
                    .productId(t.getProductId())
                    .productName(t.getProductName())
                    .productSku(t.getProductSku())
                    .productImageUrl(t.getProductImageUrl())
                    .totalQuantitySold(t.getTotalQuantity() != null ? t.getTotalQuantity() : 0L)
                    .totalRevenue(t.getTotalRevenue() != null ? t.getTotalRevenue() : BigDecimal.ZERO)
                    .currentStock(t.getCurrentStock() != null ? t.getCurrentStock() : 0L)
                    .build());
        }

        // 7. Cảnh báo tồn kho thấp (stock <= 10)
        List<Product> lowStockEntities = productRepository.findTopLowStockProducts(10, PageRequest.of(0, 8));
        List<LowStockAlertDto> lowStockAlerts = new ArrayList<>();
        for (Product p : lowStockEntities) {
            lowStockAlerts.add(LowStockAlertDto.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .productSku(p.getSku())
                    .productImageUrl(p.getImageUrl())
                    .currentStock(p.getStock())
                    .price(p.getPrice())
                    .categoryName("Sản phẩm ERP")
                    .build());
        }

        return AnalyticsOverviewDto.builder()
                .totalRevenue(activeRevenue)
                .completedRevenue(statsAgg.completedRevenue())
                .paidRevenue(statsAgg.paidRevenue())
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .todayOrders(statsAgg.todayOrders())
                .averageOrderValue(avgOrderValue)
                .completionRate(completionRate)
                .inventoryValuation(inventoryValuation)
                .totalStockUnits(totalStockUnits)
                .lowStockCount(lowStockCount)
                .revenueTrends(trends)
                .orderStatusBreakdown(statusBreakdown)
                .paymentMethodBreakdown(payBreakdown)
                .topSellingProducts(topProducts)
                .lowStockAlerts(lowStockAlerts)
                .build();
    }

    private String resolveStatusLabel(String status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "SHIPPING" -> "Đang giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String resolveStatusColor(String status) {
        if (status == null) return "#94a3b8";
        return switch (status) {
            case "PENDING" -> "#f59e0b";
            case "CONFIRMED" -> "#3b82f6";
            case "SHIPPING" -> "#8b5cf6";
            case "COMPLETED" -> "#10b981";
            case "CANCELLED" -> "#ef4444";
            default -> "#64748b";
        };
    }

    private String resolvePaymentMethodLabel(String method) {
        if (method == null) return "Khác";
        return switch (method) {
            case "CASH" -> "Tiền mặt";
            case "BANK_TRANSFER" -> "Chuyển khoản";
            case "CREDIT_CARD" -> "Thẻ tín dụng";
            case "E_WALLET" -> "Ví điện tử";
            default -> method;
        };
    }

    private String resolvePaymentMethodColor(String method) {
        if (method == null) return "#64748b";
        return switch (method) {
            case "CASH" -> "#10b981";
            case "BANK_TRANSFER" -> "#2563eb";
            case "CREDIT_CARD" -> "#8b5cf6";
            case "E_WALLET" -> "#f59e0b";
            default -> "#94a3b8";
        };
    }

    @Transactional(readOnly = true)
    public byte[] exportAnalyticsExcelXml(LocalDate fromDate, LocalDate toDate) {
        AnalyticsOverviewDto data = getOverview(fromDate, toDate);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");

        // Styles
        sb.append("<Styles>\n");
        sb.append(" <Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Arial\" ss:Size=\"10\"/></Style>\n");
        sb.append(" <Style ss:ID=\"HeaderTitle\"><Font ss:FontName=\"Arial\" ss:Size=\"14\" ss:Bold=\"1\" ss:Color=\"#1e293b\"/></Style>\n");
        sb.append(" <Style ss:ID=\"ColHeader\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#ffffff\"/><Interior ss:Color=\"#2563eb\" ss:Pattern=\"Solid\"/><Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#cbd5e1\"/></Borders></Style>\n");
        sb.append(" <Style ss:ID=\"NumberCell\"><NumberFormat ss:Format=\"#,##0\"/></Style>\n");
        sb.append(" <Style ss:ID=\"MoneyCell\"><NumberFormat ss:Format=\"#,##0&quot; đ&quot;\"/><Font ss:Bold=\"1\" ss:Color=\"#0f172a\"/></Style>\n");
        sb.append(" <Style ss:ID=\"TotalRow\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#0f172a\"/><Interior ss:Color=\"#f1f5f9\" ss:Pattern=\"Solid\"/><Borders><Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\" ss:Color=\"#2563eb\"/></Borders></Style>\n");
        sb.append("</Styles>\n");

        // Sheet 1: Báo cáo Doanh thu theo Ngày
        sb.append("<Worksheet ss:Name=\"Doanh Thu Theo Ngay\">\n");
        sb.append("<Table>\n");
        sb.append("<Column ss:Width=\"120\"/><Column ss:Width=\"100\"/><Column ss:Width=\"150\"/>\n");
        sb.append("<Row><Cell ss:StyleID=\"HeaderTitle\"><Data ss:Type=\"String\">BÁO CÁO PHÂN TÍCH DOANH THU &amp; ĐƠN HÀNG</Data></Cell></Row>\n");
        sb.append("<Row><Cell><Data ss:Type=\"String\">Thời gian: ").append(fromDate).append(" đến ").append(toDate).append("</Data></Cell></Row>\n");
        sb.append("<Row></Row>\n");
        sb.append("<Row ss:StyleID=\"ColHeader\">\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Ngày</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Số đơn phát sinh</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Doanh thu trong ngày</Data></Cell>\n");
        sb.append("</Row>\n");

        long sumOrders = 0;
        BigDecimal sumRevenue = BigDecimal.ZERO;
        for (TimeSeriesPointDto p : data.getRevenueTrends()) {
            sb.append("<Row>\n");
            sb.append(" <Cell><Data ss:Type=\"String\">").append(escapeXml(p.getDate())).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">").append(p.getOrderCount()).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"MoneyCell\"><Data ss:Type=\"Number\">").append(p.getRevenue()).append("</Data></Cell>\n");
            sb.append("</Row>\n");
            sumOrders += p.getOrderCount();
            sumRevenue = sumRevenue.add(p.getRevenue());
        }

        sb.append("<Row ss:StyleID=\"TotalRow\">\n");
        sb.append(" <Cell><Data ss:Type=\"String\">TỔNG CỘNG</Data></Cell>\n");
        sb.append(" <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">").append(sumOrders).append("</Data></Cell>\n");
        sb.append(" <Cell ss:StyleID=\"MoneyCell\"><Data ss:Type=\"Number\">").append(sumRevenue).append("</Data></Cell>\n");
        sb.append("</Row>\n");
        sb.append("</Table>\n");
        sb.append("</Worksheet>\n");

        // Sheet 2: Top Sản phẩm Bán chạy
        sb.append("<Worksheet ss:Name=\"Top San Pham Ban Chay\">\n");
        sb.append("<Table>\n");
        sb.append("<Column ss:Width=\"50\"/><Column ss:Width=\"220\"/><Column ss:Width=\"100\"/><Column ss:Width=\"80\"/><Column ss:Width=\"140\"/><Column ss:Width=\"80\"/>\n");
        sb.append("<Row><Cell ss:StyleID=\"HeaderTitle\"><Data ss:Type=\"String\">TOP SẢN PHẨM BÁN CHẠY NHẤT</Data></Cell></Row>\n");
        sb.append("<Row></Row>\n");
        sb.append("<Row ss:StyleID=\"ColHeader\">\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Hạng</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Tên sản phẩm</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Mã SKU</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Đã bán</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Doanh thu mang lại</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Tồn hiện tại</Data></Cell>\n");
        sb.append("</Row>\n");

        int rank = 1;
        for (TopProductDto t : data.getTopSellingProducts()) {
            sb.append("<Row>\n");
            sb.append(" <Cell><Data ss:Type=\"Number\">").append(rank++).append("</Data></Cell>\n");
            sb.append(" <Cell><Data ss:Type=\"String\">").append(escapeXml(t.getProductName())).append("</Data></Cell>\n");
            sb.append(" <Cell><Data ss:Type=\"String\">").append(escapeXml(t.getProductSku())).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">").append(t.getTotalQuantitySold()).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"MoneyCell\"><Data ss:Type=\"Number\">").append(t.getTotalRevenue()).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">").append(t.getCurrentStock()).append("</Data></Cell>\n");
            sb.append("</Row>\n");
        }
        sb.append("</Table>\n");
        sb.append("</Worksheet>\n");

        // Sheet 3: Cảnh báo Tồn kho thấp
        sb.append("<Worksheet ss:Name=\"Canh Bao Ton Kho Thap\">\n");
        sb.append("<Table>\n");
        sb.append("<Column ss:Width=\"60\"/><Column ss:Width=\"220\"/><Column ss:Width=\"100\"/><Column ss:Width=\"120\"/><Column ss:Width=\"80\"/><Column ss:Width=\"120\"/>\n");
        sb.append("<Row><Cell ss:StyleID=\"HeaderTitle\"><Data ss:Type=\"String\">DANH SÁCH SẢN PHẨM SẮP HẾT HÀNG (TỒN &lt;= 10)</Data></Cell></Row>\n");
        sb.append("<Row></Row>\n");
        sb.append("<Row ss:StyleID=\"ColHeader\">\n");
        sb.append(" <Cell><Data ss:Type=\"String\">ID</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Tên sản phẩm</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Mã SKU</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Danh mục</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Tồn kho</Data></Cell>\n");
        sb.append(" <Cell><Data ss:Type=\"String\">Đơn giá niêm yết</Data></Cell>\n");
        sb.append("</Row>\n");

        for (LowStockAlertDto l : data.getLowStockAlerts()) {
            sb.append("<Row>\n");
            sb.append(" <Cell><Data ss:Type=\"Number\">").append(l.getProductId()).append("</Data></Cell>\n");
            sb.append(" <Cell><Data ss:Type=\"String\">").append(escapeXml(l.getProductName())).append("</Data></Cell>\n");
            sb.append(" <Cell><Data ss:Type=\"String\">").append(escapeXml(l.getProductSku())).append("</Data></Cell>\n");
            sb.append(" <Cell><Data ss:Type=\"String\">").append(escapeXml(l.getCategoryName())).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"NumberCell\"><Data ss:Type=\"Number\">").append(l.getCurrentStock()).append("</Data></Cell>\n");
            sb.append(" <Cell ss:StyleID=\"MoneyCell\"><Data ss:Type=\"Number\">").append(l.getPrice() != null ? l.getPrice() : BigDecimal.ZERO).append("</Data></Cell>\n");
            sb.append("</Row>\n");
        }
        sb.append("</Table>\n");
        sb.append("</Worksheet>\n");

        sb.append("</Workbook>\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
