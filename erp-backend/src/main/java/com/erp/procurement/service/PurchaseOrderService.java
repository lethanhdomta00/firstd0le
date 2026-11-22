package com.erp.procurement.service;

import com.erp.model.Product;
import com.erp.procurement.dto.CreatePurchaseOrderRequest;
import com.erp.procurement.dto.PurchaseOrderDto;
import com.erp.procurement.dto.PurchaseOrderItemDto;
import com.erp.procurement.model.PurchaseOrder;
import com.erp.procurement.model.PurchaseOrderItem;
import com.erp.procurement.model.PurchaseOrderStatus;
import com.erp.procurement.model.Supplier;
import com.erp.procurement.repository.PurchaseOrderRepository;
import com.erp.procurement.repository.SupplierRepository;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public Page<PurchaseOrderDto> searchPurchaseOrders(
            String keyword,
            PurchaseOrderStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        return purchaseOrderRepository.searchPurchaseOrders(keyword, status, startDate, endDate, pageable)
                .map(this::toSummaryDto);
    }

    public PurchaseOrderDto getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt mua với ID: " + id));
        return toFullDto(po);
    }

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePurchaseOrderRequest request, String currentUsername) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp với ID: " + request.getSupplierId()));

        String poCode = generatePoCode();
        LocalDate orderDate = request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now();

        PurchaseOrder po = PurchaseOrder.builder()
                .poCode(poCode)
                .supplier(supplier)
                .supplierName(supplier.getName())
                .status(PurchaseOrderStatus.DRAFT)
                .orderDate(orderDate)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .notes(request.getNotes())
                .creatorUsername(currentUsername != null ? currentUsername : "admin")
                .items(new ArrayList<>())
                .build();

        // Nạp thông tin sản phẩm và tạo PurchaseOrderItem
        List<Long> productIds = request.getItems().stream()
                .map(CreatePurchaseOrderRequest.ItemRequest::getProductId)
                .toList();

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        for (CreatePurchaseOrderRequest.ItemRequest itemReq : request.getItems()) {
            Product prod = productMap.get(itemReq.getProductId());
            if (prod == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + itemReq.getProductId());
            }

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .productId(prod.getId())
                    .productSku(prod.getSku())
                    .productName(prod.getName())
                    .unitName(prod.getBaseUnit() != null ? prod.getBaseUnit() : "Cái")
                    .orderedQuantity(itemReq.getOrderedQuantity())
                    .receivedQuantity(0)
                    .unitPrice(itemReq.getUnitPrice())
                    .build();

            item.calculateSubtotal();
            po.getItems().add(item);
        }

        po.recalculateTotals();
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        log.info("Tạo mới đơn đặt mua hàng PO: {} cho NCC {}", saved.getPoCode(), supplier.getName());
        return toFullDto(saved);
    }

    @Transactional
    public PurchaseOrderDto submitForApproval(Long id, String currentUsername) {
        PurchaseOrder po = purchaseOrderRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt mua với ID: " + id));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Chỉ đơn hàng ở trạng thái DRAFT mới có thể gửi phê duyệt!");
        }

        po.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        log.info("Gửi duyệt đơn PO: {} bởi {}", saved.getPoCode(), currentUsername);
        return toFullDto(saved);
    }

    @Transactional
    public PurchaseOrderDto approvePurchaseOrder(Long id, String approverUsername) {
        PurchaseOrder po = purchaseOrderRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt mua với ID: " + id));

        if (po.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL && po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái hợp lệ để phê duyệt!");
        }

        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApproverUsername(approverUsername != null ? approverUsername : "admin");
        po.setApprovedAt(LocalDateTime.now());

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        log.info("Phê duyệt thành công đơn PO: {} bởi {}", saved.getPoCode(), approverUsername);
        return toFullDto(saved);
    }

    @Transactional
    public PurchaseOrderDto cancelPurchaseOrder(Long id, String reason, String currentUsername) {
        PurchaseOrder po = purchaseOrderRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt mua với ID: " + id));

        if (po.getStatus() == PurchaseOrderStatus.COMPLETED || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã tiến hành nhập kho!");
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            po.setNotes((po.getNotes() != null ? po.getNotes() + " | " : "") + "Lý do hủy: " + reason);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        log.info("Đã hủy đơn PO: {} bởi {}", saved.getPoCode(), currentUsername);
        return toFullDto(saved);
    }

    public String generatePoCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = new Random().nextInt(900) + 100;
        String code = "PO-" + datePart + "-" + rand;
        while (purchaseOrderRepository.findByPoCode(code).isPresent()) {
            rand = new Random().nextInt(900) + 100;
            code = "PO-" + datePart + "-" + rand;
        }
        return code;
    }

    public PurchaseOrderDto toSummaryDto(PurchaseOrder po) {
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poCode(po.getPoCode())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierCode(po.getSupplier() != null ? po.getSupplier().getCode() : null)
                .supplierName(po.getSupplierName())
                .supplierPhone(po.getSupplier() != null ? po.getSupplier().getPhone() : null)
                .status(po.getStatus())
                .statusLabel(po.getStatus() != null ? po.getStatus().getLabel() : "")
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .totalAmount(po.getTotalAmount())
                .discountAmount(po.getDiscountAmount())
                .taxAmount(po.getTaxAmount())
                .finalAmount(po.getFinalAmount())
                .paidAmount(po.getPaidAmount())
                .remainingAmount(po.getRemainingAmount())
                .notes(po.getNotes())
                .creatorUsername(po.getCreatorUsername())
                .approverUsername(po.getApproverUsername())
                .approvedAt(po.getApprovedAt())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    public PurchaseOrderDto toFullDto(PurchaseOrder po) {
        PurchaseOrderDto dto = toSummaryDto(po);
        if (po.getItems() != null) {
            List<PurchaseOrderItemDto> itemDtos = po.getItems().stream()
                    .map(item -> PurchaseOrderItemDto.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productSku(item.getProductSku())
                            .productName(item.getProductName())
                            .unitName(item.getUnitName())
                            .orderedQuantity(item.getOrderedQuantity())
                            .receivedQuantity(item.getReceivedQuantity())
                            .unitPrice(item.getUnitPrice())
                            .subtotal(item.getSubtotal())
                            .build())
                    .toList();
            dto.setItems(itemDtos);
        }
        return dto;
    }
}
