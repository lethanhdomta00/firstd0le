package com.erp.controller;

import com.erp.dto.ForgotPasswordRequest;
import com.erp.dto.JwtAuthenticationResponse;
import com.erp.dto.LoginRequest;
import com.erp.dto.ResetPasswordRequest;
import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import com.erp.security.JwtTokenProvider;
import com.erp.service.EmailService;
import com.erp.service.TelegramService;
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
import java.util.Map;
import java.util.Optional;

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
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Tên đăng nhập hoặc mật khẩu không chính xác!"));
        }
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
                } else {
                    targetEmail = "dolemta090899@gmail.com";
                }
            }
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
                log.error("Lỗi khi gửi email OTP qua Gmail: {}", e.getMessage(), e);
                successMessage = "Mã xác thực OTP đã được tạo thành công! Vui lòng kiểm tra hộp thư.";
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

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.charAt(0) + "***" + email.substring(atIndex - 1);
    }
}
