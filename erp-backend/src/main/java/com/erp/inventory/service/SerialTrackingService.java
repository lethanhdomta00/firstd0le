package com.erp.inventory.service;

import com.erp.branch.model.Warehouse;
import com.erp.branch.repository.WarehouseRepository;
import com.erp.inventory.dto.CreateSerialRequest;
import com.erp.inventory.dto.ProductSerialDto;
import com.erp.inventory.dto.UpdateSerialStatusRequest;
import com.erp.inventory.model.ProductSerial;
import com.erp.inventory.model.SerialStatus;
import com.erp.inventory.repository.ProductSerialRepository;
import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import com.erp.service.RealtimeStateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialTrackingService {

    private final ProductSerialRepository serialRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final RealtimeStateSyncService realtimeStateSyncService;

    @Transactional(readOnly = true)
    public List<ProductSerialDto> getAllSerials(Long productId, Long warehouseId, SerialStatus status, String query) {
        List<ProductSerial> list;
        if (query != null && !query.trim().isEmpty()) {
            list = serialRepository.searchSerials(query.trim());
        } else if (productId != null) {
            list = serialRepository.findByProductId(productId);
        } else if (warehouseId != null) {
            list = serialRepository.findByWarehouseId(warehouseId);
        } else if (status != null) {
            list = serialRepository.findByStatus(status);
        } else {
            list = serialRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(ProductSerialDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public ProductSerialDto getSerialById(Long id) {
        ProductSerial s = serialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã Serial ID: " + id));
        return ProductSerialDto.fromEntity(s);
    }

    @Transactional(readOnly = true)
    public ProductSerialDto getSerialByNumber(String serialNumber) {
        ProductSerial s = serialRepository.findBySerialNumber(serialNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã Serial / IMEI: " + serialNumber));
        return ProductSerialDto.fromEntity(s);
    }

    @Transactional
    public ProductSerialDto createSerial(CreateSerialRequest req) {
        String sn = req.getSerialNumber().trim().toUpperCase();
        if (serialRepository.existsBySerialNumber(sn)) {
            throw new IllegalArgumentException("Mã Serial / IMEI [" + sn + "] đã tồn tại trong hệ thống");
        }

        Product p = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm ID: " + req.getProductId()));

        Warehouse w = null;
        if (req.getWarehouseId() != null) {
            w = warehouseRepository.findById(req.getWarehouseId()).orElse(null);
        }

        ProductSerial serial = new ProductSerial();
        serial.setSerialNumber(sn);
        serial.setProductId(p.getId());
        serial.setProductSku(p.getSku());
        serial.setProductName(p.getName());
        if (w != null) {
            serial.setWarehouseId(w.getId());
            serial.setWarehouseName(w.getName());
        }
        serial.setBatchId(req.getBatchId());
        serial.setStatus(SerialStatus.IN_STOCK);
        serial.setWarrantyMonths(req.getWarrantyMonths() != null ? req.getWarrantyMonths() : 12);
        serial.setNotes(req.getNotes());

        ProductSerial saved = serialRepository.save(serial);
        realtimeStateSyncService.publishEvent("/topic/serials", "CREATED", saved);
        return ProductSerialDto.fromEntity(saved);
    }

    @Transactional
    public List<ProductSerialDto> batchCreateSerials(List<CreateSerialRequest> requests) {
        List<ProductSerialDto> results = new ArrayList<>();
        for (CreateSerialRequest req : requests) {
            results.add(createSerial(req));
        }
        return results;
    }

    /**
     * Cập nhật trạng thái vòng đời Serial / IMEI & Kích hoạt bảo hành điện tử
     */
    @Transactional
    public ProductSerialDto updateSerialStatus(Long id, UpdateSerialStatusRequest req) {
        ProductSerial s = serialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã Serial ID: " + id));

        s.setStatus(req.getNewStatus());

        if (req.getCustomerId() != null) s.setCustomerId(req.getCustomerId());
        if (req.getCustomerName() != null) s.setCustomerName(req.getCustomerName());
        if (req.getCustomerPhone() != null) s.setCustomerPhone(req.getCustomerPhone());
        if (req.getOrderId() != null) s.setOrderId(req.getOrderId());
        if (req.getOrderCode() != null) s.setOrderCode(req.getOrderCode());

        // Nếu xuất kho bán cho khách (DELIVERED hoặc ACTIVE_WARRANTY) -> Kích hoạt bảo hành
        if (req.getNewStatus() == SerialStatus.DELIVERED || req.getNewStatus() == SerialStatus.ACTIVE_WARRANTY) {
            LocalDate startDate = req.getWarrantyStartDate() != null ? req.getWarrantyStartDate() : LocalDate.now();
            int months = req.getWarrantyMonths() != null ? req.getWarrantyMonths() : (s.getWarrantyMonths() != null ? s.getWarrantyMonths() : 12);
            s.setWarrantyStartDate(startDate);
            s.setWarrantyMonths(months);
            s.setWarrantyEndDate(startDate.plusMonths(months));
            if (req.getNewStatus() == SerialStatus.DELIVERED) {
                s.setStatus(SerialStatus.ACTIVE_WARRANTY);
            }
        }

        if (req.getNotes() != null) {
            s.setNotes(s.getNotes() != null ? s.getNotes() + " | " + req.getNotes() : req.getNotes());
        }

        ProductSerial saved = serialRepository.save(s);
        realtimeStateSyncService.publishEvent("/topic/serials", "UPDATED", saved);
        return ProductSerialDto.fromEntity(saved);
    }
}
