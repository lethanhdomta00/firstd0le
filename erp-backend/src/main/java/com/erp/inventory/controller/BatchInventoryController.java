package com.erp.inventory.controller;

import com.erp.inventory.dto.BatchAllocationDto;
import com.erp.inventory.dto.CreateBatchRequest;
import com.erp.inventory.dto.ExpiryRadarDto;
import com.erp.inventory.dto.ProductBatchDto;
import com.erp.inventory.service.BatchInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/batches")
@RequiredArgsConstructor
public class BatchInventoryController {

    private final BatchInventoryService batchService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<ProductBatchDto>> getAllBatches(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId) {
        return ResponseEntity.ok(batchService.getAllBatches(warehouseId, productId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<ProductBatchDto> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<ProductBatchDto> createBatch(@Valid @RequestBody CreateBatchRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(req));
    }

    @GetMapping("/radar")
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'ANALYTICS_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<ExpiryRadarDto> getExpiryRadar() {
        return ResponseEntity.ok(batchService.getExpiryRadar());
    }

    @GetMapping("/allocate")
    @PreAuthorize("hasAnyAuthority('ORDER_CREATE', 'PRODUCT_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<BatchAllocationDto> allocateBatches(
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "FEFO") String strategy) {
        return ResponseEntity.ok(batchService.allocateBatches(productId, quantity, strategy));
    }
}
