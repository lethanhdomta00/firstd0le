package com.erp.finance.service;

import com.erp.finance.dto.CreateTreasuryAccountRequest;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.dto.InternalTransferRequest;
import com.erp.finance.dto.TreasuryAccountDto;
import com.erp.finance.model.*;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreasuryService {

    private final TreasuryAccountRepository accountRepository;
    private final FinanceVoucherRepository voucherRepository;

    /**
     * Lấy danh sách toàn bộ tài khoản quỹ đang hoạt động
     */
    @Transactional(readOnly = true)
    public List<TreasuryAccountDto> getAllActiveAccounts() {
        return accountRepository.findByActiveTrueOrderByAccountTypeAscAccountNameAsc().stream()
                .map(TreasuryAccountDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TreasuryAccountDto getAccountById(Long id) {
        TreasuryAccount acc = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quỹ với ID: " + id));
        return TreasuryAccountDto.fromEntity(acc);
    }

    /**
     * Tạo mới tài khoản quỹ tiền mặt hoặc ngân hàng
     */
    @Transactional
    public TreasuryAccountDto createAccount(CreateTreasuryAccountRequest req) {
        if (accountRepository.findByAccountCode(req.getAccountCode().trim().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Mã tài khoản quỹ '" + req.getAccountCode() + "' đã tồn tại!");
        }

        BigDecimal initBal = req.getInitialBalance() != null ? req.getInitialBalance() : BigDecimal.ZERO;
        TreasuryAccount acc = TreasuryAccount.builder()
                .accountCode(req.getAccountCode().trim().toUpperCase())
                .accountName(req.getAccountName().trim())
                .accountType(req.getAccountType())
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .accountHolder(req.getAccountHolder())
                .currentBalance(initBal)
                .active(true)
                .note(req.getNote())
                .build();

        TreasuryAccount saved = accountRepository.save(acc);
        log.info("Đã tạo mới tài khoản quỹ [{}]: {} với số dư ban đầu: {}", saved.getAccountCode(), saved.getAccountName(), initBal);
        return TreasuryAccountDto.fromEntity(saved);
    }

    /**
     * Luân chuyển tiền nội bộ giữa các quỹ (Internal Transfer)
     * Áp dụng Transaction REPEATABLE_READ để chống race condition
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public FinanceVoucherDto transferInternal(InternalTransferRequest req, String username) {
        if (req.getSourceAccountId().equals(req.getTargetAccountId())) {
            throw new IllegalArgumentException("Tài khoản nguồn và tài khoản đích không được trùng nhau!");
        }

        TreasuryAccount source = accountRepository.findById(req.getSourceAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản trích tiền ID: " + req.getSourceAccountId()));

        TreasuryAccount target = accountRepository.findById(req.getTargetAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhận tiền ID: " + req.getTargetAccountId()));

        BigDecimal amount = req.getAmount();
        if (source.getCurrentBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(String.format("Tài khoản nguồn [%s - %s] không đủ số dư để chuyển! (Số dư hiện tại: %,.0f đ, Cần chuyển: %,.0f đ)",
                    source.getAccountCode(), source.getAccountName(), source.getCurrentBalance(), amount));
        }

        // 1. Trừ tiền tài khoản nguồn & Cộng tiền tài khoản đích
        source.setCurrentBalance(source.getCurrentBalance().subtract(amount));
        target.setCurrentBalance(target.getCurrentBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(target);

        // 2. Tạo chứng từ luân chuyển quỹ (Status: APPROVED)
        String code = generateVoucherCode("TF");
        String note = req.getNote() != null && !req.getNote().isBlank()
                ? req.getNote()
                : String.format("Chuyển tiền nội bộ từ [%s] sang [%s]", source.getAccountName(), target.getAccountName());

        FinanceVoucher voucher = FinanceVoucher.builder()
                .voucherCode(code)
                .voucherType(VoucherType.INTERNAL_TRANSFER)
                .costCenter(CostCenter.INTERNAL_TRANSFER)
                .amount(amount)
                .sourceAccount(source)
                .targetAccount(target)
                .payeePayorName(target.getAccountName())
                .note(note)
                .status(VoucherStatus.APPROVED)
                .createdByUsername(username)
                .approvedByUsername(username)
                .approvedAt(LocalDateTime.now())
                .build();

        FinanceVoucher savedVoucher = voucherRepository.save(voucher);
        log.info("Chuyển quỹ nội bộ thành công: Mã [{}] - Số tiền: {} từ [{}] sang [{}]",
                code, amount, source.getAccountCode(), target.getAccountCode());

        return FinanceVoucherDto.fromEntity(savedVoucher);
    }

    /**
     * Điều chỉnh số dư tài khoản quỹ (hàm nội bộ khi duyệt phiếu)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void adjustAccountBalance(Long accountId, BigDecimal amount, boolean isCredit) {
        TreasuryAccount acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quỹ ID: " + accountId));

        if (isCredit) {
            // Cộng tiền vào quỹ (Thu)
            acc.setCurrentBalance(acc.getCurrentBalance().add(amount));
        } else {
            // Trừ tiền khỏi quỹ (Chi)
            if (acc.getCurrentBalance().compareTo(amount) < 0) {
                throw new IllegalStateException(String.format("Tài khoản [%s] không đủ số dư! (Hiện có: %,.0f đ, Cần xuất: %,.0f đ)",
                        acc.getAccountName(), acc.getCurrentBalance(), amount));
            }
            acc.setCurrentBalance(acc.getCurrentBalance().subtract(amount));
        }
        accountRepository.save(acc);
    }

    private String generateVoucherCode(String prefix) {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 100 + new Random().nextInt(900);
        return String.format("%s-%s-%03d", prefix, datePart, rand);
    }
}
