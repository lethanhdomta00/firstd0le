package com.erp.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String identifier;     // username hoặc email
    private String channel;        // "EMAIL" hoặc "TELEGRAM" (mặc định "EMAIL")
    private String email;          // địa chỉ email nhận OTP
    private String telegramChatId; // Chat ID nhận tin nhắn Telegram
}
