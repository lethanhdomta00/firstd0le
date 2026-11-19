package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.model.Customer;
import com.erp.order.dto.CustomerDebtAgingDto;
import com.erp.order.service.OrderService;
import com.erp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtAgentTool implements AgentTool {

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "getCustomerDebtSummary";
    }

    @Override
    public String getDescription() {
        return "Tra cứu tổng công nợ, phân tích tuổi nợ đại lý (trong hạn, quá hạn 30-90 ngày, nợ xấu) và hỗ trợ kích hoạt khóa tín dụng nợ rủi ro.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> keyword = new HashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "Tên khách hàng, số điện thoại hoặc để trống nếu muốn quét khách hàng nợ nhiều nhất / nợ quá hạn lâu nhất");
        properties.put("keyword", keyword);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String keyword = args.get("keyword") != null ? String.valueOf(args.get("keyword")).trim() : "";

        List<CustomerDebtAgingDto> agingList = orderService.getDebtAgingReport(keyword);
        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);

        if (agingList.isEmpty()) {
            Map<String, Object> data = Map.of("count", 0);
            return ToolExecutionResult.success(getToolName(), "🎉 Không tìm thấy công nợ quá hạn hoặc chưa thanh toán nào phù hợp với yêu cầu!", data, null);
        }

        // Lấy khách hàng có rủi ro cao nhất (nợ xấu nhiều nhất hoặc ngày quá hạn lâu nhất)
        CustomerDebtAgingDto topRisk = agingList.stream()
                .max(Comparator.comparingInt(c -> c.getMaxOverdueDays() != null ? c.getMaxOverdueDays() : 0))
                .orElse(agingList.get(0));

        BigDecimal totalOutstandingDebt = BigDecimal.ZERO;
        BigDecimal totalBadDebt = BigDecimal.ZERO;

        for (CustomerDebtAgingDto c : agingList) {
            if (c.getCurrentDebt() != null) totalOutstandingDebt = totalOutstandingDebt.add(c.getCurrentDebt());
            if (c.getBadDebtOver90() != null) totalBadDebt = totalBadDebt.add(c.getBadDebtOver90());
        }

        StringBuilder sb = new StringBuilder();
        if (keyword.isEmpty()) {
            sb.append(String.format("⚠️ **TỔNG HỢP CÔNG NỢ ĐẠI LÝ RỦI RO** (Tổng nợ: **%s** | Nợ xấu >90 ngày: **%s**):\n\n",
                    currFmt.format(totalOutstandingDebt), currFmt.format(totalBadDebt)));

            int limit = Math.min(agingList.size(), 5);
            for (int i = 0; i < limit; i++) {
                CustomerDebtAgingDto c = agingList.get(i);
                int overdue = c.getMaxOverdueDays() != null ? c.getMaxOverdueDays() : 0;
                String status = overdue > 90 ? "🔴 NỢ XẤU BÁO ĐỘNG" : (overdue > 30 ? "🟡 Quá hạn nhẹ" : "🟢 Trong hạn");

                sb.append(String.format("%d. **%s** (`%s`) — Nợ: **%s** | Quá hạn: **%d ngày** (%s)\n",
                        i + 1, c.getCustomerName(), c.getCustomerPhone(),
                        currFmt.format(c.getCurrentDebt() != null ? c.getCurrentDebt() : BigDecimal.ZERO),
                        overdue, status));
            }

            if (agingList.size() > limit) {
                sb.append(String.format("\n_...và còn %d khách hàng khác đang nợ tồn đọng._\n", agingList.size() - limit));
            }
        } else {
            CustomerDebtAgingDto c = agingList.get(0);
            sb.append(String.format("📋 **HỒ SƠ CÔNG NỢ KHÁCH HÀNG: %s** (`%s`):\n", c.getCustomerName(), c.getCustomerPhone()));
            sb.append(String.format("- **Hạn mức tín dụng**: %s (Hạng giá: %s)\n", currFmt.format(c.getCreditLimit()), c.getPriceTier()));
            sb.append(String.format("- **Tổng nợ hiện tại**: **%s**\n", currFmt.format(c.getCurrentDebt())));
            sb.append(String.format("- **Trong hạn (0-30 ngày)**: %s\n", currFmt.format(c.getCurrentDue() != null ? c.getCurrentDue() : BigDecimal.ZERO)));
            sb.append(String.format("- **Quá hạn 31-60 ngày**: %s\n", currFmt.format(c.getOverdue31To60() != null ? c.getOverdue31To60() : BigDecimal.ZERO)));
            sb.append(String.format("- **Cảnh báo đỏ 61-90 ngày**: %s\n", currFmt.format(c.getOverdue61To90() != null ? c.getOverdue61To90() : BigDecimal.ZERO)));
            sb.append(String.format("- **Nợ xấu khó đòi (>90 ngày)**: **%s**\n", currFmt.format(c.getBadDebtOver90() != null ? c.getBadDebtOver90() : BigDecimal.ZERO)));
            sb.append(String.format("- **Số ngày trễ hẹn lớn nhất**: **%d ngày**\n", c.getMaxOverdueDays() != null ? c.getMaxOverdueDays() : 0));
            sb.append(String.format("- **Trạng thái tín dụng**: %s\n", Boolean.TRUE.equals(c.getIsCreditLocked()) ? "🔒 ĐÃ BỊ KHÓA XUẤT HÀNG" : "🔓 Bình thường"));
        }

        // Sinh Widget ACTION_CONFIRMATION_CARD nếu có khách nợ quá hạn nguy hiểm (> 60 ngày) và chưa bị khóa
        UiWidgetDto widget = null;
        if (topRisk != null && (topRisk.getMaxOverdueDays() != null && topRisk.getMaxOverdueDays() >= 30) && !Boolean.TRUE.equals(topRisk.getIsCreditLocked())) {
            Map<String, Object> widgetData = new LinkedHashMap<>();
            widgetData.put("actionType", "HOLD_CREDIT");
            widgetData.put("title", "Yêu cầu Khóa Nợ Đại lý Rủi ro");
            widgetData.put("targetId", topRisk.getCustomerId());
            widgetData.put("targetName", topRisk.getCustomerName());
            widgetData.put("targetPhone", topRisk.getCustomerPhone());
            widgetData.put("debtAmountFormatted", currFmt.format(topRisk.getCurrentDebt()));
            widgetData.put("overdueDays", topRisk.getMaxOverdueDays());
            widgetData.put("reason", "Nợ quá hạn " + topRisk.getMaxOverdueDays() + " ngày vượt ngưỡng kiểm soát rủi ro dòng tiền.");
            widgetData.put("confirmButtonText", "🔴 Khóa Xuất Hàng Đại Lý Này");
            widgetData.put("cancelButtonText", "Để sau");

            widget = UiWidgetDto.builder()
                    .type("ACTION_CONFIRMATION_CARD")
                    .data(widgetData)
                    .build();

            sb.append(String.format("\n🛡️ **ĐỀ XUẤT BẢO TOÀN DÒNG VỐN**: Đại lý **%s** đang có số ngày trễ hẹn **%d ngày**. Bạn có muốn tôi khóa hạn mức xuất hàng đại lý này không?",
                    topRisk.getCustomerName(), topRisk.getMaxOverdueDays()));
        }

        Map<String, Object> resData = new LinkedHashMap<>();
        resData.put("count", agingList.size());
        resData.put("totalDebt", totalOutstandingDebt);
        resData.put("totalBadDebt", totalBadDebt);

        return ToolExecutionResult.success(getToolName(), sb.toString(), resData, widget);
    }
}
