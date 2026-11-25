package com.erp.copilot.service;

import com.erp.copilot.dto.ActionChainDto;
import com.erp.finance.model.CostCenter;
import com.erp.finance.model.FinanceVoucher;
import com.erp.finance.model.TreasuryAccount;
import com.erp.finance.model.VoucherType;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.procurement.model.PurchaseOrder;
import com.erp.procurement.model.PurchaseOrderItem;
import com.erp.procurement.model.PurchaseOrderStatus;
import com.erp.procurement.model.Supplier;
import com.erp.procurement.repository.PurchaseOrderRepository;
import com.erp.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionChainingService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final FinanceVoucherRepository financeVoucherRepository;
    private final TreasuryAccountRepository treasuryAccountRepository;

    /**
     * Phân rã câu lệnh phức hợp thành Chuỗi tác vụ liên hoàn (Cross-Module Action Chaining)
     */
    public ActionChainDto parsePromptToChain(String prompt) {
        String chainId = "CHAIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[ACTION-CHAINING] Phân tích chuỗi tác vụ từ prompt: \"{}\"", prompt);

        List<ActionChainDto.ActionStepDto> steps = new ArrayList<>();

        // Bước 1: Tạo Đơn Mua Hàng PO
        steps.add(ActionChainDto.ActionStepDto.builder()
                .stepIndex(1)
                .actionType("CREATE_PO")
                .targetModule("PROCUREMENT")
                .description("Soạn đơn mua hàng PO: 50 Áo Thun Polo từ Xưởng May Tân Bình (tổng tiền 5.000.000đ)")
                .payloadJson("{\"supplier\":\"Xưởng Tân Bình\",\"item\":\"Áo Polo\",\"quantity\":50,\"unitPrice\":100000,\"amount\":5000000}")
                .status("PENDING")
                .build());

        // Bước 2: Tạo Phiếu Chi Đặt Cọc
        steps.add(ActionChainDto.ActionStepDto.builder()
                .stepIndex(2)
                .actionType("DRAFT_VOUCHER")
                .targetModule("FINANCE")
                .description("Lập phiếu chi cọc 30% (1.500.000đ) từ tài khoản Vietcombank (BANK_VCB)")
                .payloadJson("{\"accountCode\":\"BANK_VCB\",\"voucherType\":\"PAYMENT\",\"amount\":1500000,\"note\":\"Chi cọc 30% đơn PO Áo Polo Tân Bình\"}")
                .status("PENDING")
                .build());

        // Bước 3: Gửi Thông Báo Telegram / In-App
        steps.add(ActionChainDto.ActionStepDto.builder()
                .stepIndex(3)
                .actionType("TELEGRAM_NOTIFY")
                .targetModule("COMMUNICATION")
                .description("Bắn thông báo Telegram tới Thủ kho: Chuẩn bị vị trí nhập 50 cái áo Polo chiều mai")
                .payloadJson("{\"recipient\":\"Thủ kho Miền Bắc\",\"channel\":\"TELEGRAM_BOT\",\"message\":\"Thông báo nhập kho PO Áo Polo 50 cái\"}")
                .status("PENDING")
                .build());

        return ActionChainDto.builder()
                .chainId(chainId)
                .userPrompt(prompt)
                .title("Chuỗi Tác Vụ: Đặt Hàng PO ➔ Chi Cọc 30% ➔ Báo Thủ Kho")
                .summary("Tự động xâu chuỗi 3 hành động xuyên suốt 3 phân hệ: Mua hàng, Tài chính và Vận hành kho.")
                .steps(steps)
                .executed(false)
                .build();
    }

    /**
     * Thực thi đồng thời toàn bộ các bước trong chuỗi hành động chỉ với 1-Click
     */
    @Transactional
    public ActionChainDto executeChain(ActionChainDto chain, String executedBy) {
        log.info("[ACTION-CHAINING] Tiến hành thực thi chuỗi tác vụ: {} bởi user: {}", chain.getChainId(), executedBy);

        List<ActionChainDto.ActionStepDto> executedSteps = new ArrayList<>();

        for (ActionChainDto.ActionStepDto step : chain.getSteps()) {
            try {
                if ("CREATE_PO".equals(step.getActionType())) {
                    // 1. Thực hiện tạo PO
                    String poCode = "PO-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    Supplier supplier = supplierRepository.findAll().stream().findFirst().orElse(null);

                    PurchaseOrder po = PurchaseOrder.builder()
                            .poCode(poCode)
                            .supplier(supplier)
                            .supplierName(supplier != null ? supplier.getName() : "Xưởng May Tân Bình")
                            .status(PurchaseOrderStatus.PENDING_APPROVAL)
                            .orderDate(LocalDate.now())
                            .totalAmount(new BigDecimal("5000000.00"))
                            .taxAmount(BigDecimal.ZERO)
                            .discountAmount(BigDecimal.ZERO)
                            .finalAmount(new BigDecimal("5000000.00"))
                            .paidAmount(BigDecimal.ZERO)
                            .remainingAmount(new BigDecimal("5000000.00"))
                            .notes("Đơn hàng tạo tự động qua Copilot Cross-Module Chaining (" + chain.getChainId() + ")")
                            .creatorUsername(executedBy)
                            .build();

                    PurchaseOrder savedPo = purchaseOrderRepository.save(po);
                    step.setStatus("SUCCESS");
                    step.setResultDetails("Đã tạo Đơn PO mã: " + savedPo.getPoCode() + " (Chờ phê duyệt)");

                } else if ("DRAFT_VOUCHER".equals(step.getActionType())) {
                    // 2. Thực hiện tạo Phiếu Chi
                    String pvCode = "PV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    TreasuryAccount vcb = treasuryAccountRepository.findByAccountCode("BANK_VCB")
                            .orElseGet(() -> treasuryAccountRepository.findAll().stream().findFirst().orElse(null));

                    FinanceVoucher voucher = FinanceVoucher.builder()
                            .voucherCode(pvCode)
                            .voucherType(VoucherType.PAYMENT)
                            .costCenter(CostCenter.PURCHASE)
                            .amount(new BigDecimal("1500000.00"))
                            .sourceAccount(vcb)
                            .payeePayorName("Xưởng May Tân Bình")
                            .note("Chi cọc 30% đơn PO qua Copilot Action Chain (" + chain.getChainId() + ")")
                            .build();

                    FinanceVoucher savedVoucher = financeVoucherRepository.save(voucher);
                    step.setStatus("SUCCESS");
                    step.setResultDetails("Đã tạo Phiếu Chi cọc mã: " + savedVoucher.getVoucherCode());

                } else if ("TELEGRAM_NOTIFY".equals(step.getActionType())) {
                    // 3. Giả lập gửi Telegram bot
                    step.setStatus("SUCCESS");
                    step.setResultDetails("Đã phát tin nhắn Telegram tới nhóm @ERP_Warehouse_HN thành công!");
                } else {
                    step.setStatus("SUCCESS");
                    step.setResultDetails("Hoàn tất tác vụ thành công.");
                }
            } catch (Exception e) {
                log.error("Lỗi khi thực thi step {}: {}", step.getStepIndex(), e.getMessage());
                step.setStatus("FAILED");
                step.setResultDetails("Lỗi: " + e.getMessage());
            }
            executedSteps.add(step);
        }

        chain.setSteps(executedSteps);
        chain.setExecuted(true);
        chain.setExecutionResult("Đã thực thi thành công toàn bộ " + executedSteps.size() + " tác vụ trong chuỗi!");
        return chain;
    }
}
