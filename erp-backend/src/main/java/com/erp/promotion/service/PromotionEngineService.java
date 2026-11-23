package com.erp.promotion.service;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.promotion.dto.CalculateCartDiscountsRequest;
import com.erp.promotion.dto.CalculateCartDiscountsResponse;
import com.erp.promotion.dto.PromotionOverviewDto;
import com.erp.promotion.dto.VoucherValidationResultDto;
import com.erp.promotion.repository.PromotionVoucherRepository;
import com.erp.promotion.repository.TieredPriceRuleRepository;
import com.erp.promotion.repository.VoucherUsageHistoryRepository;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionEngineService {

    private final PromotionVoucherService voucherService;
    private final LoyaltyService loyaltyService;
    private final TieredPricingService tieredPricingService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PromotionVoucherRepository voucherRepository;
    private final VoucherUsageHistoryRepository usageHistoryRepository;
    private final TieredPriceRuleRepository ruleRepository;

    /**
     * Động cơ tính toán tổng hợp toàn bộ chiết khấu giỏ hàng
     * Kết hợp: Giá sỉ đa tầng (Tiered) + Mã Voucher + Điểm thưởng Loyalty
     */
    @Transactional(readOnly = true)
    public CalculateCartDiscountsResponse calculateCartDiscounts(CalculateCartDiscountsRequest request) {
        BigDecimal originalSubtotal = BigDecimal.ZERO;
        BigDecimal tieredDiscountTotal = BigDecimal.ZERO;
        List<CalculateCartDiscountsResponse.CalculatedItemResult> itemResults = new ArrayList<>();

        if (request.getItems() != null) {
            for (CalculateCartDiscountsRequest.CartItemInput input : request.getItems()) {
                if (input.getProductId() == null || input.getQuantity() == null || input.getQuantity() <= 0) {
                    continue;
                }

                Product product = productRepository.findById(input.getProductId()).orElse(null);
                if (product == null) continue;

                TieredPricingService.TieredPricingResult res = tieredPricingService.calculateEffectivePrice(product, input.getQuantity());

                BigDecimal lineOrig = res.getBaseUnitPrice().multiply(BigDecimal.valueOf(input.getQuantity()));
                originalSubtotal = originalSubtotal.add(lineOrig);
                tieredDiscountTotal = tieredDiscountTotal.add(res.getLineDiscount());

                itemResults.add(CalculateCartDiscountsResponse.CalculatedItemResult.builder()
                        .productId(product.getId())
                        .quantity(input.getQuantity())
                        .baseUnitPrice(res.getBaseUnitPrice())
                        .effectiveUnitPrice(res.getEffectiveUnitPrice())
                        .lineTotal(res.getLineTotal())
                        .lineDiscount(res.getLineDiscount())
                        .appliedTierName(res.getAppliedTierName())
                        .build());
            }
        }

        BigDecimal subtotalAfterTiered = originalSubtotal.subtract(tieredDiscountTotal).max(BigDecimal.ZERO);

        // 1. Áp dụng Voucher (nếu có)
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        String appliedVoucherCode = null;
        String voucherMessage = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            VoucherValidationResultDto vRes = voucherService.validateVoucher(
                    request.getVoucherCode(), 
                    request.getCustomerId(), 
                    subtotalAfterTiered);

            if (vRes.isValid()) {
                voucherDiscount = vRes.getDiscountAmount();
                appliedVoucherCode = vRes.getCode();
                voucherMessage = "Áp dụng thành công mã " + vRes.getCode() + " (-" + voucherDiscount.toPlainString() + "đ)";
            } else {
                voucherMessage = vRes.getMessage();
            }
        }

        BigDecimal subtotalAfterVoucher = subtotalAfterTiered.subtract(voucherDiscount).max(BigDecimal.ZERO);

        // 2. Áp dụng Điểm thưởng Loyalty (nếu có)
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        int loyaltyPointsRedeemed = 0;

        if (request.getLoyaltyPointsToRedeem() != null && request.getLoyaltyPointsToRedeem() > 0 && request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null) {
                int maxCustomerPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
                int requestedPoints = Math.min(request.getLoyaltyPointsToRedeem(), maxCustomerPoints);

                BigDecimal potentialDiscount = BigDecimal.valueOf(requestedPoints).multiply(LoyaltyService.POINT_CONVERSION_RATE);
                // Giới hạn không quá 50% số tiền còn lại sau voucher
                BigDecimal maxAllowed = subtotalAfterVoucher.multiply(new BigDecimal("0.5"));
                if (potentialDiscount.compareTo(maxAllowed) > 0) {
                    potentialDiscount = maxAllowed;
                    requestedPoints = potentialDiscount.divide(LoyaltyService.POINT_CONVERSION_RATE, 0, RoundingMode.FLOOR).intValue();
                    potentialDiscount = BigDecimal.valueOf(requestedPoints).multiply(LoyaltyService.POINT_CONVERSION_RATE);
                }

                loyaltyPointsRedeemed = requestedPoints;
                loyaltyDiscount = potentialDiscount;
            }
        }

        BigDecimal finalTotal = subtotalAfterVoucher.subtract(loyaltyDiscount).max(BigDecimal.ZERO);

        // 3. Ước tính điểm tích lũy dự kiến
        int estimatedPoints = 0;
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId()).orElse(null);
            if (customer != null) {
                estimatedPoints = loyaltyService.calculatePointsEarned(customer, finalTotal);
            }
        }

        return CalculateCartDiscountsResponse.builder()
                .originalSubtotal(originalSubtotal)
                .tieredDiscountTotal(tieredDiscountTotal)
                .subtotalAfterTiered(subtotalAfterTiered)
                .voucherDiscount(voucherDiscount)
                .appliedVoucherCode(appliedVoucherCode)
                .voucherMessage(voucherMessage)
                .loyaltyPointsRedeemed(loyaltyPointsRedeemed)
                .loyaltyDiscount(loyaltyDiscount)
                .finalTotal(finalTotal)
                .estimatedPointsEarned(estimatedPoints)
                .items(itemResults)
                .build();
    }

    /**
     * Thống kê KPI tổng quan cho Dashboard Phân hệ Khuyến Mại & Loyalty
     */
    @Transactional(readOnly = true)
    public PromotionOverviewDto getOverview() {
        long totalVouchers = voucherRepository.count();
        long activeVouchers = voucherRepository.countByActiveTrue();
        BigDecimal totalDiscountSponsored = usageHistoryRepository.sumTotalDiscountSponsored();
        if (totalDiscountSponsored == null) totalDiscountSponsored = BigDecimal.ZERO;

        long totalLoyaltyMembers = customerRepository.count();
        Long pointsSum = customerRepository.findAll().stream()
                .mapToLong(c -> c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0)
                .sum();

        long totalTieredRules = ruleRepository.count();

        return PromotionOverviewDto.builder()
                .totalVouchers(totalVouchers)
                .activeVouchers(activeVouchers)
                .totalDiscountSponsored(totalDiscountSponsored)
                .totalLoyaltyMembers(totalLoyaltyMembers)
                .totalPointsOutstanding(pointsSum != null ? pointsSum : 0L)
                .totalTieredRules(totalTieredRules)
                .build();
    }
}
