package com.erp.order.service;

import com.erp.model.Product;
import com.erp.order.dto.*;
import com.erp.order.model.*;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter DATE_CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(readOnly = true)
    public Page<OrderDto> searchOrders(
            String keyword,
            OrderStatus status,
            PaymentStatus paymentStatus,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        String safeSortBy = switch (sortBy != null ? sortBy : "createdAt") {
            case "orderCode", "customerName", "totalAmount", "status", "paymentStatus", "createdAt", "id" -> sortBy;
            default -> "createdAt";
        };
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(safeSortBy).ascending()
                : Sort.by(safeSortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), sort);

        return orderRepository.searchOrders(
                trimOrNull(keyword),
                status,
                paymentStatus,
                fromDate,
                toDate,
                pageable
        ).map(this::toOrderDto);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));
        return toOrderDto(order);
    }

    @Transactional(readOnly = true)
    public OrderStatsDto getOrderStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        com.erp.order.dto.OrderStatsAggregate agg = orderRepository.getOrderStatsAggregate(startOfToday);

        long totalOrders = agg.totalOrders();
        long pendingOrders = agg.pendingOrders();
        long confirmedOrders = agg.confirmedOrders();
        long shippingOrders = agg.shippingOrders();
        long completedOrders = agg.completedOrders();
        long cancelledOrders = agg.cancelledOrders();
        long todayOrders = agg.todayOrders();
        long activeCount = totalOrders - cancelledOrders;

        BigDecimal activeRevenue = agg.activeRevenue();
        BigDecimal completedRevenue = agg.completedRevenue();
        BigDecimal paidRevenue = agg.paidRevenue();

        BigDecimal avgOrderValue = activeCount > 0
                ? activeRevenue.divide(BigDecimal.valueOf(activeCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double completionRate = totalOrders > 0
                ? Math.round((completedOrders * 1000.0) / totalOrders) / 10.0
                : 0.0;

        return OrderStatsDto.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .shippingOrders(shippingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .todayOrders(todayOrders)
                .pendingFulfillmentCount(pendingOrders + confirmedOrders)
                .activeRevenue(activeRevenue)
                .completedRevenue(completedRevenue)
                .paidRevenue(paidRevenue)
                .avgOrderValue(avgOrderValue)
                .completionRate(completionRate)
                .build();
    }

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, String username) {
        Map<Long, Integer> mergedQuantities = mergeItemQuantities(request.getItems());

        Order order = new Order();
        order.setOrderCode(generateUniqueOrderCode());
        applyCustomerAndPaymentFields(order, request);
        order.setStatus(OrderStatus.PENDING);
        order.setStockDeducted(true);
        order.setCreatedByUsername(username);

        applyItemsAndDeductStock(order, mergedQuantities, username, "Tạo đơn hàng mới");

        Order saved = orderRepository.save(order);
        return toOrderDto(saved);
    }

    @Transactional
    public OrderDto updateOrder(Long id, CreateOrderRequest request, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Chỉ được phép chỉnh sửa thông tin & sản phẩm khi đơn hàng ở trạng thái Chờ xác nhận!");
        }

        if (order.isStockDeducted()) {
            restoreOrderStock(order, username, "Hoàn kho tạm do cập nhật lại đơn hàng " + order.getOrderCode());
        }

        order.clearItems();
        orderRepository.flush();

        applyCustomerAndPaymentFields(order, request);
        order.setStockDeducted(true);

        Map<Long, Integer> mergedQuantities = mergeItemQuantities(request.getItems());
        applyItemsAndDeductStock(order, mergedQuantities, username, "Cập nhật đơn hàng");

        Order saved = orderRepository.save(order);
        return toOrderDto(saved);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long id, OrderStatus newStatus, String note, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == newStatus) {
            return toOrderDto(order);
        }

        if (currentStatus == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Đơn hàng đã hoàn thành, không thể chuyển trạng thái!");
        }
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng đã bị hủy, không thể khôi phục trạng thái!");
        }
        if (newStatus == OrderStatus.PENDING) {
            throw new IllegalStateException("Không thể quay ngược về trạng thái Chờ xác nhận!");
        }
        if (currentStatus == OrderStatus.SHIPPING && newStatus == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Đơn hàng đang giao, không thể lùi về trạng thái Đã xác nhận!");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CONFIRMED && order.getConfirmedAt() == null) {
            order.setConfirmedAt(now);
        } else if (newStatus == OrderStatus.SHIPPING && order.getConfirmedAt() == null) {
            order.setConfirmedAt(now);
        } else if (newStatus == OrderStatus.COMPLETED) {
            if (order.getConfirmedAt() == null) {
                order.setConfirmedAt(now);
            }
            order.setCompletedAt(now);
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
            if (order.isStockDeducted()) {
                String cancelNote = (note != null && !note.trim().isEmpty())
                        ? "Hủy đơn " + order.getOrderCode() + ": " + note.trim()
                        : "Hoàn kho tự động do hủy đơn hàng " + order.getOrderCode();
                restoreOrderStock(order, username, cancelNote);
                order.setStockDeducted(false);
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }

        if (note != null && !note.trim().isEmpty()) {
            String existingNote = order.getNote() != null ? order.getNote().trim() : "";
            String appended = "[" + newStatus.getLabel() + "] " + note.trim();
            order.setNote(existingNote.isEmpty() ? appended : existingNote + "\n" + appended);
        }

        return toOrderDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (order.getStatus() == OrderStatus.CANCELLED && paymentStatus == PaymentStatus.PAID) {
            throw new IllegalStateException("Không thể đánh dấu Đã thanh toán cho đơn hàng đã hủy!");
        }

        order.setPaymentStatus(paymentStatus);
        return toOrderDto(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(Long id, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Chỉ có thể xóa đơn hàng ở trạng thái Chờ xác nhận hoặc Đã hủy!");
        }

        if (order.isStockDeducted()) {
            restoreOrderStock(order, username, "Hoàn kho do xóa đơn hàng " + order.getOrderCode());
        }

        orderRepository.delete(order);
    }

    @Transactional(readOnly = true)
    public Page<InventoryLogDto> searchInventoryLogs(
            String keyword,
            InventoryMovementType movementType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by("createdAt").descending().and(Sort.by("id").descending())
        );
        return inventoryLogRepository.searchLogs(trimOrNull(keyword), movementType, fromDate, toDate, pageable)
                .map(this::toInventoryLogDto);
    }

    @Transactional
    public InventoryLogDto adjustStock(StockAdjustRequest request, String username) {
        if (request.getQuantityDelta() == 0) {
            throw new IllegalArgumentException("Số lượng điều chỉnh phải khác 0!");
        }

        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm #" + request.getProductId()));

        int stockBefore = product.getStock() != null ? product.getStock() : 0;
        int stockAfter = stockBefore + request.getQuantityDelta();
        if (stockAfter < 0) {
            throw new IllegalArgumentException(
                    "Không thể trừ " + Math.abs(request.getQuantityDelta()) + " sản phẩm '" + product.getName()
                    + "' vì tồn kho hiện chỉ còn " + stockBefore + "!"
            );
        }

        product.setStock(stockAfter);
        productRepository.save(product);

        String refCode = "ADJ-" + LocalDate.now().format(DATE_CODE_FMT) + "-" + ThreadLocalRandom.current().nextInt(100, 999);
        String note = trimOrNull(request.getNote());
        if (note == null) {
            note = request.getQuantityDelta() > 0 ? "Nhập bổ sung tồn kho thủ công" : "Xuất/giảm trừ kiểm kê kho";
        }

        InventoryLog savedLog = recordInventoryLog(
                product,
                InventoryMovementType.MANUAL_ADJUST,
                request.getQuantityDelta(),
                stockBefore,
                stockAfter,
                refCode,
                note,
                username
        );

        return toInventoryLogDto(savedLog);
    }

    private Map<Long, Integer> mergeItemQuantities(List<OrderItemRequest> items) {
        // TreeMap keeps productIds sorted deterministically to prevent lock deadlocks
        Map<Long, Integer> merged = new TreeMap<>();
        for (OrderItemRequest req : items) {
            if (req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0!");
            }
            merged.merge(req.getProductId(), req.getQuantity(), Integer::sum);
        }
        return merged;
    }

    private void applyCustomerAndPaymentFields(Order order, CreateOrderRequest request) {
        order.setCustomerName(request.getCustomerName().trim());
        order.setCustomerPhone(trimOrNull(request.getCustomerPhone()));
        order.setCustomerEmail(trimOrNull(request.getCustomerEmail()));
        order.setShippingAddress(trimOrNull(request.getShippingAddress()));
        order.setNote(trimOrNull(request.getNote()));
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CASH);
        order.setPaymentStatus(request.getPaymentStatus() != null ? request.getPaymentStatus() : PaymentStatus.UNPAID);
        order.setDiscountAmount(sanitizeNonNegative(request.getDiscountAmount()));
        order.setTaxRate(sanitizeTaxRate(request.getTaxRate()));
    }

    private void applyItemsAndDeductStock(Order order, Map<Long, Integer> qtyByProduct, String username, String logPrefix) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm ID #" + productId + " không tồn tại!"));

            int stockBefore = product.getStock() != null ? product.getStock() : 0;
            if (stockBefore < qty) {
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' (" + product.getSku() + ") chỉ còn "
                        + stockBefore + " trong kho, không đủ cho số lượng đặt " + qty + "!"
                );
            }

            int stockAfter = stockBefore - qty;
            product.setStock(stockAfter);
            productRepository.save(product);

            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setProductImageUrl(product.getImageUrl());
            item.setUnitPrice(unitPrice);
            item.setQuantity(qty);
            item.setLineTotal(lineTotal);
            order.addItem(item);

            recordInventoryLog(
                    product,
                    InventoryMovementType.EXPORT_ORDER,
                    -qty,
                    stockBefore,
                    stockAfter,
                    order.getOrderCode(),
                    logPrefix + " (" + order.getCustomerName() + ")",
                    username
            );
        }

        BigDecimal discount = order.getDiscountAmount();
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
            order.setDiscountAmount(discount);
        }

        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal taxAmount = taxableAmount
                .multiply(order.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
    }

    private void restoreOrderStock(Order order, String username, String reason) {
        Map<Long, Integer> qtyByProduct = new TreeMap<>();
        for (OrderItem item : order.getItems()) {
            qtyByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            productRepository.findByIdForUpdate(productId).ifPresent(product -> {
                int stockBefore = product.getStock() != null ? product.getStock() : 0;
                int stockAfter = stockBefore + qty;
                product.setStock(stockAfter);
                productRepository.save(product);

                recordInventoryLog(
                        product,
                        InventoryMovementType.IMPORT_CANCEL,
                        qty,
                        stockBefore,
                        stockAfter,
                        order.getOrderCode(),
                        reason,
                        username
                );
            });
        }
    }

    private InventoryLog recordInventoryLog(
            Product product,
            InventoryMovementType type,
            int delta,
            int before,
            int after,
            String referenceCode,
            String note,
            String actor
    ) {
        InventoryLog log = new InventoryLog();
        log.setProductId(product.getId());
        log.setProductName(product.getName());
        log.setProductSku(product.getSku());
        log.setMovementType(type);
        log.setQuantityDelta(delta);
        log.setStockBefore(before);
        log.setStockAfter(after);
        log.setReferenceCode(referenceCode);
        log.setNote(note);
        log.setActorUsername(actor);
        return inventoryLogRepository.save(log);
    }

    private String generateUniqueOrderCode() {
        String datePart = LocalDate.now().format(DATE_CODE_FMT);
        for (int attempt = 0; attempt < 20; attempt++) {
            int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
            String code = "ORD-" + datePart + "-" + rand;
            if (!orderRepository.existsByOrderCode(code)) {
                return code;
            }
        }
        return "ORD-" + datePart + "-" + (System.currentTimeMillis() % 100000);
    }

    private BigDecimal sanitizeNonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sanitizeTaxRate(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public OrderDto toOrderDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .productImageUrl(item.getProductImageUrl())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        int totalQuantity = order.getItems().stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();

        return OrderDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerEmail(order.getCustomerEmail())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .status(order.getStatus())
                .statusLabel(order.getStatus().getLabel())
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusLabel(order.getPaymentStatus().getLabel())
                .paymentMethod(order.getPaymentMethod())
                .paymentMethodLabel(order.getPaymentMethod().getLabel())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxRate(order.getTaxRate())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .stockDeducted(order.isStockDeducted())
                .createdByUsername(order.getCreatedByUsername())
                .confirmedAt(order.getConfirmedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .totalQuantity(totalQuantity)
                .items(itemDtos)
                .build();
    }

    public InventoryLogDto toInventoryLogDto(InventoryLog log) {
        return InventoryLogDto.builder()
                .id(log.getId())
                .productId(log.getProductId())
                .productName(log.getProductName())
                .productSku(log.getProductSku())
                .movementType(log.getMovementType())
                .movementTypeLabel(log.getMovementType().getLabel())
                .quantityDelta(log.getQuantityDelta())
                .stockBefore(log.getStockBefore())
                .stockAfter(log.getStockAfter())
                .referenceCode(log.getReferenceCode())
                .note(log.getNote())
                .actorUsername(log.getActorUsername())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
