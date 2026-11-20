package com.erp.finance.controller;

import com.erp.finance.dto.CreateTreasuryAccountRequest;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.dto.InternalTransferRequest;
import com.erp.finance.dto.TreasuryAccountDto;
import com.erp.finance.service.TreasuryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class TreasuryController {

    private final TreasuryService treasuryService;

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<TreasuryAccountDto>> getAllAccounts() {
        return ResponseEntity.ok(treasuryService.getAllActiveAccounts());
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<TreasuryAccountDto> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(treasuryService.getAccountById(id));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<TreasuryAccountDto> createAccount(@Valid @RequestBody CreateTreasuryAccountRequest req) {
        return ResponseEntity.ok(treasuryService.createAccount(req));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinanceVoucherDto> transferInternal(
            @Valid @RequestBody InternalTransferRequest req,
            Authentication auth
    ) {
        String username = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(treasuryService.transferInternal(req, username));
    }
}
