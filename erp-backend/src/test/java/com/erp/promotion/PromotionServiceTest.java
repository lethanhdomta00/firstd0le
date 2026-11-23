package com.erp.promotion;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.promotion.dto.*;
import com.erp.promotion.model.*;
import com.erp.promotion.repository.PromotionVoucherRepository;
import com.erp.promotion.repository.TieredPriceRuleRepository;
import com.erp.promotion.service.LoyaltyService;
import com.erp.promotion.service.PromotionEngineService;
import com.erp.promotion.service.PromotionVoucherService;
import com.erp.promotion.service.TieredPricingService;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
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
public class PromotionServiceTest {

    @Autowired
    private PromotionVoucherService voucherService;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private TieredPricingService tieredPricingService;

    @Autowired
    private PromotionEngineService promotionEngineService;

    @Autowired
    private PromotionVoucherRepository voucherRepository;

    @Autowired
    private TieredPriceRuleRepository tieredRuleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    private Customer testCustomer;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setName("Khách Hàng Test VIP");
        testCustomer.setPhone("0999888777");
        testCustomer.setLoyaltyPoints(100);
        testCustomer.setTotalSpent(new BigDecimal("22000000.00")); // Hạng Vàng GOLD (1.5x)
        testCustomer.setMemberTier(MemberTier.GOLD);
        testCustomer = customerRepository.save(testCustomer);

        testProduct = new Product();
        testProduct.setName("Áo Sơ Mi Oxford Cao Cấp");
        testProduct.setSku("OXFORD-TEST-" + System.currentTimeMillis());
        testProduct.setPrice(new BigDecimal("200000.00"));
        testProduct.setStock(500);
        testProduct.setPhysicalStock(500);
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Test 1: Áp dụng Voucher phần trăm có giới hạn mức giảm tối đa")
    void testVoucherPercentageWithMaxCap() {
        PromotionVoucher voucher = new PromotionVoucher();
        voucher.setCode("TEST10");
        voucher.setName("Giảm 10% tối đa 50k");
        voucher.setDiscountType(DiscountType.PERCENTAGE);
        voucher.setDiscountValue(new BigDecimal("10.00"));
        voucher.setMaxDiscountAmount(new BigDecimal("50000.00"));
        voucher.setMinOrderValue(new BigDecimal("100000.00"));
        voucher.setActive(true);
        voucherRepository.save(voucher);

        // Đơn hàng 800.000đ: 10% là 80.000đ, nhưng chạm maxDiscountAmount là 50.000đ
        VoucherValidationResultDto res = voucherService.validateVoucher("TEST10", testCustomer.getId(), new BigDecimal("800000.00"));
        assertTrue(res.isValid());
        assertEquals(new BigDecimal("50000.00"), res.getDiscountAmount());
    }

    @Test
    @DisplayName("Test 2: Từ chối Voucher khi đơn hàng chưa đạt giá trị tối thiểu")
    void testVoucherMinOrderValueNotMet() {
        PromotionVoucher voucher = new PromotionVoucher();
        voucher.setCode("MIN500K");
        voucher.setName("Giảm 50k cho đơn từ 500k");
        voucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        voucher.setDiscountValue(new BigDecimal("50000.00"));
        voucher.setMinOrderValue(new BigDecimal("500000.00"));
        voucher.setActive(true);
        voucherRepository.save(voucher);

        VoucherValidationResultDto res = voucherService.validateVoucher("MIN500K", testCustomer.getId(), new BigDecimal("300000.00"));
        assertFalse(res.isValid());
        assertTrue(res.getMessage().contains("chưa đạt giá trị tối thiểu"));
    }

    @Test
    @DisplayName("Test 3: Ghi nhận sử dụng voucher và hoàn lại khi hủy đơn")
    void testVoucherUsageTrackingAndRevert() {
        PromotionVoucher voucher = new PromotionVoucher();
        voucher.setCode("TESTCOUNT");
        voucher.setName("Test đếm lượt");
        voucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        voucher.setDiscountValue(new BigDecimal("20000.00"));
        voucher.setMaxUsage(10);
        voucher.setUsedCount(0);
        voucher.setActive(true);
        PromotionVoucher saved = voucherRepository.save(voucher);

        voucherService.recordVoucherUsage("TESTCOUNT", 9999L, "ORD-9999", testCustomer.getId(), testCustomer.getPhone(), new BigDecimal("200000.00"), new BigDecimal("20000.00"));

        PromotionVoucher afterUsed = voucherRepository.findById(saved.getId()).orElseThrow();
        assertEquals(1, afterUsed.getUsedCount());

        voucherService.revertVoucherUsage(9999L);
        PromotionVoucher afterRevert = voucherRepository.findById(saved.getId()).orElseThrow();
        assertEquals(0, afterRevert.getUsedCount());
    }

    @Test
    @DisplayName("Test 4: Tính điểm thưởng theo hạng hội viên và tự động thăng hạng")
    void testLoyaltyEarnAndTierUpgrade() {
        // testCustomer đang là GOLD (hệ số 1.5x)
        // Đơn thanh toán 2.000.000đ -> Điểm cơ sở: 20 điểm * 1.5 = 30 điểm
        int earned = loyaltyService.calculatePointsEarned(testCustomer, new BigDecimal("2000000.00"));
        assertEquals(30, earned);

        // Khách có chi tiêu 22tr, cộng thêm 30tr -> Tổng chi tiêu = 52tr -> Thăng hạng DIAMOND (>= 50tr)
        loyaltyService.awardPointsForCompletedOrder(testCustomer.getId(), 8888L, "ORD-8888", new BigDecimal("30000000.00"));

        Customer upgraded = customerRepository.findById(testCustomer.getId()).orElseThrow();
        assertEquals(MemberTier.DIAMOND, upgraded.getMemberTier());
        assertTrue(upgraded.getLoyaltyPoints() > 100);
    }

    @Test
    @DisplayName("Test 5: Khấu trừ điểm tích lũy thanh toán (1 điểm = 1.000đ, tối đa 50% đơn)")
    void testLoyaltyRedeemPoints() {
        // Khách có 100 điểm (= 100.000đ)
        // Đơn hàng 500.000đ, khách dùng 50 điểm -> Giảm 50.000đ
        BigDecimal discount = loyaltyService.redeemPoints(testCustomer.getId(), 7777L, "ORD-7777", 50, new BigDecimal("500000.00"));
        assertEquals(new BigDecimal("50000"), discount);

        Customer afterRedeem = customerRepository.findById(testCustomer.getId()).orElseThrow();
        assertEquals(50, afterRedeem.getLoyaltyPoints());
    }

    @Test
    @DisplayName("Test 6: Động cơ Bảng giá sỉ đa tầng (Tiered Pricing Engine)")
    void testTieredPricingCalculation() {
        TieredPriceRule rule = new TieredPriceRule();
        rule.setProduct(testProduct);
        rule.setTierName("Sỉ từ 20 cái giảm 15%");
        rule.setMinQuantity(20);
        rule.setMaxQuantity(50);
        rule.setPricingType(PricingType.PERCENT_DISCOUNT);
        rule.setDiscountPercent(new BigDecimal("15.00"));
        rule.setActive(true);
        tieredRuleRepository.save(rule);

        // Mua 5 cái (chưa đủ bậc sỉ nào) -> Giữ nguyên giá 200.000đ/cái
        var res5 = tieredPricingService.calculateEffectivePrice(testProduct, 5);
        assertEquals(new BigDecimal("200000.00"), res5.getEffectiveUnitPrice());
        assertEquals(BigDecimal.ZERO, res5.getLineDiscount());

        // Mua 20 cái (đạt bậc sỉ riêng của sản phẩm giảm 15%) -> Giảm 15% còn 170.000đ/cái
        var res20 = tieredPricingService.calculateEffectivePrice(testProduct, 20);
        assertEquals(new BigDecimal("170000"), res20.getEffectiveUnitPrice());
        assertEquals(new BigDecimal("600000.00"), res20.getLineDiscount()); // (200k - 170k) * 20 = 600k
    }

    @Test
    @DisplayName("Test 7: Promotion Engine phối hợp toàn diện Giỏ hàng (Tiered + Voucher + Loyalty)")
    void testFullCartPromotionEngine() {
        // Tạo voucher 20k
        PromotionVoucher voucher = new PromotionVoucher();
        voucher.setCode("CART20K");
        voucher.setName("Giảm 20k");
        voucher.setDiscountType(DiscountType.FIXED_AMOUNT);
        voucher.setDiscountValue(new BigDecimal("20000.00"));
        voucher.setActive(true);
        voucherRepository.save(voucher);

        CalculateCartDiscountsRequest req = new CalculateCartDiscountsRequest();
        req.setCustomerId(testCustomer.getId());
        req.setVoucherCode("CART20K");
        req.setLoyaltyPointsToRedeem(20); // 20 điểm = 20.000đ

        CalculateCartDiscountsRequest.CartItemInput item = new CalculateCartDiscountsRequest.CartItemInput();
        item.setProductId(testProduct.getId());
        item.setQuantity(2); // 2 * 200.000đ = 400.000đ
        req.setItems(List.of(item));

        CalculateCartDiscountsResponse res = promotionEngineService.calculateCartDiscounts(req);

        assertEquals(new BigDecimal("400000.00"), res.getOriginalSubtotal());
        assertEquals(new BigDecimal("20000.00"), res.getVoucherDiscount());
        assertEquals(new BigDecimal("20000"), res.getLoyaltyDiscount());
        // Tổng thanh toán = 400k - 20k (voucher) - 20k (loyalty) = 360k
        assertEquals(new BigDecimal("360000.00"), res.getFinalTotal());
        assertTrue(res.getEstimatedPointsEarned() > 0);
    }
}
