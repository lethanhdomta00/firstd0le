package com.erp.finance.service;

import com.erp.finance.dto.CreateFinanceVoucherRequest;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.model.*;
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
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceVoucherService {

    private final FinanceVoucherRepository voucherRepository;
    private final TreasuryAccountRepository accountRepository;
    private final TreasuryService treasuryService;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public Page<FinanceVoucherDto> searchVouchers(
            VoucherType voucherType,
            VoucherStatus status,
            CostCenter costCenter,
            Long sourceAccountId,
            Long targetAccountId,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return voucherRepository.searchVouchers(voucherType, status, costCenter, sourceAccountId, targetAccountId, keyword, startDate, endDate, pageable)
                .map(FinanceVoucherDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public FinanceVoucherDto getVoucherById(Long id) {
        FinanceVoucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu tài chính ID: " + id));
        return FinanceVoucherDto.fromEntity(v);
    }

    /**
     * Tạo phiếu Thu / Chi mới (có thể lưu DRAFT hoặc DUYỆT NGAY)
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public FinanceVoucherDto createVoucher(CreateFinanceVoucherRequest req, String username) {
        VoucherType type = req.getVoucherType();
        TreasuryAccount sourceAccount = null;
        TreasuryAccount targetAccount = null;

        if (type == VoucherType.PAYMENT) {
            if (req.getSourceAccountId() == null) {
                throw new IllegalArgumentException("Phiếu chi bắt buộc phải chỉ định tài khoản quỹ trích tiền!");
            }
            sourceAccount = accountRepository.findById(req.getSourceAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nguồn ID: " + req.getSourceAccountId()));
        } else if (type == VoucherType.RECEIPT) {
            if (req.getTargetAccountId() == null) {
                throw new IllegalArgumentException("Phiếu thu bắt buộc phải chỉ định tài khoản quỹ nhận tiền!");
            }
            targetAccount = accountRepository.findById(req.getTargetAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đích ID: " + req.getTargetAccountId()));
        }

        String prefix = type == VoucherType.RECEIPT ? "RC" : "PV";
        String voucherCode = generateVoucherCode(prefix);

        FinanceVoucher voucher = FinanceVoucher.builder()
                .voucherCode(voucherCode)
                .voucherType(type)
                .costCenter(req.getCostCenter())
                .amount(req.getAmount())
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .payeePayorName(req.getPayeePayorName())
                .payeePayorPhone(req.getPayeePayorPhone())
                .referenceOrderCode(req.getReferenceOrderCode())
                .note(req.getNote())
                .status(VoucherStatus.DRAFT)
                .createdByUsername(username)
                .build();

        FinanceVoucher saved = voucherRepository.save(voucher);

        // Nếu người tạo yêu cầu tự động duyệt ngay (autoApprove)
        if (req.isAutoApprove()) {
            return approveVoucher(saved.getId(), username);
        }

        log.info("Đã tạo phiếu dự thảo [{}]: loại {}, số tiền: {}", voucherCode, type, req.getAmount());
        return FinanceVoucherDto.fromEntity(saved);
    }

    /**
     * Phê duyệt phiếu tài chính (APPROVED) & Hạch toán tăng/giảm số dư quỹ
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public FinanceVoucherDto approveVoucher(Long voucherId, String username) {
        FinanceVoucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu ID: " + voucherId));

        if (v.getStatus() == VoucherStatus.APPROVED) {
            throw new IllegalStateException("Phiếu [" + v.getVoucherCode() + "] đã được phê duyệt trước đó!");
        }
        if (v.getStatus() == VoucherStatus.CANCELLED) {
            throw new IllegalStateException("Không thể duyệt phiếu đã bị hủy!");
        }

        // Hạch toán số dư quỹ
        if (v.getVoucherType() == VoucherType.PAYMENT) {
            treasuryService.adjustAccountBalance(v.getSourceAccount().getId(), v.getAmount(), false);
        } else if (v.getVoucherType() == VoucherType.RECEIPT) {
            treasuryService.adjustAccountBalance(v.getTargetAccount().getId(), v.getAmount(), true);

            // Nếu phiếu thu liên kết với mã đơn hàng -> cập nhật đơn hàng thành PAID
            if (v.getReferenceOrderCode() != null && !v.getReferenceOrderCode().isBlank()) {
                Optional<Order> orderOpt = orderRepository.findByOrderCode(v.getReferenceOrderCode().trim());
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    if (order.getPaymentStatus() != PaymentStatus.PAID) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                        orderRepository.save(order);
                        log.info("Phiếu thu [{}] đã tự động đổi trạng thái đơn hàng [{}] sang PAID", v.getVoucherCode(), order.getOrderCode());
                    }
                }
            }
        }

        v.setStatus(VoucherStatus.APPROVED);
        v.setApprovedByUsername(username);
        v.setApprovedAt(LocalDateTime.now());
        FinanceVoucher approved = voucherRepository.save(v);

        log.info("Đã phê duyệt thành công phiếu [{}]: Số tiền {} đ", approved.getVoucherCode(), approved.getAmount());
        return FinanceVoucherDto.fromEntity(approved);
    }

    /**
     * Hủy bỏ phiếu tài chính
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public FinanceVoucherDto cancelVoucher(Long voucherId, String username) {
        FinanceVoucher v = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu ID: " + voucherId));

        if (v.getStatus() == VoucherStatus.CANCELLED) {
            throw new IllegalStateException("Phiếu đã bị hủy trước đó!");
        }

        // Nếu phiếu đã duyệt thì phải hoàn tiền (rollback balance)
        if (v.getStatus() == VoucherStatus.APPROVED) {
            if (v.getVoucherType() == VoucherType.PAYMENT) {
                treasuryService.adjustAccountBalance(v.getSourceAccount().getId(), v.getAmount(), true);
            } else if (v.getVoucherType() == VoucherType.RECEIPT) {
                treasuryService.adjustAccountBalance(v.getTargetAccount().getId(), v.getAmount(), false);
            } else if (v.getVoucherType() == VoucherType.INTERNAL_TRANSFER) {
                // Hoàn lại tiền chuyển nội bộ
                treasuryService.adjustAccountBalance(v.getTargetAccount().getId(), v.getAmount(), false);
                treasuryService.adjustAccountBalance(v.getSourceAccount().getId(), v.getAmount(), true);
            }
        }

        v.setStatus(VoucherStatus.CANCELLED);
        v.setNote((v.getNote() != null ? v.getNote() : "") + " [Đã hủy bởi @" + username + " lúc " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) + "]");
        FinanceVoucher cancelled = voucherRepository.save(v);
        log.info("Đã hủy phiếu [{}]: Hoàn tiền về tài khoản", cancelled.getVoucherCode());
        return FinanceVoucherDto.fromEntity(cancelled);
    }

    private String generateVoucherCode(String prefix) {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 100 + new Random().nextInt(900);
        return String.format("%s-%s-%03d", prefix, datePart, rand);
    }
}
