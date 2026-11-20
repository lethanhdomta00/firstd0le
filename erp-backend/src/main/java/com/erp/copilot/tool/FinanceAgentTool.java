package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceAgentTool implements AgentTool {

    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "draftExpenseVoucher";
    }

    @Override
    public String getDescription() {
        return "Soạn thảo phiếu chi tiền mặt hoặc ủy nhiệm chi ngân hàng dự thảo (tiền thuê mặt bằng, điện nước, vận chuyển, tiếp khách) kèm thẻ phê duyệt có thẩm quyền.";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> amount = new HashMap<>();
        amount.put("type", "number");
        amount.put("description", "Số tiền chi (VNĐ)");
        properties.put("amount", amount);

        Map<String, Object> payeeName = new HashMap<>();
        payeeName.put("type", "string");
        payeeName.put("description", "Họ tên người nhận tiền hoặc đơn vị thụ hưởng");
        properties.put("payeeName", payeeName);

        Map<String, Object> category = new HashMap<>();
        category.put("type", "string");
        category.put("description", "Danh mục chi: TIEN_THUE_MAT_BANG, DIEN_NUOC, VAN_CHUYEN, TIEP_KHACH, KHAC");
        properties.put("category", category);

        Map<String, Object> note = new HashMap<>();
        note.put("type", "string");
        note.put("description", "Lý do chi chi tiết");
        properties.put("note", note);

        schema.put("properties", properties);
        schema.put("required", Arrays.asList("amount", "payeeName"));
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        BigDecimal amount = BigDecimal.ZERO;
        if (args.get("amount") != null) {
            try {
                amount = new BigDecimal(String.valueOf(args.get("amount")).replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {
            }
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ToolExecutionResult.failure(getToolName(), "Số tiền chi phải lớn hơn 0 đồng!");
        }

        String payeeName = args.get("payeeName") != null ? String.valueOf(args.get("payeeName")).trim() : "Đối tác";
        String category = args.get("category") != null ? String.valueOf(args.get("category")).trim() : "CHI_HOAT_DONG";
        String note = args.get("note") != null ? String.valueOf(args.get("note")).trim() : "Chi phí vận hành theo đề xuất";

        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);
        String amountFormatted = currFmt.format(amount);

        String voucherCode = "PC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        Map<String, Object> widgetData = new LinkedHashMap<>();
        widgetData.put("actionType", "APPROVE_EXPENSE");
        widgetData.put("title", "Phiếu Chi Quỹ Dự Thảo: " + voucherCode);
        widgetData.put("voucherCode", voucherCode);
        widgetData.put("payeeName", payeeName);
        widgetData.put("amountFormatted", amountFormatted);
        widgetData.put("category", category);
        widgetData.put("note", note);
        widgetData.put("createdBy", currentUsername);
        widgetData.put("confirmButtonText", "🔴 Phê Duyệt Xuất Quỹ Ngay");
        widgetData.put("cancelButtonText", "Hủy phiếu");
        widgetData.put("requiredRoles", List.of("ADMIN", "MANAGER", "ACCOUNTANT"));
        widgetData.put("permissionNote", "Chỉ Quản trị viên (ADMIN), Quản lý (MANAGER) hoặc Kế toán (ACCOUNTANT) mới có quyền phê duyệt xuất quỹ");

        UiWidgetDto widget = UiWidgetDto.builder()
                .type("ACTION_CONFIRMATION_CARD")
                .data(widgetData)
                .build();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📝 **ĐÃ SOẠN THẢO PHIẾU CHI DỰ THẢO (%s)**:\n", voucherCode));
        sb.append(String.format("- **Đơn vị / Người thụ hưởng**: **%s**\n", payeeName));
        sb.append(String.format("- **Số tiền đề xuất**: **%s**\n", amountFormatted));
        sb.append(String.format("- **Khoản mục chi**: `%s`\n", category));
        sb.append(String.format("- **Nội dung / Diễn giải**: %s\n", note));
        sb.append(String.format("- **Người lập phiếu**: @%s\n", currentUsername != null ? currentUsername : "system"));
        sb.append("\n⚠️ _Vui lòng kiểm tra lại thông tin và bấm **'Phê Duyệt Xuất Quỹ Ngay'** bên dưới để hoàn tất hạch toán vào Sổ quỹ!_");

        Map<String, Object> resData = new LinkedHashMap<>();
        resData.put("voucherCode", voucherCode);
        resData.put("amount", amount);
        resData.put("payeeName", payeeName);

        return ToolExecutionResult.success(getToolName(), sb.toString(), resData, widget);
    }
}
