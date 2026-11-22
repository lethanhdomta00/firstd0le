package com.erp.procurement.service;

import com.erp.order.model.InventoryLog;
import com.erp.order.model.InventoryMovementType;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.model.Product;
import com.erp.procurement.dto.CreateGoodsReceiptRequest;
import com.erp.procurement.dto.GoodsReceiptItemDto;
import com.erp.procurement.dto.GoodsReceiptNoteDto;
import com.erp.procurement.model.*;
import com.erp.procurement.repository.GoodsReceiptNoteRepository;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoodsReceiptService {

    private final GoodsReceiptNoteRepository goodsReceiptNoteRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public Page<GoodsReceiptNoteDto> searchReceiptNotes(String keyword, Pageable pageable) {
        return goodsReceiptNoteRepository.searchReceiptNotes(keyword, pageable)
                .map(this::toSummaryDto);
    }

    public GoodsReceiptNoteDto getReceiptNoteById(Long id) {
        GoodsReceiptNote grn = goodsReceiptNoteRepository.findWithItemsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhập kho với ID: " + id));
        return toFullDto(grn);
    }

    public List<GoodsReceiptNoteDto> getReceiptNotesByPoId(Long poId) {
        return goodsReceiptNoteRepository.findByPurchaseOrderIdOrderByReceiptDateDesc(poId).stream()
                .map(this::toFullDto)
                .toList();
    }

    /**
     * THUẬT TOÁN GIÁ VỐN BÌNH QUÂN GIA QUYỀN (MOVING WEIGHTED AVERAGE COST) & NHẬP KHO KIỂM NHẬN
     */
    @Transactional
    public GoodsReceiptNoteDto createGoodsReceipt(CreateGoodsReceiptRequest request, String currentUsername) {
        PurchaseOrder po = purchaseOrderRepository.findWithItemsById(request.getPurchaseOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt mua PO với ID: " + request.getPurchaseOrderId()));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Chỉ đơn đặt mua ở trạng thái ĐÃ PHÊ DUYỆT (APPROVED) hoặc ĐÃ NHẬP MỘT PHẦN mới có thể lập phiếu nhập kho!");
        }

        Supplier supplier = po.getSupplier();
        String grnCode = generateGrnCode();
        String location = (request.getWarehouseLocation() != null && !request.getWarehouseLocation().isBlank())
                ? request.getWarehouseLocation() : "Kho Chính Trung Tâm";

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
                .grnCode(grnCode)
                .purchaseOrder(po)
                .supplier(supplier)
                .receiptDate(LocalDateTime.now())
                .warehouseLocation(location)
                .receivedByUsername(currentUsername != null ? currentUsername : "thukho")
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        BigDecimal totalReceiptAmount = BigDecimal.ZERO;

        // Map PO Items theo ID để tra cứu nhanh
        Map<Long, PurchaseOrderItem> poItemMap = new HashMap<>();
        for (PurchaseOrderItem item : po.getItems()) {
            poItemMap.put(item.getId(), item);
        }

        for (CreateGoodsReceiptRequest.ReceiptItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantityReceived() == null || itemReq.getQuantityReceived() <= 0) {
                continue; // Bỏ qua nếu số lượng nhận bằng 0
            }

            PurchaseOrderItem poItem = poItemMap.get(itemReq.getPurchaseOrderItemId());
            if (poItem == null) {
                throw new IllegalArgumentException("Không tìm thấy dòng PO với ID: " + itemReq.getPurchaseOrderItemId());
            }

            // Kiểm tra không nhận vượt quá số lượng đặt mua
            int alreadyReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : 0;
            int remainingAllowed = poItem.getOrderedQuantity() - alreadyReceived;
            if (itemReq.getQuantityReceived() > remainingAllowed) {
                throw new IllegalArgumentException(String.format(
                        "Số lượng kiểm nhận (%d) vượt quá số lượng còn lại có thể nhận (%d) cho sản phẩm %s!",
                        itemReq.getQuantityReceived(), remainingAllowed, poItem.getProductName()
                ));
            }

            Product product = productRepository.findById(poItem.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + poItem.getProductId()));

            BigDecimal importPrice = itemReq.getImportUnitPrice() != null ? itemReq.getImportUnitPrice() : poItem.getUnitPrice();
            int qty = itemReq.getQuantityReceived();
            BigDecimal lineTotal = importPrice.multiply(BigDecimal.valueOf(qty));
            totalReceiptAmount = totalReceiptAmount.add(lineTotal);

            // =========================================================================
            // 4.2. THUẬT TOÁN GIÁ VỐN BÌNH QUÂN GIA QUYỀN (MOVING WEIGHTED AVERAGE COST)
            // =========================================================================
            int oldPhysicalStock = (product.getPhysicalStock() != null) ? product.getPhysicalStock() : 0;
            BigDecimal oldCostPrice = (product.getMovingAvgCost() != null && product.getMovingAvgCost().compareTo(BigDecimal.ZERO) > 0)
                    ? product.getMovingAvgCost()
                    : ((product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO);

            BigDecimal newCostPrice;
            int newPhysicalStock = oldPhysicalStock + qty;

            if (oldPhysicalStock <= 0) {
                // Nếu tồn kho cũ <= 0: Lấy luôn đơn giá nhập mới làm giá vốn
                newCostPrice = importPrice;
            } else {
                // Công thức chuẩn: (Tồn cũ * Giá vốn cũ + Lượng nhập * Giá nhập) / (Tồn cũ + Lượng nhập)
                BigDecimal oldStockValue = oldCostPrice.multiply(BigDecimal.valueOf(oldPhysicalStock));
                BigDecimal newStockValue = importPrice.multiply(BigDecimal.valueOf(qty));
                BigDecimal totalStockValue = oldStockValue.add(newStockValue);
                newCostPrice = totalStockValue.divide(BigDecimal.valueOf(newPhysicalStock), 2, RoundingMode.HALF_UP);
            }

            // Cập nhật Product
            product.setPhysicalStock(newPhysicalStock);
            product.setMovingAvgCost(newCostPrice);
            product.syncStockFields();
            productRepository.save(product);

            // Cập nhật PO Item
            poItem.setReceivedQuantity(alreadyReceived + qty);

            // Tạo GoodsReceiptItem
            GoodsReceiptItem grItem = GoodsReceiptItem.builder()
                    .goodsReceiptNote(grn)
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .quantityReceived(qty)
                    .importUnitPrice(importPrice)
                    .lineTotal(lineTotal)
                    .oldCostPrice(oldCostPrice)
                    .newCostPrice(newCostPrice)
                    .oldPhysicalStock(oldPhysicalStock)
                    .newPhysicalStock(newPhysicalStock)
                    .build();

            grn.getItems().add(grItem);

            // Ghi nhật ký kho InventoryLog
            InventoryLog invLog = new InventoryLog();
            invLog.setProductId(product.getId());
            invLog.setProductName(product.getName());
            invLog.setProductSku(product.getSku());
            invLog.setMovementType(InventoryMovementType.IMPORT_PURCHASE);
            invLog.setQuantityDelta(qty);
            invLog.setStockBefore(oldPhysicalStock);
            invLog.setStockAfter(newPhysicalStock);
            invLog.setReferenceCode(grnCode);
            invLog.setNote(String.format("Nhập kho từ PO %s (NCC: %s) - Giá vốn cũ: %,.0fđ -> Giá vốn mới: %,.0fđ",
                    po.getPoCode(), supplier.getName(), oldCostPrice, newCostPrice));
            invLog.setActorUsername(currentUsername != null ? currentUsername : "thukho");
            inventoryLogRepository.save(invLog);

            log.info("Cập nhật Giá vốn Bình quân Gia quyền cho SKU {}: Tồn cũ {} (giá {}) + Nhập {} (giá {}) -> Tồn mới {} (Giá vốn mới {})",
                    product.getSku(), oldPhysicalStock, oldCostPrice, qty, importPrice, newPhysicalStock, newCostPrice);
        }

        grn.setTotalReceiptAmount(totalReceiptAmount);
        GoodsReceiptNote savedGrn = goodsReceiptNoteRepository.save(grn);

        // Cập nhật công nợ Nhà Cung Cấp (Accounts Payable tăng lên)
        BigDecimal currentDebt = supplier.getCurrentDebt() != null ? supplier.getCurrentDebt() : BigDecimal.ZERO;
        supplier.setCurrentDebt(currentDebt.add(totalReceiptAmount));
        supplierRepository.save(supplier);
        log.info("Tăng công nợ NCC {} thêm {}, dư nợ hiện tại: {}", supplier.getName(), totalReceiptAmount, supplier.getCurrentDebt());

        // Kiểm tra xem đơn PO đã hoàn tất nhận đủ 100% chưa
        boolean allCompleted = true;
        for (PurchaseOrderItem item : po.getItems()) {
            int received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
            if (received < item.getOrderedQuantity()) {
                allCompleted = false;
                break;
            }
        }

        if (allCompleted) {
            po.setStatus(PurchaseOrderStatus.COMPLETED);
        } else {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
        purchaseOrderRepository.save(po);

        log.info("Lập thành công phiếu nhập kho GRN: {} cho đơn PO: {}, trạng thái PO mới: {}",
                savedGrn.getGrnCode(), po.getPoCode(), po.getStatus());

        return toFullDto(savedGrn);
    }

    /**
     * API Tiện ích: Mô phỏng trước công thức giá vốn bình quân gia quyền để hiển thị trên UI
     */
    public Map<String, Object> simulateCostCalculation(Long productId, Integer quantityReceived, BigDecimal importUnitPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + productId));

        int oldPhysicalStock = (product.getPhysicalStock() != null) ? product.getPhysicalStock() : 0;
        BigDecimal oldCostPrice = (product.getMovingAvgCost() != null && product.getMovingAvgCost().compareTo(BigDecimal.ZERO) > 0)
                ? product.getMovingAvgCost()
                : ((product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO);

        int qty = (quantityReceived != null && quantityReceived > 0) ? quantityReceived : 1;
        BigDecimal price = (importUnitPrice != null) ? importUnitPrice : oldCostPrice;

        BigDecimal newCostPrice;
        int newPhysicalStock = oldPhysicalStock + qty;

        if (oldPhysicalStock <= 0) {
            newCostPrice = price;
        } else {
            BigDecimal oldStockValue = oldCostPrice.multiply(BigDecimal.valueOf(oldPhysicalStock));
            BigDecimal newStockValue = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal totalStockValue = oldStockValue.add(newStockValue);
            newCostPrice = totalStockValue.divide(BigDecimal.valueOf(newPhysicalStock), 2, RoundingMode.HALF_UP);
        }

        return Map.of(
                "productId", product.getId(),
                "productSku", product.getSku(),
                "productName", product.getName(),
                "oldPhysicalStock", oldPhysicalStock,
                "oldCostPrice", oldCostPrice,
                "quantityReceived", qty,
                "importUnitPrice", price,
                "newPhysicalStock", newPhysicalStock,
                "newCostPrice", newCostPrice,
                "costDifference", newCostPrice.subtract(oldCostPrice)
        );
    }

    public String generateGrnCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = new Random().nextInt(900) + 100;
        String code = "GRN-" + datePart + "-" + rand;
        while (goodsReceiptNoteRepository.findByGrnCode(code).isPresent()) {
            rand = new Random().nextInt(900) + 100;
            code = "GRN-" + datePart + "-" + rand;
        }
        return code;
    }

    public GoodsReceiptNoteDto toSummaryDto(GoodsReceiptNote grn) {
        return GoodsReceiptNoteDto.builder()
                .id(grn.getId())
                .grnCode(grn.getGrnCode())
                .purchaseOrderId(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getId() : null)
                .poCode(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoCode() : null)
                .supplierId(grn.getSupplier() != null ? grn.getSupplier().getId() : null)
                .supplierName(grn.getSupplier() != null ? grn.getSupplier().getName() : "")
                .receiptDate(grn.getReceiptDate())
                .warehouseLocation(grn.getWarehouseLocation())
                .totalReceiptAmount(grn.getTotalReceiptAmount())
                .receivedByUsername(grn.getReceivedByUsername())
                .notes(grn.getNotes())
                .createdAt(grn.getCreatedAt())
                .build();
    }

    public GoodsReceiptNoteDto toFullDto(GoodsReceiptNote grn) {
        GoodsReceiptNoteDto dto = toSummaryDto(grn);
        if (grn.getItems() != null) {
            List<GoodsReceiptItemDto> itemDtos = grn.getItems().stream()
                    .map(item -> GoodsReceiptItemDto.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productSku(item.getProductSku())
                            .productName(item.getProductName())
                            .quantityReceived(item.getQuantityReceived())
                            .importUnitPrice(item.getImportUnitPrice())
                            .lineTotal(item.getLineTotal())
                            .oldCostPrice(item.getOldCostPrice())
                            .newCostPrice(item.getNewCostPrice())
                            .oldPhysicalStock(item.getOldPhysicalStock())
                            .newPhysicalStock(item.getNewPhysicalStock())
                            .build())
                    .toList();
            dto.setItems(itemDtos);
        }
        return dto;
    }
}
