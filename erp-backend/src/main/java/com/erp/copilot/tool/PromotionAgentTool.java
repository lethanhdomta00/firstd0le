package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.model.Customer;
import com.erp.promotion.dto.VoucherDto;
import com.erp.promotion.model.DiscountType;
import com.erp.promotion.model.PromotionVoucher;
import com.erp.promotion.repository.PromotionVoucherRepository;
import com.erp.promotion.service.LoyaltyService;
import com.erp.promotion.service.PromotionVoucherService;
import com.erp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionAgentTool implements AgentTool {

    private final PromotionVoucherService voucherService;
    private final PromotionVoucherRepository voucherRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyService loyaltyService;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "queryPromotionsAndLoyalty";
    }

    @Override
    public String getDescription() {
        return "Tra cứu mã Voucher/Coupon đang kích hoạt, tra cứu điểm tích lũy & hạng hội viên khách hàng, gợi ý mã giảm giá tối ưu.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> queryType = new HashMap<>();
        queryType.put("type", "string");
        queryType.put("description", "Loại tra cứu: VOUCHERS (Danh sách voucher đang chạy), CUSTOMER_LOYALTY (Điểm tích lũy khách hàng), BEST_VOUCHER (Gợi ý mã)");
        properties.put("queryType", queryType);

        Map<String, Object> keyword = new HashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "Tên hoặc số điện thoại khách hàng (nếu tra cứu điểm)");
        properties.put("keyword", keyword);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String queryType = args.get("queryType") != null ? String.valueOf(args.get("queryType")).trim().toUpperCase() : "VOUCHERS";
        String keyword = args.get("keyword") != null ? String.valueOf(args.get("keyword")).trim() : "";
        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);

        StringBuilder sb = new StringBuilder();

        // 1. TRA CỨU ĐIỂM HỘI VIÊN
        if ("CUSTOMER_LOYALTY".equals(queryType) || (!keyword.isEmpty() && !"VOUCHERS".equals(queryType))) {
            List<Customer> customers;
            if (!keyword.isEmpty()) {
                customers = customerRepository.searchCustomers(keyword, org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
            } else {
                customers = customerRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 5)).getContent();
            }

            if (customers.isEmpty()) {
                return ToolExecutionResult.builder()
                        .toolName(getToolName())
                        .success(true)
                        .message("Không tìm thấy thông tin hội viên nào khớp với từ khóa: \"" + keyword + "\"")
                        .data(Map.of("found", false))
                        .build();
            }

            sb.append("👑 **THÔNG TIN HỘI VIÊN & ĐIỂM TÍCH LŨY (LOYALTY)**:\n\n");
            for (Customer c : customers) {
                int points = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
                BigDecimal pointValue = BigDecimal.valueOf(points).multiply(LoyaltyService.POINT_CONVERSION_RATE);
                sb.append(String.format("- **%s** (SĐT: %s)\n", c.getName(), c.getPhone()));
                sb.append(String.format("  • Hạng thẻ: **%s** (%s) | Hệ số tích điểm: `%s`\n",
                        c.getMemberTier() != null ? c.getMemberTier().getDisplayName() : "Đồng",
                        c.getMemberTier() != null ? c.getMemberTier().name() : "BRONZE",
                        c.getMemberTier() != null ? c.getMemberTier().getPointMultiplier() + "x" : "1.0x"));
                sb.append(String.format("  • Điểm khả dụng: **%d điểm** (&asymp; `%s` khấu trừ đơn hàng)\n", points, currFmt.format(pointValue)));
                sb.append(String.format("  • Tổng chi tiêu tích lũy: `%s`\n\n", currFmt.format(c.getTotalSpent() != null ? c.getTotalSpent() : BigDecimal.ZERO)));
            }

            return ToolExecutionResult.builder()
                    .toolName(getToolName())
                    .success(true)
                    .message(sb.toString().trim())
                    .data(Map.of("customerCount", customers.size()))
                    .build();
        }

        // 2. TRA CỨU DANH SÁCH VOUCHER ĐANG CHẠY (MẶC ĐỊNH)
        List<VoucherDto> activeVouchers = voucherService.getActiveVouchers();
        if (activeVouchers.isEmpty()) {
            return ToolExecutionResult.builder()
                    .toolName(getToolName())
                    .success(true)
                    .message("Hiện tại hệ thống không có mã Voucher/Coupon nào đang trong thời gian áp dụng.")
                    .data(Map.of("activeCount", 0))
                    .build();
        }

        sb.append(String.format("🎟️ **DANH SÁCH MÃ VOUCHER ĐANG KÍCH HOẠT (%d MÃ)**:\n\n", activeVouchers.size()));
        for (VoucherDto v : activeVouchers) {
            String discountStr = v.getDiscountType() == DiscountType.PERCENTAGE
                    ? "Giảm " + v.getDiscountValue() + "%" + (v.getMaxDiscountAmount() != null ? " (Tối đa " + currFmt.format(v.getMaxDiscountAmount()) + ")" : "")
                    : "Giảm " + currFmt.format(v.getDiscountValue());

            sb.append(String.format("- Mã: **`%s`** &mdash; *%s*\n", v.getCode(), v.getName()));
            sb.append(String.format("  • Mức giảm: **%s** | Đơn tối thiểu: `%s`\n", discountStr, currFmt.format(v.getMinOrderValue() != null ? v.getMinOrderValue() : BigDecimal.ZERO)));
            sb.append(String.format("  • Lượt dùng: `%d/%s` (Tối đa %d lần/khách)\n\n",
                    v.getUsedCount() != null ? v.getUsedCount() : 0,
                    v.getMaxUsage() != null ? v.getMaxUsage() : "∞",
                    v.getMaxUsagePerCustomer() != null ? v.getMaxUsagePerCustomer() : 1));
        }
        sb.append("💡 Mày có thể nhập trực tiếp các mã này vào ô Voucher khi Tạo đơn hàng để được chiết khấu ngay!");

        return ToolExecutionResult.builder()
                .toolName(getToolName())
                .success(true)
                .message(sb.toString().trim())
                .data(Map.of("activeCount", activeVouchers.size()))
                .build();
    }
}
