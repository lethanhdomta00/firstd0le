package com.erp.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseOrderMessageRequest {
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String text;
}
