package com.erp.inventory.controller;

import com.erp.inventory.dto.CreateSerialRequest;
import com.erp.inventory.dto.ProductSerialDto;
import com.erp.inventory.dto.UpdateSerialStatusRequest;
import com.erp.inventory.model.SerialStatus;
import com.erp.inventory.service.SerialTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/serials")
@RequiredArgsConstructor
public class SerialTrackingController {

    private final SerialTrackingService serialService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'BRANCH_VIEW', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<ProductSerialDto>> getAllSerials(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) SerialStatus status,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(serialService.getAllSerials(productId, warehouseId, status, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<ProductSerialDto> getSerialById(@PathVariable Long id) {
        return ResponseEntity.ok(serialService.getSerialById(id));
    }

    @GetMapping("/by-number/{serialNumber}")
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<ProductSerialDto> getSerialByNumber(@PathVariable String serialNumber) {
        return ResponseEntity.ok(serialService.getSerialByNumber(serialNumber));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<ProductSerialDto> createSerial(@Valid @RequestBody CreateSerialRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serialService.createSerial(req));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAnyAuthority('PRODUCT_STOCK', 'BRANCH_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<List<ProductSerialDto>> batchCreateSerials(@Valid @RequestBody List<CreateSerialRequest> list) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serialService.batchCreateSerials(list));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ORDER_STATUS', 'ORDER_CREATE', 'PRODUCT_STOCK', 'ROLE_ADMIN')")
    public ResponseEntity<ProductSerialDto> updateSerialStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSerialStatusRequest req) {
        return ResponseEntity.ok(serialService.updateSerialStatus(id, req));
    }
}
