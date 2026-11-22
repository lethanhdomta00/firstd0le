package com.erp.procurement.service;

import com.erp.procurement.dto.CreateSupplierRequest;
import com.erp.procurement.dto.SupplierDto;
import com.erp.procurement.model.Supplier;
import com.erp.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public Page<SupplierDto> searchSuppliers(String keyword, String status, Pageable pageable) {
        return supplierRepository.searchSuppliers(keyword, status, pageable)
                .map(this::toDto);
    }

    public List<SupplierDto> getAllActiveSuppliers() {
        return supplierRepository.findAll().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .map(this::toDto)
                .toList();
    }

    public SupplierDto getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp với ID: " + id));
        return toDto(supplier);
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request) {
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            code = generateSupplierCode();
        } else {
            code = code.trim().toUpperCase();
            if (supplierRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Mã nhà cung cấp đã tồn tại: " + code);
            }
        }

        Supplier supplier = Supplier.builder()
                .code(code)
                .name(request.getName().trim())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone().trim())
                .email(request.getEmail())
                .address(request.getAddress())
                .taxCode(request.getTaxCode())
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : new BigDecimal("200000000.00"))
                .currentDebt(BigDecimal.ZERO)
                .paymentTermsDays(request.getPaymentTermsDays() != null ? request.getPaymentTermsDays() : 30)
                .status(request.getStatus() != null ? request.getStatus().toUpperCase() : "ACTIVE")
                .notes(request.getNotes())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Tạo mới nhà cung cấp: {} - {}", saved.getCode(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public SupplierDto updateSupplier(Long id, CreateSupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp với ID: " + id));

        supplier.setName(request.getName().trim());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone().trim());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setTaxCode(request.getTaxCode());
        if (request.getCreditLimit() != null) {
            supplier.setCreditLimit(request.getCreditLimit());
        }
        if (request.getPaymentTermsDays() != null) {
            supplier.setPaymentTermsDays(request.getPaymentTermsDays());
        }
        if (request.getStatus() != null) {
            supplier.setStatus(request.getStatus().toUpperCase());
        }
        supplier.setNotes(request.getNotes());

        Supplier saved = supplierRepository.save(supplier);
        log.info("Cập nhật nhà cung cấp: {} - {}", saved.getCode(), saved.getName());
        return toDto(saved);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp với ID: " + id));

        if (supplier.getCurrentDebt() != null && supplier.getCurrentDebt().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Không thể xóa nhà cung cấp đang còn dư nợ công nợ: " + supplier.getCurrentDebt() + " VNĐ!");
        }

        supplierRepository.delete(supplier);
        log.info("Đã xóa nhà cung cấp ID: {}", id);
    }

    public List<SupplierDto> getTopDebtors() {
        return supplierRepository.findTop10ByOrderByCurrentDebtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public String generateSupplierCode() {
        String prefix = "NCC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        int rand = new Random().nextInt(900) + 100;
        String code = prefix + "-" + rand;
        while (supplierRepository.existsByCode(code)) {
            rand = new Random().nextInt(900) + 100;
            code = prefix + "-" + rand;
        }
        return code;
    }

    public SupplierDto toDto(Supplier s) {
        return SupplierDto.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .taxCode(s.getTaxCode())
                .creditLimit(s.getCreditLimit())
                .currentDebt(s.getCurrentDebt())
                .paymentTermsDays(s.getPaymentTermsDays())
                .status(s.getStatus())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
