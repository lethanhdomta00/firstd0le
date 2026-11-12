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

    @GetMapping
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
    public ResponseEntity<OrderStatsDto> getStats() {
        return ResponseEntity.ok(orderService.getOrderStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.updateOrder(id, request, username));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus(), request.getNote(), username));
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<OrderDto> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, request.getPaymentStatus()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        orderService.deleteOrder(id, username);
        return ResponseEntity.ok(Map.of("message", "Đã xóa đơn hàng thành công!"));
    }

    @GetMapping("/inventory-logs")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryLogDto> adjustInventory(
            @Valid @RequestBody StockAdjustRequest request,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "system";
        return ResponseEntity.ok(orderService.adjustStock(request, username));
    }

    @GetMapping("/export-excel")
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

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBusinessException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
