package com.erp.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String identifier; // username hoặc email
}
