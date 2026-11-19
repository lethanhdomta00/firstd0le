package com.erp.copilot.service;

import com.erp.order.dto.CustomerDebtAgingDto;
import com.erp.order.model.Order;
import com.erp.order.repository.OrderRepository;
import com.erp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dịch vụ sinh file CSV chuẩn UTF-8 có BOM (\uFEFF) giúp mở trực tiếp bằng Excel không bị lỗi font tiếng Việt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvReportGeneratorService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateOrdersCsv(LocalDateTime fromDate, LocalDateTime toDate) throws IOException {
        List<Order> orders;
        if (fromDate != null && toDate != null) {
            orders = orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDate, toDate);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // Ghi UTF-8 BOM
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            writer.println("STT,Mã đơn hàng,Khách hàng,Số điện thoại,Địa chỉ nhận,Tổng tiền (VNĐ),Thanh toán,Trạng thái,ĐVVC,Mã vận đơn,Ngày tạo");

            int stt = 1;
            for (Order o : orders) {
                writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.0f,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        stt++,
                        escapeCsv(o.getOrderCode()),
                        escapeCsv(o.getCustomerName()),
                        escapeCsv(o.getCustomerPhone()),
                        escapeCsv(o.getShippingAddress()),
                        o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0.0,
                        escapeCsv(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "UNPAID"),
                        escapeCsv(o.getStatus() != null ? o.getStatus().name() : "PENDING"),
                        escapeCsv(o.getShippingCarrier()),
                        escapeCsv(o.getTrackingCode()),
                        o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_TIME_FMT) : ""
                );
            }
            writer.flush();
            return out.toByteArray();
        }
    }

    public byte[] generateDebtOverdueCsv(int minOverdueDays) throws IOException {
        List<CustomerDebtAgingDto> debts = orderService.getDebtAgingReport(null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            writer.println("STT,Khách hàng / Đại lý,Số điện thoại,Hạn mức nợ (VNĐ),Tổng nợ hiện tại (VNĐ),Trong hạn 0-30 ngày,Quá hạn 31-60 ngày,Quá hạn 61-90 ngày,Nợ xấu >90 ngày,Ngày trễ hẹn lớn nhất");

            int stt = 1;
            for (CustomerDebtAgingDto d : debts) {
                writer.printf("%d,\"%s\",\"%s\",%.0f,%.0f,%.0f,%.0f,%.0f,%.0f,%d%n",
                        stt++,
                        escapeCsv(d.getCustomerName()),
                        escapeCsv(d.getCustomerPhone()),
                        d.getCreditLimit() != null ? d.getCreditLimit().doubleValue() : 0.0,
                        d.getCurrentDebt() != null ? d.getCurrentDebt().doubleValue() : 0.0,
                        d.getCurrentDue() != null ? d.getCurrentDue().doubleValue() : 0.0,
                        d.getOverdue31To60() != null ? d.getOverdue31To60().doubleValue() : 0.0,
                        d.getOverdue61To90() != null ? d.getOverdue61To90().doubleValue() : 0.0,
                        d.getBadDebtOver90() != null ? d.getBadDebtOver90().doubleValue() : 0.0,
                        d.getMaxOverdueDays() != null ? d.getMaxOverdueDays() : 0
                );
            }
            writer.flush();
            return out.toByteArray();
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "—";
        return val.replace("\"", "\"\"");
    }
}
