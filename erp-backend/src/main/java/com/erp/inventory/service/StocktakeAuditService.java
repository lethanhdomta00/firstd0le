package com.erp.inventory.service;

import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseStock;
import com.erp.branch.repository.WarehouseRepository;
import com.erp.branch.repository.WarehouseStockRepository;
import com.erp.branch.service.BranchWarehouseService;
import com.erp.inventory.dto.*;
import com.erp.inventory.model.StocktakeAudit;
import com.erp.inventory.model.StocktakeItem;
import com.erp.inventory.model.StocktakeStatus;
import com.erp.inventory.repository.StocktakeAuditRepository;
import com.erp.inventory.repository.StocktakeItemRepository;
import com.erp.order.model.InventoryLog;
import com.erp.order.model.InventoryMovementType;
import com.erp.order.repository.InventoryLogRepository;
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
public class StocktakeAuditService {

    private final StocktakeAuditRepository auditRepository;
    private final StocktakeItemRepository itemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final BranchWarehouseService branchWarehouseService;
    private final InventoryLogRepository inventoryLogRepository;
    private final RealtimeStateSyncService realtimeStateSyncService;

    @Transactional(readOnly = true)
    public List<StocktakeAuditDto> getAllAudits(Long warehouseId, StocktakeStatus status) {
        List<StocktakeAudit> list;
        if (warehouseId != null) {
            list = auditRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
        } else if (status != null) {
            list = auditRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = auditRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(StocktakeAuditDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public StocktakeAuditDto getAuditById(Long id) {
        StocktakeAudit a = auditRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + id));
        return StocktakeAuditDto.fromEntity(a);
    }

    /**
     * Tạo Phiếu Kiểm Kê Kho (Lấy snapshot tồn hệ thống)
     */
    @Transactional
    public StocktakeAuditDto createAudit(CreateStocktakeRequest req, String username) {
        Warehouse w = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho hàng ID: " + req.getWarehouseId()));

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String auditCode = "AUDIT-" + today + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        StocktakeAudit audit = new StocktakeAudit();
        audit.setAuditCode(auditCode);
        audit.setTitle(req.getTitle().trim());
        audit.setWarehouseId(w.getId());
        audit.setWarehouseName(w.getName());
        audit.setAuditType(req.getAuditType() != null ? req.getAuditType() : "PERIODIC");
        audit.setStatus(StocktakeStatus.DRAFT);
        audit.setCreatedBy(username != null ? username : "system");
        audit.setNotes(req.getNotes());

        // Lấy danh sách tồn kho hiện có của kho đó
        List<WarehouseStock> stockList;
        if (req.getProductIds() != null && !req.getProductIds().isEmpty()) {
            stockList = warehouseStockRepository.findByWarehouseId(w.getId()).stream()
                    .filter(s -> req.getProductIds().contains(s.getProductId()))
                    .toList();
        } else {
            stockList = warehouseStockRepository.findByWarehouseId(w.getId());
        }

        for (WarehouseStock ws : stockList) {
            StocktakeItem item = new StocktakeItem();
            item.setProductId(ws.getProductId());
            item.setProductSku(ws.getProductSku());
            item.setProductName(ws.getProductName());
            item.setSystemStock(ws.getPhysicalStock() != null ? ws.getPhysicalStock() : 0);
            item.setCountedStock(item.getSystemStock()); // Mặc định khởi tạo bằng tồn hệ thống
            item.calculateDifference();
            audit.addItem(item);
        }
        audit.setTotalItems(audit.getItems().size());
        audit.setTotalDiscrepancyQty(0);

        StocktakeAudit saved = auditRepository.save(audit);
        realtimeStateSyncService.publishEvent("/topic/stocktakes", "CREATED", saved);
        return StocktakeAuditDto.fromEntity(saved);
    }

    /**
     * Ghi nhận kết quả kiểm đếm thực tế
     */
    @Transactional
    public StocktakeAuditDto recordCounts(Long auditId, RecordCountRequest req) {
        StocktakeAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + auditId));

        if (audit.getStatus() == StocktakeStatus.BALANCED || audit.getStatus() == StocktakeStatus.CANCELLED) {
            throw new IllegalStateException("Phiếu kiểm kê đã hoàn tất hoặc đã hủy, không thể sửa kết quả đếm!");
        }

        Map<Long, RecordCountRequest.CountItemRequest> countMap = new HashMap<>();
        for (RecordCountRequest.CountItemRequest c : req.getCounts()) {
            if (c.getItemId() != null) {
                countMap.put(c.getItemId(), c);
            } else if (c.getProductId() != null) {
                countMap.put(c.getProductId(), c);
            }
        }

        int totalDiscrepancy = 0;
        for (StocktakeItem item : audit.getItems()) {
            RecordCountRequest.CountItemRequest c = countMap.get(item.getId());
            if (c == null) {
                c = countMap.get(item.getProductId());
            }

            if (c != null && c.getCountedStock() != null) {
                item.setCountedStock(c.getCountedStock());
                if (c.getReason() != null) {
                    item.setReason(c.getReason());
                }
                item.calculateDifference();
            }
            totalDiscrepancy += Math.abs(item.getDifferenceQty());
        }

        audit.setTotalDiscrepancyQty(totalDiscrepancy);
        audit.setStatus(StocktakeStatus.IN_PROGRESS);
        if (audit.getStartedAt() == null) {
            audit.setStartedAt(LocalDateTime.now());
        }
        if (req.getNotes() != null) {
            audit.setNotes(audit.getNotes() != null ? audit.getNotes() + " | " + req.getNotes() : req.getNotes());
        }

        StocktakeAudit saved = auditRepository.save(audit);
        realtimeStateSyncService.publishEvent("/topic/stocktakes", "UPDATED", saved);
        return StocktakeAuditDto.fromEntity(saved);
    }

    /**
     * Cân Đối Tồn Kho & Hoàn Tất Kiểm Kê (Tự động sinh điều chỉnh tồn)
     */
    @Transactional
    public StocktakeAuditDto balanceAndComplete(Long auditId, String username) {
        StocktakeAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + auditId));

        if (audit.getStatus() == StocktakeStatus.BALANCED) {
            throw new IllegalStateException("Phiếu kiểm kê này đã được cân đối hoàn tất!");
        }

        Warehouse w = warehouseRepository.findById(audit.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho hàng!"));

        int totalDiscrepancy = 0;
        for (StocktakeItem item : audit.getItems()) {
            int diff = item.getDifferenceQty();
            totalDiscrepancy += Math.abs(diff);

            if (diff != 0) {
                // Có chênh lệch thừa / thiếu -> Cân đối lại tồn kho
                WarehouseStock ws = warehouseStockRepository.findByWarehouseIdAndProductId(w.getId(), item.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tồn kho sản phẩm: " + item.getProductName()));

                int oldPhys = ws.getPhysicalStock() != null ? ws.getPhysicalStock() : 0;
                ws.setPhysicalStock(item.getCountedStock());
                ws.setAvailableStock(Math.max(0, ws.getPhysicalStock() - ws.getReservedStock()));
                warehouseStockRepository.save(ws);

                // Ghi nhật ký thẻ kho
                InventoryLog logEntry = new InventoryLog();
                logEntry.setProductId(item.getProductId());
                logEntry.setProductSku(item.getProductSku());
                logEntry.setProductName(item.getProductName());
                logEntry.setMovementType(InventoryMovementType.MANUAL_ADJUST);
                logEntry.setQuantityDelta(diff);
                logEntry.setStockBefore(oldPhys);
                logEntry.setStockAfter(item.getCountedStock());
                logEntry.setReferenceCode(audit.getAuditCode());
                logEntry.setNote("Cân đối kiểm kê kho " + w.getName() + " (Chênh lệch: " + (diff > 0 ? "+" + diff : diff) + ")");
                logEntry.setActorUsername(username != null ? username : "system");
                inventoryLogRepository.save(logEntry);

                // Đồng bộ tổng tồn sản phẩm
                branchWarehouseService.syncProductTotalStock(item.getProductId());
            }
        }

        audit.setTotalDiscrepancyQty(totalDiscrepancy);
        audit.setStatus(StocktakeStatus.BALANCED);
        audit.setCompletedAt(LocalDateTime.now());
        audit.setApprovedBy(username != null ? username : "system");

        StocktakeAudit saved = auditRepository.save(audit);
        realtimeStateSyncService.publishEvent("/topic/stocktakes", "BALANCED", saved);
        return StocktakeAuditDto.fromEntity(saved);
    }

    /**
     * Hủy Phiếu Kiểm Kê
     */
    @Transactional
    public StocktakeAuditDto cancelAudit(Long auditId, String username) {
        StocktakeAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + auditId));

        if (audit.getStatus() == StocktakeStatus.BALANCED) {
            throw new IllegalStateException("Không thể hủy phiếu kiểm kê đã cân đối hoàn tất!");
        }

        audit.setStatus(StocktakeStatus.CANCELLED);
        StocktakeAudit saved = auditRepository.save(audit);
        realtimeStateSyncService.publishEvent("/topic/stocktakes", "CANCELLED", saved);
        return StocktakeAuditDto.fromEntity(saved);
    }
}
