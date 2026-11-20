package com.erp.finance.service;

import com.erp.finance.dto.BankStatementItemDto;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.dto.UploadBankStatementRequest;
import com.erp.finance.model.*;
import com.erp.finance.repository.BankStatementItemRepository;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.order.model.Order;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankReconciliationService {

    private final BankStatementItemRepository statementRepository;
    private final OrderRepository orderRepository;
    private final TreasuryAccountRepository accountRepository;
    private final FinanceVoucherRepository voucherRepository;
    private final TreasuryService treasuryService;

    @Transactional(readOnly = true)
    public Page<BankStatementItemDto> getStatementItems(ReconciliationStatus status, String keyword, Pageable pageable) {
        return statementRepository.searchStatementItems(status, keyword, pageable)
                .map(BankStatementItemDto::fromEntity);
    }

    /**
     * Bóc tách và nạp dữ liệu sao kê ngân hàng thông minh (Fuzzy Matching với Đơn hàng)
     */
    @Transactional
    public List<BankStatementItemDto> parseAndSaveStatements(UploadBankStatementRequest req) {
        String rawContent = req.getStatementRawContent();
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalArgumentException("Dữ liệu sao kê không được để trống!");
        }

        String bankName = req.getBankName() != null ? req.getBankName() : "Vietcombank";
        String accountNumber = req.getAccountNumber() != null ? req.getAccountNumber() : "0123456789";

        String[] lines = rawContent.split("\\r?\\n");
        List<BankStatementItem> parsedItems = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.toLowerCase().startsWith("ngay") || line.toLowerCase().startsWith("date")) {
                continue; // Bỏ qua header
            }

            try {
                BankStatementItem item = parseSingleLine(line, bankName, accountNumber);
                if (item != null) {
                    // Kiểm tra xem mã giao dịch đã tồn tại chưa
                    if (statementRepository.findByTransactionCode(item.getTransactionCode()).isEmpty()) {
                        // Tìm kiếm khớp đơn hàng thông minh (Fuzzy Matching)
                        matchOrderForStatement(item);
                        parsedItems.add(item);
                    }
                }
            } catch (Exception ex) {
                log.warn("Không thể bóc tách dòng sao kê [{}]: {}", line, ex.getMessage());
            }
        }

        List<BankStatementItem> saved = statementRepository.saveAll(parsedItems);
        log.info("Đã nạp và đối soát sơ bộ {} dòng sao kê từ ngân hàng {}", saved.size(), bankName);

        return saved.stream().map(BankStatementItemDto::fromEntity).toList();
    }

    /**
     * Đối soát 1 dòng sao kê và hạch toán tự động vào hệ thống (1-click Reconciliation)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public FinanceVoucherDto reconcileItem(Long itemId, Long targetBankAccountId, String username) {
        BankStatementItem item = statementRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng sao kê ID: " + itemId));

        if (item.getStatus() == ReconciliationStatus.RECONCILED) {
            throw new IllegalStateException("Dòng sao kê này đã được đối soát và hạch toán trước đó!");
        }

        if (item.getMatchedOrderCode() == null || item.getMatchedOrderCode().isBlank()) {
            throw new IllegalStateException("Dòng sao kê này chưa khớp với đơn hàng nào! Vui lòng chỉ định đơn hàng trước.");
        }

        TreasuryAccount bankAccount = accountRepository.findById(targetBankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ngân hàng đích ID: " + targetBankAccountId));

        // 1. Cập nhật đơn hàng thành PAID
        Optional<Order> orderOpt = orderRepository.findByOrderCode(item.getMatchedOrderCode());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            log.info("Đối soát ngân hàng: Đã đổi trạng thái đơn [{}] sang PAID", order.getOrderCode());
        }

        // 2. Tạo phiếu Thu tự động và cộng tiền vào tài khoản ngân hàng
        String voucherCode = "RC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + (100 + new Random().nextInt(900));
        FinanceVoucher voucher = FinanceVoucher.builder()
                .voucherCode(voucherCode)
                .voucherType(VoucherType.RECEIPT)
                .costCenter(CostCenter.SALES_REVENUE)
                .amount(item.getAmount())
                .targetAccount(bankAccount)
                .payeePayorName(item.getMatchedCustomerName() != null ? item.getMatchedCustomerName() : "Khách hàng chuyển khoản")
                .referenceOrderCode(item.getMatchedOrderCode())
                .note("Đối soát tự động sao kê [" + item.getTransactionCode() + "]: " + item.getDescription())
                .status(VoucherStatus.APPROVED)
                .createdByUsername(username)
                .approvedByUsername(username)
                .approvedAt(LocalDateTime.now())
                .build();

        FinanceVoucher savedVoucher = voucherRepository.save(voucher);
        treasuryService.adjustAccountBalance(bankAccount.getId(), item.getAmount(), true);

        // 3. Cập nhật trạng thái sao kê thành RECONCILED
        item.setStatus(ReconciliationStatus.RECONCILED);
        item.setCreatedVoucherId(savedVoucher.getId());
        item.setReconciliationNote("Đã đối soát thành công bởi @" + username + " -> Sinh phiếu thu " + voucherCode);
        statementRepository.save(item);

        return FinanceVoucherDto.fromEntity(savedVoucher);
    }

    /**
     * Đối soát hàng loạt tất cả các dòng sao kê đã khớp đơn (MATCHED)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> reconcileAllMatched(Long targetBankAccountId, String username) {
        List<BankStatementItem> matchedItems = statementRepository.findByStatusOrderByTransactionDateDesc(ReconciliationStatus.MATCHED);
        int successCount = 0;
        BigDecimal totalReconciledAmount = BigDecimal.ZERO;

        for (BankStatementItem item : matchedItems) {
            try {
                reconcileItem(item.getId(), targetBankAccountId, username);
                successCount++;
                totalReconciledAmount = totalReconciledAmount.add(item.getAmount());
            } catch (Exception ex) {
                log.warn("Không thể tự động đối soát dòng sao kê ID {}: {}", item.getId(), ex.getMessage());
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("totalMatched", matchedItems.size());
        res.put("successCount", successCount);
        res.put("totalAmount", totalReconciledAmount);
        res.put("message", String.format("🎉 Đã đối soát và hạch toán thành công %d đơn hàng (Tổng thu: %,.0f đ)!",
                successCount, totalReconciledAmount));

        return res;
    }

    /**
     * Thuật toán Fuzzy Matching trích xuất mã đơn hàng từ nội dung chuyển khoản
     */
    private void matchOrderForStatement(BankStatementItem item) {
        String desc = item.getDescription();
        if (desc == null) return;

        // Tìm kiếm pattern mã đơn hàng ORD-...
        Pattern p = Pattern.compile("(?i)\\b(ORD-[0-9A-Za-z_-]+)\\b");
        Matcher m = p.matcher(desc);
        if (m.find()) {
            String candidateCode = m.group(1).toUpperCase();
            Optional<Order> orderOpt = orderRepository.findByOrderCode(candidateCode);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                item.setMatchedOrderCode(order.getOrderCode());
                item.setMatchedOrderId(order.getId());
                item.setMatchedCustomerName(order.getCustomerName());
                item.setStatus(ReconciliationStatus.MATCHED);
                item.setReconciliationNote("Khớp 100% mã đơn hàng " + order.getOrderCode() + " (" + order.getCustomerName() + ")");
                return;
            }
        }

        // Fallback: Tìm mã số ngắn gọn dạng ORD123 hoặc 1001
        Pattern pShort = Pattern.compile("(?i)(?:don|dh|order|ord)[\\s:#]*([0-9]{4,8})");
        Matcher mShort = pShort.matcher(desc);
        if (mShort.find()) {
            String shortCode = mShort.group(1);
            List<Order> allOrders = orderRepository.findAll();
            for (Order o : allOrders) {
                if (o.getOrderCode().contains(shortCode)) {
                    item.setMatchedOrderCode(o.getOrderCode());
                    item.setMatchedOrderId(o.getId());
                    item.setMatchedCustomerName(o.getCustomerName());
                    item.setStatus(ReconciliationStatus.MATCHED);
                    item.setReconciliationNote("Khớp số thứ tự đơn " + o.getOrderCode());
                    return;
                }
            }
        }

        item.setStatus(ReconciliationStatus.UNMATCHED);
        item.setReconciliationNote("Chưa phát hiện mã đơn hàng trong nội dung giao dịch");
    }

    /**
     * Bóc tách 1 dòng sao kê
     * Định dạng hỗ trợ: `MãGD | NgàyGiờ | SốTiền | NộiDung` hoặc CSV `MãGD,NgàyGiờ,SốTiền,NộiDung`
     */
    private BankStatementItem parseSingleLine(String line, String bankName, String accountNumber) {
        String[] parts;
        if (line.contains("|")) {
            parts = line.split("\\|");
        } else if (line.contains(",")) {
            parts = line.split(",");
        } else if (line.contains(";")) {
            parts = line.split(";");
        } else {
            // Dòng tự do
            parts = line.split("\\s{2,}");
        }

        if (parts.length < 3) {
            return null;
        }

        String transCode = parts[0].trim();
        String dateStr = parts[1].trim();
        String amountStr = parts[2].trim().replaceAll("[^0-9.-]", "");
        String description = parts.length > 3 ? parts[3].trim() : line;

        if (transCode.isEmpty()) {
            transCode = "FT" + System.currentTimeMillis() + (new Random().nextInt(90) + 10);
        }

        BigDecimal amount = new BigDecimal(amountStr);
        String transType = amount.compareTo(BigDecimal.ZERO) >= 0 ? "CR" : "DR";
        amount = amount.abs();

        LocalDateTime transDate = LocalDateTime.now();
        try {
            if (dateStr.length() >= 10) {
                if (dateStr.contains("/")) {
                    transDate = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                } else if (dateStr.contains("-")) {
                    transDate = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            }
        } catch (Exception ignored) {
        }

        return BankStatementItem.builder()
                .transactionCode(transCode)
                .transactionDate(transDate)
                .amount(amount)
                .transactionType(transType)
                .description(description)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .status(ReconciliationStatus.PENDING)
                .build();
    }
}
