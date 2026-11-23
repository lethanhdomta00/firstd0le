package com.erp.promotion.service;

import com.erp.model.Customer;
import com.erp.promotion.dto.AdjustLoyaltyPointsRequest;
import com.erp.promotion.dto.LoyaltyMemberDto;
import com.erp.promotion.model.LoyaltyTransaction;
import com.erp.promotion.model.LoyaltyTransactionType;
import com.erp.promotion.model.MemberTier;
import com.erp.promotion.repository.LoyaltyTransactionRepository;
import com.erp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    // Quy ước: 1 điểm tích lũy = 1.000 VNĐ giá trị thanh toán
    public static final BigDecimal POINT_CONVERSION_RATE = new BigDecimal("1000");

    // Cơ số tích điểm: Cứ 100.000 VNĐ chi tiêu = 1 điểm cơ sở
    public static final BigDecimal EARN_BASE_AMOUNT = new BigDecimal("100000");

    @Transactional(readOnly = true)
    public LoyaltyMemberDto getCustomerLoyaltyInfo(Long customerId) {
        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng ID: " + customerId));
        return mapToMemberDto(c);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyMemberDto> searchLoyaltyMembers(String keyword, Pageable pageable) {
        Page<Customer> page = customerRepository.searchCustomers(keyword, pageable);
        return page.map(this::mapToMemberDto);
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransaction> getTransactionsByCustomer(Long customerId, Pageable pageable) {
        return loyaltyTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    /**
     * Tính số điểm tích lũy dự kiến khi thanh toán số tiền paymentAmount
     */
    public int calculatePointsEarned(Customer customer, BigDecimal paymentAmount) {
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        MemberTier tier = (customer != null && customer.getMemberTier() != null) 
                ? customer.getMemberTier() 
                : MemberTier.BRONZE;

        // Điểm cơ sở = paymentAmount / 100.000đ
        BigDecimal basePoints = paymentAmount.divide(EARN_BASE_AMOUNT, 2, RoundingMode.FLOOR);
        // Nhân hệ số hạng
        BigDecimal totalPoints = basePoints.multiply(BigDecimal.valueOf(tier.getPointMultiplier()));
        return totalPoints.setScale(0, RoundingMode.FLOOR).intValue();
    }

    /**
     * Khấu trừ điểm khi người dùng chọn dùng điểm thanh toán đơn hàng
     * Trả về số tiền được giảm tương ứng (VNĐ)
     */
    @Transactional
    public BigDecimal redeemPoints(Long customerId, Long orderId, String orderCode, int pointsToUse, BigDecimal orderSubtotal) {
        if (pointsToUse <= 0) return BigDecimal.ZERO;

        Customer c = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng ID: " + customerId));

        int currentPoints = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        if (pointsToUse > currentPoints) {
            throw new IllegalArgumentException("Khách hàng chỉ có " + currentPoints + " điểm, không đủ " + pointsToUse + " điểm để sử dụng!");
        }

        // Giá trị quy đổi tiền mặt
        BigDecimal discount = BigDecimal.valueOf(pointsToUse).multiply(POINT_CONVERSION_RATE);

        // Quy tắc an toàn: Không được trừ điểm vượt quá 50% giá trị đơn hàng
        BigDecimal maxAllowedDiscount = orderSubtotal.multiply(new BigDecimal("0.5"));
        if (discount.compareTo(maxAllowedDiscount) > 0) {
            discount = maxAllowedDiscount;
            pointsToUse = discount.divide(POINT_CONVERSION_RATE, 0, RoundingMode.FLOOR).intValue();
            discount = BigDecimal.valueOf(pointsToUse).multiply(POINT_CONVERSION_RATE);
        }

        // Cập nhật số điểm của khách
        int newBalance = currentPoints - pointsToUse;
        c.setLoyaltyPoints(newBalance);
        customerRepository.save(c);

        // Lưu vết lịch sử giao dịch điểm
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setCustomerId(customerId);
        tx.setOrderId(orderId);
        tx.setOrderCode(orderCode);
        tx.setType(LoyaltyTransactionType.REDEEM);
        tx.setPoints(-pointsToUse);
        tx.setBalanceAfter(newBalance);
        tx.setReason("Dùng " + pointsToUse + " điểm giảm trừ " + discount.toPlainString() + "đ cho đơn hàng " + orderCode);
        loyaltyTransactionRepository.save(tx);

        log.info("Khách hàng ID {} đã dùng {} điểm giảm trừ {}đ cho đơn {}", customerId, pointsToUse, discount, orderCode);
        return discount;
    }

    /**
     * Tích điểm và nâng hạng hội viên khi đơn hàng hoàn thành
     */
    @Transactional
    public void awardPointsForCompletedOrder(Long customerId, Long orderId, String orderCode, BigDecimal paymentAmount) {
        if (customerId == null || paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) return;

        int earned = calculatePointsEarned(c, paymentAmount);
        if (earned <= 0) return;

        int currentPoints = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        int newBalance = currentPoints + earned;
        c.setLoyaltyPoints(newBalance);

        // Cập nhật chi tiêu trọn đời và tự động thăng hạng
        c.addSpentAndEvaluateTier(paymentAmount);
        customerRepository.save(c);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setCustomerId(customerId);
        tx.setOrderId(orderId);
        tx.setOrderCode(orderCode);
        tx.setType(LoyaltyTransactionType.EARN);
        tx.setPoints(earned);
        tx.setBalanceAfter(newBalance);
        tx.setReason("Tích " + earned + " điểm từ đơn hàng hoàn tất " + orderCode + " (Doanh số: " + paymentAmount.toPlainString() + "đ)");
        loyaltyTransactionRepository.save(tx);

        log.info("Đã tích {} điểm cho khách hàng ID {} từ đơn {}. Hạng hiện tại: {}", earned, customerId, orderCode, c.getMemberTier());
    }

    /**
     * Hoàn lại điểm tích lũy khi đơn hàng bị hủy
     */
    @Transactional
    public void refundPointsForCancelledOrder(Long orderId, String orderCode, Long customerId, int pointsUsed) {
        if (pointsUsed <= 0 || customerId == null) return;

        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) return;

        int currentPoints = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        int newBalance = currentPoints + pointsUsed;
        c.setLoyaltyPoints(newBalance);
        customerRepository.save(c);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setCustomerId(customerId);
        tx.setOrderId(orderId);
        tx.setOrderCode(orderCode);
        tx.setType(LoyaltyTransactionType.REFUND);
        tx.setPoints(pointsUsed);
        tx.setBalanceAfter(newBalance);
        tx.setReason("Hoàn lại " + pointsUsed + " điểm do hủy đơn hàng " + orderCode);
        loyaltyTransactionRepository.save(tx);

        log.info("Đã hoàn lại {} điểm cho khách hàng ID {} từ đơn hủy {}", pointsUsed, customerId, orderCode);
    }

    /**
     * Admin điều chỉnh điểm thủ công
     */
    @Transactional
    public LoyaltyMemberDto adjustPointsManually(AdjustLoyaltyPointsRequest request) {
        Customer c = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng ID: " + request.getCustomerId()));

        int current = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        int newBalance = Math.max(0, current + request.getPoints());
        int actualDiff = newBalance - current;

        c.setLoyaltyPoints(newBalance);
        customerRepository.save(c);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setCustomerId(c.getId());
        tx.setType(LoyaltyTransactionType.ADJUST);
        tx.setPoints(actualDiff);
        tx.setBalanceAfter(newBalance);
        tx.setReason(request.getReason() != null ? request.getReason() : "Quản trị viên điều chỉnh điểm thủ công");
        loyaltyTransactionRepository.save(tx);

        log.info("Admin đã điều chỉnh điểm khách hàng ID {}: {} điểm. Số dư mới: {}", c.getId(), actualDiff, newBalance);
        return mapToMemberDto(c);
    }

    private LoyaltyMemberDto mapToMemberDto(Customer c) {
        int points = c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0;
        MemberTier tier = c.getMemberTier() != null ? c.getMemberTier() : MemberTier.BRONZE;
        BigDecimal pointsVal = BigDecimal.valueOf(points).multiply(POINT_CONVERSION_RATE);

        return LoyaltyMemberDto.builder()
                .customerId(c.getId())
                .customerName(c.getName())
                .customerPhone(c.getPhone())
                .memberTier(tier)
                .tierDisplayName(tier.getDisplayName())
                .pointMultiplier(tier.getPointMultiplier())
                .loyaltyPoints(points)
                .totalSpent(c.getTotalSpent() != null ? c.getTotalSpent() : BigDecimal.ZERO)
                .pointsValueVnd(pointsVal)
                .build();
    }
}
