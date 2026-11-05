package com.erp.scheduler.dto;

import lombok.Data;

@Data
public class UpdateJobRequest {
    private String jobName;
    private String jobGroup;
    private String description;
    private String cronExpression;
    private String paramsJson;
}

