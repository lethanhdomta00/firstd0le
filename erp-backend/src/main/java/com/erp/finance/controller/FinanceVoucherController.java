package com.erp.finance.controller;

import com.erp.finance.dto.CreateFinanceVoucherRequest;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.model.CostCenter;
import com.erp.finance.model.VoucherStatus;
import com.erp.finance.model.VoucherType;
import com.erp.finance.service.FinanceVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/finance/vouchers")
@RequiredArgsConstructor
public class FinanceVoucherController {

    private final FinanceVoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Page<FinanceVoucherDto>> searchVouchers(
            @RequestParam(required = false) VoucherType voucherType,
            @RequestParam(required = false) VoucherStatus status,
            @RequestParam(required = false) CostCenter costCenter,
            @RequestParam(required = false) Long sourceAccountId,
            @RequestParam(required = false) Long targetAccountId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(voucherService.searchVouchers(voucherType, status, costCenter, sourceAccountId, targetAccountId, keyword, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<FinanceVoucherDto> getVoucherById(@PathVariable Long id) {
        return ResponseEntity.ok(voucherService.getVoucherById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinanceVoucherDto> createVoucher(
            @Valid @RequestBody CreateFinanceVoucherRequest req,
            Authentication auth
    ) {
        String username = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(voucherService.createVoucher(req, username));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<FinanceVoucherDto> approveVoucher(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(voucherService.approveVoucher(id, username));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinanceVoucherDto> cancelVoucher(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(voucherService.cancelVoucher(id, username));
    }
}
