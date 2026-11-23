package com.erp.promotion.controller;

import com.erp.promotion.dto.*;
import com.erp.promotion.model.LoyaltyTransaction;
import com.erp.promotion.service.LoyaltyService;
import com.erp.promotion.service.PromotionEngineService;
import com.erp.promotion.service.PromotionVoucherService;
import com.erp.promotion.service.TieredPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionEngineService promotionEngineService;
    private final PromotionVoucherService voucherService;
    private final LoyaltyService loyaltyService;
    private final TieredPricingService tieredPricingService;

    // =========================================================================
    // 1. TỔNG QUAN & ĐỘNG CƠ TÍNH GIỎ HÀNG (PROMOTION ENGINE)
    // =========================================================================

    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PromotionOverviewDto> getOverview() {
        return ResponseEntity.ok(promotionEngineService.getOverview());
    }

    @PostMapping("/calculate-cart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CalculateCartDiscountsResponse> calculateCartDiscounts(
            @RequestBody CalculateCartDiscountsRequest request) {
        return ResponseEntity.ok(promotionEngineService.calculateCartDiscounts(request));
    }

    // =========================================================================
    // 2. QUẢN LÝ MÃ VOUCHER / COUPON
    // =========================================================================

    @GetMapping("/vouchers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<VoucherDto>> searchVouchers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(voucherService.searchVouchers(keyword, active, pageable));
    }

    @GetMapping("/vouchers/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VoucherDto>> getActiveVouchers() {
        return ResponseEntity.ok(voucherService.getActiveVouchers());
    }

    @GetMapping("/vouchers/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VoucherDto> getVoucherById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @PostMapping("/vouchers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VoucherDto> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.createVoucher(request));
    }

    @PutMapping("/vouchers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VoucherDto> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, request));
    }

    @DeleteMapping("/vouchers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/vouchers/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> toggleActiveVoucher(@PathVariable Long id) {
        voucherService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/vouchers/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VoucherValidationResultDto> validateVoucher(
            @Valid @RequestBody ValidateVoucherRequest request) {
        return ResponseEntity.ok(voucherService.validateVoucher(
                request.getCode(), 
                request.getCustomerId(), 
                request.getOrderSubtotal()));
    }

    // =========================================================================
    // 3. TÍCH ĐIỂM HỘI VIÊN & QUẢN TRỊ LOYALTY
    // =========================================================================

    @GetMapping("/loyalty/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LoyaltyMemberDto>> searchLoyaltyMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalSpent").descending());
        return ResponseEntity.ok(loyaltyService.searchLoyaltyMembers(keyword, pageable));
    }

    @GetMapping("/loyalty/members/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoyaltyMemberDto> getCustomerLoyaltyInfo(@PathVariable Long customerId) {
        return ResponseEntity.ok(loyaltyService.getCustomerLoyaltyInfo(customerId));
    }

    @GetMapping("/loyalty/members/{customerId}/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<LoyaltyTransaction>> getTransactionsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(loyaltyService.getTransactionsByCustomer(customerId, pageable));
    }

    @PostMapping("/loyalty/adjust-points")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoyaltyMemberDto> adjustPointsManually(
            @Valid @RequestBody AdjustLoyaltyPointsRequest request) {
        return ResponseEntity.ok(loyaltyService.adjustPointsManually(request));
    }

    // =========================================================================
    // 4. BẢNG GIÁ SỈ ĐA TẦNG (TIERED PRICING)
    // =========================================================================

    @GetMapping("/tiered-rules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TieredPriceRuleDto>> getAllTieredRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(tieredPricingService.getAllRules(pageable));
    }

    @PostMapping("/tiered-rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TieredPriceRuleDto> createTieredRule(
            @Valid @RequestBody CreateTieredPriceRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tieredPricingService.createRule(request));
    }

    @PutMapping("/tiered-rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TieredPriceRuleDto> updateTieredRule(
            @PathVariable Long id,
            @Valid @RequestBody CreateTieredPriceRuleRequest request) {
        return ResponseEntity.ok(tieredPricingService.updateRule(id, request));
    }

    @DeleteMapping("/tiered-rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTieredRule(@PathVariable Long id) {
        tieredPricingService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
