package com.erp.order.service;

import com.erp.model.Customer;
import com.erp.model.Product;
import com.erp.order.dto.*;
import com.erp.order.model.*;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.order.repository.OrderRepository;
import com.erp.promotion.service.LoyaltyService;
import com.erp.promotion.service.PromotionVoucherService;
import com.erp.repository.CustomerRepository;
import com.erp.repository.ProductRepository;
import com.erp.service.RealtimeStateSyncService;
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
    private final PromotionVoucherService voucherService;
    private final LoyaltyService loyaltyService;
    private final RealtimeStateSyncService realtimeSyncService;

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
     * - Quy đổi đơn vị tính nâng cao (UoM Multi-level Hierarchy)
     * - Tự động áp giá thông minh (Pricing Resolution Engine theo Tier & Volume)
     * - Kiểm soát hạn mức công nợ Credit Gatekeeper
     */
    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, String username) {
        Order order = new Order();
        order.setOrderCode(generateUniqueOrderCode());
        applyCustomerAndPaymentFields(order, request);
        order.setStatus(OrderStatus.PENDING);
        order.setStockDeducted(true);
        order.setCreatedByUsername(username);

        // Nạp thông tin khách hàng trước để xác định priceTier cho Pricing Engine
        syncCustomerPriceTier(order);

        // Áp dụng sản phẩm, tính toán đơn giá theo bậc/UoM, đóng băng giá vốn & giữ chỗ tồn kho nguyên tử
        List<OrderItemRequest> consolidatedItems = consolidateItemRequests(request.getItems());
        applyItemsAndReserveStock(order, consolidatedItems, username, "Tạo đơn hàng mới (Giữ chỗ)");

        // Áp dụng Voucher khuyến mại & Điểm thưởng Loyalty
        applyPromotionsAndLoyalty(order, request);

        // Kiểm tra Hạn mức nợ của Khách hàng (Credit Gatekeeper)
        checkAndApplyCustomerCreditLimit(order);

        Order saved = orderRepository.save(order);

        // Ghi nhận lịch sử sử dụng voucher sau khi Order đã được lưu thành công
        if (saved.getVoucherCode() != null && saved.getVoucherDiscount() != null 
                && saved.getVoucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
            Long customerId = findCustomerId(saved.getCustomerPhone());
            voucherService.recordVoucherUsage(
                    saved.getVoucherCode(),
                    saved.getId(),
                    saved.getOrderCode(),
                    customerId,
                    saved.getCustomerPhone(),
                    saved.getSubtotal(),
                    saved.getVoucherDiscount());
        }

        OrderDto dto = toOrderDto(saved);
        realtimeSyncService.publishOrderEvent("CREATED", dto);
        return dto;
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

        syncCustomerPriceTier(order);
        List<OrderItemRequest> consolidatedItems = consolidateItemRequests(request.getItems());
        applyItemsAndReserveStock(order, consolidatedItems, username, "Cập nhật đơn hàng (Giữ chỗ mới)");
        checkAndApplyCustomerCreditLimit(order);

        Order saved = orderRepository.save(order);
        OrderDto dto = toOrderDto(saved);
        realtimeSyncService.publishOrderEvent("UPDATED", dto);
        return dto;
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

            // Tích điểm hội viên & tự động thăng hạng khách hàng
            Long customerId = findCustomerId(order.getCustomerPhone());
            if (customerId != null) {
                loyaltyService.awardPointsForCompletedOrder(customerId, order.getId(), order.getOrderCode(), order.getTotalAmount());
            }
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
            // Xử lý hoàn kho tùy thuộc vào trạng thái trước khi hủy
            handleCancelOrderStock(order, currentStatus, username, note);
            order.setStockDeducted(false);

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }

            // Hoàn lại điểm tích lũy & lượt dùng voucher khi đơn hàng bị hủy
            Long customerId = findCustomerId(order.getCustomerPhone());
            if (order.getLoyaltyPointsUsed() != null && order.getLoyaltyPointsUsed() > 0 && customerId != null) {
                loyaltyService.refundPointsForCancelledOrder(order.getId(), order.getOrderCode(), customerId, order.getLoyaltyPointsUsed());
            }
            if (order.getVoucherCode() != null) {
                voucherService.revertVoucherUsage(order.getId());
            }
        }

        if (note != null && !note.trim().isEmpty()) {
            String existingNote = order.getNote() != null ? order.getNote().trim() : "";
            String appended = "[" + newStatus.getLabel() + "] " + note.trim();
            order.setNote(existingNote.isEmpty() ? appended : existingNote + "\n" + appended);
        }

        Order saved = orderRepository.save(order);
        OrderDto dto = toOrderDto(saved);
        realtimeSyncService.publishOrderEvent("STATUS_CHANGED", dto);
        return dto;
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
     * Đồng bộ priceTier của khách hàng vào Order trước khi chạy Pricing Engine
     */
    private void syncCustomerPriceTier(Order order) {
        if (order.getCustomerPhone() != null && !order.getCustomerPhone().trim().isEmpty()) {
            customerRepository.findByPhone(order.getCustomerPhone().trim()).ifPresent(c -> {
                order.setPriceTier(c.getPriceTier());
            });
        }
    }

    /**
     * Gộp các dòng sản phẩm trùng lặp cùng productId và cùng đơn vị tính UoM
     */
    private List<OrderItemRequest> consolidateItemRequests(List<OrderItemRequest> items) {
        if (items == null) return Collections.emptyList();
        Map<String, OrderItemRequest> map = new LinkedHashMap<>();
        for (OrderItemRequest req : items) {
            if (req.getProductId() == null) continue;
            String key = req.getProductId() + "_" + (req.getUomName() != null ? req.getUomName().trim() : "default")
                    + "_" + (req.getUomConversionRate() != null ? req.getUomConversionRate() : 1);
            if (map.containsKey(key)) {
                OrderItemRequest existing = map.get(key);
                int q1 = existing.getQuantity() != null ? existing.getQuantity() : 0;
                int q2 = req.getQuantity() != null ? req.getQuantity() : 0;
                existing.setQuantity(q1 + q2);
            } else {
                OrderItemRequest copy = new OrderItemRequest();
                copy.setProductId(req.getProductId());
                copy.setQuantity(req.getQuantity());
                copy.setUnitPrice(req.getUnitPrice());
                copy.setUomName(req.getUomName());
                copy.setUomConversionRate(req.getUomConversionRate());
                map.put(key, copy);
            }
        }
        return new ArrayList<>(map.values());
    }

    /**
     * Giữ chỗ tồn kho nguyên tử (Atomic Stock Reservation)
     * - Quy đổi đơn vị tính (UoM Multi-level Hierarchy): Cái, Hộp, Thùng...
     * - Tự động áp giá (Pricing Resolution Engine): Ưu tiên Contract -> Customer Tier -> Volume Tier (>= 10) -> Retail Price
     * - Đóng băng giá vốn Moving Average Cost (MAC)
     */
    private void applyItemsAndReserveStock(Order order, List<OrderItemRequest> itemRequests, String username, String logPrefix) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất 1 sản phẩm!");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCostAmount = BigDecimal.ZERO;

        for (OrderItemRequest req : itemRequests) {
            if (req.getProductId() == null) {
                throw new IllegalArgumentException("Product ID không được để trống!");
            }
            if (req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0!");
            }

            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm ID #" + req.getProductId() + " không tồn tại!"));

            // 1. Hệ số quy đổi UoM
            String uomName = (req.getUomName() != null && !req.getUomName().trim().isEmpty())
                    ? req.getUomName().trim()
                    : (product.getBaseUnit() != null ? product.getBaseUnit() : "Cái");
            int rate = (req.getUomConversionRate() != null && req.getUomConversionRate() > 0)
                    ? req.getUomConversionRate()
                    : 1;

            // Số lượng cơ sở cần giữ chỗ trên kệ kho
            int baseQty = req.getQuantity() * rate;

            // 2. Giữ chỗ tồn kho nguyên tử (Atomic Check & Update theo số lượng cơ sở)
            int updatedRows = productRepository.reserveStockAtomic(product.getId(), baseQty);
            if (updatedRows == 0) {
                int available = product.getAvailableStock();
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' (" + product.getSku() + ") chỉ còn "
                        + available + " " + (product.getBaseUnit() != null ? product.getBaseUnit() : "Cái")
                        + " khả dụng, không đủ để giữ chỗ " + req.getQuantity() + " " + uomName + " (= " + baseQty + " cái)!"
                );
            }

            // 3. Pricing Resolution Engine (Bảng giá thông minh)
            BigDecimal lineUnitPrice;
            if (req.getUnitPrice() != null && req.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                // Giá tùy chỉnh chỉ định
                lineUnitPrice = req.getUnitPrice();
            } else {
                String tier = order.getPriceTier() != null ? order.getPriceTier() : "RETAIL";
                BigDecimal basePrice;
                if ("WHOLESALE".equalsIgnoreCase(tier) && product.getWholesalePrice() != null) {
                    basePrice = product.getWholesalePrice();
                } else if ("VIP".equalsIgnoreCase(tier) && product.getVipPrice() != null) {
                    basePrice = product.getVipPrice();
                } else if (baseQty >= 10 && product.getWholesalePrice() != null) {
                    // Volume Tier: Mua >= 10 cái cơ sở tự động áp giá sỉ
                    basePrice = product.getWholesalePrice();
                } else {
                    basePrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                }
                // Nhân theo hệ số đơn vị tính UoM
                lineUnitPrice = basePrice.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal lineTotal = lineUnitPrice.multiply(BigDecimal.valueOf(req.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            // 4. Đóng băng giá vốn Moving Average Cost
            BigDecimal baseSnapshotCost = product.getMovingAvgCost() != null ? product.getMovingAvgCost() : BigDecimal.ZERO;
            BigDecimal lineTotalCost = baseSnapshotCost.multiply(BigDecimal.valueOf(baseQty)).setScale(2, RoundingMode.HALF_UP);
            totalCostAmount = totalCostAmount.add(lineTotalCost);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSku(product.getSku());
            item.setProductImageUrl(product.getImageUrl());
            item.setUomName(uomName);
            item.setUomConversionRate(BigDecimal.valueOf(rate));
            item.setUnitPrice(lineUnitPrice);
            item.setQuantity(req.getQuantity());
            item.setLineTotal(lineTotal);
            // Giá vốn trên 1 đơn vị bán = giá vốn cơ sở * hệ số UoM
            item.setSnapshotCostPrice(baseSnapshotCost.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP));
            item.setGrossProfit(lineTotal.subtract(lineTotalCost).setScale(2, RoundingMode.HALF_UP));
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
     * Xuất kho thực tế (trừ Physical Stock và Reserved Stock theo số lượng cơ sở UoM, ghi nhận Thẻ kho)
     */
    private void deductPhysicalStockForOrder(Order order, String username, String note) {
        for (OrderItem item : order.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                int physicalBefore = product.getPhysicalStock() != null ? product.getPhysicalStock() : 0;
                int rate = item.getUomConversionRate() != null ? item.getUomConversionRate().intValue() : 1;
                int baseQty = item.getQuantity() * (rate > 0 ? rate : 1);

                productRepository.deductPhysicalStock(product.getId(), baseQty);
                int physicalAfter = Math.max(0, physicalBefore - baseQty);

                recordInventoryLog(
                        product,
                        InventoryMovementType.EXPORT_ORDER,
                        -baseQty,
                        physicalBefore,
                        physicalAfter,
                        order.getOrderCode(),
                        note + " (" + item.getQuantity() + " " + item.getUomName() + ")",
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
            int rate = item.getUomConversionRate() != null ? item.getUomConversionRate().intValue() : 1;
            int baseQty = item.getQuantity() * (rate > 0 ? rate : 1);
            productRepository.releaseReservedStock(item.getProductId(), baseQty);
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
                    int rate = item.getUomConversionRate() != null ? item.getUomConversionRate().intValue() : 1;
                    int baseQty = item.getQuantity() * (rate > 0 ? rate : 1);

                    productRepository.restorePhysicalStock(product.getId(), baseQty);
                    int physicalAfter = physicalBefore + baseQty;

                    recordInventoryLog(
                            product,
                            InventoryMovementType.IMPORT_CANCEL,
                            baseQty,
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
     * Báo cáo Tuổi nợ Đại lý (Aging Debt Report):
     * Phân loại công nợ theo 0-30 ngày (Trong hạn), 31-60 ngày, 61-90 ngày, và Nợ xấu (> 90 ngày)
     */
    @Transactional(readOnly = true)
    public List<CustomerDebtAgingDto> getDebtAgingReport(String keyword) {
        List<Customer> customers;
        if (keyword != null && !keyword.trim().isEmpty()) {
            customers = customerRepository.searchCustomers(keyword.trim(), PageRequest.of(0, 100)).getContent();
        } else {
            customers = customerRepository.findByCurrentDebtGreaterThanOrderByCurrentDebtDesc(BigDecimal.ZERO);
            if (customers.isEmpty()) {
                customers = customerRepository.findAll(PageRequest.of(0, 50)).getContent();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<CustomerDebtAgingDto> report = new ArrayList<>();

        for (Customer c : customers) {
            String phone = c.getPhone();
            List<Order> unpaidOrders = (phone != null && !phone.trim().isEmpty())
                    ? orderRepository.findByCustomerPhoneAndPaymentStatusNotAndStatusNot(phone.trim(), PaymentStatus.PAID, OrderStatus.CANCELLED)
                    : Collections.emptyList();

            BigDecimal currentDue = BigDecimal.ZERO;
            BigDecimal overdue31To60 = BigDecimal.ZERO;
            BigDecimal overdue61To90 = BigDecimal.ZERO;
            BigDecimal badDebtOver90 = BigDecimal.ZERO;
            int maxDays = 0;

            for (Order o : unpaidOrders) {
                long days = (o.getCreatedAt() != null)
                        ? java.time.temporal.ChronoUnit.DAYS.between(o.getCreatedAt(), now)
                        : 0;
                if (days < 0) days = 0;
                if (days > maxDays) maxDays = (int) days;

                BigDecimal amount = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                if (days <= 30) {
                    currentDue = currentDue.add(amount);
                } else if (days <= 60) {
                    overdue31To60 = overdue31To60.add(amount);
                } else if (days <= 90) {
                    overdue61To90 = overdue61To90.add(amount);
                } else {
                    badDebtOver90 = badDebtOver90.add(amount);
                }
            }

            // Nếu khách có dư nợ sổ cái lớn hơn tổng đơn chưa thanh toán, đưa phần lệch vào nợ hiện tại
            BigDecimal sumUnpaid = currentDue.add(overdue31To60).add(overdue61To90).add(badDebtOver90);
            BigDecimal currentDebt = c.getCurrentDebt() != null ? c.getCurrentDebt() : BigDecimal.ZERO;
            if (currentDebt.compareTo(sumUnpaid) > 0) {
                currentDue = currentDue.add(currentDebt.subtract(sumUnpaid));
            }

            boolean isLocked = c.isCreditLocked() || maxDays > 60 || c.isExceedingCreditLimit(BigDecimal.ZERO);

            report.add(CustomerDebtAgingDto.builder()
                    .customerId(c.getId())
                    .customerName(c.getName())
                    .customerPhone(c.getPhone())
                    .priceTier(c.getPriceTier())
                    .creditLimit(c.getCreditLimit())
                    .currentDebt(currentDebt)
                    .currentDue(currentDue)
                    .overdue31To60(overdue31To60)
                    .overdue61To90(overdue61To90)
                    .badDebtOver90(badDebtOver90)
                    .maxOverdueDays(maxDays)
                    .isCreditLocked(isLocked)
                    .build());
        }

        return report;
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
        order.setVoucherCode(trimOrNull(request.getVoucherCode()));
    }

    private Long findCustomerId(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        return customerRepository.findByPhone(phone.trim())
                .map(Customer::getId)
                .orElse(null);
    }

    private void applyPromotionsAndLoyalty(Order order, CreateOrderRequest request) {
        Long customerId = findCustomerId(order.getCustomerPhone());

        // 1. Áp dụng Voucher khuyến mại
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            var vRes = voucherService.validateVoucher(request.getVoucherCode(), customerId, order.getSubtotal());
            if (vRes.isValid()) {
                order.setVoucherCode(vRes.getCode());
                order.setVoucherDiscount(vRes.getDiscountAmount());
            } else {
                log.warn("Mã voucher '{}' không hợp lệ cho đơn {}: {}", request.getVoucherCode(), order.getOrderCode(), vRes.getMessage());
                order.setVoucherDiscount(BigDecimal.ZERO);
            }
        } else {
            order.setVoucherDiscount(BigDecimal.ZERO);
        }

        // 2. Áp dụng Điểm thưởng Loyalty
        if (request.getLoyaltyPointsToRedeem() != null && request.getLoyaltyPointsToRedeem() > 0 && customerId != null) {
            BigDecimal subtotalAfterVoucher = order.getSubtotal()
                    .subtract(order.getDiscountAmount())
                    .subtract(order.getVoucherDiscount() != null ? order.getVoucherDiscount() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);

            BigDecimal loyaltyDiscount = loyaltyService.redeemPoints(
                    customerId,
                    order.getId(),
                    order.getOrderCode(),
                    request.getLoyaltyPointsToRedeem(),
                    subtotalAfterVoucher);

            order.setLoyaltyPointsUsed(request.getLoyaltyPointsToRedeem());
            order.setLoyaltyDiscount(loyaltyDiscount);
        } else {
            order.setLoyaltyPointsUsed(0);
            order.setLoyaltyDiscount(BigDecimal.ZERO);
        }

        // 3. Tính toán lại toàn bộ tài chính đơn hàng
        BigDecimal totalDiscount = order.getDiscountAmount()
                .add(order.getVoucherDiscount() != null ? order.getVoucherDiscount() : BigDecimal.ZERO)
                .add(order.getLoyaltyDiscount() != null ? order.getLoyaltyDiscount() : BigDecimal.ZERO);

        if (totalDiscount.compareTo(order.getSubtotal()) > 0) {
            totalDiscount = order.getSubtotal();
        }

        BigDecimal taxableAmount = order.getSubtotal().subtract(totalDiscount);
        BigDecimal taxAmount = taxableAmount
                .multiply(order.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
        order.setGrossProfit(totalAmount.subtract(order.getTotalCostAmount()).setScale(2, RoundingMode.HALF_UP));

        // 4. Ước tính điểm tích lũy sẽ được nhận khi hoàn thành
        if (customerId != null) {
            customerRepository.findById(customerId).ifPresent(c -> {
                int earned = loyaltyService.calculatePointsEarned(c, totalAmount);
                order.setLoyaltyPointsEarned(earned);
            });
        }
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
                .voucherCode(order.getVoucherCode())
                .voucherDiscount(order.getVoucherDiscount() != null ? order.getVoucherDiscount() : BigDecimal.ZERO)
                .loyaltyPointsUsed(order.getLoyaltyPointsUsed() != null ? order.getLoyaltyPointsUsed() : 0)
                .loyaltyDiscount(order.getLoyaltyDiscount() != null ? order.getLoyaltyDiscount() : BigDecimal.ZERO)
                .loyaltyPointsEarned(order.getLoyaltyPointsEarned() != null ? order.getLoyaltyPointsEarned() : 0)
                .taxRate(order.getTaxRate())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .totalCostAmount(order.getTotalCostAmount())
                .grossProfit(order.getGrossProfit())
                .creditExceeded(order.isCreditExceeded())
                .creditApprovalStatus(order.getCreditApprovalStatus())
                .shippingCarrier(order.getShippingCarrier())
                .trackingCode(order.getTrackingCode())
                .shippingFee(order.getShippingFee())
                .codAmount(order.getCodAmount())
                .shippingStatus(order.getShippingStatus())
                .dispatchedAt(order.getDispatchedAt())
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
