package com.erp.dto;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio;
}

