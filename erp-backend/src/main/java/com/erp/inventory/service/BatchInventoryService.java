package com.erp.inventory.service;

import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseStock;
import com.erp.branch.repository.WarehouseRepository;
import com.erp.branch.repository.WarehouseStockRepository;
import com.erp.branch.service.BranchWarehouseService;
import com.erp.inventory.dto.*;
import com.erp.inventory.model.BatchStatus;
import com.erp.inventory.model.ProductBatch;
import com.erp.inventory.repository.ProductBatchRepository;
import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import com.erp.service.RealtimeStateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchInventoryService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final BranchWarehouseService branchWarehouseService;
    private final RealtimeStateSyncService realtimeStateSyncService;

    @Transactional(readOnly = true)
    public List<ProductBatchDto> getAllBatches(Long warehouseId, Long productId) {
        List<ProductBatch> list;
        if (warehouseId != null) {
            list = batchRepository.findByWarehouseIdOrderByExpiryDateAsc(warehouseId);
        } else if (productId != null) {
            list = batchRepository.findByProductId(productId);
        } else {
            list = batchRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream()
                .map(b -> {
                    b.refreshStatus();
                    return ProductBatchDto.fromEntity(b);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductBatchDto getBatchById(Long id) {
        ProductBatch b = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng với ID: " + id));
        b.refreshStatus();
        return ProductBatchDto.fromEntity(b);
    }

    @Transactional
    public ProductBatchDto createBatch(CreateBatchRequest req) {
        Product p = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + req.getProductId()));
        Warehouse w = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho hàng ID: " + req.getWarehouseId()));

        String code = req.getBatchCode();
        if (code == null || code.trim().isEmpty()) {
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            code = "LOT-" + month + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        } else {
            code = code.trim().toUpperCase();
            if (batchRepository.existsByBatchCode(code)) {
                throw new IllegalArgumentException("Mã lô hàng " + code + " đã tồn tại trong hệ thống");
            }
        }

        ProductBatch batch = new ProductBatch();
        batch.setBatchCode(code);
        batch.setProductId(p.getId());
        batch.setProductSku(p.getSku());
        batch.setProductName(p.getName());
        batch.setWarehouseId(w.getId());
        batch.setWarehouseName(w.getName());
        batch.setManufacturingDate(req.getManufacturingDate());
        batch.setExpiryDate(req.getExpiryDate());
        batch.setInitialQuantity(req.getInitialQuantity());
        batch.setRemainingQuantity(req.getInitialQuantity());
        batch.setCostPrice(req.getCostPrice() != null ? req.getCostPrice() : p.getMovingAvgCost());
        batch.setSupplierName(req.getSupplierName());
        batch.setNotes(req.getNotes());
        batch.refreshStatus();

        ProductBatch saved = batchRepository.save(batch);

        // Đảm bảo tồn kho tại WarehouseStock được cộng dồn
        WarehouseStock ws = warehouseStockRepository.findByWarehouseIdAndProductId(w.getId(), p.getId())
                .orElseGet(() -> {
                    WarehouseStock ns = new WarehouseStock();
                    ns.setWarehouseId(w.getId());
                    ns.setWarehouseName(w.getName());
                    ns.setBranchId(w.getBranchId());
                    ns.setProductId(p.getId());
                    ns.setProductSku(p.getSku());
                    ns.setProductName(p.getName());
                    ns.setPhysicalStock(0);
                    ns.setReservedStock(0);
                    ns.setInTransitStock(0);
                    ns.setMinStockAlert(10);
                    return ns;
                });
        ws.setPhysicalStock(ws.getPhysicalStock() + req.getInitialQuantity());
        ws.setAvailableStock(Math.max(0, ws.getPhysicalStock() - ws.getReservedStock()));
        warehouseStockRepository.save(ws);
        branchWarehouseService.syncProductTotalStock(p.getId());

        realtimeStateSyncService.publishEvent("/topic/batches", "CREATED", saved);
        return ProductBatchDto.fromEntity(saved);
    }

    /**
     * Thuật toán FEFO (First Expired, First Out) / FIFO
     * Tự động phân bổ số lượng cần xuất từ các lô có hạn dùng gần nhất
     */
    @Transactional(readOnly = true)
    public BatchAllocationDto allocateBatches(Long productId, Integer requestedQty, String strategy) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + productId));

        if (requestedQty == null || requestedQty <= 0) {
            throw new IllegalArgumentException("Số lượng yêu cầu phải lớn hơn 0");
        }

        boolean isFifo = "FIFO".equalsIgnoreCase(strategy);
        List<ProductBatch> availableBatches = isFifo
                ? batchRepository.findBatchesForFifo(productId, 0)
                : batchRepository.findBatchesForFefo(productId, 0);

        int remainingNeed = requestedQty;
        int totalAllocated = 0;
        List<BatchAllocationDto.AllocatedBatchItemDto> allocations = new ArrayList<>();

        for (ProductBatch b : availableBatches) {
            if (remainingNeed <= 0) break;
            int take = Math.min(b.getRemainingQuantity(), remainingNeed);
            allocations.add(BatchAllocationDto.AllocatedBatchItemDto.builder()
                    .batchId(b.getId())
                    .batchCode(b.getBatchCode())
                    .warehouseId(b.getWarehouseId())
                    .warehouseName(b.getWarehouseName())
                    .expiryDate(b.getExpiryDate())
                    .remainingQuantity(b.getRemainingQuantity())
                    .quantityToPick(take)
                    .build());
            totalAllocated += take;
            remainingNeed -= take;
        }

        return BatchAllocationDto.builder()
                .productId(p.getId())
                .productSku(p.getSku())
                .productName(p.getName())
                .requestedQuantity(requestedQty)
                .allocatedQuantity(totalAllocated)
                .strategy(isFifo ? "FIFO" : "FEFO")
                .allocations(allocations)
                .build();
    }

    /**
     * Trừ tồn của Lô hàng khi bán hoặc xuất kho
     */
    @Transactional
    public void deductBatchStock(Long batchId, Integer quantity) {
        ProductBatch b = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng ID: " + batchId));

        if (b.getRemainingQuantity() < quantity) {
            throw new IllegalArgumentException("Lô hàng " + b.getBatchCode() + " không đủ tồn để trừ (Còn: " + 
                    b.getRemainingQuantity() + ", Cần trừ: " + quantity + ")");
        }

        b.setRemainingQuantity(b.getRemainingQuantity() - quantity);
        b.refreshStatus();
        batchRepository.save(b);
    }

    /**
     * Radar Cảnh Báo Hàng Cận Date Đa Cấp Độ (Expiry Risk Radar)
     * Phân loại theo 3 vùng màu:
     * - Vùng Đỏ (Red): Cận date dưới 30 ngày hoặc đã quá hạn
     * - Vùng Vàng (Yellow): Cận date 30 - 90 ngày (Gợi ý xả hàng/khuyến mại)
     * - Vùng Xanh (Green): An toàn trên 90 ngày
     */
    @Transactional(readOnly = true)
    public ExpiryRadarDto getExpiryRadar() {
        List<ProductBatch> allBatches = batchRepository.findAll();
        LocalDate today = LocalDate.now();

        List<ProductBatchDto> redZone = new ArrayList<>();
        List<ProductBatchDto> yellowZone = new ArrayList<>();
        List<ProductBatchDto> greenZone = new ArrayList<>();
        int expiredCount = 0;

        for (ProductBatch b : allBatches) {
            if (b.getRemainingQuantity() == null || b.getRemainingQuantity() <= 0) {
                continue; // Bỏ qua lô đã hết sạch tồn
            }
            b.refreshStatus();
            ProductBatchDto dto = ProductBatchDto.fromEntity(b);
            long days = dto.getDaysUntilExpiry();

            if (days <= 0) {
                expiredCount++;
                redZone.add(dto);
            } else if (days <= 30) {
                redZone.add(dto);
            } else if (days <= 90) {
                yellowZone.add(dto);
            } else {
                greenZone.add(dto);
            }
        }

        // Sắp xếp các lô có hạn dùng gần nhất lên đầu
        redZone.sort(Comparator.comparing(ProductBatchDto::getDaysUntilExpiry));
        yellowZone.sort(Comparator.comparing(ProductBatchDto::getDaysUntilExpiry));
        greenZone.sort(Comparator.comparing(ProductBatchDto::getDaysUntilExpiry));

        ExpiryRadarSummaryDto summary = ExpiryRadarSummaryDto.builder()
                .totalActiveBatches(redZone.size() + yellowZone.size() + greenZone.size())
                .redCount(redZone.size())
                .yellowCount(yellowZone.size())
                .greenCount(greenZone.size())
                .expiredCount(expiredCount)
                .build();

        return ExpiryRadarDto.builder()
                .summary(summary)
                .redZone(redZone)
                .yellowZone(yellowZone)
                .greenZone(greenZone)
                .build();
    }
}
