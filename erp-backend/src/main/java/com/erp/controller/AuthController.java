package com.erp.controller;

import com.erp.dto.ForgotPasswordRequest;
import com.erp.dto.JwtAuthenticationResponse;
import com.erp.dto.LoginRequest;
import com.erp.dto.ResetPasswordRequest;
import com.erp.dto.UserProfileUpdateRequest;
import com.erp.dto.UserResponseDto;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import com.erp.security.JwtTokenProvider;
import com.erp.service.EmailService;
import com.erp.service.RolePermissionService;
import com.erp.service.TelegramService;
import com.erp.workflow.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final TelegramService telegramService;
    private final RolePermissionService rolePermissionService;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    @GetMapping("/version")
    public ResponseEntity<?> getVersion() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "version", "1.0.3-email-telegram-otp",
            "message", "Spring Boot CI/CD & Auth Service is working!"
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest servletRequest) {
        String ipAddress = servletRequest != null ? servletRequest.getRemoteAddr() : "127.0.0.1";
        String userAgent = servletRequest != null ? servletRequest.getHeader("User-Agent") : "Unknown";
        String attemptUsername = loginRequest.getUsername() != null ? loginRequest.getUsername() : "unknown";

        try {
            // Kiểm tra trạng thái tài khoản trước khi cấp token
            Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
            if (userOpt.isPresent() && "LOCKED".equalsIgnoreCase(userOpt.get().getStatus())) {
                auditLogService.logActionSafe(attemptUsername, userOpt.get().getRole() != null ? userOpt.get().getRole().name() : "USER",
                        "LOGIN_FAILED", "AUTH", userOpt.get().getId(), attemptUsername,
                        null, null, "Đăng nhập thất bại: Tài khoản đang bị khóa", ipAddress, userAgent);
                return ResponseEntity.status(403).body(Map.of("message", "Tài khoản của bạn đã bị khóa! Vui lòng liên hệ Quản trị viên."));
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            User user = userOpt.orElseGet(() -> userRepository.findByUsername(loginRequest.getUsername()).orElse(null));
            Set<String> permissions = Collections.emptySet();
            String userRole = "USER";
            if (user != null && user.getRole() != null) {
                userRole = user.getRole().name();
                permissions = rolePermissionService.getPermissionsForRole(userRole);
            }
            auditLogService.logActionSafe(attemptUsername, userRole,
                    "LOGIN_SUCCESS", "AUTH", user != null ? user.getId() : null, attemptUsername,
                    null, null, "Đăng nhập hệ thống thành công", ipAddress, userAgent);
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt, UserResponseDto.fromEntity(user, permissions)));
        } catch (AuthenticationException e) {
            auditLogService.logActionSafe(attemptUsername, "ANONYMOUS",
                    "LOGIN_FAILED", "AUTH", null, attemptUsername,
                    null, null, "Đăng nhập thất bại: Sai tên đăng nhập hoặc mật khẩu", ipAddress, userAgent);
            return ResponseEntity.status(401).body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không chính xác!"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập!"));
        }
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy thông tin tài khoản!"));
        }
        User user = userOpt.get();
        Set<String> permissions = Collections.emptySet();
        if (user.getRole() != null) {
            permissions = rolePermissionService.getPermissionsForRole(user.getRole().name());
        }
        return ResponseEntity.ok(UserResponseDto.fromEntity(user, permissions));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập!"));
        }
        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy thông tin tài khoản!"));
        }

        User user = userOpt.get();
        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getEmail() != null) user.setEmail(request.getEmail().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl().trim());
        if (request.getBio() != null) user.setBio(request.getBio().trim());

        User updated = userRepository.save(user);
        Set<String> permissions = Collections.emptySet();
        if (updated.getRole() != null) {
            permissions = rolePermissionService.getPermissionsForRole(updated.getRole().name());
        }
        return ResponseEntity.ok(UserResponseDto.fromEntity(updated, permissions));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody LoginRequest signUpRequest) {
        if(userRepository.findByUsername(signUpRequest.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên đăng nhập đã tồn tại trong hệ thống!"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đăng ký tài khoản thành công!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập tên đăng nhập hoặc email!"));
        }

        identifier = identifier.trim();
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy tài khoản với tên đăng nhập hoặc email này!"));
        }

        User user = userOpt.get();

        // Tạo mã OTP ngẫu nhiên 6 chữ số
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setResetPasswordToken(otp);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));

        String channel = request.getChannel() != null ? request.getChannel().trim().toUpperCase() : "EMAIL";
        String successMessage;
        String targetDisplay = "";

        if ("TELEGRAM".equals(channel)) {
            String chatId = request.getTelegramChatId();
            boolean sent = telegramService.sendOtpMessage(chatId, user.getUsername(), otp);
            if (sent) {
                successMessage = "Mã xác thực OTP đã được gửi tới tài khoản Telegram của bạn!";
                targetDisplay = "Telegram (Chat ID: " + (chatId != null ? chatId : "Mặc định") + ")";
            } else {
                successMessage = "Mã OTP đã được tạo! (Lưu ý: Bạn có thể nhập mã OTP hiển thị trên màn hình để tiếp tục).";
            }
        } else {
            // Mặc định gửi qua Gmail
            String targetEmail = request.getEmail();
            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    targetEmail = user.getEmail();
                } else if (identifier.contains("@")) {
                    targetEmail = identifier;
                }
            }

            if (targetEmail != null && !targetEmail.trim().isEmpty()) {
                targetEmail = targetEmail.trim();
                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    user.setEmail(targetEmail);
                }

                try {
                    emailService.sendOtpEmail(targetEmail, user.getUsername(), otp);
                    String maskedEmail = maskEmail(targetEmail);
                    successMessage = "Mã xác thực OTP đã được gửi tới email " + maskedEmail + "!";
                    targetDisplay = maskedEmail;
                } catch (Exception e) {
                    log.error("Lỗi khi gửi email OTP: {}", e.getMessage(), e);
                    successMessage = "Mã xác thực OTP đã được tạo thành công! Vui lòng kiểm tra hộp thư.";
                }
            } else {
                successMessage = "Mã xác thực OTP đã được tạo thành công!";
            }
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", successMessage,
                "channel", channel,
                "target", targetDisplay,
                "otp", otp,
                "username", user.getUsername(),
                "expiresInMinutes", 15
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        String otp = request.get("otp");
        if (otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập mã OTP!"));
        }

        Optional<User> userOpt = userRepository.findByResetPasswordToken(otp.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã xác thực OTP không chính xác!"));
        }

        User user = userOpt.get();
        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã OTP đã hết hạn! Vui lòng yêu cầu mã mới."));
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Mã OTP hợp lệ! Bạn có thể thiết lập mật khẩu mới ngay bây giờ."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã xác thực không được để trống!"));
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu mới phải có ít nhất 6 ký tự!"));
        }

        Optional<User> userOpt = userRepository.findByResetPasswordToken(token.trim());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã xác thực không hợp lệ hoặc đã qua sử dụng!"));
        }

        User user = userOpt.get();
        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã xác thực đã hết hạn! Vui lòng thực hiện lại."));
        }

        // Cập nhật mật khẩu mới và hủy token
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập ngay với mật khẩu mới."
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập!"));
        }

        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập mật khẩu hiện tại!"));
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu mới phải có tối thiểu 6 ký tự!"));
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy thông tin tài khoản!"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu hiện tại không chính xác!"));
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu tài khoản thành công!"));
    }

    @PostMapping("/test-telegram")
    public ResponseEntity<?> testTelegram(@RequestBody Map<String, String> request) {
        String chatId = request.get("chatId");
        String testMsg = "🌿 <b>[ERP Cloud Enterprise]</b>\n"
                + "Kết nối thành công tới Telegram Bot!\n"
                + "Thời gian: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")) + "\n"
                + "Kênh thông báo tự động đã sẵn sàng tiếp nhận sự kiện.";
        boolean sent = telegramService.sendMessage(chatId, testMsg);
        if (sent) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã gửi tin nhắn test tới Telegram thành công!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể gửi tin nhắn Telegram. Vui lòng kiểm tra lại Bot Token hoặc Chat ID!"));
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.charAt(0) + "***" + email.substring(atIndex - 1);
    }
}
