package com.erp.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPointDto {
    private String date;             // Định dạng YYYY-MM-DD
    private String label;            // Nhãn hiển thị ngắn (vd: 12/11)
    private BigDecimal revenue;      // Doanh thu trong ngày
    private long orderCount;         // Số đơn tạo trong ngày
}
