package com.erp.controller;

import com.erp.dto.RoleMatrixDto;
import com.erp.dto.UserPasswordResetRequest;
import com.erp.dto.UserResponseDto;
import com.erp.dto.UserUpdateRequest;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import com.erp.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('USER_VIEW', 'ROLE_ADMIN')") // Cần quyền USER_VIEW hoặc ADMIN để truy cập phân hệ
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolePermissionService rolePermissionService;

    // GET /api/users/roles-matrix -> Danh mục Ma trận Phân quyền RBAC Động
    @GetMapping("/roles-matrix")
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ROLE_ADMIN')")
    public ResponseEntity<List<RoleMatrixDto>> getRolesMatrix() {
        return ResponseEntity.ok(rolePermissionService.getRolesMatrix());
    }

    // GET /api/users/permissions -> Danh sách toàn bộ các quyền hạn có trong hệ thống
    @GetMapping("/permissions")
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ROLE_ADMIN')")
    public ResponseEntity<List<Map<String, String>>> getAllPermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllAvailablePermissions());
    }

    // PUT /api/users/roles-matrix -> Cập nhật Ma trận Phân quyền Động
    @PutMapping("/roles-matrix")
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ROLE_ADMIN')")
    public ResponseEntity<List<RoleMatrixDto>> updateRolesMatrix(@RequestBody Map<String, Set<String>> matrixUpdate) {
        List<RoleMatrixDto> updated = rolePermissionService.updateRolesMatrix(matrixUpdate);
        return ResponseEntity.ok(updated);
    }

    // POST /api/users/roles-matrix/reset -> Khôi phục Ma trận Phân quyền về mặc định
    @PostMapping("/roles-matrix/reset")
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ROLE_ADMIN')")
    public ResponseEntity<List<RoleMatrixDto>> resetRolesMatrix() {
        List<RoleMatrixDto> reset = rolePermissionService.resetToDefault();
        return ResponseEntity.ok(reset);
    }

    // PATCH /api/users/{id}/role -> Cập nhật nhanh vai trò cho tài khoản (1-Click Role Update)
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyAuthority('USER_ROLE', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateUserRoleQuick(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn vai trò hợp lệ!"));
        }

        Role newRole;
        try {
            newRole = Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vai trò không hợp lệ: " + roleStr));
        }

        return userRepository.findById(id)
                .map(user -> {
                    user.setRole(newRole);
                    User updated = userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "Đã cập nhật vai trò @" + user.getUsername() + " thành " + newRole.getTitle(),
                            "user", UserResponseDto.fromEntity(updated)
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/users -> Danh sách tài khoản có Phân trang, Tìm kiếm & Lọc theo Role/Status
    @GetMapping
    public Page<UserResponseDto> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Page<User> usersPage = userRepository.searchUsers(
                keyword != null ? keyword.trim() : null,
                role,
                status != null ? status.trim() : null,
                pageable
        );

        return usersPage.map(UserResponseDto::fromEntity);
    }

    // POST /api/users -> Tạo tài khoản mới
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER_CREATE', 'ROLE_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập không được để trống!"));
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu phải có tối thiểu 6 ký tự!"));
        }
        if (userRepository.findByUsername(user.getUsername().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập đã tồn tại trong hệ thống!"));
        }

        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        if (user.getStatus() == null || user.getStatus().trim().isEmpty()) {
            user.setStatus("ACTIVE");
        }

        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(UserResponseDto.fromEntity(savedUser));
    }

    // PUT /api/users/{id} -> Cập nhật thông tin tài khoản
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER_EDIT', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    if (request.getFullName() != null) {
                        user.setFullName(request.getFullName().trim());
                    }
                    if (request.getEmail() != null) {
                        user.setEmail(request.getEmail().trim());
                    }
                    if (request.getPhone() != null) {
                        user.setPhone(request.getPhone().trim());
                    }
                    if (request.getAvatarUrl() != null) {
                        user.setAvatarUrl(request.getAvatarUrl().trim());
                    }
                    if (request.getBio() != null) {
                        user.setBio(request.getBio().trim());
                    }
                    if (request.getRole() != null) {
                        user.setRole(request.getRole());
                    }
                    if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                        user.setStatus(request.getStatus().trim());
                    }
                    User updated = userRepository.save(user);
                    return ResponseEntity.ok(UserResponseDto.fromEntity(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/users/{id}/toggle-status -> Khóa / Mở khóa tài khoản
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyAuthority('USER_STATUS', 'ROLE_ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "";

        return userRepository.findById(id)
                .map(user -> {
                    if (user.getUsername().equalsIgnoreCase(currentUsername)) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Bạn không thể tự khóa tài khoản của chính mình!"));
                    }
                    String newStatus = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "LOCKED" : "ACTIVE";
                    user.setStatus(newStatus);
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of(
                            "message", "Đã chuyển trạng thái tài khoản thành: " + newStatus,
                            "status", newStatus
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/users/{id}/reset-password -> Đặt lại mật khẩu tài khoản (Chỉ dành cho ADMIN)
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody UserPasswordResetRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu mới phải có tối thiểu 6 ký tự!"));
        }

        return userRepository.findById(id)
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu cho tài khoản @" + user.getUsername() + " thành công!"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/users/{id} -> Xóa tài khoản (Chỉ dành cho ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth != null ? auth.getName() : "";

        return userRepository.findById(id)
                .map(user -> {
                    if (user.getUsername().equalsIgnoreCase(currentUsername)) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Bạn không thể tự xóa tài khoản đang đăng nhập!"));
                    }
                    userRepository.delete(user);
                    return ResponseEntity.ok(Map.of("message", "Đã xóa tài khoản thành công!"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
