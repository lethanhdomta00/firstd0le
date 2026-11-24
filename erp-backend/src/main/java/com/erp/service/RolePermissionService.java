package com.erp.service;

import com.erp.config.CacheConfig;
import com.erp.dto.RoleMatrixDto;
import com.erp.model.Role;
import com.erp.model.RolePermission;
import com.erp.repository.RolePermissionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    // Danh sách toàn bộ các mã quyền hạn hợp lệ trong hệ thống ERP Cloud
    public static final List<Map<String, String>> ALL_SYSTEM_PERMISSIONS = List.of(
            // Module 1: Quản lý Người dùng
            Map.of("code", "USER_VIEW", "name", "Xem danh sách nhân sự", "module", "Quản lý Tài khoản"),
            Map.of("code", "USER_CREATE", "name", "Tạo tài khoản mới", "module", "Quản lý Tài khoản"),
            Map.of("code", "USER_EDIT", "name", "Chỉnh sửa thông tin tài khoản", "module", "Quản lý Tài khoản"),
            Map.of("code", "USER_STATUS", "name", "Khóa / Mở khóa tài khoản", "module", "Quản lý Tài khoản"),
            Map.of("code", "USER_ROLE", "name", "Phân quyền & Gán vai trò", "module", "Quản lý Tài khoản"),

            // Module 2: Sản phẩm & Kho hàng
            Map.of("code", "PRODUCT_VIEW", "name", "Xem danh mục sản phẩm", "module", "Kho & Sản phẩm"),
            Map.of("code", "PRODUCT_CREATE", "name", "Thêm sản phẩm mới", "module", "Kho & Sản phẩm"),
            Map.of("code", "PRODUCT_EDIT", "name", "Sửa giá & thông tin sản phẩm", "module", "Kho & Sản phẩm"),
            Map.of("code", "PRODUCT_STOCK", "name", "Điều chỉnh xuất/nhập/tồn kho", "module", "Kho & Sản phẩm"),
            Map.of("code", "PRODUCT_DELETE", "name", "Xóa sản phẩm khỏi kho", "module", "Kho & Sản phẩm"),

            // Module 3: Đơn hàng & Cổng VietQR
            Map.of("code", "ORDER_VIEW", "name", "Xem danh sách đơn hàng", "module", "Đơn hàng & Thanh toán"),
            Map.of("code", "ORDER_CREATE", "name", "Tạo mới đơn hàng", "module", "Đơn hàng & Thanh toán"),
            Map.of("code", "ORDER_STATUS", "name", "Phê duyệt trạng thái đơn hàng", "module", "Đơn hàng & Thanh toán"),
            Map.of("code", "ORDER_PAY", "name", "Xác nhận thanh toán & VietQR", "module", "Đơn hàng & Thanh toán"),
            Map.of("code", "ORDER_EXPORT", "name", "Xuất hóa đơn & Báo cáo PDF/Excel", "module", "Đơn hàng & Thanh toán"),
            Map.of("code", "ORDER_CANCEL", "name", "Hủy đơn hàng & hoàn trả kho", "module", "Đơn hàng & Thanh toán"),

            // Module 4: Báo cáo & Phân tích BI
            Map.of("code", "ANALYTICS_VIEW", "name", "Xem báo cáo BI & Doanh thu", "module", "Báo cáo & Phân tích BI"),
            Map.of("code", "ANALYTICS_EXPORT", "name", "Xuất file phân tích kinh doanh", "module", "Báo cáo & Phân tích BI"),

            // Module 5: Tác vụ tự động & Cron Jobs
            Map.of("code", "JOB_VIEW", "name", "Xem danh sách Cron Jobs", "module", "Tác vụ tự động"),
            Map.of("code", "JOB_TRIGGER", "name", "Kích hoạt chạy ngay (Trigger Now)", "module", "Tác vụ tự động"),
            Map.of("code", "JOB_MANAGE", "name", "Tạo / Sửa / Xóa cấu hình Cron", "module", "Tác vụ tự động"),

            // Module 6: Trò chuyện & AI Copilot
            Map.of("code", "CHAT_USE", "name", "Trò chuyện nội bộ công ty", "module", "Giao tiếp & AI Copilot"),
            Map.of("code", "COPILOT_USE", "name", "Tương tác với trợ lý AI Copilot", "module", "Giao tiếp & AI Copilot"),

            // Module 7: Mua hàng & Nhà cung cấp SRM (Giai đoạn 3)
            Map.of("code", "PROCUREMENT_VIEW", "name", "Xem phân hệ mua hàng & NCC", "module", "Mua hàng & NCC (SRM)"),
            Map.of("code", "PROCUREMENT_MANAGE", "name", "Quản lý NCC & Duyệt đơn PO", "module", "Mua hàng & NCC (SRM)"),
            Map.of("code", "PROCUREMENT_RECEIVE", "name", "Nhập kho kiểm nhận GRN & Giá vốn", "module", "Mua hàng & NCC (SRM)")
    );

    @PostConstruct
    public void init() {
        if (rolePermissionRepository.count() == 0) {
            log.info("Khởi tạo bảng role_permissions lần đầu từ cấu hình mặc định...");
            resetToDefault();
        }
    }

    /**
     * Lấy toàn bộ Ma trận phân quyền động từ cơ sở dữ liệu
     */
    public List<RoleMatrixDto> getRolesMatrix() {
        List<RolePermission> allPermissions = rolePermissionRepository.findAll();
        Map<String, Set<String>> rolePermMap = allPermissions.stream()
                .collect(Collectors.groupingBy(
                        RolePermission::getRoleName,
                        Collectors.mapping(RolePermission::getPermissionCode, Collectors.toSet())
                ));

        List<RoleMatrixDto> dtoList = new ArrayList<>();
        for (Role role : Role.values()) {
            Set<String> perms = rolePermMap.getOrDefault(role.name(), new HashSet<>(getDefaultPermissionsForRole(role)));
            dtoList.add(RoleMatrixDto.builder()
                    .role(role.name())
                    .title(role.getTitle())
                    .description(role.getDescription())
                    .permissions(perms)
                    .build());
        }
        return dtoList;
    }

    /**
     * Cập nhật Ma trận phân quyền mới do Admin tùy chỉnh trên giao diện
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ROLE_PERMISSIONS, allEntries = true)
    public List<RoleMatrixDto> updateRolesMatrix(Map<String, Set<String>> matrixUpdate) {
        log.info("Cập nhật ma trận phân quyền động cho {} vai trò", matrixUpdate.size());

        for (Map.Entry<String, Set<String>> entry : matrixUpdate.entrySet()) {
            String roleName = entry.getKey();
            Set<String> permissions = entry.getValue();

            // ADMIN luôn bảo lưu toàn bộ quyền hạn để tránh lock out
            if ("ADMIN".equalsIgnoreCase(roleName)) {
                permissions = ALL_SYSTEM_PERMISSIONS.stream()
                        .map(p -> p.get("code"))
                        .collect(Collectors.toSet());
            }

            rolePermissionRepository.deleteByRoleName(roleName);
            rolePermissionRepository.flush();

            if (permissions != null && !permissions.isEmpty()) {
                List<RolePermission> toSave = permissions.stream()
                        .map(pCode -> RolePermission.builder()
                                .roleName(roleName)
                                .permissionCode(pCode)
                                .build())
                        .toList();
                rolePermissionRepository.saveAll(toSave);
            }
        }

        return getRolesMatrix();
    }

    /**
     * Khôi phục toàn bộ Ma trận phân quyền về chuẩn mặc định
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_ROLE_PERMISSIONS, allEntries = true)
    public List<RoleMatrixDto> resetToDefault() {
        rolePermissionRepository.deleteAll();

        List<RolePermission> initialList = new ArrayList<>();
        for (Role role : Role.values()) {
            Set<String> defaults = getDefaultPermissionsForRole(role);
            for (String p : defaults) {
                initialList.add(RolePermission.builder()
                        .roleName(role.name())
                        .permissionCode(p)
                        .build());
            }
        }
        rolePermissionRepository.saveAll(initialList);
        log.info("Đã khôi phục ma trận phân quyền về giá trị chuẩn mặc định ({} quyền)", initialList.size());
        return getRolesMatrix();
    }

    public List<Map<String, String>> getAllAvailablePermissions() {
        return ALL_SYSTEM_PERMISSIONS;
    }

    /**
     * Lấy danh sách mã quyền hạn động của một vai trò cụ thể từ cơ sở dữ liệu
     */
    @Cacheable(value = CacheConfig.CACHE_ROLE_PERMISSIONS, key = "#roleName != null ? #roleName.toUpperCase() : 'EMPTY'")
    public Set<String> getPermissionsForRole(String roleName) {
        if (roleName == null) return Collections.emptySet();

        // ADMIN luôn có toàn quyền bảo vệ hệ thống
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            return ALL_SYSTEM_PERMISSIONS.stream()
                    .map(p -> p.get("code"))
                    .collect(Collectors.toSet());
        }

        List<RolePermission> perms = rolePermissionRepository.findByRoleName(roleName.toUpperCase());
        if (perms != null && !perms.isEmpty()) {
            return perms.stream()
                    .map(RolePermission::getPermissionCode)
                    .collect(Collectors.toSet());
        }

        // Fallback về quyền mặc định của vai trò
        try {
            Role role = Role.valueOf(roleName.toUpperCase());
            return getDefaultPermissionsForRole(role);
        } catch (IllegalArgumentException e) {
            return Collections.emptySet();
        }
    }

    private Set<String> getDefaultPermissionsForRole(Role role) {
        Set<String> set = new HashSet<>();
        switch (role) {
            case ADMIN -> ALL_SYSTEM_PERMISSIONS.forEach(p -> set.add(p.get("code")));
            case MANAGER -> set.addAll(List.of(
                    "USER_VIEW", "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_EDIT", "PRODUCT_STOCK",
                    "ORDER_VIEW", "ORDER_CREATE", "ORDER_STATUS", "ORDER_PAY", "ORDER_EXPORT", "ORDER_CANCEL",
                    "ANALYTICS_VIEW", "ANALYTICS_EXPORT", "JOB_VIEW", "CHAT_USE", "COPILOT_USE",
                    "PROCUREMENT_VIEW", "PROCUREMENT_MANAGE", "PROCUREMENT_RECEIVE"
            ));
            case ACCOUNTANT -> set.addAll(List.of(
                    "ORDER_VIEW", "ORDER_PAY", "ORDER_EXPORT",
                    "ANALYTICS_VIEW", "ANALYTICS_EXPORT", "CHAT_USE", "COPILOT_USE",
                    "PROCUREMENT_VIEW", "PROCUREMENT_MANAGE"
            ));
            case WAREHOUSE -> set.addAll(List.of(
                    "PRODUCT_VIEW", "PRODUCT_CREATE", "PRODUCT_EDIT", "PRODUCT_STOCK",
                    "ORDER_VIEW", "CHAT_USE", "COPILOT_USE",
                    "PROCUREMENT_VIEW", "PROCUREMENT_RECEIVE"
            ));
            case SALES -> set.addAll(List.of(
                    "PRODUCT_VIEW", "ORDER_VIEW", "ORDER_CREATE", "ORDER_STATUS", "ORDER_EXPORT",
                    "CHAT_USE", "COPILOT_USE"
            ));
            case USER -> set.addAll(List.of(
                    "PRODUCT_VIEW", "ORDER_VIEW", "ORDER_CREATE", "CHAT_USE", "COPILOT_USE"
            ));
        }
        return set;
    }
}
