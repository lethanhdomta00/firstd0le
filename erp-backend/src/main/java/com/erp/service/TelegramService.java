package com.erp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Slf4j
public class TelegramService {

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.default-chat-id:}")
    private String defaultChatId;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean sendOtpMessage(String targetChatId, String username, String otpCode) {
        String chatId = (targetChatId != null && !targetChatId.trim().isEmpty()) ? targetChatId.trim() : defaultChatId;

        if (botToken == null || botToken.trim().isEmpty()) {
            log.warn("Telegram Bot Token chưa được cấu hình! Bỏ qua gửi Telegram.");
            return false;
        }

        if (chatId == null || chatId.trim().isEmpty()) {
            log.warn("Telegram Chat ID chưa được cung cấp! Bỏ qua gửi Telegram.");
            return false;
        }

        try {
            String text = "<b>[ERP Cloud System] Security Alert</b>\n\n"
                    + "Hello <b>" + escapeHtml(username) + "</b>,\n\n"
                    + "A password reset request was initiated for your account: <code>" + escapeHtml(username) + "</code>.\n\n"
                    + "Your One-Time Password (OTP) verification code is:\n"
                    + "👉 <b><code>" + otpCode + "</code></b>\n\n"
                    + "• <b>Expires in:</b> 15 minutes\n"
                    + "• <b>Security Notice:</b> Never share this OTP with anyone. If you did not request this, please contact your system administrator.";

            String jsonPayload = "{\"chat_id\":\"" + escapeJson(chatId) + "\","
                    + "\"text\":\"" + escapeJson(text) + "\","
                    + "\"parse_mode\":\"HTML\"}";

            String url = "https://api.telegram.org/bot" + botToken.trim() + "/sendMessage";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                log.info("Đã gửi tin nhắn OTP qua Telegram thành công tới Chat ID: {}", chatId);
                return true;
            } else {
                log.error("Gửi Telegram thất bại. Status: {}, Body: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối tới Telegram API: {}", e.getMessage());
            return false;
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
}

