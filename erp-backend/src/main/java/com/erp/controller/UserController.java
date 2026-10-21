package com.erp.controller;

import com.erp.model.User;
import com.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // GET /api/users -> Lấy danh sách (giống Route::get trong Laravel)
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // POST /api/users -> Tạo tài khoản mới (Admin tạo)
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username đã tồn tại!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(com.erp.model.Role.USER); // Default role
        }
        return ResponseEntity.ok(userRepository.save(user));
    }

    // DELETE /api/users/{id} -> Xóa tài khoản
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

