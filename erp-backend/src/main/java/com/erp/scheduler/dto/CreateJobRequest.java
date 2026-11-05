package com.erp.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateJobRequest {
    @NotBlank(message = "Mã jobCode không được để trống")
    private String jobCode;

    @NotBlank(message = "Tên jobName không được để trống")
    private String jobName;

    private String jobGroup;
    private String description;

    @NotBlank(message = "Biểu thức cronExpression không được để trống")
    private String cronExpression;

    @NotBlank(message = "Loại jobType không được để trống")
    private String jobType;

    private String paramsJson;
}

