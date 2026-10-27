package com.erp.dto;

import com.erp.model.Role;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio;
    private Role role;
    private String status;
}
