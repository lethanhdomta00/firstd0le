package com.erp.order.controller;

import com.erp.order.dto.*;
import com.erp.order.model.InventoryMovementType;
import com.erp.order.model.OrderStatus;
import com.erp.order.model.PaymentStatus;
import com.erp.order.service.OrderExportService;
import com.erp.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderExportService orderExportService;
    private final com.erp.order.service.VietQrService vietQrService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<Page<OrderDto>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(orderService.searchOrders(
                keyword, status, paymentStatus, fromDate, toDate, page, size, sortBy, sortDir
        ));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ORDER_VIEW', 'ANALYTICS_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<OrderStatsDto> getStats() {
        return ResponseEntity.ok(orderService.getOrderStats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ORDER_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, username));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORDER_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.updateOrder(id, request, username));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ORDER_STATUS', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus(), request.getNote(), username));
    }

    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyAuthority('ORDER_PAY', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, request.getPaymentStatus()));
    }

    @PatchMapping("/{id}/approve-credit")
    @PreAuthorize("hasAnyAuthority('ORDER_STATUS', 'ORDER_PAY', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> approveCredit(
            @PathVariable Long id,
            @RequestParam boolean approved,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.approveCredit(id, username, approved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ORDER_CANCEL', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        orderService.deleteOrder(id, username);
        return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng thành công!"));
    }

    @GetMapping("/inventory-logs")
    @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<Page<InventoryLogDto>> getInventoryLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) InventoryMovementType movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return ResponseEntity.ok(orderService.searchInventoryLogs(keyword, movementType, fromDate, toDate, page, size));
    }

    @PostMapping("/inventory-adjust")
    @PreAuthorize("hasAnyAuthority('INVENTORY_ADJUST', 'ROLE_ADMIN')")
    public ResponseEntity<InventoryLogDto> adjustInventory(
            @Valid @RequestBody StockAdjustRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.adjustStock(request, username));
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAnyAuthority('ORDER_EXPORT', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus
    ) {
        byte[] content = orderExportService.exportOrdersExcelXml(keyword, status, paymentStatus);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"erp-orders-report.xls\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel; charset=UTF-8"))
                .body(content);
    }

    @GetMapping("/export-csv")
    @PreAuthorize("hasAnyAuthority('ORDER_EXPORT', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus
    ) {
        byte[] content = orderExportService.exportOrdersCsv(keyword, status, paymentStatus);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"erp-orders.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(content);
    }

    // =========================================================================
    //                        CỔNG THANH TOÁN VIETQR & NAPAS 24/7
    // =========================================================================

    @GetMapping("/{id}/vietqr")
    @PreAuthorize("hasAnyAuthority('ORDER_PAY', 'ORDER_VIEW', 'ROLE_ADMIN')")
    public ResponseEntity<VietQrPaymentDto> getVietQr(@PathVariable Long id) {
        return ResponseEntity.ok(vietQrService.generateVietQrForOrder(id));
    }

    @PostMapping("/{id}/pay-vietqr-mock")
    @PreAuthorize("hasAnyAuthority('ORDER_PAY', 'ROLE_ADMIN')")
    public ResponseEntity<OrderDto> payVietQrMock(@PathVariable Long id) {
        vietQrService.processPaymentSuccess(id, "TXN-MOCK-" + System.currentTimeMillis(), "VIETQR_MOCK");
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping("/payment-webhook")
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(@RequestBody PaymentWebhookRequest request) {
        return ResponseEntity.ok(vietQrService.handlePaymentWebhook(request));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBusinessException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
