package com.erp.branch.controller;

import com.erp.branch.dto.*;
import com.erp.branch.model.TransferStatus;
import com.erp.branch.service.InternalTransferService;
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
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class InternalTransferController {

    private final InternalTransferService transferService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TRANSFER_VIEW', 'BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<List<InternalTransferDto>> getAllTransfers(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(transferService.getAllTransfers(status, warehouseId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TRANSFER_VIEW', 'BRANCH_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<InternalTransferDto> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getTransferById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TRANSFER_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<InternalTransferDto> createTransfer(
            @Valid @RequestBody CreateTransferRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.createTransfer(req, username));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAnyAuthority('TRANSFER_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<InternalTransferDto> dispatchTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(transferService.dispatchTransfer(id, username));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyAuthority('TRANSFER_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<InternalTransferDto> receiveTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ReceiveTransferRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(transferService.receiveTransfer(id, req, username));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('TRANSFER_MANAGE', 'ROLE_ADMIN')")
    public ResponseEntity<InternalTransferDto> cancelTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : "system";
        return ResponseEntity.ok(transferService.cancelTransfer(id, username));
    }
}
