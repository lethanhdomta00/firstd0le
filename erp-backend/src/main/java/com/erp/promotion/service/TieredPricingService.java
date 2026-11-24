package com.erp.promotion.service;

import com.erp.config.CacheConfig;
import com.erp.model.Product;
import com.erp.promotion.dto.CreateTieredPriceRuleRequest;
import com.erp.promotion.dto.TieredPriceRuleDto;
import com.erp.promotion.model.PricingType;
import com.erp.promotion.model.TieredPriceRule;
import com.erp.promotion.repository.TieredPriceRuleRepository;
import com.erp.repository.ProductRepository;
import lombok.Builder;
import lombok.Data;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TieredPricingService {

    private final TieredPriceRuleRepository ruleRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<TieredPriceRuleDto> getAllRules(Pageable pageable) {
        return ruleRepository.findAllWithProduct(pageable).map(this::mapToDto);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TIERED_PRICING, allEntries = true)
    public TieredPriceRuleDto createRule(CreateTieredPriceRuleRequest request) {
        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + request.getProductId()));
        }

        TieredPriceRule rule = new TieredPriceRule();
        rule.setProduct(product);
        rule.setTierName(request.getTierName().trim());
        rule.setMinQuantity(request.getMinQuantity());
        rule.setMaxQuantity(request.getMaxQuantity());
        rule.setPricingType(request.getPricingType());
        rule.setDiscountPercent(request.getDiscountPercent());
        rule.setFixedPrice(request.getFixedPrice());
        rule.setActive(request.getActive() != null ? request.getActive() : true);

        TieredPriceRule saved = ruleRepository.save(rule);
        log.info("Đã tạo quy tắc giá sỉ bậc thang ID {}: {}", saved.getId(), saved.getTierName());
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TIERED_PRICING, allEntries = true)
    public TieredPriceRuleDto updateRule(Long id, CreateTieredPriceRuleRequest request) {
        TieredPriceRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc giá sỉ ID: " + id));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + request.getProductId()));
        }

        rule.setProduct(product);
        rule.setTierName(request.getTierName().trim());
        rule.setMinQuantity(request.getMinQuantity());
        rule.setMaxQuantity(request.getMaxQuantity());
        rule.setPricingType(request.getPricingType());
        rule.setDiscountPercent(request.getDiscountPercent());
        rule.setFixedPrice(request.getFixedPrice());
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }

        TieredPriceRule saved = ruleRepository.save(rule);
        log.info("Đã cập nhật quy tắc giá sỉ bậc thang ID {}", id);
        return mapToDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_TIERED_PRICING, allEntries = true)
    public void deleteRule(Long id) {
        TieredPriceRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc giá sỉ ID: " + id));
        ruleRepository.delete(rule);
        log.info("Đã xóa quy tắc giá sỉ bậc thang ID {}", id);
    }

    /**
     * Tính toán đơn giá thực tế sau khi áp dụng bảng giá sỉ bậc thang
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_TIERED_PRICING, key = "(#product != null ? #product.id : 0) + '_' + #quantity")
    public TieredPricingResult calculateEffectivePrice(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            return TieredPricingResult.builder()
                    .baseUnitPrice(BigDecimal.ZERO)
                    .effectiveUnitPrice(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.ZERO)
                    .lineDiscount(BigDecimal.ZERO)
                    .appliedTierName(null)
                    .build();
        }

        BigDecimal basePrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
        List<TieredPriceRule> rules = ruleRepository.findActiveRulesForProduct(product.getId());

        // Tìm rule khớp với số lượng mua (ưu tiên minQuantity lớn nhất)
        TieredPriceRule matchedRule = rules.stream()
                .filter(r -> r.matchesQuantity(quantity))
                .findFirst()
                .orElse(null);

        if (matchedRule == null) {
            BigDecimal lineTotal = basePrice.multiply(BigDecimal.valueOf(quantity));
            return TieredPricingResult.builder()
                    .baseUnitPrice(basePrice)
                    .effectiveUnitPrice(basePrice)
                    .lineTotal(lineTotal)
                    .lineDiscount(BigDecimal.ZERO)
                    .appliedTierName(null)
                    .build();
        }

        BigDecimal effectivePrice = basePrice;
        if (matchedRule.getPricingType() == PricingType.PERCENT_DISCOUNT && matchedRule.getDiscountPercent() != null) {
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                    matchedRule.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            effectivePrice = basePrice.multiply(discountFactor).setScale(0, RoundingMode.HALF_UP);
        } else if (matchedRule.getPricingType() == PricingType.FIXED_PRICE && matchedRule.getFixedPrice() != null) {
            effectivePrice = matchedRule.getFixedPrice();
        }

        BigDecimal lineTotal = effectivePrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal originalTotal = basePrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal lineDiscount = originalTotal.subtract(lineTotal).max(BigDecimal.ZERO);

        return TieredPricingResult.builder()
                .baseUnitPrice(basePrice)
                .effectiveUnitPrice(effectivePrice)
                .lineTotal(lineTotal)
                .lineDiscount(lineDiscount)
                .appliedTierName(matchedRule.getTierName())
                .build();
    }

    private TieredPriceRuleDto mapToDto(TieredPriceRule r) {
        return TieredPriceRuleDto.builder()
                .id(r.getId())
                .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                .productSku(r.getProduct() != null ? r.getProduct().getSku() : "Áp dụng chung")
                .productName(r.getProduct() != null ? r.getProduct().getName() : "Tất cả sản phẩm")
                .tierName(r.getTierName())
                .minQuantity(r.getMinQuantity())
                .maxQuantity(r.getMaxQuantity())
                .pricingType(r.getPricingType())
                .discountPercent(r.getDiscountPercent())
                .fixedPrice(r.getFixedPrice())
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Data
    @Builder
    public static class TieredPricingResult {
        private BigDecimal baseUnitPrice;
        private BigDecimal effectiveUnitPrice;
        private BigDecimal lineTotal;
        private BigDecimal lineDiscount;
        private String appliedTierName;
    }
}
