package com.erp.order.service;

import com.erp.order.model.InventoryLog;
import com.erp.order.model.Order;
import com.erp.order.model.OrderItem;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderExportService {

    private final OrderRepository orderRepository;
    private final InventoryLogRepository inventoryLogRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public byte[] exportOrdersExcelXml(String keyword, OrderStatus status, PaymentStatus paymentStatus) {
        List<Order> orders = orderRepository.findAllForExport(trimOrNull(keyword), status, paymentStatus);
        List<InventoryLog> logs = inventoryLogRepository.findTop200ByOrderByCreatedAtDesc();

        StringBuilder sb = new StringBuilder(16384);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");

        // Styles
        sb.append("<Styles>\n");
        sb.append("<Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#0F172A\"/></Style>\n");
        sb.append("<Style ss:ID=\"Title\"><Font ss:FontName=\"Calibri\" ss:Size=\"14\" ss:Bold=\"1\" ss:Color=\"#1E3A8A\"/></Style>\n");
        sb.append("<Style ss:ID=\"Header\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>");
        sb.append("<Interior ss:Color=\"#1D4ED8\" ss:Pattern=\"Solid\"/><Alignment ss:Vertical=\"Center\"/></Style>\n");
        sb.append("<Style ss:ID=\"Money\"><NumberFormat ss:Format=\"#,##0\"/><Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/></Style>\n");
        sb.append("<Style ss:ID=\"MoneyBold\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#1D4ED8\"/>");
        sb.append("<NumberFormat ss:Format=\"#,##0\"/><Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/></Style>\n");
        sb.append("<Style ss:ID=\"Center\"><Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/></Style>\n");
        sb.append("<Style ss:ID=\"BoldLabel\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\"/></Style>\n");
        sb.append("</Styles>\n");

        // Sheet 1: Danh sách Đơn hàng
        sb.append("<Worksheet ss:Name=\"Don Hang ERP\">\n<Table>\n");
        int[] orderWidths = {140, 160, 100, 120, 110, 110, 95, 95, 95, 110, 260, 120};
        for (int w : orderWidths) {
            sb.append("<Column ss:Width=\"").append(w).append("\"/>\n");
        }

        sb.append("<Row ss:Height=\"26\"><Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">BÁO CÁO ĐƠN HÀNG &amp; DOANH THU - ERP CLOUD</Data></Cell></Row>\n");
        sb.append("<Row ss:Height=\"24\">");
        String[] orderHeaders = {
                "Mã đơn", "Khách hàng", "SĐT", "Trạng thái", "Thanh toán", "Phương thức",
                "Tạm tính", "Giảm giá", "Thuế VAT", "Tổng thanh toán", "Chi tiết sản phẩm", "Ngày tạo"
        };
        for (String h : orderHeaders) {
            sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">").append(xmlEscape(h)).append("</Data></Cell>");
        }
        sb.append("</Row>\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order o : orders) {
            String itemsSummary = o.getItems().stream()
                    .map(i -> i.getProductName() + " x" + i.getQuantity())
                    .collect(Collectors.joining("; "));
            if (o.getStatus() != OrderStatus.CANCELLED && o.getTotalAmount() != null) {
                totalRevenue = totalRevenue.add(o.getTotalAmount());
            }

            sb.append("<Row ss:Height=\"21\">");
            strCell(sb, o.getOrderCode(), "BoldLabel");
            strCell(sb, o.getCustomerName(), "Default");
            strCell(sb, safeStr(o.getCustomerPhone()), "Center");
            strCell(sb, o.getStatus().getLabel(), "Center");
            strCell(sb, o.getPaymentStatus().getLabel(), "Center");
            strCell(sb, o.getPaymentMethod().getLabel(), "Center");
            numCell(sb, o.getSubtotal(), "Money");
            numCell(sb, o.getDiscountAmount(), "Money");
            numCell(sb, o.getTaxAmount(), "Money");
            numCell(sb, o.getTotalAmount(), "MoneyBold");
            strCell(sb, itemsSummary, "Default");
            strCell(sb, o.getCreatedAt() != null ? o.getCreatedAt().format(DT_FMT) : "", "Center");
            sb.append("</Row>\n");
        }

        // Summary Row
        sb.append("<Row ss:Height=\"24\">");
        sb.append("<Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">TỔNG DOANH THU HIỆU LỰC (KHÔNG TÍNH ĐƠN HỦY)</Data></Cell>");
        for (int i = 0; i < 8; i++) {
            sb.append("<Cell/>");
        }
        numCell(sb, totalRevenue, "MoneyBold");
        sb.append("</Row>\n");
        sb.append("</Table>\n</Worksheet>\n");

        // Sheet 2: Lịch sử Xuất/Nhập Kho
        sb.append("<Worksheet ss:Name=\"Lich Su Xuat Nhap Kho\">\n<Table>\n");
        int[] logWidths = {110, 180, 140, 80, 80, 80, 130, 220, 100, 120};
        for (int w : logWidths) {
            sb.append("<Column ss:Width=\"").append(w).append("\"/>\n");
        }
        sb.append("<Row ss:Height=\"26\"><Cell ss:StyleID=\"Title\"><Data ss:Type=\"String\">NHẬT KÝ XUẤT / NHẬP KHO HÀNG HÓA</Data></Cell></Row>\n");
        sb.append("<Row ss:Height=\"24\">");
        String[] logHeaders = {"Mã SKU", "Sản phẩm", "Loại biến động", "Thay đổi", "Tồn trước", "Tồn sau", "Chứng từ", "Ghi chú", "Người thực hiện", "Thời gian"};
        for (String h : logHeaders) {
            sb.append("<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">").append(xmlEscape(h)).append("</Data></Cell>");
        }
        sb.append("</Row>\n");

        for (InventoryLog l : logs) {
            sb.append("<Row ss:Height=\"20\">");
            strCell(sb, l.getProductSku(), "BoldLabel");
            strCell(sb, l.getProductName(), "Default");
            strCell(sb, l.getMovementType().getLabel(), "Center");
            sb.append("<Cell ss:StyleID=\"Center\"><Data ss:Type=\"Number\">").append(l.getQuantityDelta()).append("</Data></Cell>");
            sb.append("<Cell ss:StyleID=\"Center\"><Data ss:Type=\"Number\">").append(l.getStockBefore()).append("</Data></Cell>");
            sb.append("<Cell ss:StyleID=\"Center\"><Data ss:Type=\"Number\">").append(l.getStockAfter()).append("</Data></Cell>");
            strCell(sb, safeStr(l.getReferenceCode()), "Center");
            strCell(sb, safeStr(l.getNote()), "Default");
            strCell(sb, safeStr(l.getActorUsername()), "Center");
            strCell(sb, l.getCreatedAt() != null ? l.getCreatedAt().format(DT_FMT) : "", "Center");
            sb.append("</Row>\n");
        }
        sb.append("</Table>\n</Worksheet>\n");
        sb.append("</Workbook>");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportOrdersCsv(String keyword, OrderStatus status, PaymentStatus paymentStatus) {
        List<Order> orders = orderRepository.findAllForExport(trimOrNull(keyword), status, paymentStatus);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM for Excel
        sb.append("Mã đơn,Khách hàng,Số điện thoại,Email,Trạng thái,Thanh toán,Phương thức,Tạm tính,Giảm giá,VAT,Tổng cộng,Sản phẩm,Ngày tạo\n");
        for (Order o : orders) {
            String itemsSummary = o.getItems().stream()
                    .map((OrderItem i) -> i.getProductName() + " (x" + i.getQuantity() + ")")
                    .collect(Collectors.joining(" | "));
            sb.append(csvEscape(o.getOrderCode())).append(',')
              .append(csvEscape(o.getCustomerName())).append(',')
              .append(csvEscape(safeStr(o.getCustomerPhone()))).append(',')
              .append(csvEscape(safeStr(o.getCustomerEmail()))).append(',')
              .append(csvEscape(o.getStatus().getLabel())).append(',')
              .append(csvEscape(o.getPaymentStatus().getLabel())).append(',')
              .append(csvEscape(o.getPaymentMethod().getLabel())).append(',')
              .append(o.getSubtotal() != null ? o.getSubtotal().toPlainString() : "0").append(',')
              .append(o.getDiscountAmount() != null ? o.getDiscountAmount().toPlainString() : "0").append(',')
              .append(o.getTaxAmount() != null ? o.getTaxAmount().toPlainString() : "0").append(',')
              .append(o.getTotalAmount() != null ? o.getTotalAmount().toPlainString() : "0").append(',')
              .append(csvEscape(itemsSummary)).append(',')
              .append(csvEscape(o.getCreatedAt() != null ? o.getCreatedAt().format(DT_FMT) : ""))
              .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void strCell(StringBuilder sb, String val, String style) {
        sb.append("<Cell ss:StyleID=\"").append(style).append("\"><Data ss:Type=\"String\">")
          .append(xmlEscape(val)).append("</Data></Cell>");
    }

    private void numCell(StringBuilder sb, BigDecimal val, String style) {
        BigDecimal safe = val != null ? val : BigDecimal.ZERO;
        sb.append("<Cell ss:StyleID=\"").append(style).append("\"><Data ss:Type=\"Number\">")
          .append(safe.toPlainString()).append("</Data></Cell>");
    }

    private String xmlEscape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String csvEscape(String input) {
        if (input == null) return "\"\"";
        return "\"" + input.replace("\"", "\"\"") + "\"";
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
