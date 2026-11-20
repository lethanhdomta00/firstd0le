package com.erp.finance.controller;

import com.erp.finance.dto.BankStatementItemDto;
import com.erp.finance.dto.FinanceVoucherDto;
import com.erp.finance.dto.UploadBankStatementRequest;
import com.erp.finance.model.ReconciliationStatus;
import com.erp.finance.service.BankReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/reconciliation")
@RequiredArgsConstructor
public class BankReconciliationController {

    private final BankReconciliationService reconciliationService;

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<Page<BankStatementItemDto>> getStatementItems(
            @RequestParam(required = false) ReconciliationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(reconciliationService.getStatementItems(status, keyword, pageable));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<List<BankStatementItemDto>> uploadStatement(
            @Valid @RequestBody UploadBankStatementRequest req
    ) {
        return ResponseEntity.ok(reconciliationService.parseAndSaveStatements(req));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<FinanceVoucherDto> reconcileSingleItem(
            @PathVariable Long id,
            @RequestParam Long targetBankAccountId,
            Authentication auth
    ) {
        String username = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(reconciliationService.reconcileItem(id, targetBankAccountId, username));
    }

    @PostMapping("/reconcile-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Map<String, Object>> reconcileAllMatched(
            @RequestParam Long targetBankAccountId,
            Authentication auth
    ) {
        String username = auth != null ? auth.getName() : "admin";
        return ResponseEntity.ok(reconciliationService.reconcileAllMatched(targetBankAccountId, username));
    }
}
