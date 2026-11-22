package com.erp.procurement.controller;

import com.erp.procurement.dto.*;
import com.erp.procurement.model.PurchaseOrderStatus;
import com.erp.procurement.service.GoodsReceiptService;
import com.erp.procurement.service.ProcurementPaymentService;
import com.erp.procurement.service.PurchaseOrderService;
import com.erp.procurement.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
@Slf4j
public class ProcurementController {

    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;
    private final GoodsReceiptService goodsReceiptService;
    private final ProcurementPaymentService procurementPaymentService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "admin";
    }

    // =========================================================================
    // 1. TỔNG QUAN & THỐNG KÊ (OVERVIEW)
    // =========================================================================
    @GetMapping("/overview")
    public ResponseEntity<ProcurementOverviewDto> getOverview() {
        return ResponseEntity.ok(procurementPaymentService.getProcurementOverview());
    }

    // =========================================================================
    // 2. NHÀ CUNG CẤP & XƯỞNG MAY (SUPPLIERS SRM)
    // =========================================================================
    @GetMapping("/suppliers")
    public ResponseEntity<Page<SupplierDto>> searchSuppliers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(supplierService.searchSuppliers(keyword, status, pageable));
    }

    @GetMapping("/suppliers/active")
    public ResponseEntity<List<SupplierDto>> getAllActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getAllActiveSuppliers());
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierDto> createSupplier(@Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity.ok(supplierService.createSupplier(request));
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody CreateSupplierRequest request
    ) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Map<String, Object>> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa nhà cung cấp thành công"));
    }

    @GetMapping("/suppliers/top-debtors")
    public ResponseEntity<List<SupplierDto>> getTopDebtors() {
        return ResponseEntity.ok(supplierService.getTopDebtors());
    }

    // =========================================================================
    // 3. ĐƠN ĐẶT HÀNG MUA (PURCHASE ORDERS - PO)
    // =========================================================================
    @GetMapping("/purchase-orders")
    public ResponseEntity<Page<PurchaseOrderDto>> searchPurchaseOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(purchaseOrderService.searchPurchaseOrders(keyword, status, startDate, endDate, pageable));
    }

    @GetMapping("/purchase-orders/{id}")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.ok(purchaseOrderService.createPurchaseOrder(request, getCurrentUsername()));
    }

    @PutMapping("/purchase-orders/{id}/submit")
    public ResponseEntity<PurchaseOrderDto> submitPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.submitForApproval(id, getCurrentUsername()));
    }

    @PutMapping("/purchase-orders/{id}/approve")
    public ResponseEntity<PurchaseOrderDto> approvePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.approvePurchaseOrder(id, getCurrentUsername()));
    }

    @PutMapping("/purchase-orders/{id}/cancel")
    public ResponseEntity<PurchaseOrderDto> cancelPurchaseOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(purchaseOrderService.cancelPurchaseOrder(id, reason, getCurrentUsername()));
    }

    // =========================================================================
    // 4. NHẬP KHO KIỂM NHẬN (GRN) & GIÁ VỐN BÌNH QUÂN GIA QUYỀN
    // =========================================================================
    @GetMapping("/goods-receipts")
    public ResponseEntity<Page<GoodsReceiptNoteDto>> searchReceiptNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receiptDate"));
        return ResponseEntity.ok(goodsReceiptService.searchReceiptNotes(keyword, pageable));
    }

    @GetMapping("/goods-receipts/{id}")
    public ResponseEntity<GoodsReceiptNoteDto> getReceiptNoteById(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.getReceiptNoteById(id));
    }

    @GetMapping("/goods-receipts/by-po/{poId}")
    public ResponseEntity<List<GoodsReceiptNoteDto>> getReceiptNotesByPoId(@PathVariable Long poId) {
        return ResponseEntity.ok(goodsReceiptService.getReceiptNotesByPoId(poId));
    }

    @PostMapping("/goods-receipts")
    public ResponseEntity<GoodsReceiptNoteDto> createGoodsReceipt(@Valid @RequestBody CreateGoodsReceiptRequest request) {
        return ResponseEntity.ok(goodsReceiptService.createGoodsReceipt(request, getCurrentUsername()));
    }

    @PostMapping("/goods-receipts/simulate-cost")
    public ResponseEntity<Map<String, Object>> simulateCost(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantityReceived,
            @RequestParam(required = false) BigDecimal importUnitPrice
    ) {
        return ResponseEntity.ok(goodsReceiptService.simulateCostCalculation(productId, quantityReceived, importUnitPrice));
    }

    // =========================================================================
    // 5. THANH TOÁN CÔNG NỢ NHÀ CUNG CẤP (AP & PAYMENTS)
    // =========================================================================
    @PostMapping("/payments/pay-debt")
    public ResponseEntity<Map<String, Object>> paySupplierDebt(@Valid @RequestBody PaySupplierDebtRequest request) {
        return ResponseEntity.ok(procurementPaymentService.paySupplierDebt(request, getCurrentUsername()));
    }
}
