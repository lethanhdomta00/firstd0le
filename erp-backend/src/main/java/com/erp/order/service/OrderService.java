package com.erp.order.service;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.order.dto.*;
import com.erp.order.model.*;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

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

    /**
     * 1. Tạo đơn hàng mới:
     * - Áp dụng Atomic Stock Reservation (chống bán lố Race Condition)
     * - Đóng băng giá vốn snapshot_cost_price từ moving_avg_cost
     * - Kiểm soát hạn mức công nợ Credit Gatekeeper
     */
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, String username) {
        Map<Long, Integer> mergedQuantities = mergeItemQuantities(request.getItems());

        Order order = new Order();
        order.setOrderCode(generateUniqueOrderCode());
        applyCustomerAndPaymentFields(order, request);
        order.setStatus(OrderStatus.PENDING);
        order.setStockDeducted(true);
        order.setCreatedByUsername(username);

        // Áp dụng sản phẩm, tính toán giá vốn & giữ chỗ tồn kho nguyên tử
        applyItemsAndReserveStock(order, mergedQuantities, username, "Tạo đơn hàng mới (Giữ chỗ)");

        // Kiểm tra Hạn mức nợ của Khách hàng (Credit Gatekeeper)
        checkAndApplyCustomerCreditLimit(order);

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

        // Giải phóng giữ chỗ cũ trước khi áp dụng mới
        if (order.isStockDeducted()) {
            releaseOrderReservation(order, username, "Hoàn giữ chỗ tạm do cập nhật lại đơn hàng " + order.getOrderCode());
        }

        order.clearItems();
        orderRepository.flush();

        applyCustomerAndPaymentFields(order, request);
        order.setStockDeducted(true);

        Map<Long, Integer> mergedQuantities = mergeItemQuantities(request.getItems());
        applyItemsAndReserveStock(order, mergedQuantities, username, "Cập nhật đơn hàng (Giữ chỗ mới)");
        checkAndApplyCustomerCreditLimit(order);

        Order saved = orderRepository.save(order);
        return toOrderDto(saved);
    }

    /**
     * Cập nhật trạng thái đơn hàng (State Machine):
     * - PENDING -> CONFIRMED/SHIPPING: Xuất kho thực tế (trừ physicalStock và reservedStock, ghi thẻ kho EXPORT_ORDER)
     * - PENDING -> CANCELLED: Giải phóng giữ chỗ (trừ reservedStock, trả lại availableStock)
     * - Đã xuất kho -> CANCELLED: Hoàn kho vật lý (cộng lại physicalStock, ghi thẻ kho IMPORT_CANCEL)
     */
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

        // Kiểm tra chặn xuất kho nếu đang bị treo duyệt công nợ
        if ((newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.SHIPPING)
                && order.isCreditExceeded() && "PENDING".equalsIgnoreCase(order.getCreditApprovalStatus())) {
            throw new IllegalStateException("Đơn hàng đang vượt hạn mức công nợ đại lý! Cần được Giám đốc/Kế toán duyệt công nợ trước khi xuất kho.");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.SHIPPING) {
            if (order.getConfirmedAt() == null) {
                order.setConfirmedAt(now);
            }
            // Xuất kho thực tế: chuyển từ Tồn giữ chỗ sang Xuất kho vật lý
            deductPhysicalStockForOrder(order, username, "Xuất kho giao hàng theo đơn " + order.getOrderCode());
        } else if (newStatus == OrderStatus.COMPLETED) {
            if (order.getConfirmedAt() == null) {
                order.setConfirmedAt(now);
            }
            order.setCompletedAt(now);
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
            // Xử lý hoàn kho tùy thuộc vào trạng thái trước khi hủy
            handleCancelOrderStock(order, currentStatus, username, note);
            order.setStockDeducted(false);

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

    /**
     * Phê duyệt công nợ cho đơn hàng bị treo hạn mức
     */
    @Transactional
    public OrderDto approveCredit(Long orderId, String username, boolean approved) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (!order.isCreditExceeded()) {
            throw new IllegalStateException("Đơn hàng này không thuộc diện kiểm soát hạn mức nợ!");
        }

        order.setCreditApprovalStatus(approved ? "APPROVED" : "REJECTED");
        String note = "[" + (approved ? "ĐÃ DUYỆT CÔNG NỢ" : "TỪ CHỐI CÔNG NỢ") + "] Người duyệt: " + username;
        order.setNote(order.getNote() != null ? order.getNote() + "\n" + note : note);

        return toOrderDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (order.getStatus() == OrderStatus.CANCELLED && paymentStatus == PaymentStatus.PAID) {
            throw new IllegalStateException("Không thể đánh dấu Đã thanh toán cho đơn hàng đã hủy!");
        }

        PaymentStatus oldPaymentStatus = order.getPaymentStatus();
        order.setPaymentStatus(paymentStatus);

        // Nếu chuyển sang PAID: Gạch nợ khách hàng nếu có
        if (oldPaymentStatus != PaymentStatus.PAID && paymentStatus == PaymentStatus.PAID) {
            if (order.getCustomerPhone() != null) {
                customerRepository.findByPhone(order.getCustomerPhone())
                        .ifPresent(c -> customerRepository.decreaseDebt(c.getId(), order.getTotalAmount()));
            }
        }

        return toOrderDto(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(Long id, String username) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Chỉ có thể xóa đơn hàng ở trạng thái Chờ xác nhận hoặc Đã hủy!");
        }

        if (order.isStockDeducted() && order.getStatus() == OrderStatus.PENDING) {
            releaseOrderReservation(order, username, "Hủy giữ chỗ do xóa đơn hàng " + order.getOrderCode());
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
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        return inventoryLogRepository.searchLogs(
                trimOrNull(keyword),
                movementType,
                fromDate,
                toDate,
                pageable
        ).map(this::toInventoryLogDto);
    }

    /**
     * Điều chỉnh tồn kho thủ công & Tự động tính lại Giá vốn MAC nếu có nhập kho
     */
    @Transactional
    public InventoryLogDto adjustStock(StockAdjustRequest request, String username) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm ID #" + request.getProductId() + " không tồn tại!"));

        int delta = request.getQuantityDelta();
        if (delta == 0) {
            throw new IllegalArgumentException("Số lượng điều chỉnh kho phải khác 0!");
        }

        int physicalBefore = product.getPhysicalStock() != null ? product.getPhysicalStock() : (product.getStock() != null ? product.getStock() : 0);
        int physicalAfter = physicalBefore + delta;

        if (physicalAfter < 0) {
            throw new IllegalArgumentException(
                    "Không thể điều chỉnh! Tồn kho thực tế hiện tại là " + physicalBefore
                    + ", trừ " + Math.abs(delta) + " sẽ làm âm kho."
            );
        }

        // Thuật toán Moving Average Cost (MAC) khi nhập kho
        if (delta > 0 && request.getInboundCostPrice() != null && request.getInboundCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentMac = product.getMovingAvgCost() != null ? product.getMovingAvgCost() : BigDecimal.ZERO;
            BigDecimal inboundPrice = request.getInboundCostPrice();

            BigDecimal totalOldValue = currentMac.multiply(BigDecimal.valueOf(physicalBefore));
            BigDecimal totalNewInbound = inboundPrice.multiply(BigDecimal.valueOf(delta));
            BigDecimal newTotalValue = totalOldValue.add(totalNewInbound);

            BigDecimal newMac = newTotalValue.divide(BigDecimal.valueOf(physicalAfter), 2, RoundingMode.HALF_UP);
            product.setMovingAvgCost(newMac);
            log.info("Sản phẩm [{}] cập nhật Giá vốn MAC mới: {} đ (Cũ: {} đ, Nhập mới: {} đ)", product.getSku(), newMac, currentMac, inboundPrice);
        }

        product.setPhysicalStock(physicalAfter);
        product.syncStockFields();
        productRepository.save(product);

        InventoryMovementType type = delta > 0 ? InventoryMovementType.MANUAL_ADJUST : InventoryMovementType.EXPORT_ORDER;
        String note = (request.getNote() != null && !request.getNote().trim().isEmpty())
                ? request.getNote().trim()
                : "Điều chỉnh kho thủ công";

        InventoryLog savedLog = recordInventoryLog(
                product,
                type,
                delta,
                physicalBefore,
                physicalAfter,
                "ADJUST-" + System.currentTimeMillis() % 100000,
                note,
                username
        );

        return toInventoryLogDto(savedLog);
    }

    // =========================================================================
    //                        CƠ CHẾ KHO & GIÁ VỐN NỘI BỘ
    // =========================================================================

    /**
     * Giữ chỗ tồn kho nguyên tử (Atomic Stock Reservation) & Đóng băng giá vốn MAC
     */
    private void applyItemsAndReserveStock(Order order, Map<Long, Integer> qtyByProduct, String username, String logPrefix) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCostAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : qtyByProduct.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm ID #" + productId + " không tồn tại!"));

            // 1. Giữ chỗ tồn kho nguyên tử (Atomic Check & Update)
            int updatedRows = productRepository.reserveStockAtomic(productId, qty);
            if (updatedRows == 0) {
                int available = product.getAvailableStock();
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' (" + product.getSku() + ") chỉ còn "
                        + available + " tồn khả dụng, không đủ để giữ chỗ số lượng " + qty + "!"
                );
            }

            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            // 2. Đóng băng giá vốn Moving Average Cost
            BigDecimal snapshotCost = product.getMovingAvgCost() != null ? product.getMovingAvgCost() : BigDecimal.ZERO;
            BigDecimal lineCost = snapshotCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            totalCostAmount = totalCostAmount.add(lineCost);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setProductImageUrl(product.getImageUrl());
            item.setUomName(product.getBaseUnit() != null ? product.getBaseUnit() : "Cái");
            item.setUomConversionRate(BigDecimal.ONE);
            item.setUnitPrice(unitPrice);
            item.setQuantity(qty);
            item.setLineTotal(lineTotal);
            item.setSnapshotCostPrice(snapshotCost);
            item.calculateGrossProfit();
            order.addItem(item);
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
        order.setTotalCostAmount(totalCostAmount);
        order.setGrossProfit(totalAmount.subtract(totalCostAmount).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Xuất kho thực tế (trừ Physical Stock và Reserved Stock, ghi nhận Thẻ kho)
     */
    private void deductPhysicalStockForOrder(Order order, String username, String note) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int physicalBefore = product.getPhysicalStock() != null ? product.getPhysicalStock() : 0;
                int qty = item.getQuantity();

                productRepository.deductPhysicalStock(product.getId(), qty);
                int physicalAfter = Math.max(0, physicalBefore - qty);

                recordInventoryLog(
                        product,
                        InventoryMovementType.EXPORT_ORDER,
                        -qty,
                        physicalBefore,
                        physicalAfter,
                        order.getOrderCode(),
                        note,
                        username
                );
            });
        }
    }

    /**
     * Giải phóng giữ chỗ (Khi Hủy đơn ở trạng thái PENDING)
     */
    private void releaseOrderReservation(Order order, String username, String reason) {
        for (OrderItem item : order.getItems()) {
            productRepository.releaseReservedStock(item.getProductId(), item.getQuantity());
        }
    }

    /**
     * Xử lý hoàn kho khi Hủy đơn
     */
    private void handleCancelOrderStock(Order order, OrderStatus previousStatus, String username, String reason) {
        String logNote = (reason != null && !reason.trim().isEmpty())
                ? "Hủy đơn " + order.getOrderCode() + ": " + reason.trim()
                : "Hủy đơn hàng " + order.getOrderCode();

        if (previousStatus == OrderStatus.PENDING) {
            // Mới chỉ giữ chỗ, chưa xuất kho -> Giải phóng giữ chỗ
            releaseOrderReservation(order, username, logNote);
        } else {
            // Đã xuất kho vật lý -> Hoàn trả lại hàng vào kệ kho
            for (OrderItem item : order.getItems()) {
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    int physicalBefore = product.getPhysicalStock() != null ? product.getPhysicalStock() : 0;
                    int qty = item.getQuantity();

                    productRepository.restorePhysicalStock(product.getId(), qty);
                    int physicalAfter = physicalBefore + qty;

                    recordInventoryLog(
                            product,
                            InventoryMovementType.IMPORT_CANCEL,
                            qty,
                            physicalBefore,
                            physicalAfter,
                            order.getOrderCode(),
                            logNote,
                            username
                    );
                });
            }
        }
    }

    /**
     * Kiểm tra hạn mức nợ khách hàng (Credit Gatekeeper)
     */
    private void checkAndApplyCustomerCreditLimit(Order order) {
        if (order.getCustomerPhone() == null || order.getCustomerPhone().trim().isEmpty()) {
            return;
        }

        String phone = order.getCustomerPhone().trim();
        Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer c = new Customer();
            c.setName(order.getCustomerName());
            c.setPhone(phone);
            c.setEmail(order.getCustomerEmail());
            c.setAddress(order.getShippingAddress());
            c.setPriceTier("RETAIL");
            c.setCreditLimit(new BigDecimal("50000000.00"));
            c.setCurrentDebt(BigDecimal.ZERO);
            return customerRepository.save(c);
        });

        order.setPriceTier(customer.getPriceTier());

        // Kiểm tra vượt hạn mức nợ
        if (customer.isExceedingCreditLimit(order.getTotalAmount())) {
            order.setCreditExceeded(true);
            order.setCreditApprovalStatus("PENDING");
            log.warn("Đơn hàng [{}] của khách [{}] vượt hạn mức nợ! (Dư nợ: {} đ, Đơn mới: {} đ, Hạn mức: {} đ)",
                    order.getOrderCode(), customer.getName(), customer.getCurrentDebt(), order.getTotalAmount(), customer.getCreditLimit());
        } else {
            order.setCreditExceeded(false);
            order.setCreditApprovalStatus("NONE");
            // Tăng dư nợ nếu đơn chưa thanh toán ngay
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                customerRepository.increaseDebt(customer.getId(), order.getTotalAmount());
            }
        }
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

    private Map<Long, Integer> mergeItemQuantities(List<OrderItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất 1 sản phẩm!");
        }

        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (OrderItemRequest req : itemRequests) {
            if (req.getProductId() == null) {
                throw new IllegalArgumentException("Product ID không được để trống!");
            }
            if (req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0!");
            }
            merged.merge(req.getProductId(), req.getQuantity(), Integer::sum);
        }
        return merged;
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
                        .uomName(item.getUomName())
                        .uomConversionRate(item.getUomConversionRate())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .snapshotCostPrice(item.getSnapshotCostPrice())
                        .grossProfit(item.getGrossProfit())
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
                .priceTier(order.getPriceTier())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxRate(order.getTaxRate())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .totalCostAmount(order.getTotalCostAmount())
                .grossProfit(order.getGrossProfit())
                .creditExceeded(order.isCreditExceeded())
                .creditApprovalStatus(order.getCreditApprovalStatus())
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
