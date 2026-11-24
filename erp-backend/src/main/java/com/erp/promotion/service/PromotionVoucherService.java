package com.erp.promotion.service;

import com.erp.config.CacheConfig;
import com.erp.promotion.dto.CreateVoucherRequest;
import com.erp.promotion.dto.ValidateVoucherRequest;
import com.erp.promotion.dto.VoucherDto;
import com.erp.promotion.dto.VoucherValidationResultDto;
import com.erp.promotion.model.DiscountType;
import com.erp.promotion.model.PromotionVoucher;
import com.erp.promotion.model.VoucherUsageHistory;
import com.erp.promotion.repository.PromotionVoucherRepository;
import com.erp.promotion.repository.VoucherUsageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionVoucherService {

    private final PromotionVoucherRepository voucherRepository;
    private final VoucherUsageHistoryRepository usageHistoryRepository;

    @Transactional(readOnly = true)
    public Page<VoucherDto> searchVouchers(String keyword, Boolean active, Pageable pageable) {
        return voucherRepository.searchVouchers(keyword, active, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_ACTIVE_VOUCHERS)
    public List<VoucherDto> getActiveVouchers() {
        return voucherRepository.findActiveVouchers(LocalDateTime.now())
                .stream().map(this::mapToDto).toList();
    }

    @Transactional(readOnly = true)
    public VoucherDto getVoucherById(Long id) {
        PromotionVoucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Voucher có ID: " + id));
        return mapToDto(v);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_VOUCHERS, allEntries = true)
    public VoucherDto createVoucher(CreateVoucherRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (voucherRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã voucher '" + code + "' đã tồn tại trong hệ thống!");
        }

        PromotionVoucher v = new PromotionVoucher();
        updateVoucherFields(v, request);
        v.setCode(code);
        v.setUsedCount(0);

        PromotionVoucher saved = voucherRepository.save(v);
        log.info("Đã tạo mới Promotion Voucher: {}", saved.getCode());
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_VOUCHERS, allEntries = true)
    public VoucherDto updateVoucher(Long id, CreateVoucherRequest request) {
        PromotionVoucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Voucher có ID: " + id));

        String newCode = request.getCode().trim().toUpperCase();
        if (!v.getCode().equalsIgnoreCase(newCode) && voucherRepository.existsByCodeIgnoreCase(newCode)) {
            throw new IllegalArgumentException("Mã voucher '" + newCode + "' đã tồn tại trên voucher khác!");
        }

        v.setCode(newCode);
        updateVoucherFields(v, request);
        PromotionVoucher saved = voucherRepository.save(v);
        log.info("Đã cập nhật Promotion Voucher ID {}: {}", id, saved.getCode());
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_VOUCHERS, allEntries = true)
    public void deleteVoucher(Long id) {
        PromotionVoucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Voucher có ID: " + id));
        voucherRepository.delete(v);
        log.info("Đã xóa Promotion Voucher ID {}", id);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_VOUCHERS, allEntries = true)
    public void toggleActive(Long id) {
        PromotionVoucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Voucher có ID: " + id));
        v.setActive(!v.isActive());
        voucherRepository.save(v);
    }

    /**
     * Xác thực voucher và tính số tiền giảm giá
     */
    @Transactional(readOnly = true)
    public VoucherValidationResultDto validateVoucher(String code, Long customerId, BigDecimal subtotal) {
        if (code == null || code.trim().isEmpty()) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Mã voucher rỗng!")
                    .build();
        }

        String cleanCode = code.trim().toUpperCase();
        Optional<PromotionVoucher> opt = voucherRepository.findByCodeIgnoreCase(cleanCode);
        if (opt.isEmpty()) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Mã voucher '" + cleanCode + "' không tồn tại!")
                    .build();
        }

        PromotionVoucher v = opt.get();
        LocalDateTime now = LocalDateTime.now();

        if (!v.isActive()) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Mã voucher hiện đang bị tạm khóa!")
                    .build();
        }

        if (v.getStartDate() != null && now.isBefore(v.getStartDate())) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Chương trình khuyến mại chưa bắt đầu!")
                    .build();
        }

        if (v.getEndDate() != null && now.isAfter(v.getEndDate())) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Mã voucher đã hết hạn sử dụng!")
                    .build();
        }

        if (!v.hasAvailableUsage()) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Mã voucher đã hết lượt sử dụng toàn hệ thống!")
                    .build();
        }

        if (v.getMinOrderValue() != null && subtotal.compareTo(v.getMinOrderValue()) < 0) {
            return VoucherValidationResultDto.builder()
                    .valid(false)
                    .code(cleanCode)
                    .discountAmount(BigDecimal.ZERO)
                    .message("Đơn hàng chưa đạt giá trị tối thiểu " + v.getMinOrderValue().toPlainString() + " VNĐ")
                    .build();
        }

        if (customerId != null && v.getMaxUsagePerCustomer() != null) {
            long usedByCustomer = usageHistoryRepository.countByVoucherCodeIgnoreCaseAndCustomerId(cleanCode, customerId);
            if (usedByCustomer >= v.getMaxUsagePerCustomer()) {
                return VoucherValidationResultDto.builder()
                        .valid(false)
                        .code(cleanCode)
                        .discountAmount(BigDecimal.ZERO)
                        .message("Bạn đã dùng hết " + v.getMaxUsagePerCustomer() + " lượt cho phép của voucher này!")
                        .build();
            }
        }

        // Tính số tiền giảm giá
        BigDecimal discount = BigDecimal.ZERO;
        if (v.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percent = v.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = subtotal.multiply(percent).setScale(0, RoundingMode.HALF_UP);
            if (v.getMaxDiscountAmount() != null && discount.compareTo(v.getMaxDiscountAmount()) > 0) {
                discount = v.getMaxDiscountAmount();
            }
        } else {
            discount = v.getDiscountValue();
        }

        // Số tiền giảm không được vượt quá giá trị đơn hàng
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return VoucherValidationResultDto.builder()
                .valid(true)
                .code(cleanCode)
                .voucherName(v.getName())
                .discountAmount(discount)
                .message("Áp dụng mã voucher thành công!")
                .build();
    }

    /**
     * Ghi nhận lịch sử dùng voucher khi hoàn tất tạo đơn
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_VOUCHERS, allEntries = true)
    public void recordVoucherUsage(String code, Long orderId, String orderCode, Long customerId, 
                                   String phone, BigDecimal subtotal, BigDecimal discount) {
        if (code == null || code.trim().isEmpty() || discount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String cleanCode = code.trim().toUpperCase();
        voucherRepository.findByCodeIgnoreCase(cleanCode).ifPresent(v -> {
            v.setUsedCount((v.getUsedCount() != null ? v.getUsedCount() : 0) + 1);
            voucherRepository.save(v);

            VoucherUsageHistory history = new VoucherUsageHistory();
            history.setVoucherCode(cleanCode);
            history.setOrderId(orderId);
            history.setOrderCode(orderCode);
            history.setCustomerId(customerId);
            history.setCustomerPhone(phone);
            history.setOrderSubtotal(subtotal);
            history.setDiscountApplied(discount);
            usageHistoryRepository.save(history);

            log.info("Đã lưu vết sử dụng voucher {} cho đơn hàng {}", cleanCode, orderCode);
        });
    }

    /**
     * Hoàn lại lượt dùng voucher khi đơn hàng bị hủy
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ACTIVE_VOUCHERS, allEntries = true)
    public void revertVoucherUsage(Long orderId) {
        usageHistoryRepository.findByOrderId(orderId).ifPresent(history -> {
            voucherRepository.findByCodeIgnoreCase(history.getVoucherCode()).ifPresent(v -> {
                int count = v.getUsedCount() != null ? v.getUsedCount() : 0;
                if (count > 0) {
                    v.setUsedCount(count - 1);
                    voucherRepository.save(v);
                }
            });
            usageHistoryRepository.delete(history);
            log.info("Đã hoàn lại lượt sử dụng voucher cho đơn hàng bị hủy ID {}", orderId);
        });
    }

    private void updateVoucherFields(PromotionVoucher v, CreateVoucherRequest request) {
        v.setName(request.getName().trim());
        v.setDescription(request.getDescription());
        v.setDiscountType(request.getDiscountType());
        v.setDiscountValue(request.getDiscountValue());
        v.setMaxDiscountAmount(request.getMaxDiscountAmount());
        v.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        v.setStartDate(request.getStartDate());
        v.setEndDate(request.getEndDate());
        v.setMaxUsage(request.getMaxUsage());
        v.setMaxUsagePerCustomer(request.getMaxUsagePerCustomer() != null ? request.getMaxUsagePerCustomer() : 1);
        if (request.getActive() != null) {
            v.setActive(request.getActive());
        }
    }

    private VoucherDto mapToDto(PromotionVoucher v) {
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = (v.getEndDate() != null && now.isAfter(v.getEndDate())) || 
                            (!v.hasAvailableUsage());

        return VoucherDto.builder()
                .id(v.getId())
                .code(v.getCode())
                .name(v.getName())
                .description(v.getDescription())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .maxDiscountAmount(v.getMaxDiscountAmount())
                .minOrderValue(v.getMinOrderValue())
                .startDate(v.getStartDate())
                .endDate(v.getEndDate())
                .maxUsage(v.getMaxUsage())
                .usedCount(v.getUsedCount())
                .maxUsagePerCustomer(v.getMaxUsagePerCustomer())
                .active(v.isActive())
                .createdAt(v.getCreatedAt())
                .isExpired(isExpired)
                .build();
    }
}
