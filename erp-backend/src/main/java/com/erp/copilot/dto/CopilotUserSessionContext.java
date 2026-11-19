package com.erp.copilot.dto;

import com.erp.order.dto.CreateOrderRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lưu vết ngữ cảnh phiên làm việc của người dùng trong Copilot (Multi-turn Context Memory)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotUserSessionContext {

    // Thực thể Sản phẩm gần nhất vừa tra cứu / trao đổi
    private Long lastProductId;
    private String lastProductSku;
    private String lastProductName;
    private BigDecimal lastProductPrice;
    private Integer lastProductStock;

    // Thực thể Khách hàng gần nhất vừa tra cứu / trao đổi
    private Long lastCustomerId;
    private String lastCustomerName;
    private String lastCustomerPhone;
    private BigDecimal lastCustomerDebt;

    // Mã đơn hàng gần nhất vừa tra cứu / thao tác
    private String lastOrderCode;

    // Khoảng thời gian gần nhất vừa xem báo cáo (TODAY, THIS_WEEK, THIS_MONTH)
    private String lastTimeRange;

    // Bản dự thảo đơn hàng Conversational POS gần nhất
    private CreateOrderRequest pendingDraftOrder;

    // Thời điểm tương tác gần nhất (để quản lý timeout 30 phút)
    @Builder.Default
    private long lastActiveTimestamp = System.currentTimeMillis();
}
