package com.erp.branch.controller;

import com.erp.branch.dto.*;
import com.erp.branch.service.BranchWarehouseService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class BranchWarehouseController {

    private final BranchWarehouseService branchWarehouseService;

    // ==========================================
    // 1. CHI NHÁNH (BRANCH)
    // ==========================================

    @GetMapping("/branches")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW', 'USER_VIEW', 'ORDER_VIEW', 'PRODUCT_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        return ResponseEntity.ok(branchWarehouseService.getAllBranches());
    }

    @GetMapping("/branches/{id}")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<BranchDto> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(branchWarehouseService.getBranchById(id));
    }

    @PostMapping("/branches")
    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<BranchDto> createBranch(@Valid @RequestBody CreateBranchRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchWarehouseService.createBranch(req));
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<BranchDto> updateBranch(@PathVariable Long id, @Valid @RequestBody CreateBranchRequest req) {
        return ResponseEntity.ok(branchWarehouseService.updateBranch(id, req));
    }

    @PatchMapping("/branches/{id}/toggle-status")
    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<BranchDto> toggleBranchStatus(@PathVariable Long id) {
        return ResponseEntity.ok(branchWarehouseService.toggleBranchStatus(id));
    }

    // ==========================================
    // 2. KHO HÀNG (WAREHOUSE)
    // ==========================================

    @GetMapping("/warehouses")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW', 'PRODUCT_VIEW', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<WarehouseDto>> getAllWarehouses(@RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(branchWarehouseService.getAllWarehouses(branchId));
    }

    @GetMapping("/warehouses/{id}")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW', 'PRODUCT_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<WarehouseDto> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(branchWarehouseService.getWarehouseById(id));
    }

    @PostMapping("/warehouses")
    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<WarehouseDto> createWarehouse(@Valid @RequestBody CreateWarehouseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchWarehouseService.createWarehouse(req));
    }

    @PutMapping("/warehouses/{id}")
    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<WarehouseDto> updateWarehouse(@PathVariable Long id, @Valid @RequestBody CreateWarehouseRequest req) {
        return ResponseEntity.ok(branchWarehouseService.updateWarehouse(id, req));
    }

    // ==========================================
    // 3. TỒN KHO THEO KHO (WAREHOUSE STOCKS)
    // ==========================================

    @GetMapping("/warehouses/stocks")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW', 'PRODUCT_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<WarehouseStockDto>> getWarehouseStocks(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(branchWarehouseService.getWarehouseStocks(warehouseId, branchId, query));
    }

    @GetMapping("/warehouses/multi-stock/{productId}")
    @PreAuthorize("hasAnyAuthority('BRANCH_VIEW', 'PRODUCT_VIEW', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<MultiWarehouseStockDto> getMultiWarehouseStock(@PathVariable Long productId) {
        return ResponseEntity.ok(branchWarehouseService.getMultiWarehouseStock(productId));
    }

    @PostMapping("/warehouses/stocks/adjust")
    @PreAuthorize("hasAnyAuthority('BRANCH_MANAGE', 'PRODUCT_STOCK', 'ROLE_ADMIN')")
    public ResponseEntity<WarehouseStockDto> adjustStock(
            @Valid @RequestBody AdjustStockRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(branchWarehouseService.adjustStock(req, username));
    }
}
