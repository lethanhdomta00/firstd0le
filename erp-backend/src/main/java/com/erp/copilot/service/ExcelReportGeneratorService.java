package com.erp.copilot.service;

import com.erp.order.dto.CustomerDebtAgingDto;
import com.erp.order.service.OrderService;
import com.erp.order.dto.InventoryForecastDto;
import com.erp.order.model.Order;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.InventoryForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dịch vụ sinh file Excel chuyên nghiệp cấp Enterprise sử dụng Apache POI SXSSFWorkbook.
 * Cơ chế Streaming ghi đĩa đệm (window size 100 rows) chống tràn RAM hoàn hảo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelReportGeneratorService {

    private final OrderRepository orderRepository;
    private final InventoryForecastService inventoryForecastService;
    private final OrderService orderService;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0 'VNĐ'");

    /**
     * Tạo file Excel Báo Cáo Doanh Thu & Đơn Hàng (2 Sheets)
     */
    public byte[] generateSalesReport(LocalDateTime fromDate, LocalDateTime toDate) throws IOException {
        List<Order> orders;
        if (fromDate != null && toDate != null) {
            orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDate, toDate);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.setCompressTempFiles(true);

            // STYLES
            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.ROYAL_BLUE.getIndex());
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle centerStyle = createCenterStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            // SHEET 1: TỔNG HỢP KPI DOANH SỐ
            SXSSFSheet summarySheet = wb.createSheet("1. Tổng quan KPI");
            summarySheet.trackAllColumnsForAutoSizing();

            long totalOrders = orders.size();
            BigDecimal totalRevenue = orders.stream()
                    .filter(o -> !"CANCELLED".equals(o.getStatus()))
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paidRevenue = orders.stream()
                    .filter(o -> "PAID".equals(o.getPaymentStatus()))
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long completedOrders = orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();

            Row titleRow = summarySheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO TỔNG HỢP TÌNH HÌNH DOANH THU & KINH DOANH");
            titleCell.setCellStyle(boldStyle);

            Row periodRow = summarySheet.createRow(1);
            periodRow.createCell(0).setCellValue("Kỳ báo cáo: " + (fromDate != null ? fromDate.format(DATE_TIME_FMT) : "Toàn thời gian") +
                    " đến " + (toDate != null ? toDate.format(DATE_TIME_FMT) : LocalDateTime.now().format(DATE_TIME_FMT)));

            String[] kpiHeaders = {"Chỉ số KPI", "Giá trị thực tế", "Ghi chú phân tích"};
            Row kpiHeadRow = summarySheet.createRow(3);
            for (int i = 0; i < kpiHeaders.length; i++) {
                Cell c = kpiHeadRow.createCell(i);
                c.setCellValue(kpiHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            Object[][] kpiData = {
                    {"Tổng số đơn hàng phát sinh", totalOrders + " đơn", "Bao gồm cả đơn đã hoàn thành và đang xử lý"},
                    {"Doanh thu hiệu lực (trừ đơn hủy)", CURRENCY_FMT.format(totalRevenue), "Tổng giá trị các đơn hợp lệ"},
                    {"Thực thu (Đã thanh toán đủ)", CURRENCY_FMT.format(paidRevenue), "Dòng tiền thực tế đã vào quỹ/ngân hàng"},
                    {"Số đơn hoàn thành xuất sắc", completedOrders + " đơn", "Giao thành công đến tay khách hàng"},
                    {"Tỷ lệ hoàn thành đơn hàng", (totalOrders > 0 ? Math.round((double) completedOrders * 100 / totalOrders) : 0) + "%", "Hiệu suất xử lý giao nhận"}
            };

            int rowIdx = 4;
            for (Object[] rowData : kpiData) {
                Row r = summarySheet.createRow(rowIdx++);
                r.createCell(0).setCellValue((String) rowData[0]);
                r.createCell(1).setCellValue((String) rowData[1]);
                r.createCell(2).setCellValue((String) rowData[2]);
            }
            for (int i = 0; i < 3; i++) summarySheet.autoSizeColumn(i);

            // SHEET 2: CHI TIẾT ĐƠN HÀNG
            SXSSFSheet detailSheet = wb.createSheet("2. Chi tiết Đơn hàng");
            detailSheet.trackAllColumnsForAutoSizing();

            String[] detailHeaders = {"STT", "Mã đơn hàng", "Khách hàng", "SĐT", "Số lượng sp", "Tổng tiền thanh toán", "Thanh toán", "Trạng thái", "ĐVVC", "Mã vận đơn", "Ngày tạo"};
            Row dHeadRow = detailSheet.createRow(0);
            for (int i = 0; i < detailHeaders.length; i++) {
                Cell c = dHeadRow.createCell(i);
                c.setCellValue(detailHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            int dRowIdx = 1;
            int stt = 1;
            for (Order o : orders) {
                Row r = detailSheet.createRow(dRowIdx++);
                r.createCell(0).setCellValue(stt++);
                r.createCell(1).setCellValue(o.getOrderCode());
                r.createCell(2).setCellValue(o.getCustomerName() != null ? o.getCustomerName() : "—");
                r.createCell(3).setCellValue(o.getCustomerPhone() != null ? o.getCustomerPhone() : "—");
                int totalQty = o.getItems() != null ? o.getItems().stream().mapToInt(com.erp.order.model.OrderItem::getQuantity).sum() : 0;
                r.createCell(4).setCellValue(totalQty);

                Cell amtCell = r.createCell(5);
                amtCell.setCellValue(o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0);
                amtCell.setCellStyle(currencyStyle);

                r.createCell(6).setCellValue(translatePaymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : null));
                r.createCell(7).setCellValue(translateOrderStatus(o.getStatus() != null ? o.getStatus().name() : null));
                r.createCell(8).setCellValue(o.getShippingCarrier() != null ? o.getShippingCarrier() : "—");
                r.createCell(9).setCellValue(o.getTrackingCode() != null ? o.getTrackingCode() : "—");
                r.createCell(10).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_TIME_FMT) : "—");
            }
            for (int i = 0; i < detailHeaders.length; i++) detailSheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Tạo file Excel Báo Cáo Dự Báo Cạn Kho & Đề Xuất Nhập Hàng
     */
    public byte[] generateInventoryForecastReport() throws IOException {
        List<InventoryForecastDto> forecastList = inventoryForecastService.getInventoryForecast();

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.setCompressTempFiles(true);

            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.DARK_RED.getIndex());
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            SXSSFSheet sheet = wb.createSheet("Dự báo cạn kho AI");
            sheet.trackAllColumnsForAutoSizing();

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO DỰ BÁO CẠN KHO & ĐỀ XUẤT NHẬP HÀNG THÔNG MINH");
            titleCell.setCellStyle(boldStyle);

            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue("Dữ liệu phân tích dựa trên lịch sử bán 30 ngày gần nhất lúc " + LocalDateTime.now().format(DATE_TIME_FMT));

            String[] headers = {"STT", "Mã SKU", "Tên sản phẩm", "Tồn khả dụng", "ĐVT", "Đã bán 30 ngày", "Tốc độ bán TB/ngày", "Số ngày còn lại", "Mức độ rủi ro", "Đề xuất nhập thêm", "Dự toán kinh phí"};
            Row headRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            int stt = 1;
            for (InventoryForecastDto f : forecastList) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(stt++);
                r.createCell(1).setCellValue(f.getProductSku());
                r.createCell(2).setCellValue(f.getProductName());
                r.createCell(3).setCellValue(f.getAvailableStock());
                r.createCell(4).setCellValue(f.getBaseUnit());
                r.createCell(5).setCellValue(f.getTotalSoldLast30Days());
                r.createCell(6).setCellValue(f.getAvgDailySales());
                r.createCell(7).setCellValue(f.getDaysOfStockRemaining() == 0 ? "HẾT HÀNG" : f.getDaysOfStockRemaining() + " ngày");
                r.createCell(8).setCellValue(f.getRiskLevelLabel());
                r.createCell(9).setCellValue(f.getSuggestedRestockQty() + " " + f.getBaseUnit());

                Cell costCell = r.createCell(10);
                costCell.setCellValue(f.getEstimatedRestockCost() != null ? f.getEstimatedRestockCost().doubleValue() : 0);
                costCell.setCellStyle(currencyStyle);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Tạo file Excel Báo Cáo Tuổi Nợ Đại Lý (Debt Aging 30/60/90)
     */
    public byte[] generateDebtAgingReport() throws IOException {
        List<CustomerDebtAgingDto> debtList = orderService.getDebtAgingReport(null);

        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.setCompressTempFiles(true);

            CellStyle headerStyle = createHeaderStyle(wb, IndexedColors.DARK_TEAL.getIndex());
            CellStyle currencyStyle = createCurrencyStyle(wb);
            CellStyle boldStyle = createBoldStyle(wb);

            SXSSFSheet sheet = wb.createSheet("Tuổi nợ Đại lý");
            sheet.trackAllColumnsForAutoSizing();

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO PHÂN TÍCH TUỔI NỢ & RỦI RO CÔNG NỢ ĐẠI LÝ");
            titleCell.setCellStyle(boldStyle);

            String[] headers = {"STT", "Tên khách hàng / Đại lý", "Số điện thoại", "Hạn mức tín dụng", "Tổng dư nợ", "Trong hạn (0-30 ngày)", "Quá hạn (31-60 ngày)", "Quá hạn (61-90 ngày)", "Nợ xấu (>90 ngày)", "Ngày trễ hẹn lớn nhất"};
            Row headRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 3;
            int stt = 1;
            for (CustomerDebtAgingDto d : debtList) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(stt++);
                r.createCell(1).setCellValue(d.getCustomerName());
                r.createCell(2).setCellValue(d.getCustomerPhone() != null ? d.getCustomerPhone() : "—");

                Cell c1 = r.createCell(3);
                c1.setCellValue(d.getCreditLimit() != null ? d.getCreditLimit().doubleValue() : 0);
                c1.setCellStyle(currencyStyle);

                Cell c2 = r.createCell(4);
                c2.setCellValue(d.getCurrentDebt() != null ? d.getCurrentDebt().doubleValue() : 0);
                c2.setCellStyle(currencyStyle);

                Cell c3 = r.createCell(5);
                c3.setCellValue(d.getCurrentDue() != null ? d.getCurrentDue().doubleValue() : 0);
                c3.setCellStyle(currencyStyle);

                Cell c4 = r.createCell(6);
                c4.setCellValue(d.getOverdue31To60() != null ? d.getOverdue31To60().doubleValue() : 0);
                c4.setCellStyle(currencyStyle);

                Cell c5 = r.createCell(7);
                c5.setCellValue(d.getOverdue61To90() != null ? d.getOverdue61To90().doubleValue() : 0);
                c5.setCellStyle(currencyStyle);

                Cell c6 = r.createCell(8);
                c6.setCellValue(d.getBadDebtOver90() != null ? d.getBadDebtOver90().doubleValue() : 0);
                c6.setCellStyle(currencyStyle);

                r.createCell(9).setCellValue(d.getMaxOverdueDays() != null ? d.getMaxOverdueDays() : 0);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // HELPER STYLES
    private CellStyle createHeaderStyle(Workbook wb, short bgColor) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(bgColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0 \"VNĐ\""));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createCenterStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        style.setFont(font);
        return style;
    }

    private String translatePaymentStatus(String st) {
        if ("PAID".equals(st)) return "Đã thanh toán";
        if ("REFUNDED".equals(st)) return "Đã hoàn tiền";
        return "Chưa thanh toán";
    }

    private String translateOrderStatus(String st) {
        if ("PENDING".equals(st)) return "Chờ duyệt";
        if ("CONFIRMED".equals(st)) return "Đã xác nhận";
        if ("SHIPPING".equals(st)) return "Đang giao hàng";
        if ("COMPLETED".equals(st)) return "Hoàn thành";
        if ("CANCELLED".equals(st)) return "Đã hủy";
        return st;
    }
}
