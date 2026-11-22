package com.erp.procurement.service;

import com.erp.finance.model.CostCenter;
import com.erp.finance.model.FinanceVoucher;
import com.erp.finance.model.TreasuryAccount;
import com.erp.finance.model.VoucherStatus;
import com.erp.finance.model.VoucherType;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.procurement.dto.PaySupplierDebtRequest;
import com.erp.procurement.dto.ProcurementOverviewDto;
import com.erp.procurement.model.PurchaseOrder;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcurementPaymentService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TreasuryAccountRepository treasuryAccountRepository;
    private final FinanceVoucherRepository financeVoucherRepository;

    /**
     * Lấy các chỉ số thống kê tổng quan Mua hàng & Quản lý Nhà cung cấp SRM
     */
    public ProcurementOverviewDto getProcurementOverview() {
        long totalSuppliers = supplierRepository.count();
        long activeSuppliers = supplierRepository.countByStatus("ACTIVE");
        long pendingPos = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        long approvedPos = purchaseOrderRepository.countByStatus(PurchaseOrderStatus.APPROVED);

        BigDecimal totalAp = supplierRepository.sumTotalAccountsPayable();
        if (totalAp == null) totalAp = BigDecimal.ZERO;

        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        BigDecimal monthlyPurchase = purchaseOrderRepository.sumTotalPurchaseSince(startOfMonth);
        if (monthlyPurchase == null) monthlyPurchase = BigDecimal.ZERO;

        BigDecimal remainingDebt = purchaseOrderRepository.sumTotalRemainingDebt();
        if (remainingDebt == null) remainingDebt = BigDecimal.ZERO;

        return ProcurementOverviewDto.builder()
                .totalSuppliers(totalSuppliers)
                .activeSuppliers(activeSuppliers)
                .pendingPurchaseOrders(pendingPos)
                .approvedPurchaseOrders(approvedPos)
                .totalAccountsPayable(totalAp)
                .totalPurchasedThisMonth(monthlyPurchase)
                .totalRemainingPoDebt(remainingDebt)
                .build();
    }

    /**
     * Thanh toán công nợ Nhà cung cấp kết nối trực tiếp Phân hệ Sổ quỹ & Phiếu chi
     */
    @Transactional
    public Map<String, Object> paySupplierDebt(PaySupplierDebtRequest request, String currentUsername) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp với ID: " + request.getSupplierId()));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0!");
        }

        // Kiểm tra tài khoản quỹ chi tiền
        TreasuryAccount sourceAccount = treasuryAccountRepository.findByAccountCode(request.getSourceAccountCode())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quỹ nguồn: " + request.getSourceAccountCode()));

        if (sourceAccount.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException(String.format(
                    "Số dư quỹ %s (%,.0fđ) không đủ để thanh toán %,.0fđ cho nhà cung cấp!",
                    sourceAccount.getAccountName(), sourceAccount.getCurrentBalance(), request.getAmount()
            ));
        }

        // 1. Tạo Phiếu Chi xuất quỹ (Payment Voucher)
        String voucherCode = generatePaymentVoucherCode();
        FinanceVoucher voucher = FinanceVoucher.builder()
                .voucherCode(voucherCode)
                .voucherType(VoucherType.PAYMENT)
                .costCenter(CostCenter.PURCHASE)
                .amount(request.getAmount())
                .sourceAccount(sourceAccount)
                .payeePayorName(supplier.getName())
                .payeePayorPhone(supplier.getPhone())
                .status(VoucherStatus.APPROVED)
                .note(request.getNotes() != null && !request.getNotes().isBlank()
                        ? request.getNotes()
                        : "Thanh toán công nợ nhà cung cấp: " + supplier.getName())
                .createdByUsername(currentUsername != null ? currentUsername : "admin")
                .approvedByUsername(currentUsername != null ? currentUsername : "admin")
                .approvedAt(LocalDateTime.now())
                .build();

        // 2. Trừ tiền quỹ nguồn
        sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(request.getAmount()));
        treasuryAccountRepository.save(sourceAccount);
        financeVoucherRepository.save(voucher);

        // 3. Giảm công nợ nhà cung cấp
        BigDecimal oldDebt = supplier.getCurrentDebt() != null ? supplier.getCurrentDebt() : BigDecimal.ZERO;
        BigDecimal newDebt = oldDebt.subtract(request.getAmount());
        if (newDebt.compareTo(BigDecimal.ZERO) < 0) {
            newDebt = BigDecimal.ZERO;
        }
        supplier.setCurrentDebt(newDebt);
        supplierRepository.save(supplier);

        // 4. Nếu có thanh toán cho 1 đơn PO cụ thể
        String targetPoCode = null;
        if (request.getPurchaseOrderId() != null) {
            PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId()).orElse(null);
            if (po != null) {
                targetPoCode = po.getPoCode();
                BigDecimal paid = po.getPaidAmount() != null ? po.getPaidAmount() : BigDecimal.ZERO;
                po.setPaidAmount(paid.add(request.getAmount()));
                po.recalculateTotals();
                purchaseOrderRepository.save(po);
                voucher.setReferenceOrderCode(po.getPoCode());
                financeVoucherRepository.save(voucher);
            }
        }

        log.info("Thanh toán thành công %,.0fđ cho NCC {} qua quỹ {}. Mã phiếu chi: {}",
                request.getAmount(), supplier.getName(), sourceAccount.getAccountName(), voucherCode);

        return Map.of(
                "success", true,
                "voucherCode", voucherCode,
                "amountPaid", request.getAmount(),
                "supplierName", supplier.getName(),
                "remainingSupplierDebt", newDebt,
                "sourceAccountName", sourceAccount.getAccountName(),
                "sourceAccountNewBalance", sourceAccount.getCurrentBalance(),
                "referencePoCode", targetPoCode != null ? targetPoCode : ""
        );
    }

    private String generatePaymentVoucherCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = new Random().nextInt(900) + 100;
        return "PV-PURCHASE-" + datePart + "-" + rand;
    }
}
