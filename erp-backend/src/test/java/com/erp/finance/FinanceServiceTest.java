package com.erp.finance;

import com.erp.finance.dto.*;
import com.erp.finance.model.*;
import com.erp.finance.repository.BankStatementItemRepository;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.finance.service.BankReconciliationService;
import com.erp.finance.service.FinanceVoucherService;
import com.erp.finance.service.ProfitAndLossService;
import com.erp.finance.service.TreasuryService;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FinanceServiceTest {

    @Autowired
    private TreasuryService treasuryService;

    @Autowired
    private TreasuryAccountRepository treasuryAccountRepository;

    @Autowired
    private FinanceVoucherService financeVoucherService;

    @Autowired
    private FinanceVoucherRepository financeVoucherRepository;

    @Autowired
    private BankReconciliationService bankReconciliationService;

    @Autowired
    private BankStatementItemRepository bankStatementItemRepository;

    @Autowired
    private ProfitAndLossService profitAndLossService;

    @Autowired
    private OrderRepository orderRepository;

    private TreasuryAccount cashAccount;
    private TreasuryAccount bankAccount;

    @BeforeEach
    void setUp() {
        // Đảm bảo có sẵn tài khoản tiền mặt và ngân hàng
        cashAccount = treasuryAccountRepository.findByAccountCode("CASH-MAIN")
                .orElseGet(() -> treasuryAccountRepository.save(TreasuryAccount.builder()
                        .accountName("Quỹ tiền mặt văn phòng")
                        .accountCode("CASH-MAIN")
                        .accountType(TreasuryAccountType.CASH)
                        .currentBalance(new BigDecimal("50000000.00"))
                        .active(true)
                        .build()));

        bankAccount = treasuryAccountRepository.findByAccountCode("VCB-8888")
                .orElseGet(() -> treasuryAccountRepository.save(TreasuryAccount.builder()
                        .accountName("Vietcombank - TK Thu tiền bán hàng")
                        .accountCode("VCB-8888")
                        .accountNumber("0123456789")
                        .bankName("Vietcombank")
                        .accountType(TreasuryAccountType.BANK)
                        .currentBalance(new BigDecimal("200000000.00"))
                        .active(true)
                        .build()));
    }

    @Test
    @DisplayName("1. Quản lý Quỹ: Lấy danh sách tài khoản quỹ và tính tổng số dư an toàn")
    void testTreasuryAccountBalance() {
        List<TreasuryAccountDto> accounts = treasuryService.getAllActiveAccounts();
        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());

        BigDecimal total = treasuryAccountRepository.getTotalBalance();
        assertNotNull(total);
        assertTrue(total.compareTo(BigDecimal.ZERO) > 0, "Tổng số dư quỹ phải lớn hơn 0");
    }

    @Test
    @DisplayName("2. Chuyển tiền nội bộ (Internal Transfer): Chuyển 10M từ VCB sang Tiền mặt văn phòng chuẩn ACID")
    void testInternalTransfer() {
        BigDecimal transferAmount = new BigDecimal("10000000.00");
        BigDecimal cashBefore = cashAccount.getCurrentBalance();
        BigDecimal bankBefore = bankAccount.getCurrentBalance();

        InternalTransferRequest request = new InternalTransferRequest();
        request.setSourceAccountId(bankAccount.getId());
        request.setTargetAccountId(cashAccount.getId());
        request.setAmount(transferAmount);
        request.setNote("Rút quỹ Vietcombank nạp quỹ tiền mặt chi tiêu tuần");

        FinanceVoucherDto voucher = treasuryService.transferInternal(request, "admin");

        assertNotNull(voucher);
        assertEquals(VoucherType.INTERNAL_TRANSFER, voucher.getVoucherType());
        assertEquals(VoucherStatus.APPROVED, voucher.getStatus());

        // Kiểm tra số dư tài khoản đích tăng và tài khoản nguồn giảm
        TreasuryAccount updatedCash = treasuryAccountRepository.findById(cashAccount.getId()).orElseThrow();
        TreasuryAccount updatedBank = treasuryAccountRepository.findById(bankAccount.getId()).orElseThrow();

        assertEquals(0, cashBefore.add(transferAmount).compareTo(updatedCash.getCurrentBalance()));
        assertEquals(0, bankBefore.subtract(transferAmount).compareTo(updatedBank.getCurrentBalance()));
    }

    @Test
    @DisplayName("3. Hạch toán Phiếu Chi: Tạo phiếu chi tiền mặt bằng 5M, duyệt phiếu và trừ quỹ thành công")
    void testCreateAndApprovePaymentVoucher() {
        BigDecimal expenseAmount = new BigDecimal("5000000.00");
        BigDecimal cashBefore = cashAccount.getCurrentBalance();

        CreateFinanceVoucherRequest request = new CreateFinanceVoucherRequest();
        request.setVoucherType(VoucherType.PAYMENT);
        request.setAmount(expenseAmount);
        request.setSourceAccountId(cashAccount.getId());
        request.setCostCenter(CostCenter.RENTAL);
        request.setPayeePayorName("Chủ nhà 123 Nguyễn Huệ");
        request.setNote("Chi tiền mặt bằng văn phòng tháng này");

        FinanceVoucherDto createdVoucher = financeVoucherService.createVoucher(request, "admin");
        assertNotNull(createdVoucher);
        assertEquals(VoucherStatus.DRAFT, createdVoucher.getStatus());

        // Duyệt phiếu chi
        FinanceVoucherDto approvedVoucher = financeVoucherService.approveVoucher(createdVoucher.getId(), "admin");
        assertNotNull(approvedVoucher);
        assertEquals(VoucherStatus.APPROVED, approvedVoucher.getStatus());
        assertEquals("admin", approvedVoucher.getApprovedByUsername());

        // Kiểm tra tiền trong quỹ tiền mặt đã bị trừ
        TreasuryAccount updatedCash = treasuryAccountRepository.findById(cashAccount.getId()).orElseThrow();
        assertEquals(0, cashBefore.subtract(expenseAmount).compareTo(updatedCash.getCurrentBalance()));
    }

    @Test
    @DisplayName("4. Smart Bank Reconciliation: Nạp sao kê CSV, auto-match ORD, đối soát 1-click cập nhật PAID đơn hàng")
    void testSmartBankReconciliation() {
        String testOrderCode = "ORD-TEST-REC-101";
        BigDecimal orderTotal = new BigDecimal("899000.00");

        // Tạo đơn hàng test ở trạng thái UNPAID
        Order order = new Order();
        order.setOrderCode(testOrderCode);
        order.setCustomerName("Nguyễn Văn A");
        order.setCustomerPhone("0988123456");
        order.setTotalAmount(orderTotal);
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        orderRepository.save(order);

        // Nạp 1 dòng sao kê ngân hàng VCB khớp với mã đơn hàng
        String rawStatement = String.format("TXN-998877,2026-11-20 10:00:00,899000,KH Nguyen Van A chuyen khoan thanh toan %s", testOrderCode);
        UploadBankStatementRequest uploadReq = new UploadBankStatementRequest();
        uploadReq.setStatementRawContent(rawStatement);
        uploadReq.setBankName("Vietcombank");
        uploadReq.setAccountNumber("0123456789");
        uploadReq.setTargetBankAccountId(bankAccount.getId());

        List<BankStatementItemDto> items = bankReconciliationService.parseAndSaveStatements(uploadReq);
        assertNotNull(items);
        assertFalse(items.isEmpty());

        BankStatementItemDto item = items.get(0);
        assertEquals(ReconciliationStatus.MATCHED, item.getStatus(), "Thuật toán phải auto-match mã đơn hàng");
        assertEquals(testOrderCode, item.getMatchedOrderCode());

        // Thực hiện đối soát 1-click
        BigDecimal bankBefore = bankAccount.getCurrentBalance();
        FinanceVoucherDto receiptVoucher = bankReconciliationService.reconcileItem(item.getId(), bankAccount.getId(), "admin");

        assertNotNull(receiptVoucher);
        assertEquals(VoucherType.RECEIPT, receiptVoucher.getVoucherType());
        assertEquals(VoucherStatus.APPROVED, receiptVoucher.getStatus());

        // Kiểm tra đơn hàng đã đổi sang PAID
        Order updatedOrder = orderRepository.findByOrderCode(testOrderCode).orElseThrow();
        assertEquals(PaymentStatus.PAID, updatedOrder.getPaymentStatus());

        // Kiểm tra số dư quỹ ngân hàng tăng thêm
        TreasuryAccount updatedBank = treasuryAccountRepository.findById(bankAccount.getId()).orElseThrow();
        assertEquals(0, bankBefore.add(orderTotal).compareTo(updatedBank.getCurrentBalance()));
    }

    @Test
    @DisplayName("5. Báo cáo P&L BI Đa kỳ: Tính Doanh thu thuần, COGS, Lợi nhuận gộp, Phân rã OPEX và EBIT")
    void testProfitAndLossCalculation() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();

        ProfitAndLossDto pnl = profitAndLossService.calculatePnL(startDate, endDate, "30 ngày gần nhất");

        assertNotNull(pnl);
        assertNotNull(pnl.getNetRevenue());
        assertNotNull(pnl.getCogs());
        assertNotNull(pnl.getGrossProfit());
        assertNotNull(pnl.getTotalOpex());
        assertNotNull(pnl.getNetProfit());
        assertNotNull(pnl.getOpexBreakdown());

        // Kiểm tra phương trình kế toán chuẩn: NetProfit = GrossProfit - TotalOpex
        BigDecimal expectedNetProfit = pnl.getGrossProfit().subtract(pnl.getTotalOpex());
        assertEquals(0, expectedNetProfit.compareTo(pnl.getNetProfit()), "NetProfit phải bằng GrossProfit - TotalOpex");
    }

    @Test
    @DisplayName("6. Cash Runway Alert: Tính Burn Rate 30 ngày và cảnh báo an toàn dòng tiền")
    void testCashRunwayCalculation() {
        CashRunwayDto runway = profitAndLossService.calculateCashRunway();

        assertNotNull(runway);
        assertNotNull(runway.getTotalCashBalance());
        assertNotNull(runway.getMonthlyBurnRate());
        assertNotNull(runway.getDailyBurnRate());
        assertTrue(runway.getRunwayDays() >= 0);
        assertNotNull(runway.getRiskLevel());
        assertNotNull(runway.getRecommendation());

        assertTrue(List.of("SAFE", "WARNING", "CRITICAL").contains(runway.getRiskLevel()), "Risk level phải hợp lệ");
    }
}
