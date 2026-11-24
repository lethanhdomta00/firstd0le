package com.erp.branch.service;

import com.erp.branch.dto.*;
import com.erp.branch.model.Branch;
import com.erp.branch.model.Warehouse;
import com.erp.branch.model.WarehouseStock;
import com.erp.branch.model.WarehouseType;
import com.erp.branch.repository.BranchRepository;
import com.erp.branch.repository.WarehouseRepository;
import com.erp.branch.repository.WarehouseStockRepository;
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

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchWarehouseService {

    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final RealtimeStateSyncService realtimeStateSyncService;

    // ==========================================
    // 1. QUẢN LÝ CHI NHÁNH (BRANCH)
    // ==========================================

    @Transactional(readOnly = true)
    public List<BranchDto> getAllBranches() {
        List<Branch> branches = branchRepository.findAllByOrderByCreatedAtDesc();
        List<Warehouse> warehouses = warehouseRepository.findAll();
        Map<Long, Integer> whCountMap = new HashMap<>();
        for (Warehouse w : warehouses) {
            whCountMap.put(w.getBranchId(), whCountMap.getOrDefault(w.getBranchId(), 0) + 1);
        }

        return branches.stream()
                .map(b -> BranchDto.fromEntity(b, whCountMap.getOrDefault(b.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));
        List<Warehouse> whList = warehouseRepository.findByBranchId(id);
        return BranchDto.fromEntity(branch, whList.size());
    }

    @Transactional
    public BranchDto createBranch(CreateBranchRequest req) {
        String code = req.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = "BR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else {
            code = code.trim().toUpperCase();
            if (branchRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Mã chi nhánh " + code + " đã tồn tại trong hệ thống");
            }
        }

        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(req.getName().trim());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setStatus(req.getStatus() != null ? req.getStatus() : "ACTIVE");
        branch.setIsHeadquarter(Boolean.TRUE.equals(req.getIsHeadquarter()));

        Branch saved = branchRepository.save(branch);
        realtimeStateSyncService.publishEvent("/topic/branches", "CREATED", saved);
        return BranchDto.fromEntity(saved, 0);
    }

    @Transactional
    public BranchDto updateBranch(Long id, CreateBranchRequest req) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));

        branch.setName(req.getName().trim());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        if (req.getStatus() != null) {
            branch.setStatus(req.getStatus());
        }
        if (req.getIsHeadquarter() != null) {
            branch.setIsHeadquarter(req.getIsHeadquarter());
        }

        Branch saved = branchRepository.save(branch);
        // Đồng bộ branchName sang các Warehouse trực thuộc
        List<Warehouse> warehouses = warehouseRepository.findByBranchId(id);
        for (Warehouse w : warehouses) {
            w.setBranchName(saved.getName());
        }
        warehouseRepository.saveAll(warehouses);

        realtimeStateSyncService.publishEvent("/topic/branches", "UPDATED", saved);
        return BranchDto.fromEntity(saved, warehouses.size());
    }

    @Transactional
    public BranchDto toggleBranchStatus(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh với ID: " + id));
        String newStatus = "ACTIVE".equalsIgnoreCase(branch.getStatus()) ? "INACTIVE" : "ACTIVE";
        branch.setStatus(newStatus);
        Branch saved = branchRepository.save(branch);
        List<Warehouse> whList = warehouseRepository.findByBranchId(id);
        realtimeStateSyncService.publishEvent("/topic/branches", "STATUS_CHANGED", saved);
        return BranchDto.fromEntity(saved, whList.size());
    }

    // ==========================================
    // 2. QUẢN LÝ KHO HÀNG (WAREHOUSE)
    // ==========================================

    @Transactional(readOnly = true)
    public List<WarehouseDto> getAllWarehouses(Long branchId) {
        List<Warehouse> warehouses = (branchId != null) 
                ? warehouseRepository.findByBranchId(branchId) 
                : warehouseRepository.findAllByOrderByCreatedAtDesc();

        return warehouses.stream()
                .map(this::enrichWarehouseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long id) {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho với ID: " + id));
        return enrichWarehouseDto(w);
    }

    private WarehouseDto enrichWarehouseDto(Warehouse w) {
        WarehouseDto dto = WarehouseDto.fromEntity(w);
        List<WarehouseStock> stocks = warehouseStockRepository.findByWarehouseId(w.getId());
        dto.setTotalSkus(stocks.size());
        dto.setTotalPhysicalStock(stocks.stream().mapToInt(s -> s.getPhysicalStock() != null ? s.getPhysicalStock() : 0).sum());
        dto.setTotalAvailableStock(stocks.stream().mapToInt(s -> s.getAvailableStock() != null ? s.getAvailableStock() : 0).sum());
        return dto;
    }

    @Transactional
    public WarehouseDto createWarehouse(CreateWarehouseRequest req) {
        Branch branch = branchRepository.findById(req.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Chi nhánh ID " + req.getBranchId() + " không tồn tại"));

        String code = req.getCode();
        if (code == null || code.trim().isEmpty()) {
            code = "WH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else {
            code = code.trim().toUpperCase();
            if (warehouseRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Mã kho " + code + " đã tồn tại trong hệ thống");
            }
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setBranchId(branch.getId());
        warehouse.setBranchName(branch.getName());
        warehouse.setCode(code);
        warehouse.setName(req.getName().trim());
        warehouse.setWarehouseType(req.getWarehouseType() != null ? req.getWarehouseType() : WarehouseType.RETAIL_STORE);
        warehouse.setAddress(req.getAddress());
        warehouse.setActive(req.getActive() != null ? req.getActive() : true);

        Warehouse saved = warehouseRepository.save(warehouse);
        realtimeStateSyncService.publishEvent("/topic/warehouses", "CREATED", saved);
        return enrichWarehouseDto(saved);
    }

    @Transactional
    public WarehouseDto updateWarehouse(Long id, CreateWarehouseRequest req) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho với ID: " + id));

        if (req.getBranchId() != null && !req.getBranchId().equals(warehouse.getBranchId())) {
            Branch branch = branchRepository.findById(req.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Chi nhánh ID " + req.getBranchId() + " không tồn tại"));
            warehouse.setBranchId(branch.getId());
            warehouse.setBranchName(branch.getName());
        }

        warehouse.setName(req.getName().trim());
        if (req.getWarehouseType() != null) {
            warehouse.setWarehouseType(req.getWarehouseType());
        }
        warehouse.setAddress(req.getAddress());
        if (req.getActive() != null) {
            warehouse.setActive(req.getActive());
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        realtimeStateSyncService.publishEvent("/topic/warehouses", "UPDATED", saved);
        return enrichWarehouseDto(saved);
    }

    // ==========================================
    // 3. QUẢN LÝ TỒN KHO ĐA ĐIỂM (WAREHOUSE STOCK)
    // ==========================================

    @Transactional(readOnly = true)
    public List<WarehouseStockDto> getWarehouseStocks(Long warehouseId, Long branchId, String query) {
        List<WarehouseStock> list;
        if (warehouseId != null) {
            list = warehouseStockRepository.findByWarehouseId(warehouseId);
        } else if (branchId != null) {
            list = warehouseStockRepository.findByBranchId(branchId);
        } else {
            list = warehouseStockRepository.findAll();
        }

        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase();
            list = list.stream()
                    .filter(s -> (s.getProductName() != null && s.getProductName().toLowerCase().contains(q)) ||
                                 (s.getProductSku() != null && s.getProductSku().toLowerCase().contains(q)))
                    .toList();
        }

        return list.stream().map(WarehouseStockDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public MultiWarehouseStockDto getMultiWarehouseStock(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + productId));

        List<Warehouse> allWarehouses = warehouseRepository.findByActiveTrue();
        List<WarehouseStock> stocks = warehouseStockRepository.findByProductId(productId);
        Map<Long, WarehouseStock> stockMap = new HashMap<>();
        for (WarehouseStock s : stocks) {
            stockMap.put(s.getWarehouseId(), s);
        }

        List<MultiWarehouseStockDto.WarehouseStockBreakdownDto> breakdown = new ArrayList<>();
        int totalPhysical = 0;
        int totalAvailable = 0;
        int totalReserved = 0;
        int totalInTransit = 0;

        for (Warehouse w : allWarehouses) {
            WarehouseStock ws = stockMap.get(w.getId());
            int phys = ws != null && ws.getPhysicalStock() != null ? ws.getPhysicalStock() : 0;
            int res = ws != null && ws.getReservedStock() != null ? ws.getReservedStock() : 0;
            int transit = ws != null && ws.getInTransitStock() != null ? ws.getInTransitStock() : 0;
            int minAlert = ws != null && ws.getMinStockAlert() != null ? ws.getMinStockAlert() : 10;
            int avail = Math.max(0, phys - res);

            totalPhysical += phys;
            totalAvailable += avail;
            totalReserved += res;
            totalInTransit += transit;

            breakdown.add(MultiWarehouseStockDto.WarehouseStockBreakdownDto.builder()
                    .warehouseId(w.getId())
                    .warehouseCode(w.getCode())
                    .warehouseName(w.getName())
                    .warehouseType(w.getWarehouseType().name())
                    .branchId(w.getBranchId())
                    .branchName(w.getBranchName())
                    .physicalStock(phys)
                    .availableStock(avail)
                    .reservedStock(res)
                    .inTransitStock(transit)
                    .minStockAlert(minAlert)
                    .isLowStock(avail <= minAlert)
                    .build());
        }

        return MultiWarehouseStockDto.builder()
                .productId(p.getId())
                .productSku(p.getSku())
                .productName(p.getName())
                .baseUnit(p.getBaseUnit())
                .price(p.getPrice())
                .totalPhysicalStock(totalPhysical)
                .totalAvailableStock(totalAvailable)
                .totalReservedStock(totalReserved)
                .totalInTransitStock(totalInTransit)
                .breakdown(breakdown)
                .build();
    }

    @Transactional
    public WarehouseStockDto adjustStock(AdjustStockRequest req, String username) {
        Warehouse w = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kho ID: " + req.getWarehouseId()));
        Product p = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + req.getProductId()));

        WarehouseStock ws = warehouseStockRepository.findByWarehouseIdAndProductId(w.getId(), p.getId())
                .orElseGet(() -> {
                    WarehouseStock newStock = new WarehouseStock();
                    newStock.setWarehouseId(w.getId());
                    newStock.setWarehouseName(w.getName());
                    newStock.setBranchId(w.getBranchId());
                    newStock.setProductId(p.getId());
                    newStock.setProductSku(p.getSku());
                    newStock.setProductName(p.getName());
                    newStock.setPhysicalStock(0);
                    newStock.setReservedStock(0);
                    newStock.setInTransitStock(0);
                    newStock.setMinStockAlert(10);
                    return newStock;
                });

        int oldPhysical = ws.getPhysicalStock() != null ? ws.getPhysicalStock() : 0;
        int delta = req.getNewPhysicalStock() - oldPhysical;
        ws.setPhysicalStock(req.getNewPhysicalStock());
        if (req.getMinStockAlert() != null) {
            ws.setMinStockAlert(req.getMinStockAlert());
        }
        ws.setAvailableStock(Math.max(0, ws.getPhysicalStock() - ws.getReservedStock()));
        WarehouseStock saved = warehouseStockRepository.save(ws);

        // Đồng bộ tổng tồn sang Product để tương thích ngược 100%
        syncProductTotalStock(p.getId());

        // Ghi nhật ký thẻ kho
        InventoryLog logEntry = new InventoryLog();
        logEntry.setProductId(p.getId());
        logEntry.setProductSku(p.getSku());
        logEntry.setProductName(p.getName());
        logEntry.setMovementType(InventoryMovementType.MANUAL_ADJUST);
        logEntry.setQuantityDelta(delta);
        logEntry.setStockBefore(oldPhysical);
        logEntry.setStockAfter(saved.getPhysicalStock());
        logEntry.setReferenceCode("ADJUST-WH-" + w.getCode());
        logEntry.setNote(req.getReason() != null ? req.getReason() : "Điều chỉnh kiểm kê tại " + w.getName());
        logEntry.setActorUsername(username != null ? username : "system");
        inventoryLogRepository.save(logEntry);

        realtimeStateSyncService.publishEvent("/topic/warehouse-stocks", "ADJUSTED", saved);
        return WarehouseStockDto.fromEntity(saved);
    }

    @Transactional
    public void syncProductTotalStock(Long productId) {
        Integer totalAvail = warehouseStockRepository.sumAvailableStockByProductId(productId);
        Integer totalPhys = warehouseStockRepository.sumPhysicalStockByProductId(productId);
        productRepository.findById(productId).ifPresent(p -> {
            p.setStock(totalAvail != null ? totalAvail : 0);
            p.setPhysicalStock(totalPhys != null ? totalPhys : 0);
            productRepository.save(p);
        });
    }
}
