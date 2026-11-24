package com.erp.branch.service;

import com.erp.branch.dto.*;
import com.erp.branch.model.*;
import com.erp.branch.repository.*;
import com.erp.model.Product;
import com.erp.order.model.InventoryLog;
import com.erp.order.model.InventoryMovementType;
import com.erp.order.repository.InventoryLogRepository;
import com.erp.repository.ProductRepository;
import com.erp.service.RealtimeStateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalTransferService {

    private final InternalTransferRepository transferRepository;
    private final InternalTransferItemRepository transferItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final BranchWarehouseService branchWarehouseService;
    private final RealtimeStateSyncService realtimeStateSyncService;

    @Transactional(readOnly = true)
    public List<InternalTransferDto> getAllTransfers(TransferStatus status, Long warehouseId) {
        List<InternalTransfer> transfers;
        if (warehouseId != null) {
            transfers = transferRepository.findByWarehouseId(warehouseId);
        } else if (status != null) {
            transfers = transferRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            transfers = transferRepository.findAllByOrderByCreatedAtDesc();
        }

        return transfers.stream()
                .map(InternalTransferDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InternalTransferDto getTransferById(Long id) {
        InternalTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển với ID: " + id));
        return InternalTransferDto.fromEntity(t);
    }

    @Transactional
    public InternalTransferDto createTransfer(CreateTransferRequest req, String username) {
        if (req.getFromWarehouseId().equals(req.getToWarehouseId())) {
            throw new IllegalArgumentException("Kho xuất và kho nhận không được trùng nhau!");
        }

        Warehouse fromWh = warehouseRepository.findById(req.getFromWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho xuất với ID: " + req.getFromWarehouseId()));
        Warehouse toWh = warehouseRepository.findById(req.getToWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho nhận với ID: " + req.getToWarehouseId()));

        if (!Boolean.TRUE.equals(fromWh.getActive()) || !Boolean.TRUE.equals(toWh.getActive())) {
            throw new IllegalArgumentException("Kho xuất hoặc kho nhận đang trong trạng thái tạm ngưng!");
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transferCode = "ITN-" + today + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        InternalTransfer transfer = new InternalTransfer();
        transfer.setTransferCode(transferCode);
        transfer.setFromWarehouseId(fromWh.getId());
        transfer.setFromWarehouseName(fromWh.getName());
        transfer.setToWarehouseId(toWh.getId());
        transfer.setToWarehouseName(toWh.getName());
        transfer.setStatus(TransferStatus.DRAFT);
        transfer.setCreatedBy(username != null ? username : "system");
        transfer.setNote(req.getNote());

        int totalQty = 0;
        for (CreateTransferRequest.CreateTransferItemRequest itemReq : req.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng điều chuyển phải lớn hơn 0");
            }

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + itemReq.getProductId()));

            InternalTransferItem item = new InternalTransferItem();
            item.setProductId(product.getId());
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setRequestedQuantity(itemReq.getQuantity());
            item.setTransferredQuantity(itemReq.getQuantity()); // Mặc định xuất bằng requested
            item.setReceivedQuantity(0);
            item.setDamagedQuantity(0);
            item.setItemNote(itemReq.getNote());

            transfer.addItem(item);
            totalQty += itemReq.getQuantity();
        }
        transfer.setTotalQuantity(totalQty);

        InternalTransfer saved = transferRepository.save(transfer);
        realtimeStateSyncService.publishEvent("/topic/transfers", "CREATED", saved);
        return InternalTransferDto.fromEntity(saved);
    }

    /**
     * Xác nhận Xuất kho Điều chuyển (Staff -> In Transit)
     * Trừ physical_stock & available_stock tại kho xuất, cộng in_transit_stock tại kho đích.
     */
    @Transactional
    public InternalTransferDto dispatchTransfer(Long transferId, String username) {
        InternalTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển ID: " + transferId));

        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new IllegalStateException("Chỉ phiếu ở trạng thái DRAFT mới có thể xác nhận xuất kho!");
        }

        Warehouse fromWh = warehouseRepository.findById(transfer.getFromWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho xuất!"));
        Warehouse toWh = warehouseRepository.findById(transfer.getToWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho nhận!"));

        for (InternalTransferItem item : transfer.getItems()) {
            WarehouseStock fromStock = warehouseStockRepository.findByWarehouseIdAndProductId(fromWh.getId(), item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm " + item.getProductName() + " chưa có bản ghi tồn kho tại " + fromWh.getName()));

            int avail = fromStock.getAvailableStock() != null ? fromStock.getAvailableStock() : 0;
            if (avail < item.getTransferredQuantity()) {
                throw new IllegalArgumentException("Kho xuất [" + fromWh.getName() + "] không đủ hàng cho sản phẩm [" + 
                        item.getProductName() + "]. Tồn khả dụng: " + avail + ", Cần xuất: " + item.getTransferredQuantity());
            }

            // 1. Trừ physical_stock và available_stock tại kho xuất
            int oldPhys = fromStock.getPhysicalStock() != null ? fromStock.getPhysicalStock() : 0;
            fromStock.setPhysicalStock(oldPhys - item.getTransferredQuantity());
            fromStock.setAvailableStock(Math.max(0, fromStock.getPhysicalStock() - fromStock.getReservedStock()));
            warehouseStockRepository.save(fromStock);

            // 2. Tăng in_transit_stock tại kho đích
            WarehouseStock toStock = warehouseStockRepository.findByWarehouseIdAndProductId(toWh.getId(), item.getProductId())
                    .orElseGet(() -> {
                        WarehouseStock ns = new WarehouseStock();
                        ns.setWarehouseId(toWh.getId());
                        ns.setWarehouseName(toWh.getName());
                        ns.setBranchId(toWh.getBranchId());
                        ns.setProductId(item.getProductId());
                        ns.setProductSku(item.getProductSku());
                        ns.setProductName(item.getProductName());
                        ns.setPhysicalStock(0);
                        ns.setReservedStock(0);
                        ns.setInTransitStock(0);
                        ns.setMinStockAlert(10);
                        return ns;
                    });
            int oldTransit = toStock.getInTransitStock() != null ? toStock.getInTransitStock() : 0;
            toStock.setInTransitStock(oldTransit + item.getTransferredQuantity());
            warehouseStockRepository.save(toStock);

            // 3. Ghi thẻ kho xuất
            InventoryLog logEntry = new InventoryLog();
            logEntry.setProductId(item.getProductId());
            logEntry.setProductSku(item.getProductSku());
            logEntry.setProductName(item.getProductName());
            logEntry.setMovementType(InventoryMovementType.TRANSFER_OUT);
            logEntry.setQuantityDelta(-item.getTransferredQuantity());
            logEntry.setStockBefore(oldPhys);
            logEntry.setStockAfter(fromStock.getPhysicalStock());
            logEntry.setReferenceCode(transfer.getTransferCode());
            logEntry.setNote("Xuất chuyển kho từ " + fromWh.getName() + " sang " + toWh.getName());
            logEntry.setActorUsername(username != null ? username : "system");
            inventoryLogRepository.save(logEntry);

            // Đồng bộ Product
            branchWarehouseService.syncProductTotalStock(item.getProductId());
        }

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setDepartureDate(LocalDateTime.now());
        transfer.setTransferredBy(username != null ? username : "system");

        InternalTransfer saved = transferRepository.save(transfer);
        realtimeStateSyncService.publishEvent("/topic/transfers", "DISPATCHED", saved);
        return InternalTransferDto.fromEntity(saved);
    }

    /**
     * Xác nhận Nhập kho Hoàn tất (Receiver kiểm đếm)
     * Trừ in_transit_stock tại kho đích, cộng physical_stock thực nhận, hạch toán hàng lỗi (nếu có).
     */
    @Transactional
    public InternalTransferDto receiveTransfer(Long transferId, ReceiveTransferRequest req, String username) {
        InternalTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển ID: " + transferId));

        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new IllegalStateException("Chỉ phiếu ở trạng thái IN_TRANSIT mới có thể xác nhận nhập kho!");
        }

        Warehouse toWh = warehouseRepository.findById(transfer.getToWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho nhận!"));

        Map<Long, ReceiveTransferRequest.ReceiveTransferItemRequest> itemMap = new HashMap<>();
        for (ReceiveTransferRequest.ReceiveTransferItemRequest rItem : req.getItems()) {
            if (rItem.getItemId() != null) {
                itemMap.put(rItem.getItemId(), rItem);
            } else if (rItem.getProductId() != null) {
                itemMap.put(rItem.getProductId(), rItem);
            }
        }

        // Tìm kho hàng lỗi nếu phát hiện hư hỏng
        List<Warehouse> damagedWarehouses = warehouseRepository.findByWarehouseType(WarehouseType.DAMAGED_GOODS);
        Warehouse defectWh = !damagedWarehouses.isEmpty() ? damagedWarehouses.get(0) : null;

        for (InternalTransferItem item : transfer.getItems()) {
            ReceiveTransferRequest.ReceiveTransferItemRequest rItem = itemMap.get(item.getId());
            if (rItem == null) {
                rItem = itemMap.get(item.getProductId());
            }

            int receivedQty = (rItem != null && rItem.getReceivedQuantity() != null) 
                    ? rItem.getReceivedQuantity() 
                    : item.getTransferredQuantity();
            int damagedQty = (rItem != null && rItem.getDamagedQuantity() != null) 
                    ? rItem.getDamagedQuantity() 
                    : 0;

            if (receivedQty < 0 || damagedQty < 0) {
                throw new IllegalArgumentException("Số lượng nhận và số lượng lỗi không được âm!");
            }

            item.setReceivedQuantity(receivedQty);
            item.setDamagedQuantity(damagedQty);
            if (rItem != null && rItem.getNote() != null) {
                item.setItemNote(rItem.getNote());
            }

            // 1. Trừ in_transit_stock và cộng physical_stock tại kho đích
            WarehouseStock toStock = warehouseStockRepository.findByWarehouseIdAndProductId(toWh.getId(), item.getProductId())
                    .orElseGet(() -> {
                        WarehouseStock ns = new WarehouseStock();
                        ns.setWarehouseId(toWh.getId());
                        ns.setWarehouseName(toWh.getName());
                        ns.setBranchId(toWh.getBranchId());
                        ns.setProductId(item.getProductId());
                        ns.setProductSku(item.getProductSku());
                        ns.setProductName(item.getProductName());
                        ns.setPhysicalStock(0);
                        ns.setReservedStock(0);
                        ns.setInTransitStock(0);
                        ns.setMinStockAlert(10);
                        return ns;
                    });

            int oldTransit = toStock.getInTransitStock() != null ? toStock.getInTransitStock() : 0;
            toStock.setInTransitStock(Math.max(0, oldTransit - item.getTransferredQuantity()));

            int oldPhys = toStock.getPhysicalStock() != null ? toStock.getPhysicalStock() : 0;
            toStock.setPhysicalStock(oldPhys + receivedQty);
            toStock.setAvailableStock(Math.max(0, toStock.getPhysicalStock() - toStock.getReservedStock()));
            warehouseStockRepository.save(toStock);

            // 2. Xử lý hàng lỗi nếu có
            if (damagedQty > 0 && defectWh != null) {
                WarehouseStock defectStock = warehouseStockRepository.findByWarehouseIdAndProductId(defectWh.getId(), item.getProductId())
                        .orElseGet(() -> {
                            WarehouseStock ns = new WarehouseStock();
                            ns.setWarehouseId(defectWh.getId());
                            ns.setWarehouseName(defectWh.getName());
                            ns.setBranchId(defectWh.getBranchId());
                            ns.setProductId(item.getProductId());
                            ns.setProductSku(item.getProductSku());
                            ns.setProductName(item.getProductName());
                            ns.setPhysicalStock(0);
                            ns.setReservedStock(0);
                            ns.setInTransitStock(0);
                            ns.setMinStockAlert(0);
                            return ns;
                        });
                int oldDefectPhys = defectStock.getPhysicalStock() != null ? defectStock.getPhysicalStock() : 0;
                defectStock.setPhysicalStock(oldDefectPhys + damagedQty);
                defectStock.setAvailableStock(0); // Kho lỗi không bán được
                warehouseStockRepository.save(defectStock);
            }

            // 3. Ghi thẻ kho nhập
            InventoryLog logEntry = new InventoryLog();
            logEntry.setProductId(item.getProductId());
            logEntry.setProductSku(item.getProductSku());
            logEntry.setProductName(item.getProductName());
            logEntry.setMovementType(InventoryMovementType.TRANSFER_IN);
            logEntry.setQuantityDelta(receivedQty);
            logEntry.setStockBefore(oldPhys);
            logEntry.setStockAfter(toStock.getPhysicalStock());
            logEntry.setReferenceCode(transfer.getTransferCode());
            logEntry.setNote("Nhập chuyển kho từ " + transfer.getFromWarehouseName() + (damagedQty > 0 ? " (Hư hỏng: " + damagedQty + ")" : ""));
            logEntry.setActorUsername(username != null ? username : "system");
            inventoryLogRepository.save(logEntry);

            // Đồng bộ Product
            branchWarehouseService.syncProductTotalStock(item.getProductId());
        }

        if (req.getNote() != null && !req.getNote().trim().isEmpty()) {
            transfer.setNote(transfer.getNote() != null ? transfer.getNote() + " | " + req.getNote() : req.getNote());
        }
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setArrivalDate(LocalDateTime.now());
        transfer.setReceivedBy(username != null ? username : "system");

        InternalTransfer saved = transferRepository.save(transfer);
        realtimeStateSyncService.publishEvent("/topic/transfers", "COMPLETED", saved);
        return InternalTransferDto.fromEntity(saved);
    }

    /**
     * Hủy Phiếu Điều Chuyển
     */
    @Transactional
    public InternalTransferDto cancelTransfer(Long transferId, String username) {
        InternalTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu điều chuyển ID: " + transferId));

        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy phiếu đã hoàn tất nhập kho!");
        }

        if (transfer.getStatus() == TransferStatus.IN_TRANSIT) {
            // Hoàn lại hàng cho kho xuất và xóa in_transit ở kho đích
            for (InternalTransferItem item : transfer.getItems()) {
                warehouseStockRepository.findByWarehouseIdAndProductId(transfer.getFromWarehouseId(), item.getProductId())
                        .ifPresent(fromStock -> {
                            fromStock.setPhysicalStock(fromStock.getPhysicalStock() + item.getTransferredQuantity());
                            fromStock.setAvailableStock(Math.max(0, fromStock.getPhysicalStock() - fromStock.getReservedStock()));
                            warehouseStockRepository.save(fromStock);
                        });

                warehouseStockRepository.findByWarehouseIdAndProductId(transfer.getToWarehouseId(), item.getProductId())
                        .ifPresent(toStock -> {
                            toStock.setInTransitStock(Math.max(0, toStock.getInTransitStock() - item.getTransferredQuantity()));
                            warehouseStockRepository.save(toStock);
                        });

                branchWarehouseService.syncProductTotalStock(item.getProductId());
            }
        }

        transfer.setStatus(TransferStatus.CANCELLED);
        InternalTransfer saved = transferRepository.save(transfer);
        realtimeStateSyncService.publishEvent("/topic/transfers", "CANCELLED", saved);
        return InternalTransferDto.fromEntity(saved);
    }
}
