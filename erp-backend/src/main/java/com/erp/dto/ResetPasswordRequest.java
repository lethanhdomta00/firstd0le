package com.erp.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token; // 6-digit OTP hoặc token
    private String newPassword;
}
