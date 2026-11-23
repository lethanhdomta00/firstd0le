package com.erp.copilot.tool;

import com.erp.copilot.dto.ToolExecutionResult;
import com.erp.copilot.dto.UiWidgetDto;
import com.erp.procurement.model.PurchaseOrder;
import com.erp.procurement.model.PurchaseOrderStatus;
import com.erp.procurement.model.Supplier;
import com.erp.procurement.repository.GoodsReceiptNoteRepository;
import com.erp.procurement.repository.PurchaseOrderRepository;
import com.erp.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcurementAgentTool implements AgentTool {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final Locale vnLocale = new Locale("vi", "VN");

    @Override
    public String getToolName() {
        return "queryProcurementAndSuppliers";
    }

    @Override
    public String getDescription() {
        return "Tra cứu tình hình mua hàng PO, danh sách nhà cung cấp, công nợ phải trả NCC (AP), và phiếu nhập kho kiểm nhận (GRN).";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> queryType = new HashMap<>();
        queryType.put("type", "string");
        queryType.put("description", "Loại tra cứu: PENDING_PO (Đơn PO chờ duyệt), SUPPLIERS (Nhà cung cấp & Nợ AP), OVERVIEW (Tổng quan mua hàng)");
        properties.put("queryType", queryType);

        schema.put("properties", properties);
        return schema;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> args, String currentUsername) {
        String queryType = args.get("queryType") != null ? String.valueOf(args.get("queryType")).trim().toUpperCase() : "OVERVIEW";
        NumberFormat currFmt = NumberFormat.getCurrencyInstance(vnLocale);

        StringBuilder sb = new StringBuilder();

        if ("PENDING_PO".equals(queryType)) {
            var pendingOrders = purchaseOrderRepository.searchPurchaseOrders(null, PurchaseOrderStatus.PENDING_APPROVAL, null, null, PageRequest.of(0, 5)).getContent();
            if (pendingOrders.isEmpty()) {
                return ToolExecutionResult.builder()
                        .toolName(getToolName())
                        .success(true)
                        .message("Hiện tại không có đơn đặt mua hàng (PO) nào đang chờ phê duyệt. Mọi đơn mua đều đã được xử lý!")
                        .data(Map.of("pendingCount", 0))
                        .build();
            }

            sb.append("📋 **DANH SÁCH ĐƠN ĐẶT MUA (PO) ĐANG CHỜ PHÊ DUYỆT**:\n\n");
            for (PurchaseOrder po : pendingOrders) {
                sb.append(String.format("- **%s** | NCC: **%s** | Tổng tiền: `%s` | Ngày tạo: %s\n",
                        po.getPoCode(),
                        po.getSupplierName(),
                        currFmt.format(po.getTotalAmount()),
                        po.getOrderDate() != null ? po.getOrderDate() : "N/A"));
            }
            sb.append("\n👉 Quản lý hoặc Giám đốc có thể vào phân hệ **Mua hàng & SRM** để kiểm tra và duyệt đơn.");

            return ToolExecutionResult.builder()
                    .toolName(getToolName())
                    .success(true)
                    .message(sb.toString())
                    .data(Map.of("pendingCount", pendingOrders.size()))
                    .build();
        }

        if ("SUPPLIERS".equals(queryType)) {
            List<Supplier> suppliers = supplierRepository.findTop10ByOrderByCurrentDebtDesc().stream()
                    .filter(s -> s.getCurrentDebt() != null && s.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            if (suppliers.isEmpty()) {
                return ToolExecutionResult.builder()
                        .toolName(getToolName())
                        .success(true)
                        .message("Hệ thống hiện không có khoản công nợ phải trả (AP) nào đối với các Nhà cung cấp.")
                        .data(Map.of("totalSuppliersWithDebt", 0))
                        .build();
            }

            BigDecimal totalAp = suppliers.stream()
                    .map(s -> s.getCurrentDebt() != null ? s.getCurrentDebt() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            sb.append(String.format("🏭 **DANH SÁCH NHÀ CUNG CẤP CÓ CÔNG NỢ PHẢI TRẢ (AP)**\n*Tổng nợ NCC:* `%s`\n\n", currFmt.format(totalAp)));
            for (Supplier sup : suppliers) {
                sb.append(String.format("- **%s** (SĐT: %s): Nợ `%s` / Hạn mức `%s`\n",
                        sup.getName(),
                        sup.getPhone() != null ? sup.getPhone() : "N/A",
                        currFmt.format(sup.getCurrentDebt()),
                        currFmt.format(sup.getCreditLimit())));
            }

            return ToolExecutionResult.builder()
                    .toolName(getToolName())
                    .success(true)
                    .message(sb.toString())
                    .data(Map.of("totalAp", totalAp, "count", suppliers.size()))
                    .build();
        }

        // Mặc định: OVERVIEW
        long totalSuppliers = supplierRepository.count();
        long activeSuppliers = supplierRepository.countByStatus("ACTIVE");
        long pendingPo = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        long approvedPo = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.APPROVED);
        BigDecimal totalDebt = supplierRepository.sumTotalAccountsPayable();
        if (totalDebt == null) totalDebt = BigDecimal.ZERO;

        sb.append("📦 **BÁO CÁO NHANH PHÂN HỆ MUA HÀNG & QUẢN TRỊ NHÀ CUNG CẤP (SRM)**:\n\n");
        sb.append(String.format("1. 🏭 **Nhà cung cấp / Xưởng**: `%d` đối tác (Đang hoạt động: `%d`)\n", totalSuppliers, activeSuppliers));
        sb.append(String.format("2. 📋 **Đơn PO chờ duyệt**: `%d` đơn | **Sẵn sàng nhập kho**: `%d` đơn\n", pendingPo, approvedPo));
        sb.append(String.format("3. 💳 **Công nợ phải trả NCC (AP)**: `%s`\n", currFmt.format(totalDebt)));
        sb.append("4. ⚖️ **Thuật toán Định giá**: Tự động hạch toán Giá vốn Bình quân Gia quyền (Moving Weighted Average Cost) ngay khi thủ kho kiểm nhận GRN.");

        return ToolExecutionResult.builder()
                .toolName(getToolName())
                .success(true)
                .message(sb.toString())
                .data(Map.of(
                        "totalSuppliers", totalSuppliers,
                        "pendingPo", pendingPo,
                        "approvedPo", approvedPo,
                        "totalDebt", totalDebt
                ))
                .build();
    }
}
