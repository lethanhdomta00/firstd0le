package com.erp.inventory.controller;

import com.erp.inventory.dto.CreateStocktakeRequest;
import com.erp.inventory.dto.RecordCountRequest;
import com.erp.inventory.dto.StocktakeAuditDto;
import com.erp.inventory.model.StocktakeStatus;
import com.erp.inventory.service.StocktakeAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/stocktakes")
@RequiredArgsConstructor
public class StocktakeAuditController {

    private final StocktakeAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<StocktakeAuditDto>> getAllAudits(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StocktakeStatus status) {
        return ResponseEntity.ok(auditService.getAllAudits(warehouseId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<StocktakeAuditDto> getAuditById(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getAuditById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<StocktakeAuditDto> createAudit(
            @Valid @RequestBody CreateStocktakeRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(auditService.createAudit(req, username));
    }

    @PostMapping("/{id}/counts")
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<StocktakeAuditDto> recordCounts(
            @PathVariable Long id,
            @Valid @RequestBody RecordCountRequest req) {
        return ResponseEntity.ok(auditService.recordCounts(id, req));
    }

    @PostMapping("/{id}/balance")
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<StocktakeAuditDto> balanceAndComplete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(auditService.balanceAndComplete(id, username));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<StocktakeAuditDto> cancelAudit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(auditService.cancelAudit(id, username));
    }
}
